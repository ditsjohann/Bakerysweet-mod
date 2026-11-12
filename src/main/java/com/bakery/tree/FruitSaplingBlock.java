package com.bakery.tree;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.function.Supplier;

/** Grows into a palm (on sand or dirt) or a lemon tree. Bone meal works. */
public class FruitSaplingBlock extends PlantBlock implements Fertilizable {
	public static final IntProperty STAGE = Properties.STAGE;
	private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 12, 14);

	private final TreeShapes.Kind kind;
	private final Supplier<WoodSet> wood;
	private final Supplier<Block> fruit;

	public FruitSaplingBlock(Settings settings, TreeShapes.Kind kind, Supplier<WoodSet> wood, Supplier<Block> fruit) {
		super(settings);
		this.kind = kind;
		this.wood = wood;
		this.fruit = fruit;
		setDefaultState(getDefaultState().with(STAGE, 0));
	}

	@Override
	protected MapCodec<? extends PlantBlock> getCodec() {
		return createCodec(settings -> this);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(STAGE);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return super.canPlantOnTop(floor, world, pos) || (kind == TreeShapes.Kind.PALM && floor.isIn(BlockTags.SAND));
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getLightLevel(pos.up()) >= 9 && random.nextInt(7) == 0) {
			advance(world, pos, state, random);
		}
	}

	private void advance(ServerWorld world, BlockPos pos, BlockState state, Random random) {
		if (state.get(STAGE) == 0) {
			world.setBlockState(pos, state.cycle(STAGE), Block.NO_REDRAW);
		} else {
			world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NO_REDRAW);
			if (!TreeShapes.grow(kind, world, pos, random, wood.get(), fruit.get(), Block.NOTIFY_ALL)) {
				world.setBlockState(pos, state, Block.NO_REDRAW);
			}
		}
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return world.random.nextFloat() < 0.45f;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		advance(world, pos, state, random);
	}
}
