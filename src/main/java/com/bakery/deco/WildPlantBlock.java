package com.bakery.deco;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A wild plant (flower, fern, bush, mushrooms...) that grows on grass or dirt, like vanilla flowers. */
public class WildPlantBlock extends PlantBlock {
	private final VoxelShape shape;

	public WildPlantBlock(Settings settings, VoxelShape shape) {
		super(settings);
		this.shape = shape;
	}

	@Override
	protected MapCodec<? extends PlantBlock> getCodec() {
		return createCodec(settings -> this);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		Vec3d offset = state.getModelOffset(world, pos);
		return shape.offset(offset.x, offset.y, offset.z);
	}
}
