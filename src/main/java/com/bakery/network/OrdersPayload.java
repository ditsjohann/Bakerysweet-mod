package com.bakery.network;

import com.bakery.Bakery;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

/** Server -> client: the open orders at one bakery counter, for the wish bubbles above customers' heads. */
public record OrdersPayload(BlockPos counter, ArrayList<OrderView> orders) implements CustomPayload {
	public record OrderView(int entityId, ItemStack wanted, float patience, boolean waiting, boolean dineIn) {
		public static final PacketCodec<RegistryByteBuf, OrderView> CODEC = PacketCodec.tuple(
				PacketCodecs.VAR_INT, OrderView::entityId,
				ItemStack.PACKET_CODEC, OrderView::wanted,
				PacketCodecs.FLOAT, OrderView::patience,
				PacketCodecs.BOOL, OrderView::waiting,
				PacketCodecs.BOOL, OrderView::dineIn,
				OrderView::new);
	}

	public static final Id<OrdersPayload> ID = new Id<>(Bakery.id("orders"));
	public static final PacketCodec<RegistryByteBuf, OrdersPayload> CODEC = PacketCodec.tuple(
			BlockPos.PACKET_CODEC, OrdersPayload::counter,
			PacketCodecs.collection(ArrayList::new, OrderView.CODEC), OrdersPayload::orders,
			OrdersPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
