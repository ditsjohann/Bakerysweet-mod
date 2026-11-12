package com.bakery.food;

/** An item that starts going stale the moment it is made. */
public interface Perishable {
	/** Shelf life in ticks, or 0 for food that never spoils. */
	int shelfLife();

	/** Whether stars and toppings matter for this item (finished bakes, not raw ingredients). */
	default boolean isBake() {
		return false;
	}
}
