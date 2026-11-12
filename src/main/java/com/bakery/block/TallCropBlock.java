package com.bakery.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.function.Supplier;

/**
 * A crop that grows up several blocks: corn reaches three blocks tall, peas climb to two.
 * Only the bottom block grows; the stalk above follows it. Right-click a ripe stalk to harvest it -
 * it drops the harvest, shrinks back down and grows again.
 */
public class TallCropBlock extends PlantBlock implements Fertilizable {
	public static final IntProperty AGE = Properties.AGE_3;
	public static final IntProperty SEGMENT = IntProperty.of("segment", 0, 2);
	private static final VoxelShape[] SHAPES = {
			Block.createCuboidShape(3, 0, 3, 13, 5, 13),
			Block.createCuboidShape(3, 0, 3, 13, 11, 13),
			Block.createCuboidShape(3, 0, 3, 13, 16, 13),
			Block.createCuboidShape(3, 0, 3, 13, 16, 13)
	};

	private final int maxHeight;
	private final Supplier<Item> produce;

	public TallCropBlock(Settings settings, int maxHeight, Supplier<Item> produce) {
		super(settings);
		this.maxHeight = maxHeight;
		this.produce = produce;
		setDefaultState(getDefaultState().with(AGE, 0).with(SEGMENT, 0));
	}

	@Override
	protected MapCodec<? extends PlantBlock> getCodec() {
		return createCodec(settings -> this);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE, SEGMENT);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(SEGMENT) > 0 ? SHAPES[3] : SHAPES[state.get(AGE)];
	}

	@Override
	protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
		return floor.isOf(Blocks.FARMLAND);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		if (state.get(SEGMENT) == 0) {
			return world.getBlockState(pos.down()).isOf(Blocks.FARMLAND);
		}
		BlockState below = world.getBlockState(pos.down());
		return below.isOf(this) && below.get(SEGMENT) == state.get(SEGMENT) - 1;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		return canPlaceAt(state, world, pos) ? state : Blocks.AIR.getDefaultState();
	}

	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return state.get(SEGMENT) == 0 && state.get(AGE) < 3;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getBaseLightLevel(pos, 0) >= 9 && random.nextInt(4) == 0) {
			grow(world, pos, state.get(AGE) + 1);
		}
	}

	/** Sets the bottom to {@code age} and grows (or trims) the stalk above to match. */
	private void grow(World world, BlockPos bottom, int age) {
		age = Math.min(3, age);
		int height = age >= 3 ? maxHeight : age >= 2 ? Math.min(2, maxHeight) : 1;
		for (int segment = 0; segment < maxHeight; segment++) {
			BlockPos p = bottom.up(segment);
			BlockState current = world.getBlockState(p);
			if (segment < height) {
				if (segment > 0 && !current.isOf(this) && !current.isAir()) {
					break;
				}
				world.setBlockState(p, getDefaultState().with(AGE, age).with(SEGMENT, segment), Block.NOTIFY_LISTENERS);
			} else if (current.isOf(this)) {
				world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
			}
		}
	}

	private BlockPos bottomOf(BlockPos pos, BlockState state) {
		return pos.down(state.get(SEGMENT));
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (state.get(AGE) < 3) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			BlockPos bottom = bottomOf(pos, state);
			Block.dropStack(world, pos, new ItemStack(produce.get(), maxHeight + world.random.nextInt(2)));
			world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1f, 1.2f);
			// Cut the stalk back so it grows again.
			for (int segment = maxHeight - 1; segment >= 1; segment--) {
				if (world.getBlockState(bottom.up(segment)).isOf(this)) {
					world.setBlockState(bottom.up(segment), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
				}
			}
			world.setBlockState(bottom, getDefaultState().with(AGE, 1), Block.NOTIFY_LISTENERS);
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return state.get(AGE) < 3;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		BlockPos bottom = bottomOf(pos, state);
		grow(world, bottom, world.getBlockState(bottom).get(AGE) + 1);
	}
}
