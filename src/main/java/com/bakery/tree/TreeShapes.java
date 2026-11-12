package com.bakery.tree;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-built fruit trees. Canopies are made of several overlapping, slightly ragged leaf clusters on visible
 * branches, so no two trees look alike:
 * <ul>
 * <li>Palm - a gently curving trunk with long drooping fronds and coconuts tucked under the crown</li>
 * <li>Lemon - a short, bushy, rounded little tree</li>
 * <li>Cherry - forked branches holding a wide, layered umbrella of blossom with petals drooping off the edges</li>
 * <li>Orange - a dense, tall oval</li>
 * <li>Peach - an open vase: branches fanning out from a low fork, each with its own puff of leaves</li>
 * <li>Mango - a big, tall tropical dome</li>
 * <li>Plum - a weeping crown with long strands hanging down</li>
 * </ul>
 */
public final class TreeShapes {
	public enum Kind { PALM, LEMON, CHERRY, ORANGE, PEACH, MANGO, PLUM }

	/** What stopped the last tree from growing (offset from the sapling and the block there), for diagnostics. */
	public static String lastBlocker = "";

	private static final Direction[] FLAT = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	private TreeShapes() {
	}

	/** Collects the blocks of one tree before anything is placed, so a blocked tree places nothing. */
	private static final class Plan {
		final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
		final List<BlockPos> fruit = new ArrayList<>();
		final WoodSet wood;
		final BlockState leaves;
		final Random random;

		Plan(WoodSet wood, Random random) {
			this.wood = wood;
			this.random = random;
			this.leaves = wood.leaves.getDefaultState().with(LeavesBlock.DISTANCE, 1).with(LeavesBlock.PERSISTENT, false);
		}

		void log(BlockPos pos, Direction.Axis axis) {
			blocks.put(pos.toImmutable(), wood.log.getDefaultState().with(PillarBlock.AXIS, axis));
		}

		void leaf(BlockPos pos) {
			blocks.putIfAbsent(pos.toImmutable(), leaves);
		}

		/** Leaves from {@code from} to {@code to} in face-connected steps, so they never float. Going down, the drop comes first, so fronds droop. */
		BlockPos leafPath(BlockPos from, BlockPos to) {
			BlockPos p = from;
			while (p.getY() > to.getY()) leaf(p = p.down());
			while (p.getX() != to.getX()) leaf(p = p.add(Integer.signum(to.getX() - p.getX()), 0, 0));
			while (p.getZ() != to.getZ()) leaf(p = p.add(0, 0, Integer.signum(to.getZ() - p.getZ())));
			while (p.getY() != to.getY()) leaf(p = p.add(0, Integer.signum(to.getY() - p.getY()), 0));
			return p;
		}

		boolean isLeaf(BlockPos pos) {
			BlockState s = blocks.get(pos);
			return s != null && s.isOf(wood.leaves);
		}

		/** A straight trunk going up {@code height} blocks; returns the top log. */
		BlockPos trunk(BlockPos base, int height) {
			BlockPos p = base;
			for (int i = 0; i < height; i++) {
				log(p, Direction.Axis.Y);
				if (i < height - 1) p = p.up();
			}
			return p;
		}

		/** Bark on all six sides: used for branches, so no pale log ends poke out of the canopy. */
		void bark(BlockPos pos) {
			blocks.put(pos.toImmutable(), wood.wood.getDefaultState());
		}

		/** A side branch: like {@link #branch} but made of bark-covered wood. */
		BlockPos bough(BlockPos from, double dx, double dy, double dz, int length) {
			barkMode = true;
			BlockPos end = branch(from, dx, dy, dz, length);
			barkMode = false;
			return end;
		}

		private boolean barkMode;

