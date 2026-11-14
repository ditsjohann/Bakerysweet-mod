package com.bakery.network;

import com.bakery.Bakery;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Server -> client: a cake was sold at the counter at {@code pos}. Pops up over the counter with its stars, the
 * verdict (good, neutral or mid), what the buyer said, the coins, and the three things they judged.
 */
public record AppraisalPayload(BlockPos pos, ItemStack cake, int stars, int verdict, String comment, int coins, int taste, int looks, int fresh)
		implements CustomPayload {
	public static final Id<AppraisalPayload> ID = new Id<>(Bakery.id("appraisal"));
	public static final PacketCodec<RegistryByteBuf, AppraisalPayload> CODEC = PacketCodec.of((p, buf) -> {
		BlockPos.PACKET_CODEC.encode(buf, p.pos);
		ItemStack.PACKET_CODEC.encode(buf, p.cake);
		buf.writeVarInt(p.stars);
		buf.writeVarInt(p.verdict);
		buf.writeString(p.comment);
		buf.writeVarInt(p.coins);
		buf.writeVarInt(p.taste);
		buf.writeVarInt(p.looks);
		buf.writeVarInt(p.fresh);
	}, buf -> new AppraisalPayload(BlockPos.PACKET_CODEC.decode(buf), ItemStack.PACKET_CODEC.decode(buf), buf.readVarInt(), buf.readVarInt(),
			buf.readString(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
