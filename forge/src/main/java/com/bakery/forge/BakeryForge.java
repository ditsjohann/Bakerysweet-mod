package com.bakery.forge;

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
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.village.TradeOffers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadFlow;
import net.minecraftforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Sweetheart Bakery on Forge. Everything is registered while Forge registers blocks; the rest is wired to Forge's
 * events. Composting goes into Minecraft's own table. The wild fruit trees are left out on Forge (see build.gradle).
 */
@Mod(Bakery.MOD_ID)
public final class BakeryForge {
	/** The mod's packets (all of them, both ways). */
	static Channel<CustomPayload> channel;
	/** What the client does with an arriving packet; set by the client entry point (nothing on a dedicated server). */
	static Consumer<CustomPayload> clientReceiver = payload -> {
	};

	public BakeryForge(FMLJavaModLoadingContext context) {
		Platform.install(new ForgePlatform());
		IEventBus modBus = context.getModEventBus();
		modBus.addListener(EventPriority.NORMAL, false, RegisterEvent.class, event -> {
			if (event.getRegistryKey().equals(RegistryKeys.BLOCK)) registerEverything();
		});
		modBus.addListener(EventPriority.NORMAL, false, EntityAttributeCreationEvent.class, event -> {
			for (Hooks.Attributes a : Hooks.ATTRIBUTES) event.put(a.type(), a.attributes().get().build());
		});
		modBus.addListener(EventPriority.NORMAL, false, FMLCommonSetupEvent.class, event -> event.enqueueWork(() -> {
			createChannel();
			FireBlock fire = (FireBlock) Blocks.FIRE;
			for (Hooks.Flammable f : Hooks.FLAMMABLE) fire.registerFlammableBlock(f.block(), f.burn(), f.spread());
			Hooks.COMPOSTABLE.forEach(ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE::put);
		}));

		IEventBus game = MinecraftForge.EVENT_BUS;
		game.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickItem.class, event -> {
			TypedActionResult<ItemStack> result = BakeryEvents.useItem(event.getEntity(), event.getLevel(), event.getHand());
			if (result.getResult() != ActionResult.PASS) {
				event.setCancellationResult(result.getResult());
				event.setCanceled(true);
			}
		});
		game.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.EntityInteract.class, event -> {
			ActionResult result = BakeryEvents.useEntity(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
			if (result != ActionResult.PASS) {
				event.setCancellationResult(result);
				event.setCanceled(true);
			}
		});
		game.addListener(EventPriority.NORMAL, false, TickEvent.ServerTickEvent.Post.class, event -> BakeryEvents.serverTick(event.getServer()));
		game.addListener(EventPriority.NORMAL, false, PlayerEvent.PlayerLoggedInEvent.class, event -> {
			if (event.getEntity() instanceof ServerPlayerEntity player) BakeryEvents.playerJoined(player);
		});
		game.register(Commands.class);
		game.addListener(EventPriority.NORMAL, false, VillagerTradesEvent.class, event -> {
			if (event.getType() != BakeryVillagers.BAKER) return;
			for (Hooks.Trades t : Hooks.BAKER_TRADES) t.factories().accept(event.getTrades().computeIfAbsent(t.level(), k -> new java.util.ArrayList<>()));
		});
		game.addListener(EventPriority.NORMAL, false, WandererTradesEvent.class, event -> {
			for (Hooks.Trades t : Hooks.WANDERER_TRADES) {
				List<TradeOffers.Factory> list = t.level() == 1 ? event.getGenericTrades() : event.getRareTrades();
				t.factories().accept(list);
			}
		});
		game.addListener(EventPriority.NORMAL, false, BlockEvent.BlockToolModificationEvent.class, event -> {
			if (event.getToolAction() != ToolActions.AXE_STRIP) return;
			BlockState state = event.getState();
			Block stripped = Hooks.STRIPPABLE.get(state.getBlock());
			if (stripped == null) return;
			BlockState result = stripped.getDefaultState();
			if (state.contains(PillarBlock.AXIS)) result = result.with(PillarBlock.AXIS, state.get(PillarBlock.AXIS));
			event.setFinalState(result);
		});

		if (FMLEnvironment.dist == Dist.CLIENT) BakeryForgeClient.init(modBus);
	}

	/** Adds /bakery when Forge registers commands. */
	public static final class Commands {
		private Commands() {
		}

		@net.minecraftforge.eventbus.api.SubscribeEvent
		public static void register(RegisterCommandsEvent event) {
			BakeryCommands.register(event.getDispatcher());
		}
	}

	/**
	 * Forge opens one registry at a time; the mod registers everything at once (its blocks, items, entities... are
	 * plain static fields), so every Forge registry is opened for the duration and closed again afterwards.
	 */
	private static void registerEverything() {
		List<net.minecraftforge.registries.ForgeRegistry<?>> opened = new java.util.ArrayList<>();
		for (var registry : net.minecraftforge.registries.RegistryManager.ACTIVE.getRegistries().values()) {
			if (registry instanceof net.minecraftforge.registries.ForgeRegistry<?> forge && forge.isLocked()) {
				forge.unfreeze();
				opened.add(forge);
			}
		}
		try {
			Bakery.init();
		} finally {
			opened.forEach(net.minecraftforge.registries.ForgeRegistry::freeze);
		}
	}

	/** One channel for every packet; built once the mod's payload types exist. */
	private static synchronized void createChannel() {
		if (channel != null) return;
		PayloadFlow<RegistryByteBuf, CustomPayload> flow = ChannelBuilder.named(Bakery.id("main")).networkProtocolVersion(1).optional()
				.payloadChannel().play().clientbound();
		for (BakeryNetworking.ToClient<?> type : BakeryNetworking.TO_CLIENT) toClient(flow, type);
		flow = flow.serverbound();
		for (BakeryNetworking.ToServer<?> type : BakeryNetworking.TO_SERVER) toServer(flow, type);
		channel = flow.build();
	}

	private static <T extends CustomPayload> void toClient(PayloadFlow<RegistryByteBuf, CustomPayload> flow, BakeryNetworking.ToClient<T> type) {
		flow.add(type.id(), type.codec(), (payload, context) -> {
			context.enqueueWork(() -> clientReceiver.accept(payload));
			context.setPacketHandled(true);
		});
	}

	private static <T extends CustomPayload> void toServer(PayloadFlow<RegistryByteBuf, CustomPayload> flow, BakeryNetworking.ToServer<T> type) {
		flow.add(type.id(), type.codec(), (payload, context) -> {
			ServerPlayerEntity player = context.getSender();
			if (player != null) context.enqueueWork(() -> type.handler().accept(payload, player));
			context.setPacketHandled(true);
		});
	}
}
