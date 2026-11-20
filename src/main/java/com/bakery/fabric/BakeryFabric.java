package com.bakery.fabric;

import com.bakery.Bakery;
import com.bakery.BakeryCommands;
import com.bakery.BakeryEvents;
import com.bakery.network.BakeryNetworking;
import com.bakery.platform.Hooks;
import com.bakery.platform.Platform;
import com.bakery.registry.BakeryVillagers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;

/** Sweetheart Bakery on Fabric. */
public final class BakeryFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Platform.install(new FabricPlatform());
		Bakery.init();

		for (BakeryNetworking.ToClient<?> type : BakeryNetworking.TO_CLIENT) registerToClient(type);
		for (BakeryNetworking.ToServer<?> type : BakeryNetworking.TO_SERVER) registerToServer(type);

		Hooks.STRIPPABLE.forEach(StrippableBlockRegistry::register);
		for (Hooks.Flammable f : Hooks.FLAMMABLE) FlammableBlockRegistry.getDefaultInstance().add(f.block(), f.burn(), f.spread());
		Hooks.COMPOSTABLE.forEach(CompostingChanceRegistry.INSTANCE::add);
		for (Hooks.Attributes a : Hooks.ATTRIBUTES) FabricDefaultAttributeRegistry.register(a.type(), a.attributes().get());
		for (Hooks.Trades t : Hooks.BAKER_TRADES) TradeOfferHelper.registerVillagerOffers(BakeryVillagers.BAKER, t.level(), t.factories());
		for (Hooks.Trades t : Hooks.WANDERER_TRADES) TradeOfferHelper.registerWanderingTraderOffers(t.level(), t.factories());
		for (Hooks.WildTree tree : Hooks.WILD_TREES) {
			BiomeModifications.addFeature(BiomeSelectors.includeByKey(tree.biomes()), GenerationStep.Feature.VEGETAL_DECORATION,
					RegistryKey.of(RegistryKeys.PLACED_FEATURE, tree.id()));
		}

		UseItemCallback.EVENT.register(BakeryEvents::useItem);
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> BakeryEvents.useEntity(player, world, hand, entity));
		ServerTickEvents.END_SERVER_TICK.register(BakeryEvents::serverTick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> BakeryEvents.playerJoined(handler.getPlayer()));
		CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> BakeryCommands.register(dispatcher));
	}

	private static <T extends net.minecraft.network.packet.CustomPayload> void registerToClient(BakeryNetworking.ToClient<T> type) {
		PayloadTypeRegistry.playS2C().register(type.id(), type.codec());
	}

	private static <T extends net.minecraft.network.packet.CustomPayload> void registerToServer(BakeryNetworking.ToServer<T> type) {
		PayloadTypeRegistry.playC2S().register(type.id(), type.codec());
		ServerPlayNetworking.registerGlobalReceiver(type.id(), (payload, context) ->
				context.server().execute(() -> type.handler().accept(payload, context.player())));
	}
}
