package com.bakery.oven;

import com.bakery.baking.BakingRecipes;
import com.bakery.food.BakeryFoods;
import com.bakery.food.Freshness;
import com.bakery.network.BakeResultPayload;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryBlocks;
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
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The Grand Brick Oven, worked entirely by hand in the world.
 * <ol>
 *   <li>Click the oven's front to swing the iron door down.</li>
 *   <li>Click again to slide the wire rack out over the door.</li>
 *   <li>Set raw bakes on the rack where you click. A cookie tray fits in one square of the 3x3 rack, a loaf takes
 *       two, a baguette three, a pie or a cake a 2x2 block.</li>
 *   <li>Click the front to slide the rack back in, sneak-click to shut the door. It only bakes with the rack in,
 *       and heat pours out of an open door.</li>
 *   <li>Cookies, sugar cookies and donuts must be flipped halfway: when they start to sizzle, pull the rack out
 *       and click them. Forget, and one side burns.</li>
 *   <li>When the bell rings, pull the rack out and click a bake to lift it off.</li>
 * </ol>
 * The heat wobbles around the dial's target (Spatula on the oven body changes it); the share of time the needle
 * spent near the bake's ideal heat becomes the "heat stars". Final stars = average of prep, timing and heat.
 */
public class GrandOvenBlockEntity extends BlockEntity implements Multiblock.MachineContents {
	public static final int COLS = 3, ROWS = 3;
	public static final int MAX_FUEL = 24000;
	public static final int HEAT_ZONE = 12;
	public static final int ACTION_FAN = 0;
	public static final int ACTION_VENT = 1;
	/** Flipping works between these doneness values; the sizzle cue starts at the first. */
	public static final float FLIP_FROM = 0.35f, FLIP_UNTIL = 0.8f;
	/** Ticks the door and rack take to move, for the renderer. */
	public static final int MOVE_TICKS = 8;

	/** Rack geometry in model pixels for a north-facing oven (the controller block is 0..16; the front is -z). */
	public static final float RACK_X = 2, RACK_W = 12, RACK_D = 14, RACK_Y = 3.2f;
	public static final float RACK_FRONT_IN = -13, RACK_FRONT_OUT = -31;

	/** One bake sitting on the rack. */
	public static final class Bake {
		public ItemStack stack;
		public int col, row, w, h;
		int progress, bakedTicks, zoneTicks;
		boolean dinged, cued;
		public boolean flipped;
		public long flippedAt = -100;

		Bake(ItemStack stack, int col, int row, int w, int h) {
			this.stack = stack;
			this.col = col;
			this.row = row;
			this.w = w;
			this.h = h;
		}

		boolean covers(int c, int r) {
			return c >= col && c < col + w && r >= row && r < row + h;
		}

		NbtCompound write(RegistryWrapper.WrapperLookup registries) {
			NbtCompound nbt = new NbtCompound();
			nbt.put("item", stack.encode(registries));
			nbt.putIntArray("cell", new int[]{col, row, w, h});
			nbt.putInt("progress", progress);
			nbt.putInt("baked", bakedTicks);
			nbt.putInt("zone", zoneTicks);
			nbt.putBoolean("dinged", dinged);
			nbt.putBoolean("cued", cued);
			nbt.putBoolean("flipped", flipped);
			nbt.putLong("flippedAt", flippedAt);
			return nbt;
		}

