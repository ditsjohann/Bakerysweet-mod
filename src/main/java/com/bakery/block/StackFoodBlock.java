package com.bakery.block;

import com.bakery.food.BakeryFoods;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** Cupcakes, donuts, cookies and macarons: several of the same flavor share one block. */
public class StackFoodBlock extends PlacedFoodBlock {
	public static final MapCodec<StackFoodBlock> CODEC = createCodec(settings -> new StackFoodBlock(settings, createCuboidShape(2, 0, 2, 14, 8, 14), 4));

	private final int maxCount;

	public StackFoodBlock(Settings settings, VoxelShape shape, int maxCount) {
		super(settings, shape);
		this.maxCount = maxCount;
		setDefaultState(getDefaultState().with(COUNT, 1));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FLAVOR, COUNT);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (stack.isOf(asItem()) && BakeryFoods.flavor(stack) == state.get(FLAVOR) && state.get(COUNT) < maxCount) {
			if (!world.isClient) {
				world.setBlockState(pos, state.with(COUNT, state.get(COUNT) + 1));
				stack.decrementUnlessCreative(1, player);
				world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 0.7f, 1.3f);
			}
			return ItemActionResult.success(world.isClient);
		}
		return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
	}

	@Override
	protected boolean serve(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		ItemStack stack = stored(world, pos);
		FoodComponent food = stack.get(DataComponentTypes.FOOD);
		if (food == null || !BakeryFoods.eatFromBlock(player, world, pos, stack, food)) {
			return false;
		}
		if (!world.isClient) {
			int count = state.get(COUNT);
			if (count <= 1) {
				removeQuietly(world, pos);
			} else {
				world.setBlockState(pos, state.with(COUNT, count - 1));
			}
		}
		return true;
	}

	@Override
	protected ItemStack pickUpStack(BlockState state, ItemStack stored) {
		return stored.copyWithCount(state.get(COUNT));
	}
}
