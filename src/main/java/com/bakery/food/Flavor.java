package com.bakery.food;

import com.bakery.registry.BakeryItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

/** The flavor a dough carries all the way to the finished bake. Also tints frosting, glaze and fillings. */
public enum Flavor implements StringIdentifiable {
	PLAIN("plain", 0xF6E7C8),
	VANILLA("vanilla", 0xFFF3CF),
	CHOCOLATE("chocolate", 0x7A4A2E),
	STRAWBERRY("strawberry", 0xFF8DB0),
	BLUEBERRY("blueberry", 0x8592E6),
	MATCHA("matcha", 0x9BC46A),
	LAVENDER("lavender", 0xC6A8EC),
	ROSE("rose", 0xF7A3BC),
	STARBLOOM("starbloom", 0xFFE47E),
	LEMON("lemon", 0xFFEE7A),
	COCONUT("coconut", 0xF4EEE2),
	RASPBERRY("raspberry", 0xEC4F78),
	MINT("mint", 0xA6E8C8),
	PINEAPPLE("pineapple", 0xFFD25A),
	CHERRY("cherry", 0xD8304A),
	ORANGE("orange", 0xFFA43A),
	PEACH("peach", 0xFFBE96),
	MANGO("mango", 0xFFC23C),
	PLUM("plum", 0x8E4FA8),
	CRANBERRY("cranberry", 0xC21E46),
	PEANUT("peanut", 0xD9A066),
	COTTON_CANDY("cotton_candy", 0xFFB8E0),
	HIBISCUS("hibiscus", 0xE8457A),
	SWEET_POTATO("sweet_potato", 0xE88A4A),
	ZUCCHINI("zucchini", 0x8CBF5A),
	RHUBARB("rhubarb", 0xE85A78),
	CORN("corn", 0xF6D45A),
	GARDEN("garden", 0x9CCB6A);

	private final String name;
	private final int color;

	Flavor(String name, int color) {
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

	public Text displayName() {
		return Text.translatable("flavor.bakery." + name);
	}

	public static Flavor byName(String name) {
		for (Flavor flavor : values()) {
			if (flavor.name.equals(name)) {
				return flavor;
			}
		}
		return PLAIN;
	}

	/** Which flavor an ingredient adds to a mixing bowl, or null if it is not a flavoring. */
	public static Flavor fromIngredient(ItemStack stack) {
		if (stack.isOf(BakeryItems.VANILLA_POD)) return VANILLA;
		if (stack.isOf(BakeryItems.CHOCOLATE_BAR) || stack.isOf(Items.COCOA_BEANS)) return CHOCOLATE;
		if (stack.isOf(BakeryItems.STRAWBERRY)) return STRAWBERRY;
		if (stack.isOf(BakeryItems.BLUEBERRY)) return BLUEBERRY;
		if (stack.isOf(BakeryItems.MATCHA_POWDER)) return MATCHA;
		if (stack.isOf(BakeryItems.LAVENDER)) return LAVENDER;
		if (stack.isOf(BakeryItems.ROSE_PETALS)) return ROSE;
		if (stack.isOf(BakeryItems.STAR_PETALS)) return STARBLOOM;
		if (stack.isOf(BakeryItems.LEMON)) return LEMON;
		if (stack.isOf(BakeryItems.COCONUT_FLAKES)) return COCONUT;
		if (stack.isOf(BakeryItems.RASPBERRY)) return RASPBERRY;
		if (stack.isOf(BakeryItems.MINT_LEAVES)) return MINT;
		if (stack.isOf(BakeryItems.PINEAPPLE)) return PINEAPPLE;
		if (stack.isOf(BakeryItems.CHERRIES)) return CHERRY;
		if (stack.isOf(BakeryItems.ORANGE)) return ORANGE;
		if (stack.isOf(BakeryItems.PEACH)) return PEACH;
		if (stack.isOf(BakeryItems.MANGO)) return MANGO;
		if (stack.isOf(BakeryItems.PLUM)) return PLUM;
		if (stack.isOf(BakeryItems.CRANBERRY)) return CRANBERRY;
		if (stack.isOf(BakeryItems.PEANUT) || stack.isOf(BakeryItems.PEANUT_BUTTER)) return PEANUT;
		if (stack.isOf(BakeryItems.CANDYFLOSS)) return COTTON_CANDY;
		if (stack.isOf(BakeryItems.HIBISCUS)) return HIBISCUS;
		if (stack.isOf(BakeryItems.SWEET_POTATO)) return SWEET_POTATO;
		if (stack.isOf(BakeryItems.ZUCCHINI)) return ZUCCHINI;
		if (stack.isOf(BakeryItems.RHUBARB)) return RHUBARB;
		if (stack.isOf(BakeryItems.CORN) || stack.isOf(BakeryItems.CORNMEAL)) return CORN;
		if (stack.isOf(BakeryItems.PEA_POD)) return GARDEN;
		return null;
	}
}
