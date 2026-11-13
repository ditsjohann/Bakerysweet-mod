package com.bakery;

import com.bakery.food.BakeryFoods;
import com.bakery.food.Topping;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import com.bakery.review.VillagerCritic;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;

public final class BakeryEvents {
	/** Sweet Coins every new baker starts with, to buy their first kitchen from the catalog. */
	public static final int STARTING_COINS = 50;

	private BakeryEvents() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack topping = player.getStackInHand(hand);
			ItemStack bake = player.getOffHandStack();
			if (hand != Hand.MAIN_HAND || !isToppable(bake)) {
				return TypedActionResult.pass(topping);
			}
			Topping kind = Topping.fromItem(topping);
			if (kind == Topping.NONE) {
				return TypedActionResult.pass(topping);
			}
			if (BakeryFoods.topping(bake) != Topping.NONE) {
				if (!world.isClient) {
					player.sendMessage(Text.translatable("message.bakery.already_decorated"), true);
				}
				return TypedActionResult.fail(topping);
			}
			if (!world.isClient) {
				// Decorate one bake out of the offhand stack.
				ItemStack decorated = bake.split(1);
				decorated.set(BakeryComponents.TOPPING, kind.asString());
				decorated.set(BakeryComponents.QUALITY, Math.min(5, BakeryFoods.quality(decorated) + kind.bonus()));
				topping.decrementUnlessCreative(1, player);
				player.getInventory().offerOrDrop(decorated);
				world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.5f);
				player.sendMessage(Text.translatable("message.bakery.decorated", kind.displayName()), true);
			}
			return TypedActionResult.success(topping, world.isClient);
		});

		// Villagers are food critics: sneak + right-click one with a bake and they'll taste it.
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			ItemStack stack = player.getStackInHand(hand);
			if (!(entity instanceof VillagerEntity villager) || villager.isBaby() || hand != Hand.MAIN_HAND
					|| !player.isSneaking() || !VillagerCritic.canReview(stack)) {
				return ActionResult.PASS;
			}
			if (player instanceof ServerPlayerEntity serverPlayer) {
				VillagerCritic.review(serverPlayer, villager, stack);
			}
			return ActionResult.SUCCESS;
		});

		// Dressed as a Sweet Baker? Every two seconds is often enough to notice.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 40 != 0) return;
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (com.bakery.sweet.Outfit.sweetBaker(player)) {
					com.bakery.sweet.Stickers.award(player, com.bakery.sweet.Sticker.DRESS_UP);
				}
			}
		});

		// Every new baker starts with the Bakery Catalog and a little purse of Sweet Coins.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			if (player.getCommandTags().add("bakery_starter")) {
				player.getInventory().offerOrDrop(new ItemStack(BakeryItems.BAKERY_CATALOG));
				player.getInventory().offerOrDrop(new ItemStack(BakeryItems.PATH_RIBBON));
				player.getInventory().offerOrDrop(new ItemStack(BakeryItems.SWEET_COIN, STARTING_COINS));
				player.sendMessage(Text.translatable("message.bakery.welcome").formatted(Formatting.LIGHT_PURPLE), false);
			}
		});
	}

	private static boolean isToppable(ItemStack stack) {
		return stack.isOf(BakeryItems.CUPCAKE) || stack.isOf(BakeryItems.DONUT) || stack.isOf(BakeryItems.COOKIES)
				|| stack.isOf(BakeryItems.MACARONS) || stack.isOf(BakeryItems.CAKE);
	}
}
