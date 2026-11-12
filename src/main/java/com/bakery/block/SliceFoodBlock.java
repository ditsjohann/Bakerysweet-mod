package com.bakery.block;

import com.bakery.food.BakeryFoods;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** Pies (and cakes) that you eat one slice at a time, four slices in total. */
public class SliceFoodBlock extends PlacedFoodBlock {
	public static final MapCodec<SliceFoodBlock> CODEC = createCodec(settings -> new SliceFoodBlock(settings, createCuboidShape(1, 0, 1, 15, 6, 15), new FoodComponent.Builder().nutrition(4).saturationModifier(0.5f).build()));

	private final FoodComponent slice;

	public SliceFoodBlock(Settings settings, VoxelShape shape, FoodComponent slice) {
		super(settings, shape);
		this.slice = slice;
		setDefaultState(getDefaultState().with(BITES, 0));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(FLAVOR, BITES);
	}

	@Override
	protected boolean serve(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		ItemStack stack = stored(world, pos);
		if (!BakeryFoods.eatFromBlock(player, world, pos, stack, slice)) {
			return false;
		}
		if (!world.isClient) {
			int bites = state.get(BITES);
			if (bites >= 3) {
				removeQuietly(world, pos);
			} else {
				world.setBlockState(pos, state.with(BITES, bites + 1));
			}
		}
		return true;
	}

	@Override
	protected ItemStack pickUpStack(BlockState state, ItemStack stored) {
		return state.get(BITES) == 0 ? stored.copyWithCount(1) : null;
	}
}
