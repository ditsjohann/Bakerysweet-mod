package com.bakery.shop;

import net.minecraft.item.ItemStack;

/** Something near the counter that customers can help themselves from (display case, stands, shelves, cloches). */
public interface SelfServe {
	/** Takes out something that fills this order, or EMPTY. */
	ItemStack takeMatching(Order order);
}
