package com.bakery.item;

import com.bakery.food.Flavor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** A bottle of coffee syrup with a pump: four pumps, each flavours one drink at the syrup rack. Also flavours dough. */
public class SyrupItem extends Item {
	public static final int PUMPS = 4;
	private final Flavor flavor;

	public SyrupItem(Flavor flavor, Settings settings) {
		super(settings.maxDamage(PUMPS));
		this.flavor = flavor;
	}

	public Flavor flavor() {
		return flavor;
	}

	public static int pumpsLeft(ItemStack stack) {
		return stack.getMaxDamage() - stack.getDamage();
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("tooltip.bakery.syrup_pumps", pumpsLeft(stack), PUMPS).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.bakery.syrup_use").formatted(Formatting.DARK_GRAY));
	}
}
