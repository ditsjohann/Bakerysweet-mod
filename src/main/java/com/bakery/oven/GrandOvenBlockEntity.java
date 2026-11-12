package com.bakery.oven;

import com.bakery.baking.BakingRecipes;
import com.bakery.food.BakeryFoods;
import com.bakery.food.Freshness;
import com.bakery.network.BakeResultPayload;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Three baking spots, a fuel store, a temperature dial and a living fire.
 * The heat wobbles around the dial's target; every bake has an ideal heat, and the share of time the needle spent
 * near it becomes the "heat stars". Fan the fire or open the door from the Bake screen to keep it there.
 * Final stars = average of prep (rolling), timing (when you peeled it out) and heat.
 */
public class GrandOvenBlockEntity extends BlockEntity implements Multiblock.MachineContents {
	public static final int SLOTS = 3;
	public static final int MAX_FUEL = 24000;
	public static final int HEAT_ZONE = 12;
	public static final int ACTION_FAN = 0;
	public static final int ACTION_VENT = 1;
	public static final int ACTION_PEEL = 2;

	private final ItemStack[] slots = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
	private final int[] progress = new int[SLOTS];
	private final int[] bakedTicks = new int[SLOTS];
	private final int[] zoneTicks = new int[SLOTS];
	private final boolean[] dinged = new boolean[SLOTS];
	private int fuel;
	private int fanned;
	private int vented;
	private float heat;
	private float wobble;
	private OvenTemperature temperature = OvenTemperature.MEDIUM;

