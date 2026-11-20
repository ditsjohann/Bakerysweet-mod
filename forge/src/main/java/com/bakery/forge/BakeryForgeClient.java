package com.bakery.forge;

import com.bakery.Bakery;
import com.bakery.client.BakeryClient;
import com.bakery.client.ClientContent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.PacketDistributor;

/** Sweetheart Bakery's client on Forge. */
final class BakeryForgeClient {
	private BakeryForgeClient() {
	}

	/** Extra (renderer-drawn) models are loaded under this variant. */
	private static ModelIdentifier extra(Identifier id) {
		return new ModelIdentifier(id, "standalone");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static void init(IEventBus modBus) {
		ClientContent.install(new ClientContent.Loader() {
			@Override
			public void sendToServer(CustomPayload payload) {
				BakeryForge.channel.send(payload, PacketDistributor.SERVER.noArg());
			}

			@Override
			public BakedModel extraModel(Identifier id) {
				return MinecraftClient.getInstance().getBakedModelManager().getModel(extra(id));
			}
		});
		BakeryForge.clientReceiver = ClientContent::handle;

		// BakeryClient.init() needs the mod's blocks and items, so it runs at the first client event after registration.
		modBus.addListener(EventPriority.NORMAL, false, RegisterColorHandlersEvent.Block.class, event -> {
			BakeryClient.init();
			for (ClientContent.BlockTint tint : ClientContent.BLOCK_TINTS) event.register(tint.provider(), tint.blocks());
		});
		modBus.addListener(EventPriority.NORMAL, false, RegisterColorHandlersEvent.Item.class, event -> {
			BakeryClient.init();
			for (ClientContent.ItemTint tint : ClientContent.ITEM_TINTS) event.register(tint.provider(), tint.items());
		});
		modBus.addListener(EventPriority.NORMAL, false, EntityRenderersEvent.RegisterRenderers.class, event -> {
			BakeryClient.init();
			for (ClientContent.BlockEntityRenderer r : ClientContent.BLOCK_ENTITY_RENDERERS) event.registerBlockEntityRenderer(r.type(), r.factory());
			for (ClientContent.EntityRenderer r : ClientContent.ENTITY_RENDERERS) event.registerEntityRenderer(r.type(), r.factory());
		});
		modBus.addListener(EventPriority.NORMAL, false, ModelEvent.RegisterAdditional.class, event -> {
			BakeryClient.init();
			for (Identifier id : ClientContent.EXTRA_MODELS) event.register(extra(id));
		});
		modBus.addListener(EventPriority.NORMAL, false, FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
			BakeryClient.init();
			for (ClientContent.Layer layer : ClientContent.LAYERS) RenderLayers.setRenderLayer(layer.block(), layer.layer());
		}));
		modBus.addListener(EventPriority.NORMAL, false, AddGuiOverlayLayersEvent.class, event ->
				event.getLayeredDraw().add(ForgeLayeredDraw.VANILLA_ROOT, Bakery.id("hud"), (context, tickCounter) -> ClientContent.renderHud(context)));

		IEventBus game = MinecraftForge.EVENT_BUS;
		game.addListener(EventPriority.NORMAL, false, TickEvent.ClientTickEvent.Post.class, event -> ClientContent.tick(MinecraftClient.getInstance()));
		game.addListener(EventPriority.NORMAL, false, RenderLevelStageEvent.class, event -> {
			if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
			MinecraftClient client = MinecraftClient.getInstance();
			var consumers = client.getBufferBuilders().getEntityVertexConsumers();
			MatrixStack matrices = new MatrixStack();
			matrices.multiplyPositionMatrix(event.getPoseStack());
			ClientContent.renderWorld(matrices, consumers, event.getCamera(), event.getPartialTick());
			consumers.draw();
		});
		game.addListener(EventPriority.NORMAL, false, ItemTooltipEvent.class, event -> ClientContent.tooltip(event.getItemStack(), event.getToolTip()));
		game.addListener(EventPriority.NORMAL, false, ClientChatReceivedEvent.System.class, event -> ClientContent.gameMessage(event.getMessage(), event.isOverlay()));
	}
}
