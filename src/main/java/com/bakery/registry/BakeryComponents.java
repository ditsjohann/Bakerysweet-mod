package com.bakery.registry;

import com.bakery.Bakery;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Item data that rides along with every bake: stars, flavor, topping and when it goes bad. */
public final class BakeryComponents {
	/** 1 to 5 stars. */
	public static final ComponentType<Integer> QUALITY = register("quality", Codec.INT, PacketCodecs.VAR_INT);
	public static final ComponentType<String> FLAVOR = register("flavor", Codec.STRING, PacketCodecs.STRING);
	public static final ComponentType<String> TOPPING = register("topping", Codec.STRING, PacketCodecs.STRING);
	/** Absolute world time (World#getTime) at which the food spoils. */
	public static final ComponentType<Long> EXPIRES = register("expires", Codec.LONG, PacketCodecs.VAR_LONG);
	public static final ComponentType<String> PRESERVATION = register("preservation", Codec.STRING, PacketCodecs.STRING);
	/** Dough that rested in a Proofing Basket: one extra star when rolled out. */
	public static final ComponentType<Boolean> PROOFED = register("proofed", Codec.BOOL, PacketCodecs.BOOL);
	/** Which cookie cutter shaped a sugar cookie: heart, star, bunny or flower. */
	public static final ComponentType<String> COOKIE_SHAPE = register("cookie_shape", Codec.STRING, PacketCodecs.STRING);
	/** RGB colour of the icing piped on a sugar cookie. */
	public static final ComponentType<Integer> ICING = register("icing", Codec.INT, PacketCodecs.INTEGER);
	/** A cake decorated at the Cake Turntable. */
	public static final ComponentType<com.bakery.sweet.CakeDesign> CAKE_DESIGN = register("cake_design", com.bakery.sweet.CakeDesign.CODEC, com.bakery.sweet.CakeDesign.PACKET_CODEC);
	/** Which piping tip an icing bag has on (see IcingBagItem.Tip). */
	public static final ComponentType<Integer> PIPING_TIP = register("piping_tip", Codec.INT, PacketCodecs.VAR_INT);
	/** Ribbon colour of a wrapped gift, and who wrapped it. */
	public static final ComponentType<Integer> GIFT_COLOR = register("gift_color", Codec.INT, PacketCodecs.INTEGER);
	public static final ComponentType<String> GIFT_FROM = register("gift_from", Codec.STRING, PacketCodecs.STRING);

	/** Bought from the catalog rather than grown, milled or made yourself. */
	public static final ComponentType<Boolean> STORE_BOUGHT = register("store_bought", Codec.BOOL, PacketCodecs.BOOL);
	/** Made only from things the baker grew or made - customers taste the difference and pay more. */
	public static final ComponentType<Boolean> HOMEMADE = register("homemade", Codec.BOOL, PacketCodecs.BOOL);

	/** The customer path being laid with a Path Ribbon: the entrance first, then each turn toward the counter. */
	public static final ComponentType<java.util.List<net.minecraft.util.math.BlockPos>> PATH_POINTS = register("path_points",
			net.minecraft.util.math.BlockPos.CODEC.listOf(), net.minecraft.util.math.BlockPos.PACKET_CODEC.collect(PacketCodecs.toList()));

	private BakeryComponents() {
	}

	private static <T> ComponentType<T> register(String name, Codec<T> codec, PacketCodec<? super net.minecraft.network.RegistryByteBuf, T> packetCodec) {
		return Registry.register(Registries.DATA_COMPONENT_TYPE, Bakery.id(name),
				ComponentType.<T>builder().codec(codec).packetCodec(packetCodec).build());
	}

	public static void init() {
	}
}
