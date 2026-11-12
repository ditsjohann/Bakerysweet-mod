package com.bakery.sweet;

import com.bakery.network.StickerPayload;
import com.bakery.review.BakeryReputation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Hands out stickers (once each), pays their coin reward and pops a little toast on the player's screen. */
public final class Stickers {
	private Stickers() {
	}

	public static boolean has(ServerPlayerEntity player, Sticker sticker) {
		return BakeryReputation.get(player.getServer()).of(player.getUuid()).stickers.contains(sticker.id());
	}

	public static void award(ServerPlayerEntity player, Sticker sticker) {
		if (player.getServer() == null) return;
		BakeryReputation reputation = BakeryReputation.get(player.getServer());
		if (!reputation.of(player.getUuid()).stickers.add(sticker.id())) return;
		reputation.markDirty();
		player.sendMessage(Text.translatable("message.bakery.sticker", sticker.title(), sticker.reward()).formatted(Formatting.LIGHT_PURPLE), false);
		player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.5f, 1.5f);
		player.getServerWorld().spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 2.1, player.getZ(), 4, 0.4, 0.2, 0.4, 0);
		ServerPlayNetworking.send(player, new StickerPayload(sticker.ordinal()));
		Coins.pay(player, sticker.reward());
	}
}
