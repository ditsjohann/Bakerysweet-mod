package com.bakery.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Bakery tools and ingredients with a short hint on how to use them. */
public class TooltipItem extends Item {
	private final String hint;

	public TooltipItem(Settings settings, String hint) {
		super(settings);
		this.hint = hint;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("tooltip.bakery." + hint).formatted(Formatting.GRAY));
	}
}
