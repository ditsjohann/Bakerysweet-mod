package com.bakery.shop;

import com.bakery.cafe.CafeLevel;
import com.bakery.cafe.CustomerEntity;
import com.bakery.cafe.Regular;
import com.bakery.food.BakeryFoods;
import com.bakery.food.Flavor;
import com.bakery.food.Freshness;
import com.bakery.network.ReviewPayload;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryItems;
import com.bakery.review.BakeryReputation;
import com.bakery.review.VillagerCritic;
import com.bakery.sweet.Coins;
import com.bakery.sweet.Friendship;
import com.bakery.sweet.Outfit;
import com.bakery.sweet.Sticker;
import com.bakery.sweet.Stickers;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Serving customers by hand: taste, pay, friendship, and the little celebrations. */
public final class ShopManager {
	private static final Map<UUID, Integer> STREAKS = new ConcurrentHashMap<>();

	private ShopManager() {
	}

	public static int streak(UUID player) {
		return STREAKS.getOrDefault(player, 0);
	}

	private static ItemStack gift(ServerWorld world) {
		net.minecraft.item.Item[] gifts = {BakeryItems.STARBLOOM_SEEDS, BakeryItems.CANDYFLOSS_SEEDS, BakeryItems.STAR_PETALS,
				BakeryItems.PINEAPPLE_SEEDS, BakeryItems.CRANBERRY_SEEDS, BakeryItems.VANILLA_POD,
				BakeryBlocks.BLOSSOM.sapling.asItem(), BakeryBlocks.MANGOWOOD.sapling.asItem(), BakeryBlocks.PLUMWOOD.sapling.asItem()};
		ItemStack gift = new ItemStack(gifts[world.random.nextInt(gifts.length)]);
		if (gift.isOf(BakeryItems.STAR_PETALS) || gift.isOf(BakeryItems.VANILLA_POD)) gift.setCount(2);
		return gift;
	}

	/** 1-5 stars from quality, freshness and this regular's taste. */
	public static int rate(Regular regular, ItemStack stack, long now) {
		Flavor flavor = BakeryFoods.flavor(stack);
		double score = BakeryFoods.quality(stack);
		switch (Freshness.stage(stack, now)) {
			case GOOD -> score -= 0.5;
			case STALE -> score -= 1.5;
			case SPOILED -> score -= 5;
			default -> {
			}
		}
		if (flavor == regular.favorite()) score += 1;
		if (flavor == regular.disliked()) score -= 1;
		if (flavor == Flavor.STARBLOOM) score += 0.5;
		if (Boolean.TRUE.equals(stack.get(com.bakery.registry.BakeryComponents.HOMEMADE))) score += 0.5;
		return Math.clamp(Math.round(score), 1, 5);
	}

