package com.bakery.neoforge;

import com.bakery.Bakery;
import com.bakery.BakeryCommands;
import com.bakery.BakeryEvents;
import com.bakery.network.BakeryNetworking;
import com.bakery.platform.Hooks;
import com.bakery.platform.Platform;
import com.bakery.registry.BakeryVillagers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.village.TradeOffers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Sweetheart Bakery on NeoForge. Everything is registered while NeoForge registers blocks; the rest is wired to
 * NeoForge's events. Composting and the wild fruit trees are data files (data maps and biome modifiers).
 */
@Mod(Bakery.MOD_ID)
public final class BakeryNeoForge {
	/** What the client does with an arriving packet; set by the client entry point (nothing on a dedicated server). */
	static Consumer<CustomPayload> clientReceiver = payload -> {
	};

	public BakeryNeoForge(IEventBus modBus) {
		Platform.install(new NeoForgePlatform());
		modBus.addListener(RegisterEvent.class, event -> {
			if (event.getRegistryKey().equals(RegistryKeys.BLOCK)) Bakery.init();
		});
		modBus.addListener(RegisterPayloadHandlersEvent.class, BakeryNeoForge::registerPayloads);
		modBus.addListener(EntityAttributeCreationEvent.class, event -> {
			for (Hooks.Attributes a : Hooks.ATTRIBUTES) event.put(a.type(), a.attributes().get().build());
		});
		modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() -> {
			FireBlock fire = (FireBlock) Blocks.FIRE;
			for (Hooks.Flammable f : Hooks.FLAMMABLE) fire.registerFlammableBlock(f.block(), f.burn(), f.spread());
		}));

		NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickItem.class, event -> {
			TypedActionResult<ItemStack> result = BakeryEvents.useItem(event.getEntity(), event.getLevel(), event.getHand());
			if (result.getResult() != ActionResult.PASS) {
				event.setCancellationResult(result.getResult());
				event.setCanceled(true);
			}
		});
		NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.EntityInteract.class, event -> {
			ActionResult result = BakeryEvents.useEntity(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
			if (result != ActionResult.PASS) {
				event.setCancellationResult(result);
				event.setCanceled(true);
			}
		});
		NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> BakeryEvents.serverTick(event.getServer()));
		NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayerEntity player) BakeryEvents.playerJoined(player);
		});
		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> BakeryCommands.register(event.getDispatcher()));
		NeoForge.EVENT_BUS.addListener(VillagerTradesEvent.class, event -> {
			if (event.getType() != BakeryVillagers.BAKER) return;
			for (Hooks.Trades t : Hooks.BAKER_TRADES) t.factories().accept(event.getTrades().computeIfAbsent(t.level(), k -> new java.util.ArrayList<>()));
		});
		NeoForge.EVENT_BUS.addListener(WandererTradesEvent.class, event -> {
			for (Hooks.Trades t : Hooks.WANDERER_TRADES) {
				List<TradeOffers.Factory> list = t.level() == 1 ? event.getGenericTrades() : event.getRareTrades();
				t.factories().accept(list);
			}
		});
		NeoForge.EVENT_BUS.addListener(BlockEvent.BlockToolModificationEvent.class, event -> {
			if (event.getItemAbility() != ItemAbilities.AXE_STRIP) return;
			BlockState state = event.getState();
			Block stripped = Hooks.STRIPPABLE.get(state.getBlock());
			if (stripped == null) return;
			BlockState result = stripped.getDefaultState();
			if (state.contains(PillarBlock.AXIS)) result = result.with(PillarBlock.AXIS, state.get(PillarBlock.AXIS));
			event.setFinalState(result);
		});

		if (FMLEnvironment.dist == Dist.CLIENT) BakeryNeoForgeClient.init(modBus);
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");
		for (BakeryNetworking.ToClient<?> type : BakeryNetworking.TO_CLIENT) toClient(registrar, type);
		for (BakeryNetworking.ToServer<?> type : BakeryNetworking.TO_SERVER) toServer(registrar, type);
	}

	private static <T extends CustomPayload> void toClient(PayloadRegistrar registrar, BakeryNetworking.ToClient<T> type) {
		registrar.playToClient(type.id(), type.codec(), (payload, context) -> context.enqueueWork(() -> clientReceiver.accept(payload)));
	}

	private static <T extends CustomPayload> void toServer(PayloadRegistrar registrar, BakeryNetworking.ToServer<T> type) {
		registrar.playToServer(type.id(), type.codec(), (payload, context) -> context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayerEntity player) type.handler().accept(payload, player);
		}));
	}
}
