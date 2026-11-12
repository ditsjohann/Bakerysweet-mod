package com.bakery.item;

import com.bakery.network.SweetPayloads;
import com.bakery.review.BakeryReputation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** The Bakery Guide: a little picture book for new bakers, with a sticker book at the back. */
public class GuideItem extends Item {
	public GuideItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (user instanceof ServerPlayerEntity player) {
			// The server sends the player's progress; the client opens the book when it arrives.
			ServerPlayNetworking.send(player, data(player));
		}
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
	}

	public static SweetPayloads.GuideData data(ServerPlayerEntity player) {
		BakeryReputation.Stats s = BakeryReputation.get(player.getServer()).of(player.getUuid());
		return new SweetPayloads.GuideData(List.copyOf(s.stickers), s.rankKey(), s.reviews, s.recipes.size(), BakeryReputation.TOTAL_RECIPES,
				s.coinsEarned, s.served, s.bestFriends());
	}
}