	/** The player hands something to a customer. Returns true if the click was handled. */
	public static boolean serve(ServerPlayerEntity player, CustomerEntity customer, ItemStack stack) {
		ServerWorld world = player.getServerWorld();
		if (customer.counter() == null || !(world.getBlockEntity(customer.counter()) instanceof CounterBlockEntity counter)) {
			return false;
		}
		Order order = counter.find(customer.getUuid());
		if (order == null) {
			return false;
		}
		Regular regular = customer.regular();
		if (!Menu.matches(order, stack)) {
			player.sendMessage(Text.translatable("message.bakery.shop.wrong", regular.displayName(), order.wanted.getName()).formatted(Formatting.GOLD), true);
			world.playSound(null, customer.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 0.8f, 1.5f);
			return true;
		}

		long now = world.getTime();
		int stars = rate(regular, stack, now);
		boolean favorite = BakeryFoods.flavor(stack) == regular.favorite();
		int price = Menu.price(order, stars, favorite);
		// Farm to table: a dish made only from what the baker grew and made sells for half again as much.
		if (Boolean.TRUE.equals(stack.get(com.bakery.registry.BakeryComponents.HOMEMADE))) {
			int extra = Math.max(1, price / 2);
			price += extra;
			player.sendMessage(Text.translatable("message.bakery.shop.homemade", extra).formatted(Formatting.GREEN), true);
		}

		// Serving streak: keep the 4-5 star dishes coming and the tips grow.
		int streak = stars >= 4 ? STREAKS.merge(player.getUuid(), 1, Integer::sum) : 0;
		if (stars < 4) STREAKS.remove(player.getUuid());
		int bonus = Math.min(3, streak / 2);
		price += bonus;
		if (streak >= 2) {
			player.sendMessage(Text.translatable("message.bakery.shop.streak", streak, bonus).formatted(Formatting.LIGHT_PURPLE), true);
		}
		// A charming bakery and the Sweet Baker outfit both earn a little extra. Diners leave the charm tip on the table.
		int charmTip = counter.getCharm() / 2 + (stars == 5 ? 1 : 0);
		int outfitTip = Outfit.sweetBaker(player) ? Outfit.TIP : 0;
		price += outfitTip + (order.dineIn ? 0 : charmTip);
		// Level perks: a Grand bakery earns a coin more on every sale, and from Cozy Corner diners tip a coin extra.
		CafeLevel perks = counter.getLevel();
		if (perks.rushHour()) price += 1;
		if (perks.tableTips()) charmTip += 1;

		ItemStack tasted = stack.copyWithCount(1);
		stack.decrementUnlessCreative(1, player);
		Coins.pay(player, price);
		int hearts = Friendship.add(player, customer, stars >= 4 ? 1 : 0);
		BakeryReputation reputation = BakeryReputation.get(player.getServer());
		BakeryReputation.Stats stats = reputation.of(player.getUuid());
		CafeLevel before = CafeLevel.of(stats.served);
		stats.served++;
		reputation.markDirty();
		Stickers.award(player, Sticker.FIRST_CUSTOMER);
		CafeLevel after = CafeLevel.of(stats.served);
		if (after != before) levelUp(player, after);

		// A delighted customer sometimes leaves a little present.
		if (stars == 5 && world.random.nextFloat() < 0.3f) {
			ItemStack gift = gift(world);
			player.getInventory().offerOrDrop(gift);
			player.sendMessage(Text.translatable("message.bakery.shop.gift", regular.displayName(), gift.getName()).formatted(Formatting.GOLD), false);
		}
		world.spawnParticles(stars >= 4 ? ParticleTypes.HEART : stars >= 3 ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ANGRY_VILLAGER,
				customer.getX(), customer.getY() + 2.1, customer.getZ(), stars >= 4 ? stars : 3, 0.3, 0.15, 0.3, 0);
		world.playSound(null, customer.getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 0.7f, 1.6f);
		world.playSound(null, customer.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.3f);

		counter.maybeReview(world, Review.served(regular.name(), net.minecraft.registry.Registries.ITEM.getId(tasted.getItem()).toString(), stars,
				order.patienceFraction(), counter.getCharm(), world.getTimeOfDay() / 24000L, world.random));
		counter.complete(order);
		customer.served(world, order, tasted, order.dineIn ? Math.max(1, charmTip) : 0);
		BakeryReputation.recordAndCelebrate(player, stars);
		ReviewPayload review = new ReviewPayload(customer.getId(), tasted, stars, comment(world.random, stars),
				favorite ? "loved" : regular.favorite().asString(), price, regular.name(), hearts);
		for (ServerPlayerEntity viewer : PlayerLookup.tracking(customer)) {
			ServerPlayNetworking.send(viewer, review);
		}
		if (!PlayerLookup.tracking(customer).contains(player)) ServerPlayNetworking.send(player, review);
		return true;
	}

	public static String comment(Random random, int stars) {
		return VillagerCritic.comment(random, stars);
	}

	private static void levelUp(ServerPlayerEntity player, CafeLevel level) {
		player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
		player.networkHandler.sendPacket(new TitleS2CPacket(level.displayName().copy().formatted(Formatting.LIGHT_PURPLE)));
		player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.translatable("cafe_level.bakery.up")));
		player.sendMessage(Text.translatable("message.bakery.level.unlocked", level.displayName()).formatted(Formatting.GOLD), false);
		for (Menu.Dish dish : Menu.dishes()) {
			if (dish.level() == level) {
				player.sendMessage(Text.literal("  ♥ ").formatted(Formatting.LIGHT_PURPLE).append(dish.item().getName()), false);
			}
		}
		player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1.2f);
		player.getServerWorld().spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 2, player.getZ(), 12, 0.6, 0.4, 0.6, 0.02);
	}
}
