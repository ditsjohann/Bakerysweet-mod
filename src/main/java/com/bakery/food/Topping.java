package com.bakery.food;

import com.bakery.registry.BakeryItems;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

/** Decorations for cakes, cupcakes, donuts and cookies. Each one adds stars to the bake. */
public enum Topping implements StringIdentifiable {
	NONE("none", 0),
	STRAWBERRIES("strawberries", 1),
	BLUEBERRIES("blueberries", 1),
	CREAM("cream", 1),
	SPRINKLES("sprinkles", 1),
	CHOCOLATE("chocolate", 1),
	ROSE_PETALS("rose_petals", 1),
	LAVENDER("lavender", 1),
	STAR_PETALS("star_petals", 2),
	RASPBERRIES("raspberries", 1),
	COCONUT_FLAKES("coconut_flakes", 1),
	MINT("mint", 1),
	LEMON_SLICES("lemon_slices", 1),
	CHERRIES("cherries", 1),
	CANDYFLOSS("candyfloss", 2);

	private final String name;
	private final int bonus;

	Topping(String name, int bonus) {
		this.name = name;
		this.bonus = bonus;
	}

	@Override
	public String asString() {
		return name;
	}

	public int bonus() {
		return bonus;
	}

	public Text displayName() {
		return Text.translatable("topping.bakery." + name);
	}

	public static Topping byName(String name) {
		for (Topping topping : values()) {
			if (topping.name.equals(name)) {
				return topping;
			}
		}
		return NONE;
	}

	public static Topping fromItem(ItemStack stack) {
		if (stack.isOf(BakeryItems.STRAWBERRY)) return STRAWBERRIES;
		if (stack.isOf(BakeryItems.BLUEBERRY)) return BLUEBERRIES;
		if (stack.isOf(BakeryItems.WHIPPED_CREAM)) return CREAM;
		if (stack.isOf(BakeryItems.SPRINKLES)) return SPRINKLES;
		if (stack.isOf(BakeryItems.CHOCOLATE_BAR)) return CHOCOLATE;
		if (stack.isOf(BakeryItems.ROSE_PETALS) || stack.isOf(BakeryItems.CANDIED_PETALS)) return ROSE_PETALS;
		if (stack.isOf(BakeryItems.LAVENDER)) return LAVENDER;
		if (stack.isOf(BakeryItems.STAR_PETALS)) return STAR_PETALS;
		if (stack.isOf(BakeryItems.RASPBERRY)) return RASPBERRIES;
		if (stack.isOf(BakeryItems.COCONUT_FLAKES)) return COCONUT_FLAKES;
		if (stack.isOf(BakeryItems.MINT_LEAVES)) return MINT;
		if (stack.isOf(BakeryItems.LEMON)) return LEMON_SLICES;
		if (stack.isOf(BakeryItems.CHERRIES)) return CHERRIES;
		if (stack.isOf(BakeryItems.CANDYFLOSS)) return CANDYFLOSS;
		return NONE;
	}
}