		/** A branch from {@code from} towards (dx, dy, dz) for {@code length} steps, logs turned along their main direction. */
		BlockPos branch(BlockPos from, double dx, double dy, double dz, int length) {
			double x = from.getX() + 0.5, y = from.getY() + 0.5, z = from.getZ() + 0.5;
			double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
			dx /= len;
			dy /= len;
			dz /= len;
			Direction.Axis axis = Math.abs(dy) >= Math.max(Math.abs(dx), Math.abs(dz)) ? Direction.Axis.Y : Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
			BlockPos last = from;
			for (int i = 0; i < length; i++) {
				x += dx;
				y += dy;
				z += dz;
				BlockPos p = BlockPos.ofFloored(x, y, z);
				// Step one axis at a time (main direction first) so the wood stays connected face to face.
				while (!p.equals(last)) {
					int sx = Integer.signum(p.getX() - last.getX()), sy = Integer.signum(p.getY() - last.getY()), sz = Integer.signum(p.getZ() - last.getZ());
					if (axis == Direction.Axis.Y && sy != 0) last = last.add(0, sy, 0);
					else if (axis == Direction.Axis.X && sx != 0) last = last.add(sx, 0, 0);
					else if (axis == Direction.Axis.Z && sz != 0) last = last.add(0, 0, sz);
					else if (sy != 0) last = last.add(0, sy, 0);
					else if (sx != 0) last = last.add(sx, 0, 0);
					else last = last.add(0, 0, sz);
					if (barkMode) bark(last);
					else log(last, axis);
				}
			}
			return last;
		}

		/** A ragged ellipsoid of leaves. Edge leaves are skipped at random so the outline is never a clean sphere. */
		void cluster(BlockPos center, double rx, double ry, double rz, double fill) {
			int ix = MathHelper.ceil(rx), iy = MathHelper.ceil(ry), iz = MathHelper.ceil(rz);
			for (int dx = -ix; dx <= ix; dx++) {
				for (int dy = -iy; dy <= iy; dy++) {
					for (int dz = -iz; dz <= iz; dz++) {
						double d = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
						if (d > 1.0) continue;
						if (d > 0.55 && random.nextFloat() > fill) continue;
						leaf(center.add(dx, dy, dz));
					}
				}
			}
		}

		/** Leaves hanging down off the bottom edge of the canopy, like blossom drooping or weeping strands. */
		void drips(double chance, int maxLength) {
			List<BlockPos> bottoms = new ArrayList<>();
			for (BlockPos p : blocks.keySet()) {
				if (isLeaf(p) && !blocks.containsKey(p.down())) bottoms.add(p);
			}
			for (BlockPos p : bottoms) {
				if (random.nextFloat() >= chance) continue;
				int length = 1 + random.nextInt(maxLength);
				for (int i = 1; i <= length; i++) leaf(p.down(i));
			}
		}

		/** Drops leaves more than 6 steps from a log, which would decay as soon as they were placed. */
		void trim() {
			Map<BlockPos, Integer> distance = new java.util.HashMap<>();
			java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
			for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
				if (!e.getValue().isOf(wood.leaves)) {
					distance.put(e.getKey(), 0);
					queue.add(e.getKey());
				}
			}
			while (!queue.isEmpty()) {
				BlockPos p = queue.poll();
				int d = distance.get(p);
				if (d >= 6) continue;
				for (Direction dir : Direction.values()) {
					BlockPos n = p.offset(dir);
					if (isLeaf(n) && !distance.containsKey(n)) {
						distance.put(n, d + 1);
						queue.add(n);
					}
				}
			}
			blocks.keySet().removeIf(p -> isLeaf(p) && !distance.containsKey(p));
		}

