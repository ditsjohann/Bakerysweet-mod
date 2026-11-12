package com.bakery.sweet;

import com.bakery.registry.BakeryItems;
import com.bakery.review.BakeryReputation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/** Sweet Coins: what customers pay with and what the Bakery Catalog takes. */
public final class Coins {
	public static final int PIGGY_BANK = 100;

	private Coins() {
	}

	/** Gives coins earned by baking (counts towards the Piggy Bank sticker). */
	public static void pay(ServerPlayerEntity player, int amount) {
		if (amount <= 0) return;
		player.getInventory().offerOrDrop(new ItemStack(BakeryItems.SWEET_COIN, amount));
		if (player.getServer() == null) return;
		BakeryReputation reputation = BakeryReputation.get(player.getServer());
		BakeryReputation.Stats stats = reputation.of(player.getUuid());
		stats.coinsEarned += amount;
		reputation.markDirty();
		if (stats.coinsEarned >= PIGGY_BANK) Stickers.award(player, Sticker.PIGGY_BANK);
	}

	public static int count(PlayerEntity player) {
		return player.getInventory().count(BakeryItems.SWEET_COIN);
	}

	/** Takes coins out of the inventory; false (and nothing taken) if there are not enough. */
	public static boolean take(PlayerEntity player, int amount) {
		if (player.getAbilities().creativeMode) return true;
		if (count(player) < amount) return false;
		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size() && amount > 0; i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isOf(BakeryItems.SWEET_COIN)) {
				int taken = Math.min(amount, stack.getCount());
				stack.decrement(taken);
				amount -= taken;
			}
		}
		inventory.markDirty();
		return true;
	}
}
