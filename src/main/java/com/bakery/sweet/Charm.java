package com.bakery.sweet;

import com.bakery.Bakery;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * How charming a bakery is: cozy decorations (anything in the bakery:cozy tag) around the counter.
 * 0-5 hearts. Charm makes customers come more often, wait longer and tip more.
 */
public final class Charm {
	public static final TagKey<Block> COZY = TagKey.of(RegistryKeys.BLOCK, Bakery.id("cozy"));
	public static final int RADIUS = 6;
	/** Decorations needed for each heart. Floors and wallpaper don't count, so these are pieces of furniture and trinkets. */
	private static final int[] STEPS = {2, 5, 9, 14, 20};

	private Charm() {
	}

	public static int count(BlockView world, BlockPos center) {
		int count = 0;
		for (BlockPos p : BlockPos.iterate(center.add(-RADIUS, -1, -RADIUS), center.add(RADIUS, 3, RADIUS))) {
			if (world.getBlockState(p).isIn(COZY)) count++;
		}
		return count;
	}

	public static int level(int count) {
		int level = 0;
		while (level < STEPS.length && count >= STEPS[level]) level++;
		return level;
	}

	/** Decorations needed for the next heart, or 0 at full charm. */
	public static int nextStep(int count) {
		for (int step : STEPS) {
			if (count < step) return step;
		}
		return 0;
	}
}
