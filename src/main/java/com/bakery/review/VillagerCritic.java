package com.bakery.review;

import com.bakery.food.BakeryFoods;
import com.bakery.food.Flavor;
import com.bakery.food.Freshness;
import com.bakery.food.Perishable;
import com.bakery.item.DrinkItem;
import com.bakery.network.ReviewPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillageGossipType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sneak + right-click a villager while holding a bake and they will taste it and give it 1-5 stars.
 * Every villager has a favorite flavor (and one they dislike), stale food is judged harshly,
 * and good reviews pay out in emeralds and better prices.
 */
public final class VillagerCritic {
	private static final int COOLDOWN = 1200;
	private static final Map<UUID, Long> LAST_TASTED = new HashMap<>();

	private VillagerCritic() {
	}

	public static boolean canReview(ItemStack stack) {
		return stack.getItem() instanceof Perishable perishable && (perishable.isBake() || stack.getItem() instanceof DrinkItem);
	}

	public static Flavor favorite(VillagerEntity villager) {
		Flavor[] flavors = Flavor.values();
		return flavors[1 + Math.floorMod(villager.getUuid().hashCode(), flavors.length - 1)];
	}

	public static Flavor disliked(VillagerEntity villager) {
		Flavor[] flavors = Flavor.values();
		Flavor pick = flavors[1 + Math.floorMod(villager.getUuid().getLeastSignificantBits() * 31 + 7, flavors.length - 1)];
		return pick == favorite(villager) ? Flavor.PLAIN : pick;
	}

	/** 1-5 stars from quality, freshness and this villager's taste. */
	public static int rate(VillagerEntity villager, ItemStack stack, long now) {
		Flavor flavor = BakeryFoods.flavor(stack);
		double score = BakeryFoods.quality(stack);
		switch (Freshness.stage(stack, now)) {
			case GOOD -> score -= 0.5;
			case STALE -> score -= 1.5;
			case SPOILED -> score -= 5;
			default -> {
			}
		}
		if (flavor == favorite(villager)) score += 1;
		if (flavor == disliked(villager)) score -= 1;
		if (flavor == Flavor.STARBLOOM) score += 0.5;
		return Math.clamp(Math.round(score), 1, 5);
	}

	public static String comment(Random random, int stars) {
		return "review.bakery.s" + stars + "." + random.nextInt(4);
	}

	public static void review(ServerPlayerEntity player, VillagerEntity villager, ItemStack stack) {
		ServerWorld world = player.getServerWorld();
		long now = world.getTime();
		Long last = LAST_TASTED.get(villager.getUuid());
		if (last != null && now - last < COOLDOWN) {
			player.sendMessage(Text.translatable("message.bakery.review.full"), true);
			world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1f, 1f);
			return;
		}
		LAST_TASTED.put(villager.getUuid(), now);

		Freshness.stamp(stack, world);
		Flavor favorite = favorite(villager);
		int stars = rate(villager, stack, now);
		ItemStack tasted = stack.copyWithCount(1);
		stack.decrementUnlessCreative(1, player);

		int coins = switch (stars) {
			case 5 -> 3;
			case 4 -> 2;
			case 3 -> 1;
			default -> 0;
		};
		com.bakery.sweet.Coins.pay(player, coins);
		if (stars >= 4) {
			villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, stars * 4);
		} else if (stars <= 2) {
			villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_NEGATIVE, 10);
		}

		byte status = stars == 5 ? EntityStatuses.ADD_VILLAGER_HEART_PARTICLES
				: stars >= 3 ? EntityStatuses.ADD_VILLAGER_HAPPY_PARTICLES : EntityStatuses.ADD_VILLAGER_ANGRY_PARTICLES;
		world.sendEntityStatus(villager, status);
		world.playSound(null, villager.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL, 1f, 1f);
		world.playSound(null, villager.getBlockPos(), stars >= 3 ? SoundEvents.ENTITY_VILLAGER_YES : SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 1f, 1f);

		String hint = BakeryFoods.flavor(tasted) == favorite ? "loved" : favorite.asString();
		BakeryReputation.recordAndCelebrate(player, stars);
		ServerPlayNetworking.send(player, new ReviewPayload(villager.getId(), tasted, stars, comment(world.random, stars), hint, coins));
	}
}
