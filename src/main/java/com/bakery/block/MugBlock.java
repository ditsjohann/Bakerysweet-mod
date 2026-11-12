package com.bakery.block;

import com.bakery.food.BakeryFoods;
import com.bakery.food.Drink;
import com.bakery.registry.BakeryItems;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** A mug on the table. Sip it right there, and the empty mug stays behind. */
public class MugBlock extends PlacedFoodBlock {
	public static final MapCodec<MugBlock> CODEC = createCodec(settings -> new MugBlock(settings, createCuboidShape(5, 0, 5, 11, 6, 11)));

	public MugBlock(Settings settings, VoxelShape shape) {
		super(settings, shape);
		setDefaultState(getDefaultState().with(DRINK, Drink.EMPTY));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(DRINK);
	}

	@Override
	protected boolean serve(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		if (state.get(DRINK) == Drink.EMPTY) {
			return false;
		}
		ItemStack stack = stored(world, pos);
		FoodComponent food = stack.get(DataComponentTypes.FOOD);
		if (food == null || !BakeryFoods.eatFromBlock(player, world, pos, stack, food)) {
			return false;
		}
		world.playSound(player, pos, SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.8f, 1f);
		if (!world.isClient) {
			world.setBlockState(pos, state.with(DRINK, Drink.EMPTY));
			if (world.getBlockEntity(pos) instanceof PlacedFoodBlockEntity mug) {
				mug.setStack(new ItemStack(BakeryItems.MUG));
			}
		}
		return true;
	}

	@Override
	protected ItemStack pickUpStack(BlockState state, ItemStack stored) {
		return state.get(DRINK) == Drink.EMPTY ? new ItemStack(BakeryItems.MUG) : stored.copyWithCount(1);
	}
}
