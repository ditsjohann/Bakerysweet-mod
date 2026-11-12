package com.bakery.sweet;

import com.bakery.registry.BakeryItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;

/** The Sweet Baker outfit: any bakery hat plus the Frilly Apron. Customers love it and tip a little extra. */
public final class Outfit {
	public static final int TIP = 1;

	private Outfit() {
	}

	public static boolean sweetBaker(PlayerEntity player) {
		return player.getEquippedStack(EquipmentSlot.HEAD).getItem() instanceof HatItem
				&& player.getEquippedStack(EquipmentSlot.CHEST).isOf(BakeryItems.FRILLY_APRON);
	}
}