	public GrandOvenBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.GRAND_OVEN, pos, state);
	}

	public ItemStack getSlot(int i) {
		return slots[i];
	}

	public int getFuel() {
		return fuel;
	}

	public float getHeat() {
		return heat;
	}

	public OvenTemperature getTemperature() {
		return temperature;
	}

	public static int bakeTime(ItemStack stack) {
		BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(stack);
		return shape == null ? 0 : shape.bakeTicks();
	}

	public static int idealHeat(ItemStack stack) {
		BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(stack);
		return shape == null ? 60 : shape.idealHeat();
	}

	/** 0 = raw, 1 = exactly done, above 1 = getting darker. */
	public float doneness(int i, float partialTicks) {
		int time = bakeTime(slots[i]);
		if (time == 0) return 0;
		float extra = isBaking(i) ? partialTicks * temperature.speed() * 100 : 0;
		return (progress[i] + extra) / 100f / time;
	}

	/** How much of this bake's time the heat was in its zone, 0-1. */
	public float heatRatio(int i) {
		return bakedTicks[i] == 0 ? 1 : zoneTicks[i] / (float) bakedTicks[i];
	}

	public boolean isBaking(int i) {
		return !slots[i].isEmpty() && fuel > 0 && bakeTime(slots[i]) > 0;
	}

	public static int heatStars(float ratio) {
		if (ratio >= 0.85f) return 5;
		if (ratio >= 0.65f) return 4;
		if (ratio >= 0.45f) return 3;
		if (ratio >= 0.25f) return 2;
		return 1;
	}

	private static final float[] SIDE = {-0.3f, 0f, 0.3f};

	/** Where baking spot {@code i} sits, relative to the controller block's corner (shared with the renderer). */
	public static net.minecraft.util.math.Vec3d slotOffset(Direction facing, int i) {
		double angle = Math.toRadians(180 - facing.asRotation());
		double x = SIDE[i], z = -0.2;
		return new net.minecraft.util.math.Vec3d(0.5 + x * Math.cos(angle) + z * Math.sin(angle), 0.03, 0.5 - x * Math.sin(angle) + z * Math.cos(angle));
	}

	/** The baking spot closest to where the player clicked. */
	private int slotNear(net.minecraft.util.math.Vec3d hit, boolean wantFilled) {
		Direction facing = getCachedState().get(GrandOvenBlock.FACING);
		int best = -1;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < SLOTS; i++) {
			if (slots[i].isEmpty() == wantFilled) continue;
			net.minecraft.util.math.Vec3d spot = slotOffset(facing, i).add(pos.getX(), pos.getY(), pos.getZ());
			// Only the sideways position matters: the player clicks the oven's front.
			Direction side = facing.rotateYClockwise();
			double d = Math.abs((hit.x - spot.x) * side.getOffsetX() + (hit.z - spot.z) * side.getOffsetZ());
			if (d < bestDistance) {
				bestDistance = d;
				best = i;
			}
		}
		return best;
	}

	/** Every click on any part of the oven ends up here. */
	public void interact(PlayerEntity player, Hand hand, ItemStack stack) {
		interact(player, hand, stack, pos.toCenterPos());
	}

	/**
	 * Every click on any part of the oven ends up here. Everything happens right at the oven: feed the fire,
	 * pump the bellows to heat it up, sneak-click to open the damper and cool it, and click a bake to peel it out.
	 */
	public void interact(PlayerEntity player, Hand hand, ItemStack stack, net.minecraft.util.math.Vec3d hit) {
		if (world == null || world.isClient) {
			return;
		}
		if (stack.isOf(BakeryItems.BELLOWS)) {
			act((ServerPlayerEntity) player, ACTION_FAN, 0);
			stack.damage(1, player, hand == Hand.MAIN_HAND ? net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND);
			return;
		}
		if (stack.isOf(BakeryItems.SPATULA)) {
			temperature = temperature.next();
			player.sendMessage(Text.translatable("message.bakery.oven.temperature", temperature.displayName()).withColor(temperature.color() & 0xFFFFFF), true);
			world.playSound(null, pos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1f, temperature == OvenTemperature.HIGH ? 1.4f : 0.9f);
			sync();
			return;
		}
		Integer burn = FuelRegistry.INSTANCE.get(stack.getItem());
		if (burn != null && burn > 0) {
			if (fuel >= MAX_FUEL - 200) {
				player.sendMessage(Text.translatable("message.bakery.oven.full_fuel"), true);
				return;
			}
			fuel = Math.min(MAX_FUEL, fuel + burn);
			ItemStack remainder = stack.getItem().getRecipeRemainder() != null ? new ItemStack(stack.getItem().getRecipeRemainder()) : ItemStack.EMPTY;
			stack.decrementUnlessCreative(1, player);
			if (!remainder.isEmpty()) {
				player.getInventory().offerOrDrop(remainder);
			}
			world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.6f, 1.2f);
			sync();
			return;
		}
		if (BakingRecipes.shapeForRaw(stack) != null) {
			int near = slotNear(hit, false);
			for (int k = -1; k < SLOTS; k++) {
				int i = k < 0 ? near : k;
				if (i >= 0 && slots[i].isEmpty()) {
					slots[i] = stack.copyWithCount(1);
					progress[i] = 0;
					bakedTicks[i] = 0;
					zoneTicks[i] = 0;
					dinged[i] = false;
					stack.decrementUnlessCreative(1, player);
					world.playSound(null, pos, SoundEvents.BLOCK_FURNACE_FIRE_CRACKLE, SoundCategory.BLOCKS, 1f, 1f);
					if (fuel <= 0) {
						player.sendMessage(Text.translatable("message.bakery.oven.needs_fuel").formatted(Formatting.GOLD), true);
					}
					sync();
					return;
				}
			}
			player.sendMessage(Text.translatable("message.bakery.oven.busy"), true);
			return;
		}
		if (!stack.isEmpty()) {
			return;
		}
		if (player.isSneaking()) {
			act((ServerPlayerEntity) player, ACTION_VENT, 0);
			player.sendMessage(Text.translatable("message.bakery.oven.vent"), true);
			return;
		}
		int slot = slotNear(hit, true);
		if (slot < 0) {
			player.sendMessage(Text.translatable(fuel > 0 ? "message.bakery.oven.empty" : "message.bakery.oven.needs_fuel").formatted(Formatting.GRAY), true);
		} else if (bakeTime(slots[slot]) > 0 && doneness(slot, 0) < 0.5f) {
			player.sendMessage(Text.translatable("message.bakery.oven.still_raw").formatted(Formatting.GRAY), true);
			world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.5f, 1.4f);
		} else {
			takeOut(slot, player);
		}
	}

	/** An action from the Bake screen. */
	public void act(ServerPlayerEntity player, int action, int slot) {
		if (world == null || player.squaredDistanceTo(pos.toCenterPos()) > 100) {
			return;
		}
		switch (action) {
			case ACTION_FAN -> {
				if (fuel <= 0) {
					player.sendMessage(Text.translatable("message.bakery.oven.needs_fuel").formatted(Formatting.GOLD), true);
					return;
				}
				heat = Math.min(100, heat + 7);
				fuel = Math.max(0, fuel - 40);
				fanned = 20;
				world.playSound(null, pos, SoundEvents.ENTITY_HORSE_BREATHE, SoundCategory.BLOCKS, 0.6f, 1.8f);
				world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 0.3f, 1.6f);
				if (world instanceof ServerWorld server) {
					Direction facing = getCachedState().get(GrandOvenBlock.FACING);
					BlockPos mouth = pos.offset(facing);
					server.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5 + facing.getOffsetX() * 0.5, pos.getY() + 0.25, pos.getZ() + 0.5 + facing.getOffsetZ() * 0.5,
							10, 0.3, 0.05, 0.3, 0.03);
					server.spawnParticles(ParticleTypes.LAVA, mouth.getX() + 0.5, pos.getY() + 0.3, mouth.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0);
				}
			}
			case ACTION_VENT -> {
				heat = Math.max(0, heat - 9);
				vented = 30;
				world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.5f, 1.5f);
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.2f, 1.8f);
				if (world instanceof ServerWorld server) {
					BlockPos chimney = pos.offset(getCachedState().get(GrandOvenBlock.FACING).getOpposite()).up();
					server.spawnParticles(ParticleTypes.CLOUD, chimney.getX() + 0.5, chimney.getY() + 1.1, chimney.getZ() + 0.5, 8, 0.15, 0.2, 0.15, 0.02);
				}
			}
			case ACTION_PEEL -> {
				if (slot >= 0 && slot < SLOTS && !slots[slot].isEmpty()) {
					if (bakeTime(slots[slot]) > 0 && doneness(slot, 0) < 0.5f) {
						player.sendMessage(Text.translatable("message.bakery.oven.still_raw").formatted(Formatting.GRAY), true);
					} else {
						takeOut(slot, player);
					}
				}
			}
			default -> {
			}
		}
		sync();
	}

	/** Fuel logs to show in the firebox (0-4). */
	public int logs() {
		return fuel <= 0 ? 0 : 1 + Math.min(3, fuel * 3 / MAX_FUEL);
	}

	private void takeOut(int i, PlayerEntity player) {
		ItemStack item = slots[i];
		BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(item);
		if (shape == null) {
			player.getInventory().offerOrDrop(item);
			clear(i);
			return;
		}
		float doneness = doneness(i, 0);
		int timing = temperature.bakeStars(doneness);
		int heatStars = heatStars(heatRatio(i));
		int prep = item.getOrDefault(BakeryComponents.QUALITY, BakeryFoods.DEFAULT_QUALITY);
		int stars = Math.clamp(Math.round((prep + timing + heatStars) / 3f), 1, 5);

		ItemStack result = new ItemStack(shape.baked(), shape.count());
		if (item.contains(BakeryComponents.FLAVOR)) {
			result.set(BakeryComponents.FLAVOR, item.get(BakeryComponents.FLAVOR));
		}
		if (item.contains(BakeryComponents.COOKIE_SHAPE)) {
			result.set(BakeryComponents.COOKIE_SHAPE, item.get(BakeryComponents.COOKIE_SHAPE));
		}
		result.set(BakeryComponents.QUALITY, stars);
		if (stars == 5 && player instanceof ServerPlayerEntity baker) {
			com.bakery.sweet.Stickers.award(baker, com.bakery.sweet.Sticker.FIVE_STARS);
		}
		Freshness.stampFresh(result, world.getTime());
		ItemStack shown = result.copy();
		player.getInventory().offerOrDrop(result);

		world.playSound(null, pos, stars == 5 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : stars >= 3 ? SoundEvents.ENTITY_PLAYER_LEVELUP : SoundEvents.ENTITY_ITEM_PICKUP,
				SoundCategory.BLOCKS, stars == 5 ? 0.5f : 0.6f, stars == 5 ? 1.2f : 1.4f);
		if (world instanceof ServerWorld server) {
			Direction facing = getCachedState().get(GrandOvenBlock.FACING);
			BlockPos mouth = pos.offset(facing);
			server.spawnParticles(stars >= 4 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE,
					mouth.getX() + 0.5, mouth.getY() + 0.6, mouth.getZ() + 0.5, 10, 0.4, 0.3, 0.4, 0);
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, new BakeResultPayload(pos.offset(getCachedState().get(GrandOvenBlock.FACING)), shown, stars, prep, timing, heatStars));
			com.bakery.review.BakeryReputation.discover(serverPlayer, shape.name().toLowerCase(), BakeryFoods.flavor(shown), shown.getName());
		}
		clear(i);
	}

	private void clear(int i) {
		slots[i] = ItemStack.EMPTY;
		progress[i] = 0;
		bakedTicks[i] = 0;
		zoneTicks[i] = 0;
		dinged[i] = false;
		sync();
	}

	@Override
	public void dropContents() {
		if (world == null) return;
		for (int i = 0; i < SLOTS; i++) {
			if (!slots[i].isEmpty()) {
				ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, slots[i]);
				slots[i] = ItemStack.EMPTY;
			}
		}
	}

	public static void tick(World world, BlockPos pos, BlockState state, GrandOvenBlockEntity oven) {
		boolean anyBaking = false;
		for (int i = 0; i < SLOTS; i++) {
			if (oven.isBaking(i)) {
				anyBaking = true;
				oven.progress[i] += Math.round(oven.temperature.speed() * 100);
			}
		}
		if (anyBaking) {
			oven.fuel = Math.max(0, oven.fuel - oven.temperature.fuelPerTick());
		}

		// The fire: drifts toward the dial's target, with a slow wander so it needs tending.
		if (oven.fuel > 0) {
			oven.wobble += (world.random.nextFloat() - 0.5f) * 0.6f;
			oven.wobble = MathHelper.clamp(oven.wobble * 0.985f, -14, 14);
			float target = oven.temperature.targetHeat() + oven.wobble;
			oven.heat += (target - oven.heat) * 0.02f;
		} else {
			oven.heat = Math.max(0, oven.heat - 0.25f);
		}
		if (world.isClient) {
			return;
		}

		boolean changed = false;
		for (int i = 0; i < SLOTS; i++) {
			if (!oven.isBaking(i)) continue;
			oven.bakedTicks[i]++;
			if (Math.abs(oven.heat - idealHeat(oven.slots[i])) <= HEAT_ZONE) {
				oven.zoneTicks[i]++;
			}
			float doneness = oven.doneness(i, 0);
			if (!oven.dinged[i] && doneness >= oven.temperature.perfectStart()) {
				oven.dinged[i] = true;
				world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1.2f, 1.4f + i * 0.2f);
				changed = true;
			}
			if (doneness >= BakingRecipes.BURNT) {
				oven.slots[i] = new ItemStack(BakeryItems.BURNT_PASTRY);
				oven.progress[i] = 0;
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.8f, 0.8f);
				changed = true;
			}
		}
		boolean lit = oven.fuel > 0;
		if (state.get(GrandOvenBlock.LIT) != lit) {
			world.setBlockState(pos, state.with(GrandOvenBlock.LIT, lit), Block.NOTIFY_ALL);
		}
		if (changed || (lit && world.getTime() % 10 == 0)) {
			oven.sync();
		}
	}

	private void sync() {
		markDirty();
		if (world != null) {
			world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		for (int i = 0; i < SLOTS; i++) {
			if (!slots[i].isEmpty()) {
				nbt.put("slot" + i, slots[i].encode(registries));
			}
			nbt.putInt("progress" + i, progress[i]);
			nbt.putInt("baked" + i, bakedTicks[i]);
			nbt.putInt("zone" + i, zoneTicks[i]);
			nbt.putBoolean("dinged" + i, dinged[i]);
		}
		nbt.putInt("fuel", fuel);
		nbt.putFloat("heat", heat);
		nbt.putFloat("wobble", wobble);
		nbt.putInt("temperature", temperature.ordinal());
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		for (int i = 0; i < SLOTS; i++) {
			slots[i] = nbt.contains("slot" + i) ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("slot" + i)) : ItemStack.EMPTY;
			progress[i] = nbt.getInt("progress" + i);
			bakedTicks[i] = nbt.getInt("baked" + i);
			zoneTicks[i] = nbt.getInt("zone" + i);
			dinged[i] = nbt.getBoolean("dinged" + i);
		}
		fuel = nbt.getInt("fuel");
		heat = nbt.getFloat("heat");
		wobble = nbt.getFloat("wobble");
		temperature = OvenTemperature.values()[Math.clamp(nbt.getInt("temperature"), 0, OvenTemperature.values().length - 1)];
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createNbt(registries);
	}
}
