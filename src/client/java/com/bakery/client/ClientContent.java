package com.bakery.client;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.item.ItemColorProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Everything the client side of the mod registers, collected by {@link BakeryClient#init()} and handed to the game by
 * each mod loader its own way: render layers, colours, renderers, extra models, packet receivers, and the listeners
 * for client ticks, world rendering, the HUD, tooltips and chat.
 */
public final class ClientContent {
	/** What the client needs from its mod loader. */
	public interface Loader {
		void sendToServer(CustomPayload payload);

		/** A model registered in {@link #EXTRA_MODELS}, baked. */
		BakedModel extraModel(Identifier id);
	}

	public interface WorldRenderer {
		void render(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta);
	}

	public record Layer(Block block, RenderLayer layer) {
	}

	public record BlockTint(BlockColorProvider provider, Block[] blocks) {
	}

	public record ItemTint(ItemColorProvider provider, ItemConvertible[] items) {
	}

	public record BlockEntityRenderer<T extends BlockEntity>(BlockEntityType<? extends T> type, BlockEntityRendererFactory<T> factory) {
	}

	public record EntityRenderer<T extends Entity>(EntityType<? extends T> type, EntityRendererFactory<T> factory) {
	}

	public static final List<Layer> LAYERS = new ArrayList<>();
	public static final List<BlockTint> BLOCK_TINTS = new ArrayList<>();
	public static final List<ItemTint> ITEM_TINTS = new ArrayList<>();
	public static final List<BlockEntityRenderer<?>> BLOCK_ENTITY_RENDERERS = new ArrayList<>();
	public static final List<EntityRenderer<?>> ENTITY_RENDERERS = new ArrayList<>();
	/** Models drawn by renderers rather than by blockstates (they must be loaded explicitly). */
	public static final List<Identifier> EXTRA_MODELS = new ArrayList<>();
	public static final Map<CustomPayload.Id<?>, Consumer<CustomPayload>> RECEIVERS = new LinkedHashMap<>();
	public static final List<Consumer<MinecraftClient>> TICK = new ArrayList<>();
	public static final List<WorldRenderer> WORLD_RENDER = new ArrayList<>();
	public static final List<Consumer<DrawContext>> HUD = new ArrayList<>();
	public static final List<BiConsumer<ItemStack, List<Text>>> TOOLTIP = new ArrayList<>();
	/** Game (system) messages arriving in chat or the action bar: (message, actionBar). */
	public static final List<BiConsumer<Text, Boolean>> GAME_MESSAGES = new ArrayList<>();

	private static Loader loader;

	private ClientContent() {
	}

	public static void install(Loader clientLoader) {
		loader = clientLoader;
	}

	public static void sendToServer(CustomPayload payload) {
		loader.sendToServer(payload);
	}

	public static BakedModel extraModel(Identifier id) {
		return loader.extraModel(id);
	}

	public static void layer(RenderLayer layer, Block... blocks) {
		for (Block block : blocks) LAYERS.add(new Layer(block, layer));
	}

	public static void blockTint(BlockColorProvider provider, Block... blocks) {
		BLOCK_TINTS.add(new BlockTint(provider, blocks));
	}

	public static void itemTint(ItemColorProvider provider, ItemConvertible... items) {
		ITEM_TINTS.add(new ItemTint(provider, items));
	}

	public static <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<? extends T> type, BlockEntityRendererFactory<T> factory) {
		BLOCK_ENTITY_RENDERERS.add(new BlockEntityRenderer<>(type, factory));
	}

	public static <T extends Entity> void entityRenderer(EntityType<? extends T> type, EntityRendererFactory<T> factory) {
		ENTITY_RENDERERS.add(new EntityRenderer<>(type, factory));
	}

	/** What the client does when one of the mod's packets arrives (always run on the client thread). */
	@SuppressWarnings("unchecked")
	public static <T extends CustomPayload> void receive(CustomPayload.Id<T> id, Consumer<T> handler) {
		RECEIVERS.put(id, payload -> handler.accept((T) payload));
	}

	/** Called by the loader when a packet arrives (on the client thread). */
	public static void handle(CustomPayload payload) {
		Consumer<CustomPayload> handler = RECEIVERS.get(payload.getId());
		if (handler != null) handler.accept(payload);
	}

	public static void tick(MinecraftClient client) {
		for (Consumer<MinecraftClient> listener : TICK) listener.accept(client);
	}

	public static void renderWorld(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float tickDelta) {
		for (WorldRenderer renderer : WORLD_RENDER) renderer.render(matrices, consumers, camera, tickDelta);
	}

	public static void renderHud(DrawContext context) {
		for (Consumer<DrawContext> renderer : HUD) renderer.accept(context);
	}

	public static void tooltip(ItemStack stack, List<Text> lines) {
		for (BiConsumer<ItemStack, List<Text>> listener : TOOLTIP) listener.accept(stack, lines);
	}

	public static void gameMessage(Text message, boolean actionBar) {
		for (BiConsumer<Text, Boolean> listener : GAME_MESSAGES) listener.accept(message, actionBar);
	}
}
