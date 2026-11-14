package com.bakery.cafe;

import net.minecraft.text.Text;

/**
 * How far along a bakery is. It grows with every customer served, and each level unlocks new dishes customers
 * may ask for, more customers at once, pickier (flavour) orders, more things in the catalog and a counter perk:
 * <ul>
 *   <li>Cozy Corner: dine-in guests leave an extra coin on the table</li>
 *   <li>Café: guests waiting at a table stay patient longer</li>
 *   <li>Pâtisserie: customers keep coming in the evening</li>
 *   <li>Grand: rush hour (customers come twice as often) and every sale earns a coin more</li>
 * </ul>
 */
public enum CafeLevel {
	STALL(0, 1, 0f, 1.6f),
	CORNER(5, 2, 0.15f, 1.35f),
	CAFE(15, 3, 0.3f, 1.15f),
	PATISSERIE(35, 4, 0.4f, 1f),
	GRAND(70, 5, 0.5f, 1f);

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

	/** Dine-in guests leave one extra coin on the table. */
	public boolean tableTips() {
		return ordinal() >= CORNER.ordinal();
	}

	/** Guests waiting at a table lose patience more slowly. */
	public boolean patientGuests() {
		return ordinal() >= CAFE.ordinal();
	}

	/** Customers keep coming in the evening, until midnight. */
	public boolean eveningService() {
		return ordinal() >= PATISSERIE.ordinal();
	}

	/** Customers come twice as often, and every sale earns one coin more. */
	public boolean rushHour() {
		return this == GRAND;
	}

	/** The perks this level adds (lang keys), for the counter screen. */
	public java.util.List<String> perks() {
		return switch (this) {
			case STALL -> java.util.List.of("counter.bakery.perk.first");
			case CORNER -> java.util.List.of("counter.bakery.perk.table_tips");
			case CAFE -> java.util.List.of("counter.bakery.perk.patient");
			case PATISSERIE -> java.util.List.of("counter.bakery.perk.evening");
			case GRAND -> java.util.List.of("counter.bakery.perk.rush", "counter.bakery.perk.golden");
		};
	}

	public Text displayName() {
		return Text.translatable("cafe_level.bakery." + name().toLowerCase());
	}
}
