package com.bakery.shop;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The walking path a baker lays from the bakery door to the counter with a Path Ribbon. Customers come in at the
 * entrance (the first point), follow each turn, and line up along the last stretch of the path, the first in line
 * standing right in front of the counter.
 */
public final class CustomerPath {
	public static final int MAX_POINTS = 24;
	public static final int MAX_STEP = 20;

	private CustomerPath() {
	}

	/** Where the first customer in line stands: right in front of the counter. */
	public static BlockPos counterFront(World world, BlockPos counter) {
		BlockState state = world.getBlockState(counter);
		Direction facing = state.contains(CounterBlock.FACING) ? state.get(CounterBlock.FACING) : Direction.NORTH;
		return counter.offset(facing);
	}

	/** Every block along the path, entrance first (straight lines between the points). */
	public static List<BlockPos> blocks(List<BlockPos> points) {
		List<BlockPos> out = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			BlockPos b = points.get(i);
			if (i == 0) {
				out.add(b);
				continue;
			}
			BlockPos a = points.get(i - 1);
			int dx = b.getX() - a.getX(), dz = b.getZ() - a.getZ(), dy = b.getY() - a.getY();
			int steps = Math.max(1, Math.max(Math.abs(dx), Math.abs(dz)));
			for (int t = 1; t <= steps; t++) {
				float f = t / (float) steps;
				BlockPos p = new BlockPos(a.getX() + Math.round(dx * f), a.getY() + Math.round(dy * f), a.getZ() + Math.round(dz * f));
				if (!p.equals(out.getLast())) out.add(p);
			}
		}
		return out;
	}

	/** Where the n-th customer in line stands (0 = at the counter), walking back along the path. */
	public static BlockPos queueSpot(List<BlockPos> points, int index) {
		List<BlockPos> blocks = blocks(points);
		return blocks.get(MathHelper.clamp(blocks.size() - 1 - index, 0, blocks.size() - 1));
	}

	/**
	 * The turns a customer walks through to reach queue place {@code index}: every path point that comes before
	 * their spot.
	 */
	public static List<BlockPos> routeTo(List<BlockPos> points, int index) {
		List<BlockPos> blocks = blocks(points);
		int spot = MathHelper.clamp(blocks.size() - 1 - index, 0, blocks.size() - 1);
		List<BlockPos> route = new ArrayList<>();
		int at = 0;
		for (BlockPos point : points) {
			int where = blocks.indexOf(point);
			if (where >= 0 && where < spot && where >= at) {
				route.add(point);
				at = where;
			}
		}
		return route;
	}

	/** The path points from the one nearest {@code from} back out to the entrance, for leaving. */
	public static List<BlockPos> routeOut(List<BlockPos> points, BlockPos from) {
		int nearest = 0;
		double best = Double.MAX_VALUE;
		for (int i = 0; i < points.size(); i++) {
			double d = points.get(i).getSquaredDistance(from);
			if (d < best) {
				best = d;
				nearest = i;
			}
		}
		List<BlockPos> route = new ArrayList<>();
		for (int i = nearest; i >= 0; i--) route.add(points.get(i));
		return route;
	}
}
