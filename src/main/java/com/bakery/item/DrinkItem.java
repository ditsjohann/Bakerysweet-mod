package com.bakery.item;

import com.bakery.food.Drink;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;

/** A mug of something warm. Drink it, or put the mug down on a table. */
public class DrinkItem extends PlaceableFoodItem {
	private final Drink drink;

	public DrinkItem(Block mugBlock, Drink drink, Settings settings, int shelfLife) {
		super(mugBlock, settings, shelfLife);
		this.drink = drink;
	}

	public Drink drink() {
		return drink;
	}

	@Override
	public boolean isBake() {
		return false;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}

	@Override
	public SoundEvent getEatSound() {
		return SoundEvents.ENTITY_GENERIC_DRINK;
	}

	@Override
	public SoundEvent getDrinkSound() {
		return SoundEvents.ENTITY_GENERIC_DRINK;
	}
}
