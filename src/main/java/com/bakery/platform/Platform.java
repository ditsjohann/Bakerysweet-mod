package com.bakery.platform;

import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;

import java.nio.file.Path;

/**
 * The few things the mod needs from its mod loader (Fabric, NeoForge or Forge). Each loader's entry point installs
 * its own implementation before anything else runs; everything else in the mod is plain Minecraft.
 */
public interface Platform {
	static Platform get() {
		if (Holder.instance == null) throw new IllegalStateException("Sweetheart Bakery: no platform installed");
		return Holder.instance;
	}

	static void install(Platform platform) {
		Holder.instance = platform;
	}

	/**
	 * Registers {@code value} under {@code id}. Plain Minecraft on Fabric and NeoForge; Forge only accepts entries
	 * through its own registry objects for the registries it manages.
	 */
	default <V, T extends V> T register(net.minecraft.registry.Registry<V> registry, Identifier id, T value) {
		return net.minecraft.registry.Registry.register(registry, id, value);
	}

	/** Like {@link #register}, returning the registry entry (holder). */
	default <T> net.minecraft.registry.entry.RegistryEntry<T> registerReference(net.minecraft.registry.Registry<T> registry, Identifier id, T value) {
		return net.minecraft.registry.Registry.registerReference(registry, id, value);
	}

	/** Sends one of the mod's packets to a player. */
	void sendToPlayer(ServerPlayerEntity player, CustomPayload payload);

	/** Sends a plain Minecraft packet (titles and the like) to a player. */
	void sendVanillaPacket(ServerPlayerEntity player, net.minecraft.network.packet.Packet<?> packet);

	/** How long an item burns in a furnace, in ticks (0 = not a fuel). */
	int fuelTime(ItemStack stack);

	/** The game's config folder. */
	Path configDir();

	/** A creative tab builder (each loader places and pages the tabs its own way). */
	ItemGroup.Builder itemGroup();

	/** Registers a point of interest made of every state of {@code block}. */
	PointOfInterestType registerPoi(Identifier id, int tickets, int searchDistance, Block block);

	final class Holder {
		private static Platform instance;

		private Holder() {
		}
	}
}
