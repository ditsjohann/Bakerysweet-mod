package com.bakery.sweet;

import com.bakery.registry.BakeryItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/** The six pastel icings: piped onto sugar cookies and spread on cakes at the Cake Turntable. */
public enum Icing {
	PINK("pink", 0xF7A8C6, () -> BakeryItems.PINK_ICING),
	MINT("mint", 0xA8E6CF, () -> BakeryItems.MINT_ICING),
	LILAC("lilac", 0xCDB4F0, () -> BakeryItems.LILAC_ICING),
	LEMON("lemon", 0xFFEE99, () -> BakeryItems.LEMON_ICING),
	SKY("sky", 0xA8D8F7, () -> BakeryItems.SKY_ICING),
	WHITE("white", 0xFFFAF2, () -> BakeryItems.WHITE_ICING),
	CHOCOLATE("chocolate", 0x8A5A3C, () -> BakeryItems.CHOCOLATE_ICING),
	STRAWBERRY("strawberry", 0xF0607E, () -> BakeryItems.STRAWBERRY_ICING),
	PEACH("peach", 0xFFC4A0, () -> BakeryItems.PEACH_ICING),
	BLUEBERRY("blueberry", 0x8C9BE8, () -> BakeryItems.BLUEBERRY_ICING),
	MATCHA("matcha", 0xB8DC8C, () -> BakeryItems.MATCHA_ICING),
	CARAMEL("caramel", 0xDDA463, () -> BakeryItems.CARAMEL_ICING),
	GOLDEN("golden", 0xF2C94C, () -> BakeryItems.GOLDEN_ICING),
	LICORICE("licorice", 0x5A4C5E, () -> BakeryItems.LICORICE_ICING);

	private final String name;
	private final int color;
	private final Supplier<Item> bag;

	Icing(String name, int color, Supplier<Item> bag) {
		this.name = name;
		this.color = color;
		this.bag = bag;
	}

	public String asString() {
		return name;
	}

	public int color() {
		return color;
	}

	public Item bag() {
		return bag.get();
	}

	public Text displayName() {
		return Text.translatable("icing.bakery." + name);
	}

	public static Icing of(ItemStack stack) {
		for (Icing icing : values()) {
			if (stack.isOf(icing.bag())) {
				return icing;
			}
		}
		return null;
	}

	public static Icing byColor(int color) {
		for (Icing icing : values()) {
			if (icing.color == color) {
				return icing;
			}
		}
		return null;
	}
}
