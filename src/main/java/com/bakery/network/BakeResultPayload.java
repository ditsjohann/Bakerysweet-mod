package com.bakery.network;

import com.bakery.Bakery;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/** Server -> client: a bake came out of the oven at {@code pos}. Pops up in the world over the oven mouth. */
public record BakeResultPayload(BlockPos pos, ItemStack result, int stars, int prep, int timing, int heat) implements CustomPayload {
	public static final Id<BakeResultPayload> ID = new Id<>(Bakery.id("bake_result"));
	public static final PacketCodec<RegistryByteBuf, BakeResultPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, BakeResultPayload::pos,
			ItemStack.PACKET_CODEC, BakeResultPayload::result,
			PacketCodecs.VAR_INT, BakeResultPayload::stars,
			PacketCodecs.VAR_INT, BakeResultPayload::prep,
			PacketCodecs.VAR_INT, BakeResultPayload::timing,
			PacketCodecs.VAR_INT, BakeResultPayload::heat,
			BakeResultPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
