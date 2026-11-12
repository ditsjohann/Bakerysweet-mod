package com.bakery.sweet;

import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/** Sugar cookie shapes, one per cookie cutter. */
public enum CookieShape {
	HEART("heart", () -> BakeryItems.HEART_CUTTER),
	STAR("star", () -> BakeryItems.STAR_CUTTER),
	BUNNY("bunny", () -> BakeryItems.BUNNY_CUTTER),
	FLOWER("flower", () -> BakeryItems.FLOWER_CUTTER);

	private final String name;
	private final Supplier<Item> cutter;

	CookieShape(String name, Supplier<Item> cutter) {
		this.name = name;
		this.cutter = cutter;
	}

	public String asString() {
		return name;
	}

	public Item cutter() {
		return cutter.get();
	}

	public Text displayName() {
		return Text.translatable("cookie_shape.bakery." + name);
	}

	public static CookieShape byName(String name) {
		for (CookieShape shape : values()) {
			if (shape.name.equals(name)) {
				return shape;
			}
		}
		return HEART;
	}

	public static CookieShape of(ItemStack stack) {
		return byName(stack.getOrDefault(BakeryComponents.COOKIE_SHAPE, "heart"));
	}

	public static CookieShape ofCutter(ItemStack stack) {
		for (CookieShape shape : values()) {
			if (stack.isOf(shape.cutter())) {
				return shape;
			}
		}
		return null;
	}
}