		/** Spots right under leaves where fruit can hang. */
		void fruitUnderLeaves() {
			for (BlockPos p : new ArrayList<>(blocks.keySet())) {
				if (isLeaf(p) && !blocks.containsKey(p.down())) fruit.add(p.down());
			}
		}
	}

	public static boolean grow(Kind kind, WorldAccess world, BlockPos base, Random random, WoodSet wood, Block fruit, int flags) {
		Plan plan = new Plan(wood, random);
		switch (kind) {
			case PALM -> palm(plan, base, random);
			case LEMON -> lemon(plan, base, random);
			case CHERRY -> cherry(plan, base, random);
			case ORANGE -> orange(plan, base, random);
			case PEACH -> peach(plan, base, random);
			case MANGO -> mango(plan, base, random);
			case PLUM -> plum(plan, base, random);
		}
		plan.trim();
		if (kind != Kind.PALM) {
			plan.fruitUnderLeaves();
		}

		for (BlockPos p : plan.blocks.keySet()) {
			if (!p.equals(base) && !canReplace(world.getBlockState(p))) {
				lastBlocker = p.subtract(base).toShortString() + " " + world.getBlockState(p);
				return false;
			}
		}
		lastBlocker = "";
		for (Map.Entry<BlockPos, BlockState> e : plan.blocks.entrySet()) {
			world.setBlockState(e.getKey(), e.getValue(), flags);
		}
		java.util.Collections.shuffle(plan.fruit, new java.util.Random(random.nextLong()));
		int fruits = 0;
		int max = kind == Kind.PALM ? 4 : 5 + random.nextInt(3);
		for (BlockPos p : plan.fruit) {
			if (fruits >= max) break;
			if (world.getBlockState(p).isAir() && HangingFruitBlock.canHangUnder(world.getBlockState(p.up()))) {
				world.setBlockState(p, fruit.getDefaultState().with(HangingFruitBlock.AGE, random.nextInt(3)), flags);
				fruits++;
			}
		}
		return true;
	}

	private static int range(Random random, int min, int max) {
		return min + random.nextInt(max - min + 1);
	}

	private static double range(Random random, double min, double max) {
		return min + random.nextDouble() * (max - min);
	}

	/** A trunk that may lean a little; returns the top log. */
	private static BlockPos leaningTrunk(Plan plan, BlockPos base, int height, double lean, Random random) {
		plan.log(base, Direction.Axis.Y);
		double angle = random.nextDouble() * Math.PI * 2;
		BlockPos top = plan.branch(base, Math.cos(angle) * lean, 1, Math.sin(angle) * lean, height - 1);
		return top;
	}

	/** Hidden spur branches inside a big crown, so its far leaves stay close enough to wood not to decay. */
	private static void spurs(Plan plan, BlockPos center, int length, Random random) {
		plan.log(center, Direction.Axis.Y);
		for (Direction d : FLAT) {
			if (random.nextFloat() < 0.85f) plan.bough(center, d.getOffsetX(), range(random, -0.2, 0.3), d.getOffsetZ(), length);
		}
	}

	/** A palm: a long trunk swaying in a C or S curve (sometimes twin trunks from one root) under a ragged crown of arching fronds. */
	private static void palm(Plan plan, BlockPos base, Random random) {
		int trunks = random.nextFloat() < 0.3f ? 2 : 1;
		Direction first = FLAT[random.nextInt(4)];
		for (int k = 0; k < trunks; k++) {
			Direction lean = k == 0 ? first : first.getOpposite();
			Direction sway = random.nextBoolean() ? lean.rotateYClockwise() : lean.rotateYCounterclockwise();
			int height = range(random, 6, 12) - k * 2;
			double bend = range(random, 0.5, 5.0);
			double swayAmp = range(random, 0.0, 2.0);
			BlockPos at = base;
			plan.log(base, Direction.Axis.Y);
			for (int i = 1; i < height; i++) {
				double t = i / (double) (height - 1);
				BlockPos next = base.up(i).offset(lean, (int) Math.round(bend * t * t)).offset(sway, (int) Math.round(swayAmp * Math.sin(t * Math.PI)));
				// Step sideways first so a strong bend never leaves a gap in the trunk.
				while (at.getX() != next.getX() || at.getZ() != next.getZ()) {
					at = at.getX() != next.getX() ? at.add(Integer.signum(next.getX() - at.getX()), 0, 0) : at.add(0, 0, Integer.signum(next.getZ() - at.getZ()));
					plan.log(at, Direction.Axis.Y);
				}
				at = next;
				plan.log(at, Direction.Axis.Y);
			}
			palmCrown(plan, at, base.getY() + 2, random);
		}
	}

	private static void palmCrown(Plan plan, BlockPos crown, int floor, Random random) {
		// The crown is a little knot of wood (a plus of logs), so fronds can reach five blocks out and still
		// droop two blocks down before leaf decay would eat them.
		BlockPos head = crown.up();
		plan.log(head, Direction.Axis.Y);
		for (Direction d : FLAT) {
			plan.bark(head.offset(d));
			plan.fruit.add(crown.offset(d));
		}
		plan.leaf(head.up());
		plan.leaf(head.up(2));
		int fronds = range(random, 6, 10);
		double start = random.nextDouble() * Math.PI * 2;
		for (int f = 0; f < fronds; f++) {
			double angle = start + f * Math.PI * 2 / fronds + range(random, -0.3, 0.3);
			int length = range(random, 4, 6);
			double arch = range(random, 0.6, 1.4), fall = range(random, 0.25, 0.45);
			BlockPos at = head.up();
			for (int s = 1; s <= length; s++) {
				// Fronds arch out and droop, but never down into the ground on a short palm.
				int y = Math.max(floor - head.getY(), (int) Math.round(arch * s - fall * s * s));
				at = plan.leafPath(at, head.add((int) Math.round(Math.cos(angle) * s), y, (int) Math.round(Math.sin(angle) * s)));
			}
			if (at.getY() - 2 >= floor) plan.leafPath(at, at.down(1 + random.nextInt(2)));
		}
	}

	/** Lemon: a short, crooked little tree with a lopsided bush of a crown made of several round puffs. */
	private static void lemon(Plan plan, BlockPos base, Random random) {
		BlockPos top = leaningTrunk(plan, base, range(random, 2, 5), range(random, 0.0, 0.6), random);
		plan.cluster(top.up(), range(random, 2.0, 3.4), range(random, 1.6, 2.8), range(random, 2.0, 3.4), 0.45);
		int branches = range(random, 1, 4);
		double start = random.nextDouble() * Math.PI * 2;
		for (int b = 0; b < branches; b++) {
			double angle = start + b * Math.PI * 2 / branches + range(random, -0.6, 0.6);
			BlockPos end = plan.bough(top.down(random.nextInt(2)), Math.cos(angle), range(random, 0.3, 1.4), Math.sin(angle), range(random, 2, 4));
			plan.cluster(end.up(), range(random, 1.5, 2.8), range(random, 1.2, 2.2), range(random, 1.5, 2.8), 0.45);
		}
		plan.drips(0.1, 1);
	}

	/** Cherry: crooked arms spreading from one fork into a wide, layered, flat umbrella of blossom that drips off the edges. */
	private static void cherry(Plan plan, BlockPos base, Random random) {
		BlockPos fork = leaningTrunk(plan, base, range(random, 3, 6), range(random, 0.0, 0.7), random);
		int arms = range(random, 3, 6);
		double start = random.nextDouble() * Math.PI * 2;
		for (int a = 0; a < arms; a++) {
			double angle = start + a * Math.PI * 2 / arms + range(random, -0.5, 0.5);
			BlockPos end = plan.bough(fork, Math.cos(angle), range(random, 0.3, 1.0), Math.sin(angle), range(random, 3, 7));
			double r = range(random, 2.4, 4.0);
			plan.cluster(end.up(), r, range(random, 1.0, 1.6), r * range(random, 0.7, 1.1), 0.55);
			if (random.nextBoolean()) plan.cluster(end.up(2), r * 0.6, 1.0, r * 0.6, 0.5);
		}
		plan.cluster(fork.up(2), range(random, 1.8, 3.0), 1.3, range(random, 1.8, 3.0), 0.5);
		plan.drips(range(random, 0.25, 0.5), range(random, 1, 3));
	}

	/** Orange: a dense, tall egg or a stack of two lobes, on a trunk that's sometimes very short. */
	private static void orange(Plan plan, BlockPos base, Random random) {
		int bare = range(random, 1, 4);
		double ry = range(random, 2.8, 5.0), rx = range(random, 2.4, 3.6);
		int center = bare + (int) Math.round(ry) - 1;
		BlockPos top = leaningTrunk(plan, base, center + 2, range(random, 0.0, 0.3), random);
		BlockPos middle = top.down(2);
		plan.cluster(middle, rx, ry, rx * range(random, 0.8, 1.15), 0.5);
		spurs(plan, middle, 2, random);
		if (random.nextFloat() < 0.4f) {
			plan.cluster(top.up(2).offset(FLAT[random.nextInt(4)]), rx * 0.7, ry * 0.5, rx * 0.7, 0.45);
		}
		int lumps = range(random, 1, 4);
		for (int i = 0; i < lumps; i++) {
			plan.cluster(middle.up(range(random, -2, 2)).offset(FLAT[random.nextInt(4)], (int) rx), 1.6, 1.6, 1.6, 0.4);
		}
		plan.leaf(top.up());
		plan.drips(0.12, 1);
	}

	/** Peach: a low fork opening into a vase of long arms, some forking again, each ending in its own puff. */
	private static void peach(Plan plan, BlockPos base, Random random) {
		BlockPos fork = leaningTrunk(plan, base, range(random, 2, 4), range(random, 0.0, 0.5), random);
		int arms = range(random, 3, 6);
		double start = random.nextDouble() * Math.PI * 2;
		for (int a = 0; a < arms; a++) {
			double angle = start + a * Math.PI * 2 / arms + range(random, -0.4, 0.4);
			BlockPos end = plan.bough(fork, Math.cos(angle), range(random, 0.8, 1.8), Math.sin(angle), range(random, 3, 6));
			plan.cluster(end.up(), range(random, 1.6, 2.8), range(random, 1.2, 2.0), range(random, 1.6, 2.8), 0.5);
			if (random.nextFloat() < 0.4f) {
				double side = angle + (random.nextBoolean() ? 1 : -1) * range(random, 0.6, 1.2);
				BlockPos twig = plan.bough(end.down(), Math.cos(side), range(random, 0.2, 0.8), Math.sin(side), range(random, 2, 3));
				plan.cluster(twig.up(), 1.6, 1.3, 1.6, 0.45);
			}
		}
		plan.drips(0.1, 1);
	}

	/** Mango: a tall, sometimes huge tree; big boughs at different heights under a broad, heavy dome. */
	private static void mango(Plan plan, BlockPos base, Random random) {
		int height = range(random, 4, 9);
		BlockPos top = leaningTrunk(plan, base, height, range(random, 0.0, 0.4), random);
		double dome = range(random, 3.0, 5.2);
		plan.cluster(top.up(), dome, range(random, 1.8, 3.0), dome * range(random, 0.75, 1.1), 0.55);
		spurs(plan, top, (int) Math.ceil(dome / 2), random);
		int boughs = range(random, 2, 6);
		double start = random.nextDouble() * Math.PI * 2;
		for (int b = 0; b < boughs; b++) {
			double angle = start + b * Math.PI * 2 / boughs + range(random, -0.4, 0.4);
			BlockPos from = top.down(range(random, 1, Math.max(1, height - 3)));
			BlockPos end = plan.bough(from, Math.cos(angle), range(random, 0.3, 0.9), Math.sin(angle), range(random, 2, 5));
			plan.cluster(end.up(), range(random, 2.0, 3.0), range(random, 1.4, 2.2), range(random, 2.0, 3.0), 0.5);
		}
		plan.drips(range(random, 0.1, 0.3), 2);
	}

	/** Plum: a leaning, twisting trunk with one to three domes and long strands weeping almost to the ground. */
	private static void plum(Plan plan, BlockPos base, Random random) {
		BlockPos top = leaningTrunk(plan, base, range(random, 5, 8), range(random, 0.3, 1.0), random);
		double r = range(random, 3.0, 4.4);
		plan.cluster(top.up(), r, range(random, 1.2, 1.8), r * range(random, 0.8, 1.1), 0.6);
		spurs(plan, top, 3, random);
		int extra = range(random, 0, 3);
		for (int i = 0; i < extra; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			BlockPos end = plan.bough(top.down(range(random, 1, 3)), Math.cos(angle), range(random, 0.0, 0.5), Math.sin(angle), range(random, 2, 5));
			plan.cluster(end.up(), range(random, 2.0, 3.0), range(random, 1.0, 1.5), range(random, 2.0, 3.0), 0.55);
		}
		// Long curtains of leaves hanging off almost every edge.
		plan.drips(range(random, 0.45, 0.7), range(random, 3, 6));
	}

	private static boolean canReplace(BlockState state) {
		return state.isAir() || state.isReplaceable() || state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.SAPLINGS);
	}
}
