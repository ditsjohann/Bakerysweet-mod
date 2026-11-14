package com.bakery.deco;

import net.minecraft.util.shape.VoxelShape;

/** A {@link DecoBlock} that takes dye (tables, awnings ...). */
public class DyeableDecoBlock extends DecoBlock {
	public DyeableDecoBlock(Settings settings, VoxelShape northShape, boolean wallMounted) {
		super(settings, northShape, wallMounted);
	}

	@Override
	protected boolean dyeable() {
		return true;
	}
}
