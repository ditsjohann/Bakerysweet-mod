package com.bakery.item;

import com.bakery.block.PlacedFoodBlock;
import com.bakery.block.PlacedFoodBlockEntity;
import com.bakery.food.BakeryFoods;
import com.bakery.food.Freshness;
import com.bakery.food.Perishable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/** A finished bake you can eat from your hand or set down as decoration (and still eat later). */
public class PlaceableFoodItem extends BlockItem implements Perishable {
	private final int shelfLife;

	public PlaceableFoodItem(Block block, Settings settings, int shelfLife) {
		super(block, settings);
		this.shelfLife = shelfLife;
	}

	@Override
	public int shelfLife() {
		return shelfLife;
	}

	@Override
	public boolean isBake() {
		return true;
	}

	/** Named after the item, not the block, so every drink in a mug keeps its own name. */
	@Override
	public String getTranslationKey() {
		return getOrCreateTranslationKey();
	}

	@Override
	public Text getName(ItemStack stack) {
		return BakeryFoods.name(stack, super.getName(stack));
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		BakeryFoods.inventoryTick(stack, world);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		BakeryFoods.appendTooltip(stack, tooltip);
		tooltip.add(Text.translatable("tooltip.bakery.placeable").formatted(net.minecraft.util.Formatting.DARK_GRAY));
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		ItemStack eaten = stack.copy();
		ItemStack result = super.finishUsing(stack, world, user);
		BakeryFoods.afterEating(eaten, world, user);
		return result;
	}

	@Override
	protected BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = super.getPlacementState(context);
		if (state == null) {
			return null;
		}
		return PlacedFoodBlock.withStackProperties(state, context.getStack());
	}

	@Override
	protected boolean postPlacement(BlockPos pos, World world, PlayerEntity player, ItemStack stack, BlockState state) {
		boolean result = super.postPlacement(pos, world, player, stack, state);
		if (!world.isClient && world.getBlockEntity(pos) instanceof PlacedFoodBlockEntity food) {
			ItemStack placed = stack.copyWithCount(1);
			Freshness.stamp(placed, world);
			food.setStack(placed);
		}
		return result;
	}
}
