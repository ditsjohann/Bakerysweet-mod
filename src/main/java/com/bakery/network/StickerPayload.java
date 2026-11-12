package com.bakery.network;

import com.bakery.Bakery;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Server -> client: a sticker was just earned (ordinal of Sticker). Shows a toast. */
public record StickerPayload(int sticker) implements CustomPayload {
	public static final Id<StickerPayload> ID = new Id<>(Bakery.id("sticker"));
	public static final PacketCodec<RegistryByteBuf, StickerPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, StickerPayload::sticker, StickerPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
