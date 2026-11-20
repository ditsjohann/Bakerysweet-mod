package com.bakery.neoforge;

import com.bakery.client.BakeryClient;
import com.bakery.client.ClientContent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sweetheart Bakery's client on NeoForge. */
final class BakeryNeoForgeClient {
	private BakeryNeoForgeClient() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static void init(IEventBus modBus) {
		ClientContent.install(new ClientContent.Loader() {
			@Override
			public void sendToServer(CustomPayload payload) {
				PacketDistributor.sendToServer(payload);
			}

			@Override
			public BakedModel extraModel(Identifier id) {
				return MinecraftClient.getInstance().getBakedModelManager().getModel(ModelIdentifier.standalone(id));
			}
		});
		BakeryNeoForge.clientReceiver = ClientContent::handle;

		// BakeryClient.init() needs the mod's blocks and items, so it runs at the first client event after registration.
		modBus.addListener(RegisterColorHandlersEvent.Block.class, event -> {
			BakeryClient.init();
			for (ClientContent.BlockTint tint : ClientContent.BLOCK_TINTS) event.register(tint.provider(), tint.blocks());
		});
		modBus.addListener(RegisterColorHandlersEvent.Item.class, event -> {
			BakeryClient.init();
			for (ClientContent.ItemTint tint : ClientContent.ITEM_TINTS) event.register(tint.provider(), tint.items());
		});
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
			BakeryClient.init();
			for (ClientContent.BlockEntityRenderer r : ClientContent.BLOCK_ENTITY_RENDERERS) event.registerBlockEntityRenderer(r.type(), r.factory());
			for (ClientContent.EntityRenderer r : ClientContent.ENTITY_RENDERERS) event.registerEntityRenderer(r.type(), r.factory());
		});
		modBus.addListener(ModelEvent.RegisterAdditional.class, event -> {
			BakeryClient.init();
			for (Identifier id : ClientContent.EXTRA_MODELS) event.register(ModelIdentifier.standalone(id));
		});
		modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
			BakeryClient.init();
			for (ClientContent.Layer layer : ClientContent.LAYERS) RenderLayers.setRenderLayer(layer.block(), layer.layer());
		}));

		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> ClientContent.tick(MinecraftClient.getInstance()));
		NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
			if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
			MinecraftClient client = MinecraftClient.getInstance();
			var consumers = client.getBufferBuilders().getEntityVertexConsumers();
			var matrices = event.getPoseStack();
			matrices.push();
			matrices.multiplyPositionMatrix(event.getModelViewMatrix());
			ClientContent.renderWorld(matrices, consumers, event.getCamera(), event.getPartialTick().getTickDelta(false));
			matrices.pop();
			consumers.draw();
		});
		NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, event -> ClientContent.renderHud(event.getGuiGraphics()));
		NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, event -> ClientContent.tooltip(event.getItemStack(), event.getToolTip()));
		NeoForge.EVENT_BUS.addListener(ClientChatReceivedEvent.System.class, event -> ClientContent.gameMessage(event.getMessage(), event.isOverlay()));
	}
}