		static Bake read(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
			int[] cell = nbt.getIntArray("cell");
			if (cell.length < 4) cell = new int[]{0, 0, 1, 1};
			Bake bake = new Bake(ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("item")), cell[0], cell[1], cell[2], cell[3]);
			bake.progress = nbt.getInt("progress");
			bake.bakedTicks = nbt.getInt("baked");
			bake.zoneTicks = nbt.getInt("zone");
			bake.dinged = nbt.getBoolean("dinged");
			bake.cued = nbt.getBoolean("cued");
			bake.flipped = nbt.getBoolean("flipped");
			bake.flippedAt = nbt.getLong("flippedAt");
			return bake;
		}
	}

	private final List<Bake> bakes = new ArrayList<>();
	private int fuel;
	private int fanned;
	private float heat;
	private float wobble;
	private boolean doorOpen;
	private boolean rackOut;
	private long doorMovedAt = -100;
	private long rackMovedAt = -100;
	private OvenTemperature temperature = OvenTemperature.MEDIUM;

	public GrandOvenBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.GRAND_OVEN, pos, state);
	}

	// ------------------------------------------------------------------ queries

	public List<Bake> getBakes() {
		return bakes;
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

	public boolean isDoorOpen() {
		return doorOpen;
	}

	public boolean isRackOut() {
		return rackOut;
	}

	/** 0 = shut, 1 = wide open, eased, for the renderer. */
	public float doorAmount(float tickDelta) {
		return moveAmount(doorOpen, doorMovedAt, tickDelta);
	}

	/** 0 = rack in, 1 = rack out, eased, for the renderer. */
	public float rackAmount(float tickDelta) {
		return moveAmount(rackOut, rackMovedAt, tickDelta);
	}

	private float moveAmount(boolean state, long movedAt, float tickDelta) {
		float t = world == null ? 1 : MathHelper.clamp((world.getTime() - movedAt + tickDelta) / MOVE_TICKS, 0, 1);
		float eased = t * t * (3 - 2 * t);
		return state ? eased : 1 - eased;
	}

	public static int bakeTime(ItemStack stack) {
		BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(stack);
		return shape == null ? 0 : shape.bakeTicks();
	}

	public static int idealHeat(ItemStack stack) {
		BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(stack);
		return shape == null ? 60 : shape.idealHeat();
	}

	/** How many rack squares a bake takes: {width across, depth}. */
	public static int[] footprint(ItemStack stack) {
		BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(stack);
		if (shape == null) return new int[]{1, 1};
		return switch (shape) {
			case LOAF -> new int[]{1, 2};
			case BAGUETTE -> new int[]{1, 3};
			case CAKE, PIE -> new int[]{2, 2};
			default -> new int[]{1, 1};
		};
	}

	/** Bakes that must be turned over halfway through. */
	public static boolean needsFlip(ItemStack stack) {
		BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(stack);
		return shape == BakingRecipes.Shape.COOKIES || shape == BakingRecipes.Shape.SUGAR_COOKIE || shape == BakingRecipes.Shape.DONUT;
	}

	/** 0 = raw, 1 = exactly done, above 1 = getting darker. */
	public float doneness(Bake bake, float partialTicks) {
		int time = bakeTime(bake.stack);
		if (time == 0) return 0;
		float extra = isBaking(bake) ? partialTicks * temperature.speed() * 100 : 0;
		return (bake.progress + extra) / 100f / time;
	}

	/** How much of this bake's time the heat was in its zone, 0-1. */
	public static float heatRatio(Bake bake) {
		return bake.bakedTicks == 0 ? 1 : bake.zoneTicks / (float) bake.bakedTicks;
	}

	public boolean isBaking(Bake bake) {
		return !rackOut && fuel > 0 && bakeTime(bake.stack) > 0;
	}

	/** True while this bake is sizzling and wants turning over. */
	public boolean wantsFlip(Bake bake) {
		if (bake.flipped || !needsFlip(bake.stack)) return false;
		float d = doneness(bake, 0);
		return d >= FLIP_FROM && d <= FLIP_UNTIL;
	}

	public static int heatStars(float ratio) {
		if (ratio >= 0.85f) return 5;
		if (ratio >= 0.65f) return 4;
		if (ratio >= 0.45f) return 3;
		if (ratio >= 0.25f) return 2;
		return 1;
	}

	/** Fuel logs to show in the firebox (0-4). */
	public int logs() {
		return fuel <= 0 ? 0 : 1 + Math.min(3, fuel * 3 / MAX_FUEL);
	}

	public Bake bakeAt(int col, int row) {
		for (Bake bake : bakes) {
			if (bake.covers(col, row)) return bake;
		}
		return null;
	}

	/** The world position of the block the rack slides out into. */
	public BlockPos rackPos() {
		return pos.offset(getCachedState().get(GrandOvenBlock.FACING), 2);
	}

	/** World position (x, z) and rack slide amount -> rack cell {col, row}, or null if off the rack. */
	public int[] cellAt(Vec3d hit) {
		Direction facing = getCachedState().get(GrandOvenBlock.FACING);
		// Undo the blockstate rotation: into model space of a north-facing oven.
		double lx = hit.x - pos.getX() - 0.5, lz = hit.z - pos.getZ() - 0.5;
		double a = Math.toRadians(facing.asRotation() + 180);
		double mx = lx * Math.cos(a) + lz * Math.sin(a), mz = -lx * Math.sin(a) + lz * Math.cos(a);
		float px = (float) (mx * 16 + 8), pz = (float) (mz * 16 + 8);
		float front = rackOut ? RACK_FRONT_OUT : RACK_FRONT_IN;
		int col = MathHelper.floor((px - RACK_X) / (RACK_W / COLS));
		int row = MathHelper.floor((pz - front) / (RACK_D / ROWS));
		if (col < -1 || col > COLS || row < -1 || row > ROWS) return null;
		return new int[]{MathHelper.clamp(col, 0, COLS - 1), MathHelper.clamp(row, 0, ROWS - 1)};
	}

	/** Finds a free spot for a bake of this size that covers the clicked square if possible. */
	private int[] fit(int w, int h, int col, int row) {
		List<int[]> options = new ArrayList<>();
		for (int c = col - w + 1; c <= col; c++) {
			for (int r = row - h + 1; r <= row; r++) options.add(new int[]{c, r});
		}
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) options.add(new int[]{c, r});
		}
		for (int[] o : options) {
			if (o[0] < 0 || o[1] < 0 || o[0] + w > COLS || o[1] + h > ROWS) continue;
			boolean free = true;
			for (int c = o[0]; c < o[0] + w && free; c++) {
				for (int r = o[1]; r < o[1] + h && free; r++) {
					if (bakeAt(c, r) != null) free = false;
				}
			}
			if (free) return o;
		}
		return null;
	}

	// ------------------------------------------------------------------ interaction

	/** Every click on any part of the oven (or its rack) ends up here. */
	public void interact(PlayerEntity player, Hand hand, ItemStack stack) {
		interact(player, hand, stack, pos.toCenterPos(), false);
	}

	public void interact(PlayerEntity player, Hand hand, ItemStack stack, Vec3d hit) {
		interact(player, hand, stack, hit, false);
	}

	/**
	 * Feed the fire, pump the bellows, turn the dial with the spatula. Work the door and rack with an empty hand
	 * on the oven; on the rack itself, set bakes down, flip them and lift them off.
	 */
	public void interact(PlayerEntity player, Hand hand, ItemStack stack, Vec3d hit, boolean onRack) {
		if (world == null || world.isClient) {
			return;
		}
		if (stack.isOf(BakeryItems.BELLOWS)) {
			act((ServerPlayerEntity) player, ACTION_FAN);
			stack.damage(1, player, hand == Hand.MAIN_HAND ? net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND);
			return;
		}
		Integer burn = FuelRegistry.INSTANCE.get(stack.getItem());
		if (burn != null && burn > 0 && !onRack) {
			feed(player, stack, burn);
			return;
		}
		if (onRack && rackOut) {
			rackClick(player, stack, hit);
			return;
		}
		if (stack.isOf(BakeryItems.SPATULA)) {
			temperature = temperature.next();
			player.sendMessage(Text.translatable("message.bakery.oven.temperature", temperature.displayName()).withColor(temperature.color() & 0xFFFFFF), true);
			world.playSound(null, pos, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.BLOCKS, 1f, temperature == OvenTemperature.HIGH ? 1.4f : 0.9f);
			sync();
			return;
		}
		if (BakingRecipes.shapeForRaw(stack) != null) {
			player.sendMessage(Text.translatable(doorOpen ? "message.bakery.oven.pull_rack" : "message.bakery.oven.open_door").formatted(Formatting.GRAY), true);
			return;
		}
		if (!stack.isEmpty()) {
			return;
		}
		// Empty hand on the oven: door and rack.
		if (player.isSneaking()) {
			if (rackOut) slideRack(false);
			if (doorOpen) setDoor(false);
			return;
		}
		if (!doorOpen) {
			setDoor(true);
		} else if (!rackOut) {
			if (!slideRack(true)) {
				player.sendMessage(Text.translatable("message.bakery.oven.rack_blocked").formatted(Formatting.GOLD), true);
			}
		} else {
			slideRack(false);
		}
	}

	private void feed(PlayerEntity player, ItemStack stack, int burn) {
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
	}

	/** A click on the pulled-out rack. */
	private void rackClick(PlayerEntity player, ItemStack stack, Vec3d hit) {
		int[] cell = cellAt(hit);
		if (cell == null) return;
		Bake bake = bakeAt(cell[0], cell[1]);
		if (BakingRecipes.shapeForRaw(stack) != null) {
			int[] size = footprint(stack);
			int[] at = fit(size[0], size[1], cell[0], cell[1]);
			if (at == null) {
				player.sendMessage(Text.translatable("message.bakery.oven.no_room").formatted(Formatting.GOLD), true);
				world.playSound(null, rackPos(), SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.BLOCKS, 0.5f, 1.4f);
				return;
			}
			bakes.add(new Bake(stack.copyWithCount(1), at[0], at[1], size[0], size[1]));
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, rackPos(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.BLOCKS, 0.7f, 1.3f);
			world.playSound(null, rackPos(), SoundEvents.BLOCK_MUD_PLACE, SoundCategory.BLOCKS, 0.4f, 1.5f);
			if (fuel <= 0) {
				player.sendMessage(Text.translatable("message.bakery.oven.needs_fuel").formatted(Formatting.GOLD), true);
			}
			sync();
			return;
		}
		if (bake == null) {
			if (stack.isEmpty()) slideRack(false);
			return;
		}
		if (!stack.isEmpty() && !stack.isOf(BakeryItems.SPATULA)) {
			return;
		}
		// Empty hand or spatula on a bake: flip it if it is sizzling, otherwise lift it off when it is ready.
		if (needsFlip(bake.stack) && !bake.flipped && bakeTime(bake.stack) > 0) {
			float d = doneness(bake, 0);
			if (d < FLIP_FROM) {
				player.sendMessage(Text.translatable("message.bakery.oven.flip_soon").formatted(Formatting.GRAY), true);
				world.playSound(null, rackPos(), SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.4f, 1.6f);
				return;
			}
			if (d <= FLIP_UNTIL) {
				bake.flipped = true;
				bake.flippedAt = world.getTime();
				world.playSound(null, rackPos(), SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM, SoundCategory.BLOCKS, 0.8f, 1.2f);
				world.playSound(null, rackPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.15f, 2f);
				if (world instanceof ServerWorld server) {
					Vec3d at = bakeCenter(bake);
					server.spawnParticles(ParticleTypes.WHITE_SMOKE, at.x, at.y + 0.15, at.z, 4, 0.05, 0.02, 0.05, 0.01);
				}
				sync();
				return;
			}
		}
		if (bakeTime(bake.stack) > 0 && doneness(bake, 0) < 0.5f) {
			player.sendMessage(Text.translatable("message.bakery.oven.still_raw").formatted(Formatting.GRAY), true);
			world.playSound(null, rackPos(), SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.5f, 1.4f);
			return;
		}
		takeOut(bake, player);
	}

	/** Where a bake sits in the world right now (for particles). */
	public Vec3d bakeCenter(Bake bake) {
		Direction facing = getCachedState().get(GrandOvenBlock.FACING);
		float front = MathHelper.lerp(rackAmount(0), RACK_FRONT_IN, RACK_FRONT_OUT);
		float px = RACK_X + (bake.col + bake.w / 2f) * RACK_W / COLS - 8, pz = front + (bake.row + bake.h / 2f) * RACK_D / ROWS - 8;
		double a = Math.toRadians(facing.asRotation() + 180);
		double wx = px * Math.cos(a) - pz * Math.sin(a), wz = px * Math.sin(a) + pz * Math.cos(a);
		return new Vec3d(pos.getX() + 0.5 + wx / 16, pos.getY() + RACK_Y / 16, pos.getZ() + 0.5 + wz / 16);
	}

	private void setDoor(boolean open) {
		if (doorOpen == open || world == null) return;
		doorOpen = open;
		doorMovedAt = world.getTime();
		world.playSound(null, pos.offset(getCachedState().get(GrandOvenBlock.FACING)), open ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
				SoundCategory.BLOCKS, 0.7f, open ? 0.9f : 1.1f);
		if (open && fuel > 0 && world instanceof ServerWorld server) {
			Direction facing = getCachedState().get(GrandOvenBlock.FACING);
			BlockPos mouth = pos.offset(facing);
			server.spawnParticles(ParticleTypes.CLOUD, mouth.getX() + 0.5 + facing.getOffsetX() * 0.5, mouth.getY() + 0.6, mouth.getZ() + 0.5 + facing.getOffsetZ() * 0.5,
					5, 0.2, 0.1, 0.2, 0.02);
		}
		sync();
	}

	/** Slides the rack out (into the block in front of the oven, which must be free) or back in. */
	private boolean slideRack(boolean out) {
		if (world == null || rackOut == out) return true;
		Direction facing = getCachedState().get(GrandOvenBlock.FACING);
		BlockPos front = rackPos();
		if (out) {
			if (!world.getBlockState(front).isReplaceable()) return false;
			world.setBlockState(front, BakeryBlocks.OVEN_RACK.getDefaultState().with(OvenRackBlock.FACING, facing), Block.NOTIFY_ALL);
		} else if (world.getBlockState(front).isOf(BakeryBlocks.OVEN_RACK)) {
			rackOut = false;
			world.removeBlock(front, false);
		}
		rackOut = out;
		rackMovedAt = world.getTime();
		world.playSound(null, pos.offset(facing), SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.BLOCKS, 1f, out ? 0.8f : 1f);
		world.playSound(null, pos.offset(facing), SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.2f, 1.8f);
		sync();
		return true;
	}

	/** The rack block in front was broken or replaced: the rack goes back in. */
	public void rackRemoved() {
		if (!rackOut || world == null) return;
		rackOut = false;
		rackMovedAt = world.getTime();
		sync();
	}

	/** Bellows and the old damper. */
	public void act(ServerPlayerEntity player, int action) {
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
					server.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5 + facing.getOffsetX() * 0.3, pos.getY() + 0.25, pos.getZ() + 0.5 + facing.getOffsetZ() * 0.3,
							10, 0.3, 0.05, 0.3, 0.03);
				}
			}
			case ACTION_VENT -> {
				heat = Math.max(0, heat - 9);
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.2f, 1.8f);
			}
			default -> {
			}
		}
		sync();
	}

	private void takeOut(Bake bake, PlayerEntity player) {
		ItemStack item = bake.stack;
		BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(item);
		if (shape == null) {
			player.getInventory().offerOrDrop(item);
			remove(bake);
			return;
		}
		float doneness = doneness(bake, 0);
		int timing = temperature.bakeStars(doneness);
		boolean scorched = needsFlip(item) && !bake.flipped;
		if (scorched) timing = Math.max(1, timing - 2);
		int heatStars = heatStars(heatRatio(bake));
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
		result.set(BakeryComponents.HOMEMADE, item.get(BakeryComponents.HOMEMADE));
		if (stars == 5 && player instanceof ServerPlayerEntity baker) {
			com.bakery.sweet.Stickers.award(baker, com.bakery.sweet.Sticker.FIVE_STARS);
		}
		Freshness.stampFresh(result, world.getTime());
		ItemStack shown = result.copy();
		Vec3d at = bakeCenter(bake);
		player.getInventory().offerOrDrop(result);
		if (scorched) {
			player.sendMessage(Text.translatable("message.bakery.oven.one_side_burnt").formatted(Formatting.GOLD), true);
		}

		world.playSound(null, pos, stars == 5 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : stars >= 3 ? SoundEvents.ENTITY_PLAYER_LEVELUP : SoundEvents.ENTITY_ITEM_PICKUP,
				SoundCategory.BLOCKS, stars == 5 ? 0.5f : 0.6f, stars == 5 ? 1.2f : 1.4f);
		if (world instanceof ServerWorld server) {
			server.spawnParticles(stars >= 4 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE, at.x, at.y + 0.3, at.z, 8, 0.15, 0.15, 0.15, 0);
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, new BakeResultPayload(BlockPos.ofFloored(at), shown, stars, prep, timing, heatStars));
			com.bakery.review.BakeryReputation.discover(serverPlayer, shape.name().toLowerCase(), BakeryFoods.flavor(shown), shown.getName());
		}
		remove(bake);
	}

	private void remove(Bake bake) {
		bakes.remove(bake);
		sync();
	}

	@Override
	public void dropContents() {
		if (world == null) return;
		for (Bake bake : bakes) {
			ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, bake.stack);
		}
		bakes.clear();
		if (rackOut && world.getBlockState(rackPos()).isOf(BakeryBlocks.OVEN_RACK)) {
			rackOut = false;
			world.removeBlock(rackPos(), false);
		}
	}

	// ------------------------------------------------------------------ ticking

	public static void tick(World world, BlockPos pos, BlockState state, GrandOvenBlockEntity oven) {
		boolean anyBaking = false;
		for (Bake bake : oven.bakes) {
			if (oven.isBaking(bake)) {
				anyBaking = true;
				bake.progress += Math.round(oven.temperature.speed() * 100);
			}
		}
		if (anyBaking) {
			oven.fuel = Math.max(0, oven.fuel - oven.temperature.fuelPerTick());
		}

		// The fire: drifts toward the dial's target, with a slow wander so it needs tending. An open door lets the heat out.
		if (oven.fuel > 0) {
			oven.wobble += (world.random.nextFloat() - 0.5f) * 0.6f;
			oven.wobble = MathHelper.clamp(oven.wobble * 0.985f, -14, 14);
			float target = oven.temperature.targetHeat() + oven.wobble - (oven.doorOpen ? 30 : 0);
			oven.heat += (target - oven.heat) * (oven.doorOpen ? 0.035f : 0.02f);
		} else {
			oven.heat = Math.max(0, oven.heat - 0.25f);
		}
		if (oven.fanned > 0) oven.fanned--;
		if (world.isClient) {
			return;
		}

		boolean changed = false;
		for (int i = 0; i < oven.bakes.size(); i++) {
			Bake bake = oven.bakes.get(i);
			if (!oven.isBaking(bake)) continue;
			bake.bakedTicks++;
			if (Math.abs(oven.heat - idealHeat(bake.stack)) <= HEAT_ZONE) {
				bake.zoneTicks++;
			}
			float doneness = oven.doneness(bake, 0);
			if (!bake.cued && needsFlip(bake.stack) && !bake.flipped && doneness >= FLIP_FROM) {
				bake.cued = true;
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.35f, 1.9f);
				world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 0.8f, 1.6f);
				changed = true;
			}
			if (!bake.dinged && doneness >= oven.temperature.perfectStart()) {
				bake.dinged = true;
				world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1.2f, 1.4f + (i % 3) * 0.2f);
				changed = true;
			}
			if (doneness >= BakingRecipes.BURNT) {
				bake.stack = new ItemStack(BakeryItems.BURNT_PASTRY);
				bake.progress = 0;
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.8f, 0.8f);
				changed = true;
			}
		}
		// A rack whose block went missing (pushed by a piston, replaced...) is back inside.
		if (oven.rackOut && world.getTime() % 20 == 0 && !world.getBlockState(oven.rackPos()).isOf(BakeryBlocks.OVEN_RACK)) {
			oven.rackRemoved();
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

	// ------------------------------------------------------------------ saving

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		NbtList list = new NbtList();
		for (Bake bake : bakes) {
			if (!bake.stack.isEmpty()) list.add(bake.write(registries));
		}
		nbt.put("bakes", list);
		nbt.putInt("fuel", fuel);
		nbt.putFloat("heat", heat);
		nbt.putFloat("wobble", wobble);
		nbt.putInt("temperature", temperature.ordinal());
		nbt.putBoolean("doorOpen", doorOpen);
		nbt.putBoolean("rackOut", rackOut);
		nbt.putLong("doorMovedAt", doorMovedAt);
		nbt.putLong("rackMovedAt", rackMovedAt);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		bakes.clear();
		NbtList list = nbt.getList("bakes", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			Bake bake = Bake.read(list.getCompound(i), registries);
			if (!bake.stack.isEmpty()) bakes.add(bake);
		}
		// Ovens from before the rack: their three baking spots go on the front row.
		for (int i = 0; i < 3; i++) {
			if (nbt.contains("slot" + i)) {
				ItemStack old = ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("slot" + i));
				if (!old.isEmpty() && bakeAt(i, 0) == null) {
					Bake bake = new Bake(old, i, 0, 1, 1);
					bake.progress = nbt.getInt("progress" + i);
					bakes.add(bake);
				}
			}
		}
		fuel = nbt.getInt("fuel");
		heat = nbt.getFloat("heat");
		wobble = nbt.getFloat("wobble");
		temperature = OvenTemperature.values()[Math.clamp(nbt.getInt("temperature"), 0, OvenTemperature.values().length - 1)];
		doorOpen = nbt.getBoolean("doorOpen");
		rackOut = nbt.getBoolean("rackOut");
		doorMovedAt = nbt.contains("doorMovedAt") ? nbt.getLong("doorMovedAt") : -100;
		rackMovedAt = nbt.contains("rackMovedAt") ? nbt.getLong("rackMovedAt") : -100;
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
