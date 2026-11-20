package com.bakery.fabric.client;

import com.bakery.client.BakeryClient;
import com.bakery.client.ClientContent;
import com.bakery.network.BakeryNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Sweetheart Bakery's client on Fabric. */
public final class BakeryFabricClient implements ClientModInitializer {
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void onInitializeClient() {
		ClientContent.install(new ClientContent.Loader() {
			@Override
			public void sendToServer(CustomPayload payload) {
				ClientPlayNetworking.send(payload);
			}

			@Override
			public BakedModel extraModel(Identifier id) {
				return MinecraftClient.getInstance().getBakedModelManager().getModel(id);
			}
		});
		BakeryClient.init();

		for (ClientContent.Layer layer : ClientContent.LAYERS) BlockRenderLayerMap.INSTANCE.putBlock(layer.block(), layer.layer());
		for (ClientContent.BlockTint tint : ClientContent.BLOCK_TINTS) ColorProviderRegistry.BLOCK.register(tint.provider(), tint.blocks());
		for (ClientContent.ItemTint tint : ClientContent.ITEM_TINTS) ColorProviderRegistry.ITEM.register(tint.provider(), tint.items());
		for (ClientContent.BlockEntityRenderer r : ClientContent.BLOCK_ENTITY_RENDERERS) BlockEntityRendererFactories.register(r.type(), r.factory());
		for (ClientContent.EntityRenderer r : ClientContent.ENTITY_RENDERERS) EntityRendererRegistry.register(r.type(), r.factory());
		ModelLoadingPlugin.register(context -> context.addModels(ClientContent.EXTRA_MODELS));

		for (BakeryNetworking.ToClient<?> type : BakeryNetworking.TO_CLIENT) {
			ClientPlayNetworking.registerGlobalReceiver((CustomPayload.Id) type.id(), (payload, context) -> ClientContent.handle((CustomPayload) payload));
		}
		ClientTickEvents.END_CLIENT_TICK.register(ClientContent::tick);
		WorldRenderEvents.AFTER_ENTITIES.register(context -> {
			if (context.consumers() == null) return;
			ClientContent.renderWorld(context.matrixStack(), context.consumers(), context.camera(), context.tickCounter().getTickDelta(false));
		});
		HudRenderCallback.EVENT.register((context, tickCounter) -> ClientContent.renderHud(context));
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> ClientContent.tooltip(stack, lines));
		ClientReceiveMessageEvents.GAME.register(ClientContent::gameMessage);
	}
}
