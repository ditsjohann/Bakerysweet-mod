package com.bakery.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import java.util.function.Supplier;

/** Strawberries, blueberries, tea, vanilla, lavender, roses and the glowing starbloom. Four growth stages each. */
public class BakeryCropBlock extends CropBlock {
	public static final MapCodec<BakeryCropBlock> CODEC = createCodec(settings -> new BakeryCropBlock(settings, () -> Items.WHEAT_SEEDS));
	public static final IntProperty AGE = Properties.AGE_3;
	private static final VoxelShape[] SHAPES = {
			Block.createCuboidShape(0, 0, 0, 16, 3, 16),
			Block.createCuboidShape(0, 0, 0, 16, 7, 16),
			Block.createCuboidShape(0, 0, 0, 16, 11, 16),
			Block.createCuboidShape(0, 0, 0, 16, 14, 16)
	};

	private final Supplier<ItemConvertible> seeds;

	public BakeryCropBlock(Settings settings, Supplier<ItemConvertible> seeds) {
		super(settings);
		this.seeds = seeds;
	}

	@Override
	public MapCodec<? extends CropBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected IntProperty getAgeProperty() {
		return AGE;
	}

	@Override
	public int getMaxAge() {
		return 3;
	}

	@Override
	protected ItemConvertible getSeedsItem() {
		return seeds.get();
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[getAge(state)];
	}
}
