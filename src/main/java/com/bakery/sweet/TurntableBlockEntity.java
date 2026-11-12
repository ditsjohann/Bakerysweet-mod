package com.bakery.sweet;

import com.bakery.food.BakeryFoods;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Cake Turntable. Everything happens on the cake itself: pipe icing exactly where you click, set toppers
 * down, stack another cake on top as a new tier, press a mould to reshape it, and give it a spin (empty hand)
 * to reach the other side. Sneak to scrape icing or lift a topper back off; sneak with an empty hand to take
 * the finished cake. The cuteness of the design turns into stars when the cake comes off.
 */
public class TurntableBlockEntity extends BlockEntity {
	/** Height of the turntable plate, in pixels. */
	public static final float PLATE = 3;
	private ItemStack cake = ItemStack.EMPTY;
	/** Stars the cake had before it was decorated. */
	private int baseQuality = 3;
	/** How far the plate is turned, degrees. */
	private int angle;
	private int lastAngle;
	private long spunAt;
	/** Where each player last piped a line, so the next click continues it. */
	private final Map<UUID, float[]> lineEnds = new HashMap<>();

	public TurntableBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.TURNTABLE, pos, state);
	}

	public ItemStack getCake() {
		return cake;
	}

	public int getAngle() {
		return angle;
	}

	/** The plate's turn for rendering, easing from the previous angle after a spin. */
	public float renderAngle(float tickDelta) {
		if (world == null) return angle;
		float t = MathHelper.clamp((world.getTime() - spunAt + tickDelta) / 8f, 0, 1);
		float eased = t * t * (3 - 2 * t);
		return MathHelper.lerp(eased, lastAngle, lastAngle + MathHelper.wrapDegrees(angle - lastAngle));
	}

	public CakeDesign design() {
		return cake.getOrDefault(BakeryComponents.CAKE_DESIGN, CakeDesign.PLAIN);
	}

	public void setCake(ItemStack cake) {
		this.cake = cake;
		if (!cake.isEmpty()) {
			CakeDesign design = cake.get(BakeryComponents.CAKE_DESIGN);
			int q = BakeryFoods.quality(cake);
			baseQuality = design == null ? q : Math.max(1, q - design.starBonus());
		}
		sync();
	}

	private void setDesign(CakeDesign design) {
		cake.set(BakeryComponents.CAKE_DESIGN, design);
		sync();
	}

	/** Takes the cake off, with its decoration turned into stars. */
	public ItemStack takeCake(PlayerEntity player) {
		ItemStack out = cake.copy();
		CakeDesign design = out.get(BakeryComponents.CAKE_DESIGN);
		if (design != null) {
			out.set(BakeryComponents.QUALITY, Math.min(5, baseQuality + design.starBonus()));
			if (player instanceof ServerPlayerEntity serverPlayer && design.cuteness() >= 3) Stickers.award(serverPlayer, Sticker.CAKE_ARTIST);
		}
		cake = ItemStack.EMPTY;
		lineEnds.clear();
		sync();
		return out;
	}

	public void spin(int degrees) {
		lastAngle = angle;
		angle = Math.floorMod(angle + degrees, 360);
		spunAt = world == null ? 0 : world.getTime();
		lineEnds.clear();
		sync();
	}

	/** World hit position -> cake space (pixels from the cake's bottom centre), undoing the plate's turn. */
	public float[] toCake(Vec3d hit) {
		float wx = (float) (hit.x - pos.getX() - 0.5) * 16, wy = (float) (hit.y - pos.getY()) * 16 - PLATE, wz = (float) (hit.z - pos.getZ() - 0.5) * 16;
		double a = Math.toRadians(angle);
		float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
		return new float[]{wx * cos - wz * sin, wy, wx * sin + wz * cos};
	}

	// ------------------------------------------------------------------ decorating

	/** A click with something in hand on the cake (which face was clicked guessed from the height). */
	public boolean use(ServerPlayerEntity player, Hand hand, ItemStack stack, Vec3d hit) {
		float[] p = toCake(hit);
		CakeDesign d = design();
		boolean top = CakeGeometry.tierWithTopAt(p[1], d.tierCount(), 0, 0, d.shape()) >= 0 && Math.abs(p[1] - Math.round(p[1] * 2) / 2f) < 2;
		return use(player, hand, stack, hit, top ? net.minecraft.util.math.Direction.UP : net.minecraft.util.math.Direction.NORTH);
	}

	/** A click with something in hand on the cake. Returns true if it did something. */
	public boolean use(ServerPlayerEntity player, Hand hand, ItemStack stack, Vec3d hit, net.minecraft.util.math.Direction side) {
		ServerWorld world = player.getServerWorld();
		CakeDesign design = design();
		float[] p = toCake(hit);

		if (stack.isOf(BakeryItems.CAKE)) {
			if (design.tierCount() >= CakeGeometry.MAX_TIERS) {
				player.sendMessage(Text.translatable("message.bakery.turntable.tall"), true);
				return true;
			}
			setDesign(design.withTier(-1));
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 1f, 1.1f);
			sparkle(world, 4);
			player.sendMessage(Text.translatable("message.bakery.turntable.tier", design.tierCount() + 1), true);
			return true;
		}
		int mould = stack.isOf(BakeryItems.ROUND_CAKE_MOLD) ? CakeGeometry.ROUND : stack.isOf(BakeryItems.SQUARE_CAKE_MOLD) ? CakeGeometry.SQUARE
				: stack.isOf(BakeryItems.HEART_CAKE_MOLD) ? CakeGeometry.HEART : -1;
		if (mould >= 0) {
			setDesign(new CakeDesign(mould, design.tiers(), List.of(), List.of()).withShape(mould));
			world.playSound(null, pos, SoundEvents.BLOCK_MUD_PLACE, SoundCategory.BLOCKS, 0.8f, 1.4f);
			sparkle(world, 3);
			player.sendMessage(Text.translatable("message.bakery.turntable.shaped", Text.translatable("cake_shape.bakery." + mould)), true);
			return true;
		}
		if (stack.getItem() instanceof IcingBagItem bag) {
			return pipe(player, hand, stack, bag, design, p, side);
		}
		if (CakeDesign.isTopper(stack)) {
			return topper(player, stack, design, p, side);
		}
		return false;
	}

	/**
	 * Which tier a click landed on: the face that was clicked says top or side, and the height says which tier.
	 * Returns {tier, 1 if on top} or null for a miss (the underside).
	 */
	private static int[] locate(float[] p, net.minecraft.util.math.Direction face, CakeDesign design) {
		int tiers = design.tierCount();
		if (face == net.minecraft.util.math.Direction.DOWN) return null;
		if (face == net.minecraft.util.math.Direction.UP) {
			int best = -1;
			float bestGap = 1.6f;
			for (int i = 0; i < tiers; i++) {
				float gap = Math.abs(p[1] - CakeGeometry.top(i, tiers));
				if (gap < bestGap) {
					bestGap = gap;
					best = i;
				}
			}
			return best < 0 ? null : new int[]{best, 1};
		}
		for (int i = tiers - 1; i >= 0; i--) {
			if (p[1] >= CakeGeometry.bottom(i, tiers) - 0.6f && p[1] <= CakeGeometry.top(i, tiers) + 0.6f) return new int[]{i, 0};
		}
		return null;
	}

	private boolean pipe(ServerPlayerEntity player, Hand hand, ItemStack stack, IcingBagItem bag, CakeDesign design, float[] p, net.minecraft.util.math.Direction face) {
		ServerWorld world = player.getServerWorld();
		int tiers = design.tierCount();
		int shape = design.shape();
		int[] where = locate(p, face, design);
		int tier = where == null ? -1 : where[0];
		int top = where != null && where[1] == 1 ? tier : -1;
		if (tier < 0) {
			player.sendMessage(Text.translatable("message.bakery.turntable.miss"), true);
			return true;
		}
		if (player.isSneaking()) {
			CakeDesign scraped = design.withoutDabsNear(p[0], p[1], p[2], 1.8f);
			if (scraped.dabs().size() != design.dabs().size()) {
				setDesign(scraped);
				world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.6f, 0.7f);
			}
			return true;
		}
		int color = bag.icing().color();
		float r = CakeGeometry.radius(tier, tiers);
		float topY = CakeGeometry.top(tier, tiers);
		IcingBagItem.Tip tip = IcingBagItem.tip(stack);
		List<CakeDesign.Dab> dabs = new ArrayList<>();
		int cost = 1;
		float[] at = surface(p, top >= 0, tier, design);
		switch (tip) {
			case FROST -> {
				setDesign(design.frost(tier, color));
				world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8f, 1.3f);
				cost = 2;
			}
			case DOTS -> dabs.add(dab(CakeDesign.Dab.DOT, color, at));
			case ROSETTES -> dabs.add(dab(CakeDesign.Dab.ROSETTE, color, at));
			case LINE -> {
				float[] last = lineEnds.get(player.getUuid());
				if (last != null && dist(last, at) < 9) {
					int steps = Math.max(1, Math.round(dist(last, at) / 0.8f));
					for (int i = 1; i <= steps; i++) {
						float t = i / (float) steps;
						dabs.add(dab(CakeDesign.Dab.DOT, color, new float[]{MathHelper.lerp(t, last[0], at[0]), MathHelper.lerp(t, last[1], at[1]), MathHelper.lerp(t, last[2], at[2])}));
					}
				} else {
					dabs.add(dab(CakeDesign.Dab.DOT, color, at));
				}
				lineEnds.put(player.getUuid(), at);
			}
			case BORDER -> {
				for (float[] e : CakeGeometry.perimeter(shape, r - 0.6f, 1.6f)) {
					dabs.add(dab(CakeDesign.Dab.ROSETTE, color, new float[]{e[0], topY + 0.3f, e[1]}));
				}
				cost = 3;
			}
			case DRIPS -> {
				int i = 0;
				for (float[] e : CakeGeometry.perimeter(shape, r + 0.25f, 1.3f)) {
					int length = 1 + Math.floorMod(i * 7 + (int) (e[0] * 3), 4);
					dabs.add(new CakeDesign.Dab(CakeDesign.Dab.DRIP + length - 1, color, Math.round(e[0] * 2), Math.round(topY * 2), Math.round(e[1] * 2)));
					i++;
				}
				// A ring of icing along the top edge the drips run down from.
				for (float[] e : CakeGeometry.perimeter(shape, r - 0.4f, 1f)) {
					dabs.add(dab(CakeDesign.Dab.DOT, color, new float[]{e[0], topY + 0.1f, e[1]}));
				}
				cost = 3;
			}
		}
		if (!dabs.isEmpty()) {
			if (design.dabs().size() + dabs.size() > CakeDesign.MAX_DABS) {
				player.sendMessage(Text.translatable("message.bakery.turntable.full_icing"), true);
				return true;
			}
			setDesign(design.withDabs(dabs));
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.5f, 1.5f + world.random.nextFloat() * 0.3f);
		}
		if (!player.getAbilities().creativeMode) {
			stack.damage(cost, player, hand == Hand.MAIN_HAND ? net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND);
		}
		return true;
	}

	/** Snaps a click to the icing surface: on top of the tier (pulled in from the edge), or just outside its side. */
	private static float[] surface(float[] p, boolean onTop, int tier, CakeDesign design) {
		int tiers = design.tierCount();
		if (onTop) {
			float r = CakeGeometry.radius(tier, tiers);
			float x = p[0], z = p[2];
			if (!CakeGeometry.inside(design.shape(), x, z, r - 0.4f)) {
				float len = (float) Math.sqrt(x * x + z * z);
				if (len > 0.01f) {
					float e = CakeGeometry.edge(design.shape(), x / len, z / len, r) - 0.6f;
					x = x / len * e;
					z = z / len * e;
				}
			}
			return new float[]{x, CakeGeometry.top(tier, tiers) + 0.3f, z};
		}
		float len = (float) Math.sqrt(p[0] * p[0] + p[2] * p[2]);
		float dx = len < 0.01f ? 1 : p[0] / len, dz = len < 0.01f ? 0 : p[2] / len;
		float e = CakeGeometry.edge(design.shape(), dx, dz, CakeGeometry.radius(tier, tiers)) + 0.35f;
		float y = MathHelper.clamp(p[1], CakeGeometry.bottom(tier, tiers) + 0.5f, CakeGeometry.top(tier, tiers) - 0.3f);
		return new float[]{dx * e, y, dz * e};
	}

	private static CakeDesign.Dab dab(int kind, int color, float[] at) {
		return new CakeDesign.Dab(kind, color, Math.round(at[0] * 2), Math.round(at[1] * 2), Math.round(at[2] * 2));
	}

	private static float dist(float[] a, float[] b) {
		float dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
		return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	private boolean topper(ServerPlayerEntity player, ItemStack stack, CakeDesign design, float[] p, net.minecraft.util.math.Direction face) {
		ServerWorld world = player.getServerWorld();
		int tiers = design.tierCount();
		if (player.isSneaking()) {
			Item[] removed = new Item[1];
			CakeDesign without = design.withoutTopperNear(p[0], p[1], p[2], 3f, removed);
			if (removed[0] != null) {
				setDesign(without);
				player.getInventory().offerOrDrop(new ItemStack(removed[0]));
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.4f);
			}
			return true;
		}
		int[] where = locate(p, face, design);
		if (where == null || where[1] != 1) {
			player.sendMessage(Text.translatable("message.bakery.turntable.on_top"), true);
			return true;
		}
		int tier = where[0];
		float[] spot = surface(p, true, tier, design);
		p = new float[]{spot[0], p[1], spot[2]};
		if (design.toppers().size() >= CakeDesign.MAX_TOPPERS) {
			player.sendMessage(Text.translatable("message.bakery.turntable.full_toppers"), true);
			return true;
		}
		float y = CakeGeometry.top(tier, tiers);
		// Toppers face outwards, towards whoever is looking at that side of the cake.
		int yaw = Math.round((float) Math.toDegrees(Math.atan2(p[0], p[2])) / 15f) * 15;
		setDesign(design.withTopper(new CakeDesign.Topper(stack.getItem(), Math.round(p[0] * 2), Math.round(y * 2), Math.round(p[2] * 2), yaw)));
		stack.decrementUnlessCreative(1, player);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.6f, 1.6f + world.random.nextFloat() * 0.3f);
		return true;
	}

	private void sparkle(ServerWorld world, int count) {
		world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, count, 0.3, 0.2, 0.3, 0);
	}

	private void sync() {
		markDirty();
		if (world != null) {
			world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		if (!cake.isEmpty()) nbt.put("cake", cake.encode(registries));
		nbt.putInt("base", baseQuality);
		nbt.putInt("angle", angle);
		nbt.putInt("lastAngle", lastAngle);
		nbt.putLong("spunAt", spunAt);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		cake = nbt.contains("cake") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("cake")) : ItemStack.EMPTY;
		baseQuality = nbt.contains("base") ? nbt.getInt("base") : 3;
		angle = nbt.getInt("angle");
		lastAngle = nbt.getInt("lastAngle");
		spunAt = nbt.getLong("spunAt");
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
