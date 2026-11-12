package com.bakery.sweet;

import com.bakery.item.BakeryItem;
import com.bakery.registry.BakeryComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** A sugar cookie cut with a cookie cutter (raw or baked). Baked ones can be iced with an icing bag. */
public class SugarCookieItem extends BakeryItem {
	public SugarCookieItem(Settings settings, int shelfLife, boolean bake) {
		super(settings, shelfLife, bake);
	}

	@Override
	public Text getName(ItemStack stack) {
		Text shaped = Text.translatable(getTranslationKey() + ".shaped", CookieShape.of(stack).displayName());
		return com.bakery.food.BakeryFoods.name(stack, shaped);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		super.appendTooltip(stack, context, tooltip, type);
		Integer icing = stack.get(BakeryComponents.ICING);
		if (icing != null) {
			Icing kind = Icing.byColor(icing);
			tooltip.add(Text.translatable("tooltip.bakery.iced", kind == null ? Text.literal("?") : kind.displayName()).withColor(icing));
		} else if (isBake()) {
			tooltip.add(Text.translatable("tooltip.bakery.ice_me").formatted(Formatting.GRAY, Formatting.ITALIC));
		}
	}
}
