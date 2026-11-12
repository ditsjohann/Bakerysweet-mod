package com.bakery.network;

import com.bakery.Bakery;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/** Server -> client: open the rolling pin minigame for the dough on this pastry board. */
public record OpenRollingPayload(BlockPos pos, int doughType) implements CustomPayload {
	public static final Id<OpenRollingPayload> ID = new Id<>(Bakery.id("open_rolling"));
	public static final PacketCodec<RegistryByteBuf, OpenRollingPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OpenRollingPayload::pos,
			PacketCodecs.VAR_INT, OpenRollingPayload::doughType,
			OpenRollingPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
