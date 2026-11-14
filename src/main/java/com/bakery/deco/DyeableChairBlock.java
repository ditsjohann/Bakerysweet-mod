package com.bakery.deco;

import net.minecraft.util.shape.VoxelShape;

/** A chair or bench whose cushion (or paint) takes dye. */
public class DyeableChairBlock extends ChairBlock {
	public DyeableChairBlock(Settings settings, VoxelShape northShape) {
		super(settings, northShape);
	}

	@Override
	protected boolean dyeable() {
		return true;
	}
}
