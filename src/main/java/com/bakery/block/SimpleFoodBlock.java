package com.bakery.block;

import com.bakery.food.BakeryFoods;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** A single loaf, baguette or croissant. Eating it eats the whole thing. */
public class SimpleFoodBlock extends PlacedFoodBlock {
	public static final MapCodec<SimpleFoodBlock> CODEC = createCodec(settings -> new SimpleFoodBlock(settings, createCuboidShape(2, 0, 2, 14, 8, 14)));

	public SimpleFoodBlock(Settings settings, VoxelShape shape) {
		super(settings, shape);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected boolean serve(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		ItemStack stack = stored(world, pos);
		FoodComponent food = stack.get(DataComponentTypes.FOOD);
		if (food == null || !BakeryFoods.eatFromBlock(player, world, pos, stack, food)) {
			return false;
		}
		if (!world.isClient) {
			removeQuietly(world, pos);
		}
		return true;
	}
}
