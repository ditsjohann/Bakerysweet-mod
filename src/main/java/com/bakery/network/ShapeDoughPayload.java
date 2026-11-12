package com.bakery.network;

import com.bakery.Bakery;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/** Client -> server: the player finished rolling. Which shape they picked and how well they rolled (0-10). */
public record ShapeDoughPayload(BlockPos pos, int shape, int score) implements CustomPayload {
	public static final Id<ShapeDoughPayload> ID = new Id<>(Bakery.id("shape_dough"));
	public static final PacketCodec<RegistryByteBuf, ShapeDoughPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, ShapeDoughPayload::pos,
			PacketCodecs.VAR_INT, ShapeDoughPayload::shape,
			PacketCodecs.VAR_INT, ShapeDoughPayload::score,
			ShapeDoughPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
