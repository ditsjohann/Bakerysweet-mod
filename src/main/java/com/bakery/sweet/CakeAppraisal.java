package com.bakery.sweet;

import com.bakery.food.BakeryFoods;
import com.bakery.food.Flavor;
import com.bakery.food.Freshness;
import com.bakery.network.AppraisalPayload;
import com.bakery.registry.BakeryComponents;
import com.bakery.shop.CounterBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * Selling a finished cake at the counter. Someone from town looks it over and tastes it: a 1-5 star rating made of
 * how well it was baked (taste), how it was decorated (looks: icing, tiers, shapes, toppers) and how fresh it is,
 * with a little luck on top. Four or five stars is "Good", three "Neutral", fewer "Mid"; the coins follow the
 * stars and the size of the cake.
 */
public final class CakeAppraisal {
	public static final int MID = 0, NEUTRAL = 1, GOOD = 2;

	private CakeAppraisal() {
	}

	/** The judgement before any luck: {stars x 10 (unrounded), taste 1-5, looks 1-5, freshness 1-5}. */
	public static int[] judge(ItemStack cake, long now) {
		CakeDesign design = cake.getOrDefault(BakeryComponents.CAKE_DESIGN, CakeDesign.PLAIN);
		int taste = Math.clamp(BakeryFoods.quality(cake), 1, 5);
		int looks = design.cuteness();
		if (design.tierCount() >= 3 && looks < 5) looks++;
		if (design.distinctShapes() > 1 && looks < 5) looks++;
		int fresh = switch (Freshness.stage(cake, now)) {
			case GOOD -> 4;
			case STALE -> 2;
			case SPOILED -> 1;
			default -> 5;
		};
		float score = taste * 0.45f + looks * 0.4f + fresh * 0.15f + 0.4f;
		if (BakeryFoods.flavor(cake) == Flavor.STARBLOOM) score += 0.4f;
		if (fresh <= 2) score -= 1.2f;
		return new int[]{Math.round(score * 10), taste, looks, fresh};
	}

	public static int verdict(int stars) {
		return stars >= 4 ? GOOD : stars == 3 ? NEUTRAL : MID;
	}

	/** Coins for a cake: the stars, how many tiers, a bonus for five stars, a little luck. */
	public static int coins(int stars, CakeDesign design, Random random, boolean rushHour) {
		return 2 + design.tierCount() * 2 + stars * 2 + (stars == 5 ? 3 : 0) + random.nextInt(3) + (rushHour ? 1 : 0);
	}

	/** The player sells the cake in their hand at this counter. */
	public static void sell(ServerPlayerEntity player, CounterBlockEntity counter, ItemStack stack) {
		ServerWorld world = player.getServerWorld();
		BlockPos pos = counter.getPos();
		ItemStack cake = stack.copyWithCount(1);
		int[] judged = judge(cake, world.getTime());
		Random random = world.random;
		int stars = MathHelper.clamp(Math.round(judged[0] / 10f + (float) random.nextGaussian() * 0.45f), 1, 5);
		int verdict = verdict(stars);
		CakeDesign design = cake.getOrDefault(BakeryComponents.CAKE_DESIGN, CakeDesign.PLAIN);
		int coins = coins(stars, design, random, counter.getLevel().rushHour());
		String comment = "appraisal.bakery." + switch (verdict) {
			case GOOD -> "good";
			case NEUTRAL -> "neutral";
			default -> "mid";
		} + "." + random.nextInt(5);

		stack.decrementUnlessCreative(1, player);
		Coins.pay(player, coins);
		world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.7f, 1.2f);
		world.playSound(null, pos, verdict == GOOD ? SoundEvents.ENTITY_VILLAGER_CELEBRATE : verdict == NEUTRAL ? SoundEvents.ENTITY_VILLAGER_AMBIENT
				: SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 0.7f, 1.4f);
		world.spawnParticles(verdict == GOOD ? ParticleTypes.HEART : verdict == NEUTRAL ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.SMOKE,
				pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, verdict == GOOD ? 6 : 4, 0.3, 0.2, 0.3, 0.01);
		player.sendMessage(Text.translatable("message.bakery.appraisal.sold", Text.translatable("appraisal.bakery.verdict." + verdict), coins)
				.formatted(verdict == GOOD ? Formatting.GOLD : verdict == NEUTRAL ? Formatting.LIGHT_PURPLE : Formatting.GRAY), true);
		if (verdict == GOOD && design.cuteness() >= 4) Stickers.award(player, Sticker.CAKE_ARTIST);

		AppraisalPayload payload = new AppraisalPayload(pos, cake, stars, verdict, comment, coins, judged[1], judged[2], judged[3]);
		for (ServerPlayerEntity viewer : PlayerLookup.around(world, Vec3d.ofCenter(pos), 32)) ServerPlayNetworking.send(viewer, payload);
	}
}
