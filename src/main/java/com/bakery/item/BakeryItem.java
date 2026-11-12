package com.bakery.item;

import com.bakery.food.BakeryFoods;
import com.bakery.food.Perishable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.List;

/** Ingredients, doughs and raw bakes. Knows its shelf life and shows flavor, stars and freshness. */
public class BakeryItem extends Item implements Perishable {
	private final int shelfLife;
	private final boolean bake;

	public BakeryItem(Settings settings, int shelfLife, boolean bake) {
		super(settings);
		this.shelfLife = shelfLife;
		this.bake = bake;
	}

	public BakeryItem(Settings settings, int shelfLife) {
		this(settings, shelfLife, false);
	}

	@Override
	public int shelfLife() {
		return shelfLife;
	}

	@Override
	public boolean isBake() {
		return bake;
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
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		ItemStack eaten = stack.copy();
		ItemStack result = super.finishUsing(stack, world, user);
		BakeryFoods.afterEating(eaten, world, user);
		return result;
	}
}
