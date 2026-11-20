package com.bakery.forge;

import com.bakery.platform.Platform;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.nio.file.Path;

/** Sweetheart Bakery on Forge: the platform hooks. */
public final class ForgePlatform implements Platform {
	@Override
	public <V, T extends V> T register(Registry<V> registry, Identifier id, T value) {
		ForgeRegistry<V> forge = RegistryManager.ACTIVE.getRegistry(registry.getKey());
		if (forge == null) return Registry.register(registry, id, value);
		forge.register(id, value);
		return value;
	}

	@Override
	public <T> RegistryEntry<T> registerReference(Registry<T> registry, Identifier id, T value) {
		ForgeRegistry<T> forge = RegistryManager.ACTIVE.getRegistry(registry.getKey());
		if (forge == null) return Registry.registerReference(registry, id, value);
		forge.register(id, value);
		return forge.getDelegateOrThrow(id);
	}

	@Override
	public void sendToPlayer(ServerPlayerEntity player, CustomPayload payload) {
		BakeryForge.channel.send(payload, PacketDistributor.PLAYER.with(player));
	}

	@Override
	public void sendVanillaPacket(ServerPlayerEntity player, net.minecraft.network.packet.Packet<?> packet) {
		player.networkHandler.sendPacket(packet);
	}

	@Override
	public int fuelTime(ItemStack stack) {
		return stack.isEmpty() ? 0 : Math.max(0, ForgeHooks.getBurnTime(stack, null));
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public ItemGroup.Builder itemGroup() {
		return ItemGroup.builder();
	}

	@Override
	public PointOfInterestType registerPoi(Identifier id, int tickets, int searchDistance, Block block) {
		// Forge maps the block's states to the new point of interest when it is registered.
		return register(Registries.POINT_OF_INTEREST_TYPE, id,
				new PointOfInterestType(ImmutableSet.copyOf(block.getStateManager().getStates()), tickets, searchDistance));
	}
}
