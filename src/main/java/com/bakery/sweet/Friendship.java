package com.bakery.sweet;

import com.bakery.cafe.CustomerEntity;
import com.bakery.cafe.Regular;
import com.bakery.food.BakeryFoods;
import com.bakery.registry.BakeryItems;
import com.bakery.review.BakeryReputation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Regulars remember you. Great service and gifts earn friendship hearts (up to 10);
 * at 5 and 10 hearts they give you something special.
 */
public final class Friendship {
	public static final int MAX = 10;
	/** regular + player -> world day of the last gift (one gift per regular per day). */
	private static final Map<String, Long> LAST_GIFT = new ConcurrentHashMap<>();

	private Friendship() {
	}

	public static int hearts(ServerPlayerEntity player, UUID regular) {
		return BakeryReputation.get(player.getServer()).of(player.getUuid()).friends.getOrDefault(regular, 0);
	}

	public static MutableText heartText(int hearts) {
		return Text.literal("♥".repeat(Math.max(0, hearts))).formatted(Formatting.LIGHT_PURPLE)
				.append(Text.literal("♥".repeat(Math.max(0, MAX - hearts))).formatted(Formatting.DARK_GRAY));
	}

	/** Adds friendship hearts and celebrates the milestones. Returns the new heart count. */
	public static int add(ServerPlayerEntity player, CustomerEntity customer, int amount) {
		Regular regular = customer.regular();
		BakeryReputation reputation = BakeryReputation.get(player.getServer());
		Map<UUID, Integer> friends = reputation.of(player.getUuid()).friends;
		int before = friends.getOrDefault(regular.id(), 0);
		int after = Math.clamp(before + amount, 0, MAX);
		if (after == before) return after;
		friends.put(regular.id(), after);
		reputation.markDirty();
		ServerWorld world = player.getServerWorld();
		if (before < 5 && after >= 5) {
			player.sendMessage(Text.translatable("message.bakery.friend.regular", regular.displayName()).formatted(Formatting.LIGHT_PURPLE), false);
			Coins.pay(player, 10);
			Stickers.award(player, Sticker.REGULAR);
			world.spawnParticles(ParticleTypes.HEART, customer.getX(), customer.getY() + 2, customer.getZ(), 6, 0.3, 0.2, 0.3, 0);
		}
		if (before < MAX && after >= MAX) {
			player.sendMessage(Text.translatable("message.bakery.friend.best", regular.displayName()).formatted(Formatting.GOLD), false);
			Item[] treasures = {BakeryItems.STARBLOOM_SEEDS, BakeryItems.CANDYFLOSS_SEEDS, BakeryItems.STAR_PETALS, BakeryItems.BUNNY_EARS, BakeryItems.CAT_EARS};
			ItemStack present = new ItemStack(treasures[world.random.nextInt(treasures.length)]);
			player.getInventory().offerOrDrop(GiftBoxItem.wrap(present, regular.name(), GiftBoxItem.RIBBONS[world.random.nextInt(GiftBoxItem.RIBBONS.length)]));
			Stickers.award(player, Sticker.BEST_FRIEND);
			world.playSound(null, customer.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 0.7f, 1.4f);
		}
		return after;
	}

	/** A regular receives a wrapped gift. Returns false if they already got one from this player today. */
	public static boolean giveGift(ServerPlayerEntity player, CustomerEntity customer, ItemStack gift) {
		ServerWorld world = player.getServerWorld();
		Regular regular = customer.regular();
		long day = world.getTimeOfDay() / 24000L;
		String key = regular.id() + "/" + player.getUuid();
		Long last = LAST_GIFT.get(key);
		if (last != null && last == day) {
			player.sendMessage(Text.translatable("message.bakery.friend.gift_today", regular.displayName()), true);
			world.playSound(null, customer.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1f, 1.5f);
			return false;
		}
		LAST_GIFT.put(key, day);
		ItemStack inside = WrappedGiftItem.contents(gift);
		boolean loved = !inside.isEmpty() && BakeryFoods.flavor(inside) == regular.favorite();
		gift.decrementUnlessCreative(1, player);
		int hearts = add(player, customer, loved ? 3 : 2);
		Coins.pay(player, 2 + world.random.nextInt(3));
		WrappedGiftItem.confetti(world, customer.getX(), customer.getEyeY(), customer.getZ());
		world.spawnParticles(ParticleTypes.HEART, customer.getX(), customer.getY() + 2, customer.getZ(), 5, 0.3, 0.2, 0.3, 0);
		world.playSound(null, customer.getBlockPos(), SoundEvents.ENTITY_VILLAGER_CELEBRATE, SoundCategory.NEUTRAL, 1f, 1.5f);
		player.sendMessage(Text.translatable(loved ? "message.bakery.friend.gift_loved" : "message.bakery.friend.gift", regular.displayName(), hearts, MAX)
				.formatted(Formatting.LIGHT_PURPLE), true);
		Stickers.award(player, Sticker.GIFT_GIVER);
		return true;
	}
}
