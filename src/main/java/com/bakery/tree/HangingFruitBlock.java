package com.bakery.tree;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
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

/** A coconut or lemon hanging under the leaves. Ripens over time; right-click a ripe one to pick it and it grows back. */
public class HangingFruitBlock extends Block implements Fertilizable {
	public static final IntProperty AGE = Properties.AGE_2;
	private static final VoxelShape[] SHAPES = {
			Block.createCuboidShape(6, 11, 6, 10, 16, 10),
			Block.createCuboidShape(5, 8, 5, 11, 16, 11),
			Block.createCuboidShape(4, 6, 4, 12, 16, 12)
	};

	private final Supplier<Item> fruit;

	public HangingFruitBlock(Settings settings, Supplier<Item> fruit) {
		super(settings);
		this.fruit = fruit;
		setDefaultState(getDefaultState().with(AGE, 0));
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return createCodec(settings -> this);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[state.get(AGE)];
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return canHangUnder(world.getBlockState(pos.up()));
	}

	/** Fruit hangs from leaves, or from wood (like coconuts under a palm's crown). */
	public static boolean canHangUnder(BlockState above) {
		return above.isIn(BlockTags.LEAVES) || above.isIn(BlockTags.LOGS);
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		return direction == Direction.UP && !canPlaceAt(state, world, pos) ? Blocks.AIR.getDefaultState() : state;
	}

	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return state.get(AGE) < 2;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (random.nextInt(4) == 0) {
			world.setBlockState(pos, state.cycle(AGE), Block.NOTIFY_LISTENERS);
		}
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (state.get(AGE) < 2) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			Block.dropStack(world, pos, new ItemStack(fruit.get(), 1 + world.random.nextInt(2)));
			world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1f, 0.8f + world.random.nextFloat() * 0.4f);
			world.setBlockState(pos, state.with(AGE, 0), Block.NOTIFY_LISTENERS);
			if (player instanceof net.minecraft.server.network.ServerPlayerEntity picker) {
				com.bakery.sweet.Stickers.award(picker, com.bakery.sweet.Sticker.ORCHARD);
			}
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return state.get(AGE) < 2;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		world.setBlockState(pos, state.cycle(AGE), Block.NOTIFY_LISTENERS);
	}
}
