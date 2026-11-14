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

	/** Turns a world direction into cake space (the plate's spin undone). */
	public float[] toCakeDir(Vec3d dir) {
		double a = Math.toRadians(angle);
		float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
		float x = (float) dir.x, z = (float) dir.z;
		return new float[]{x * cos - z * sin, (float) dir.y, x * sin + z * cos};
	}

	/**
	 * Where on the cake this player is aiming, traced from their eyes to the real cake surface:
	 * {x, y, z, tier, onTop} in cake pixels, or null if they are not looking at the cake.
	 */
	public float[] aim(net.minecraft.entity.Entity viewer, float tickDelta) {
		if (cake.isEmpty()) return null;
		CakeDesign d = design();
		float[] eye = toCake(viewer.getCameraPosVec(tickDelta));
		// The server reads the player's current look; the client smooths it between ticks for the preview.
		float[] dir = toCakeDir(tickDelta >= 1 ? viewer.getRotationVector() : viewer.getRotationVec(tickDelta));
		return CakeGeometry.raycast(d.shape(), d.tierCount(), eye, dir, 16 * 5);
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
		// Prefer exactly where the player is looking on the real cake shape over the block's boxy outline.
		float[] aim = aim(player, 1f);
		if (aim != null) {
			p = new float[]{aim[0], aim[1], aim[2]};
			side = aim[4] > 0.5f ? net.minecraft.util.math.Direction.UP : net.minecraft.util.math.Direction.NORTH;
		}

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
		if (stack.isOf(BakeryItems.SPATULA)) {
			return turnTopper(player, design, p);
		}
		if (stack.isOf(BakeryItems.CAKE_SCRAPER)) {
			return scrape(player, design, p, side);
		}
		return false;
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
			CakeDesign scraped = design.withoutDabsNear(p[0], p[1], p[2], 2.4f);
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
			case DOTS, PEARLS, STARS, ROSETTES, SHELLS, BORDER, DRIPS -> {
				dabs.addAll(piped(tip, color, design, at, tier, top >= 0));
				cost = tip == IcingBagItem.Tip.SHELLS || tip == IcingBagItem.Tip.BORDER ? 3 : tip == IcingBagItem.Tip.DRIPS && dabs.size() > 30 ? 3 : 1;
			}
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

	/**
	 * The icing one click with this tip pipes at {@code at} (already on the surface) on {@code tier}. Shared with the
	 * renderer, so the see-through preview shows exactly what a click will add.
	 */
	public static List<CakeDesign.Dab> piped(IcingBagItem.Tip tip, int color, CakeDesign design, float[] at, int tier, boolean onTop) {
		int tiers = design.tierCount(), shape = design.shape();
		float r = CakeGeometry.radius(tier, tiers), topY = CakeGeometry.top(tier, tiers);
		List<CakeDesign.Dab> dabs = new ArrayList<>();
		switch (tip) {
			case DOTS -> dabs.add(dab(CakeDesign.Dab.DOT, color, at));
			case PEARLS -> dabs.add(dab(CakeDesign.Dab.PEARL, color, at));
			case STARS -> dabs.add(dab(CakeDesign.Dab.STAR, color, at));
			case ROSETTES -> dabs.add(dab(CakeDesign.Dab.ROSETTE, color, at));
			case SHELLS -> {
				// A shell border all round the bottom of the tier.
				float y = CakeGeometry.bottom(tier, tiers) + 0.55f;
				for (float[] e : CakeGeometry.perimeter(shape, r + 0.2f, 1.25f)) dabs.add(dab(CakeDesign.Dab.SHELL, color, new float[]{e[0], y, e[1]}));
			}
			case BORDER -> {
				for (float[] e : CakeGeometry.perimeter(shape, r - 0.6f, 1.6f)) dabs.add(dab(CakeDesign.Dab.ROSETTE, color, new float[]{e[0], topY + 0.3f, e[1]}));
			}
			case DRIPS -> {
				// Drips run down the side nearest the click (about a quarter of the way round);
				// a click near the middle of the top drips the whole edge.
				double clickAngle = Math.atan2(at[2], at[0]);
				boolean all = onTop && Math.sqrt(at[0] * at[0] + at[2] * at[2]) < r * 0.45f;
				int i = 0;
				for (float[] e : CakeGeometry.perimeter(shape, r + 0.25f, 1.3f)) {
					if (all || angleGap(e, clickAngle) <= 0.85) {
						int length = 1 + Math.floorMod(i * 7 + (int) (e[0] * 3), 4);
						dabs.add(new CakeDesign.Dab(CakeDesign.Dab.DRIP + length - 1, color, Math.round(e[0] * 2), Math.round(topY * 2), Math.round(e[1] * 2)));
					}
					i++;
				}
				// A ring of icing along the top edge the drips run down from.
				for (float[] e : CakeGeometry.perimeter(shape, r - 0.4f, 1f)) {
					if (all || angleGap(e, clickAngle) <= 0.85) dabs.add(dab(CakeDesign.Dab.DOT, color, new float[]{e[0], topY + 0.1f, e[1]}));
				}
			}
			default -> {
			}
		}
		return dabs;
	}

	private static double angleGap(float[] e, double angle) {
		double a = Math.atan2(e[1], e[0]) - angle;
		return Math.abs(Math.atan2(Math.sin(a), Math.cos(a)));
	}

	/** Snaps a click to the icing surface: on top of the tier (pulled in from the edge), or just outside its side. */
	public static float[] surface(float[] p, boolean onTop, int tier, CakeDesign design) {
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
		setDesign(design.withTopper(new CakeDesign.Topper(stack.getItem(), Math.round(spot[0] * 2), Math.round(spot[1] * 2), Math.round(spot[2] * 2), (int) spot[3],
				CakeDesign.defaultScale(stack.getItem()))));
		stack.decrementUnlessCreative(1, player);
		world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.6f, 1.6f + world.random.nextFloat() * 0.3f);
		return true;
	}

	/**
	 * The scraper: a click lifts off the topper or icing under it; sneak-click scrapes the whole tier clean (frosting,
	 * piping and toppers - the toppers come back to you).
	 */
	private boolean scrape(ServerPlayerEntity player, CakeDesign design, float[] p, net.minecraft.util.math.Direction face) {
		ServerWorld world = player.getServerWorld();
		int[] where = locate(p, face, design);
		if (player.isSneaking()) {
			if (where == null) return true;
			List<Item> removed = new ArrayList<>();
			CakeDesign clean = design.withoutTier(where[0], removed);
			setDesign(clean);
			removed.forEach(item -> player.getInventory().offerOrDrop(new ItemStack(item)));
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.9f, 0.6f);
			player.sendMessage(Text.translatable("message.bakery.turntable.scraped_tier", where[0] + 1), true);
			return true;
		}
		Item[] lifted = new Item[1];
		CakeDesign without = design.withoutTopperNear(p[0], p[1], p[2], 2f, lifted);
		if (lifted[0] != null) {
			setDesign(without);
			player.getInventory().offerOrDrop(new ItemStack(lifted[0]));
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.4f);
			return true;
		}
		CakeDesign scraped = design.withoutDabsNear(p[0], p[1], p[2], 2.6f);
		if (scraped.dabs().size() != design.dabs().size()) {
			setDesign(scraped);
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.6f, 0.8f);
		} else if (where != null && design.tiers().get(where[0]) != -1) {
			setDesign(design.frost(where[0], -1));
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.7f, 0.7f);
		}
		return true;
	}

	/** The spatula turns the topper nearest the click by 22.5 degrees; sneak-click steps it through sizes instead. */
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
		if (player.isSneaking()) {
			int next = CakeDesign.SIZES[0];
			for (int size : CakeDesign.SIZES) {
				if (size > t.scale()) {
					next = size;
					break;
				}
			}
			toppers.set(best, t.withScale(next));
			player.sendMessage(Text.translatable("message.bakery.turntable.size", next), true);
		} else {
			toppers.set(best, t.withYaw(Math.floorMod(t.yaw() + 22, 360)));
		}
		setDesign(new CakeDesign(design.shape(), design.tiers(), design.dabs(), toppers));
		player.getServerWorld().playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM, SoundCategory.BLOCKS, 0.7f, player.isSneaking() ? 1.1f : 1.5f);
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
		// Always write something: the client ignores an empty update, so a taken cake would stay on screen.
		nbt.putBoolean("synced", true);
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
