package com.bakery.fabric;

import com.bakery.platform.Platform;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.poi.PointOfInterestType;

import java.nio.file.Path;

/** Sweetheart Bakery on Fabric: the platform hooks, through Fabric API. */
public final class FabricPlatform implements Platform {
	@Override
	public void sendToPlayer(ServerPlayerEntity player, CustomPayload payload) {
		ServerPlayNetworking.send(player, payload);
	}

	@Override
	public void sendVanillaPacket(ServerPlayerEntity player, net.minecraft.network.packet.Packet<?> packet) {
		player.networkHandler.sendPacket(packet);
	}

	@Override
	public int fuelTime(ItemStack stack) {
		Integer burn = FuelRegistry.INSTANCE.get(stack.getItem());
		return burn == null ? 0 : burn;
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public ItemGroup.Builder itemGroup() {
		return FabricItemGroup.builder();
	}

	@Override
	public PointOfInterestType registerPoi(Identifier id, int tickets, int searchDistance, Block block) {
		return PointOfInterestHelper.register(id, tickets, searchDistance, block);
	}
}
