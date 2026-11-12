package com.bakery.network;

import com.bakery.Bakery;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Server -> client: someone tasted your bake. Shown in the world, floating over whoever tasted it
 * ({@code entity}): the stars pop up one by one, then what they thought.
 */
public record ReviewPayload(int entity, ItemStack food, int stars, String comment, String favorite, int coins, String customer, int hearts) implements CustomPayload {
	public static final Id<ReviewPayload> ID = new Id<>(Bakery.id("review"));
	public static final PacketCodec<RegistryByteBuf, ReviewPayload> CODEC = PacketCodec.of((p, buf) -> {
		buf.writeVarInt(p.entity);
		ItemStack.PACKET_CODEC.encode(buf, p.food);
		buf.writeVarInt(p.stars);
		buf.writeString(p.comment);
		buf.writeString(p.favorite);
		buf.writeVarInt(p.coins);
		buf.writeString(p.customer);
		buf.writeVarInt(p.hearts);
	}, buf -> new ReviewPayload(buf.readVarInt(), ItemStack.PACKET_CODEC.decode(buf), buf.readVarInt(), buf.readString(), buf.readString(),
			buf.readVarInt(), buf.readString(), buf.readVarInt()));

	/** A villager tasting (no friendship involved). */
	public ReviewPayload(int entity, ItemStack food, int stars, String comment, String favorite, int coins) {
		this(entity, food, stars, comment, favorite, coins, "", -1);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
