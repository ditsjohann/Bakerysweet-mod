package com.bakery.food;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Ways to make food last longer. The multiplier stretches whatever freshness is left at the moment it is applied. */
public enum Preservation {
	NONE("none", 1, Formatting.GRAY),
	WRAPPED("wrapped", 2, Formatting.YELLOW),
	CHILLED("chilled", 3, Formatting.AQUA),
	JARRED("jarred", 5, Formatting.LIGHT_PURPLE);

	private final String name;
	private final int multiplier;
	private final Formatting color;

	Preservation(String name, int multiplier, Formatting color) {
		this.name = name;
		this.multiplier = multiplier;
		this.color = color;
	}

	public String id() {
		return name;
	}

	public int multiplier() {
		return multiplier;
	}

	public Text displayName() {
		return Text.translatable("preservation.bakery." + name).formatted(color);
	}

	public static Preservation byName(String name) {
		for (Preservation preservation : values()) {
			if (preservation.name.equals(name)) {
				return preservation;
			}
		}
		return NONE;
	}
}
