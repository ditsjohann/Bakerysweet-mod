package com.bakery.neoforge;

import com.bakery.platform.Platform;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

/** Sweetheart Bakery on NeoForge: the platform hooks. */
public final class NeoForgePlatform implements Platform {
	@Override
	public void sendToPlayer(ServerPlayerEntity player, CustomPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}

	@Override
	public void sendVanillaPacket(ServerPlayerEntity player, net.minecraft.network.packet.Packet<?> packet) {
		player.networkHandler.send(packet);
	}

	@Override
	public int fuelTime(ItemStack stack) {
		return stack.isEmpty() ? 0 : Math.max(0, stack.getBurnTime(null));
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
		// NeoForge maps the block's states to the new point of interest when it is registered.
		return register(Registries.POINT_OF_INTEREST_TYPE, id,
				new PointOfInterestType(ImmutableSet.copyOf(block.getStateManager().getStates()), tickets, searchDistance));
	}
}
