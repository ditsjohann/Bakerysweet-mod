package com.bakery.food;

import net.minecraft.util.StringIdentifiable;

/** What is inside a placed mug. Only used for the block state and the liquid tint. */
public enum Drink implements StringIdentifiable {
	EMPTY("empty", 0xFFFFFF),
	MATCHA_LATTE("matcha_latte", 0x92C25E),
	HOT_CHOCOLATE("hot_chocolate", 0x6A3B22),
	STRAWBERRY_MILK("strawberry_milk", 0xF7AFC6),
	LAVENDER_TEA("lavender_tea", 0xB89BDD),
	ROSE_TEA("rose_tea", 0xEC8EA6),
	STARLIGHT_TEA("starlight_tea", 0xFFE79A),
	LEMONADE("lemonade", 0xFFF29A),
	COCONUT_MILK("coconut_milk", 0xF8F4EC),
	MINT_TEA("mint_tea", 0x9FDDB5),
	PINEAPPLE_SMOOTHIE("pineapple_smoothie", 0xFFD66B),
	COFFEE("coffee", 0x4A2C1C),
	LATTE("latte", 0xC8996A);

	private final String name;
	private final int color;

	Drink(String name, int color) {
		this.name = name;
		this.color = color;
	}

	@Override
	public String asString() {
		return name;
	}

	public int color() {
		return color;
	}
}
