package com.bakery.cafe;

import net.minecraft.text.Text;

/**
 * How far along a bakery is. It grows with every customer served, and each level unlocks new dishes customers
 * may ask for, more customers at once, pickier (flavour) orders and more things in the catalog.
 */
public enum CafeLevel {
	STALL(0, 1, 0f, 1.6f),
	CORNER(5, 2, 0.15f, 1.35f),
	CAFE(15, 2, 0.3f, 1.15f),
	PATISSERIE(35, 3, 0.4f, 1f),
	GRAND(70, 3, 0.5f, 1f);

	private final int served;
	private final int maxCustomers;
	private final float flavorChance;
	private final float patience;

	CafeLevel(int served, int maxCustomers, float flavorChance, float patience) {
		this.served = served;
		this.maxCustomers = maxCustomers;
		this.flavorChance = flavorChance;
		this.patience = patience;
	}

	public static CafeLevel of(int served) {
		CafeLevel level = STALL;
		for (CafeLevel l : values()) {
			if (served >= l.served) level = l;
		}
		return level;
	}

	public int number() {
		return ordinal() + 1;
	}

	/** Customers served to reach this level. */
	public int served() {
		return served;
	}

	public CafeLevel next() {
		return this == GRAND ? null : values()[ordinal() + 1];
	}

	public int maxCustomers() {
		return maxCustomers;
	}

	/** Chance a customer asks for a particular flavour. */
	public float flavorChance() {
		return flavorChance;
	}

	/** Patience multiplier: new bakeries get very patient customers. */
	public float patience() {
		return patience;
	}

	public Text displayName() {
		return Text.translatable("cafe_level.bakery." + name().toLowerCase());
	}
}
