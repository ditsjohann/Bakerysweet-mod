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
 * The Cake Turntable. Everything happens on the cake itself: pipe icing exactly where you click (on any tier, on top
 * or on the sides; a faint preview shows where it will land), set toppers down as little sugar decals, stack another
 * cake on top as a new tier (the decorations below stay put), press a mould onto a tier to give just that tier a new
 * shape, and give it a spin (empty hand) to reach the other side. Sneak to scrape icing or lift a topper back off;
 * sneak with an empty hand to take the finished cake. The cuteness of the design turns into stars when it comes off.
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
			// A cake that was already shaped keeps its shape as the new tier; a plain one matches the tier below.
			CakeDesign placed = stack.get(BakeryComponents.CAKE_DESIGN);
			setDesign(design.withTier(-1, placed != null ? placed.shapeOf(0) : design.shapeOf(design.tierCount() - 1)));
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 1f, 1.1f);
			sparkle(world, 4);
			player.sendMessage(Text.translatable("message.bakery.turntable.tier", design.tierCount() + 1), true);
			return true;
		}
		int mould = mouldShape(stack);
		if (mould >= 0) {
			// Press the mould onto the tier you clicked: just that tier takes the new shape.
			int[] where = locate(p, side, design);
			int tier = where == null ? design.tierCount() - 1 : where[0];
			setDesign(design.reshape(tier, mould));
			world.playSound(null, pos, SoundEvents.BLOCK_MUD_PLACE, SoundCategory.BLOCKS, 0.8f, 1.4f);
			sparkle(world, 3);
			player.sendMessage(design.tierCount() > 1
					? Text.translatable("message.bakery.turntable.shaped_tier", Text.translatable("guide.bakery.layer" + (tier + 1)), Text.translatable("cake_shape.bakery." + mould))
					: Text.translatable("message.bakery.turntable.shaped", Text.translatable("cake_shape.bakery." + mould)), true);
			return true;
		}
		if (stack.getItem() instanceof IcingBagItem bag) {
			return pipe(player, hand, stack, bag, design, p, side);
		}
		if (CakeDesign.isTopper(stack)) {
			return topper(player, stack, design, p, side);
		}
		if (stack.isOf(BakeryItems.SPATULA)) {
			return turnTopper(player, design, p);
		}
		return false;
	}

	/** The cake shape a mould item presses, or -1. */
	public static int mouldShape(ItemStack stack) {
		Item[] moulds = BakeryItems.CAKE_MOLDS;
		for (int i = 0; i < moulds.length; i++) {
			if (stack.isOf(moulds[i])) return i;
		}
		return -1;
	}

	/**
	 * Which tier a click landed on: the face that was clicked says top or side, and the height says which tier.
	 * Returns {tier, 1 if on top} or null for a miss (the underside).
	 */
	public static int[] locate(float[] p, net.minecraft.util.math.Direction face, CakeDesign design) {
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

	/**
	 * Where a squeeze of this bag would put icing for a click at cake point {@code p} on {@code face}: the dabs it
	 * would add (for the frosting tip, a ring along the tier's top edge marks the tier that gets frosted), and the
	 * tier. Null for a miss. Shared by piping and the renderer's preview, so the preview is always exactly right.
	 */
	public static Plan plan(CakeDesign design, float[] p, net.minecraft.util.math.Direction face, IcingBagItem.Tip tip, int color, float[] lastLineEnd) {
		int tiers = design.tierCount();
		int[] where = locate(p, face, design);
		if (where == null) return null;
		int tier = where[0];
		boolean onTop = where[1] == 1;
		int shape = design.shapeOf(tier);
		float r = CakeGeometry.radius(tier, tiers);
		float topY = CakeGeometry.top(tier, tiers);
		float[] at = surface(p, onTop, tier, design);
		List<CakeDesign.Dab> dabs = new ArrayList<>();
		switch (tip) {
			case FROST -> {
				for (float[] e : CakeGeometry.perimeter(shape, r - 0.6f, 1.6f)) dabs.add(dab(CakeDesign.Dab.DOT, color, new float[]{e[0], topY + 0.2f, e[1]}));
			}
			case DOTS -> dabs.add(dab(CakeDesign.Dab.DOT, color, at));
			case ROSETTES -> dabs.add(dab(CakeDesign.Dab.ROSETTE, color, at));
			case LINE -> {
				if (lastLineEnd != null && dist(lastLineEnd, at) < 9) {
					int steps = Math.max(1, Math.round(dist(lastLineEnd, at) / 0.8f));
					for (int i = 1; i <= steps; i++) {
						float t = i / (float) steps;
						dabs.add(dab(CakeDesign.Dab.DOT, color, new float[]{MathHelper.lerp(t, lastLineEnd[0], at[0]), MathHelper.lerp(t, lastLineEnd[1], at[1]),
								MathHelper.lerp(t, lastLineEnd[2], at[2])}));
					}
				} else {
					dabs.add(dab(CakeDesign.Dab.DOT, color, at));
				}
			}
			case BORDER -> {
				if (onTop) {
					for (float[] e : CakeGeometry.perimeter(shape, r - 0.6f, 1.6f)) dabs.add(dab(CakeDesign.Dab.ROSETTE, color, new float[]{e[0], topY + 0.3f, e[1]}));
				} else {
					// Clicked on a side: a band of rosettes all the way round at that height (a bottom border, a belt...).
					for (float[] e : CakeGeometry.perimeter(shape, r, 1.6f)) {
						float out = CakeGeometry.edge(shape, e[2], e[3], r) + 0.35f;
						dabs.add(dab(CakeDesign.Dab.ROSETTE, color, new float[]{e[2] * out, at[1], e[3] * out}));
					}
				}
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
			}
		}
		return new Plan(tier, at, dabs);
	}

	/** What a squeeze would do: the tier, the exact spot clicked on the icing, and the dabs it adds. */
	public record Plan(int tier, float[] at, List<CakeDesign.Dab> dabs) {
	}

	/** The last point this player piped a line to (for the preview). */
	public float[] lineEnd(UUID player) {
		return lineEnds.get(player);
	}

	private boolean pipe(ServerPlayerEntity player, Hand hand, ItemStack stack, IcingBagItem bag, CakeDesign design, float[] p, net.minecraft.util.math.Direction face) {
		ServerWorld world = player.getServerWorld();
		if (player.isSneaking()) {
			if (locate(p, face, design) == null) return true;
			CakeDesign scraped = design.withoutDabsNear(p[0], p[1], p[2], 1.8f);
			if (scraped.dabs().size() != design.dabs().size()) {
				setDesign(scraped);
				world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.6f, 0.7f);
			}
			return true;
		}
		int color = bag.icing().color();
		IcingBagItem.Tip tip = IcingBagItem.tip(stack);
		Plan plan = plan(design, p, face, tip, color, lineEnds.get(player.getUuid()));
		if (plan == null) {
			player.sendMessage(Text.translatable("message.bakery.turntable.miss"), true);
			return true;
		}
		int cost = switch (tip) {
			case FROST -> 2;
			case BORDER, DRIPS -> 3;
			default -> 1;
		};
		if (tip == IcingBagItem.Tip.FROST) {
			setDesign(design.frost(plan.tier(), color));
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.BLOCKS, 0.8f, 1.3f);
		} else {
			if (tip == IcingBagItem.Tip.LINE) lineEnds.put(player.getUuid(), plan.at());
			if (design.dabs().size() + plan.dabs().size() > CakeDesign.MAX_DABS) {
				player.sendMessage(Text.translatable("message.bakery.turntable.full_icing"), true);
				return true;
			}
			setDesign(design.withDabs(plan.dabs()));
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.5f, 1.5f + world.random.nextFloat() * 0.3f);
		}
		if (!player.getAbilities().creativeMode) {
			stack.damage(cost, player, hand == Hand.MAIN_HAND ? net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND);
		}
		return true;
	}

	/** Snaps a click to the icing surface: on top of the tier (pulled in from the edge), or just outside its side. */
	public static float[] surface(float[] p, boolean onTop, int tier, CakeDesign design) {
		int tiers = design.tierCount();
		int shape = design.shapeOf(tier);
		if (onTop) {
			float r = CakeGeometry.radius(tier, tiers);
			float x = p[0], z = p[2];
			if (!CakeGeometry.inside(shape, x, z, r - 0.4f)) {
				float len = (float) Math.sqrt(x * x + z * z);
				if (len > 0.01f) {
					float e = CakeGeometry.edge(shape, x / len, z / len, r) - 0.6f;
					x = x / len * e;
					z = z / len * e;
				}
			}
			return new float[]{x, CakeGeometry.top(tier, tiers) + 0.3f, z};
		}
		float len = (float) Math.sqrt(p[0] * p[0] + p[2] * p[2]);
		float dx = len < 0.01f ? 1 : p[0] / len, dz = len < 0.01f ? 0 : p[2] / len;
		float e = CakeGeometry.edge(shape, dx, dz, CakeGeometry.radius(tier, tiers)) + 0.35f;
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

	/** The yaw (degrees, 15-degree steps) that turns a topper at (x, z) on the cake to face this player. */
	public int yawToward(net.minecraft.entity.Entity viewer, float x, float z) {
		float[] eye = toCake(viewer.getEyePos());
		return Math.round((float) Math.toDegrees(Math.atan2(eye[0] - x, eye[2] - z)) / 15f) * 15;
	}

	/**
	 * Where a topper would go for a click at cake point {@code p} on {@code face}: {x, y, z, yaw, onSide} in pixels,
	 * or null if it can't go there. On top of a tier it stands up facing the viewer; on a tier's side it is pressed
	 * flat against the icing, facing out. Shared with the renderer's placement preview.
	 */
	public float[] topperSpot(CakeDesign design, float[] p, net.minecraft.util.math.Direction face, net.minecraft.entity.Entity viewer) {
		int tiers = design.tierCount();
		int[] where = locate(p, face, design);
		if (where == null) return null;
		int tier = where[0];
		if (where[1] == 1) {
			float[] spot = surface(p, true, tier, design);
			float y = CakeGeometry.top(tier, tiers);
			return new float[]{spot[0], y, spot[2], yawToward(viewer, spot[0], spot[2]), 0};
		}
		float[] spot = surface(p, false, tier, design);
		float yaw = Math.round((float) Math.toDegrees(Math.atan2(spot[0], spot[2])) / 15f) * 15;
		return new float[]{spot[0], spot[1], spot[2], yaw, 1};
	}

	private boolean topper(ServerPlayerEntity player, ItemStack stack, CakeDesign design, float[] p, net.minecraft.util.math.Direction face) {
		ServerWorld world = player.getServerWorld();
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
		float[] spot = topperSpot(design, p, face, player);
		if (spot == null) {
			player.sendMessage(Text.translatable("message.bakery.turntable.on_top"), true);
			return true;
		}
		if (design.toppers().size() >= CakeDesign.MAX_TOPPERS) {
			player.sendMessage(Text.translatable("message.bakery.turntable.full_toppers"), true);
			return true;
		}
		setDesign(design.withTopper(new CakeDesign.Topper(stack.getItem(), Math.round(spot[0] * 2), Math.round(spot[1] * 2), Math.round(spot[2] * 2), (int) spot[3])));
		stack.decrementUnlessCreative(1, player);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.6f, 1.6f + world.random.nextFloat() * 0.3f);
		return true;
	}

	/** The spatula turns the topper nearest the click by 22.5 degrees (sneak to turn it back). */
	private boolean turnTopper(ServerPlayerEntity player, CakeDesign design, float[] p) {
		int best = -1;
		float bestD = 3.5f;
		for (int i = 0; i < design.toppers().size(); i++) {
			CakeDesign.Topper t = design.toppers().get(i);
			float dx = t.x() / 2f - p[0], dy = t.y() / 2f - p[1], dz = t.z() / 2f - p[2];
			float d = (float) Math.sqrt(dx * dx + dy * dy * 0.25f + dz * dz);
			if (d < bestD) {
				bestD = d;
				best = i;
			}
		}
		if (best < 0) {
			player.sendMessage(Text.translatable("message.bakery.turntable.turn_hint"), true);
			return true;
		}
		List<CakeDesign.Topper> toppers = new ArrayList<>(design.toppers());
		CakeDesign.Topper t = toppers.get(best);
		int step = player.isSneaking() ? -22 : 22;
		toppers.set(best, new CakeDesign.Topper(t.item(), t.x(), t.y(), t.z(), Math.floorMod(t.yaw() + step, 360)));
		setDesign(design.withToppers(toppers));
		player.getServerWorld().playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM, SoundCategory.BLOCKS, 0.7f, 1.5f);
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
