package com.bakery.item;

import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.UseAction;

/** A glass of fresh juice from the Juicer, in whatever fruit flavor went in. */
public class JuiceItem extends BakeryItem {
	public JuiceItem(Settings settings, int shelfLife) {
		super(settings, shelfLife);
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
