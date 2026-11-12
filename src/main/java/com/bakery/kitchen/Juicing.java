package com.bakery.kitchen;

import com.bakery.food.Flavor;
import com.bakery.registry.BakeryItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Set;

/** Which fruits can be juiced or turned into jam. */
public final class Juicing {
	private static Set<Item> fruits;

	private Juicing() {
	}

	public static boolean isFruit(ItemStack stack) {
		if (fruits == null) {
			fruits = Set.of(BakeryItems.STRAWBERRY, BakeryItems.BLUEBERRY, BakeryItems.RASPBERRY, BakeryItems.LEMON, BakeryItems.ORANGE,
					BakeryItems.CHERRIES, BakeryItems.PEACH, BakeryItems.MANGO, BakeryItems.PLUM, BakeryItems.PINEAPPLE, BakeryItems.CRANBERRY,
					BakeryItems.RHUBARB);
		}
		return fruits.contains(stack.getItem()) && Flavor.fromIngredient(stack) != null;
	}
}
