package com.bakery.client;

import com.bakery.baking.BakingRecipes;
import com.bakery.block.PlacedFoodBlock;
import com.bakery.food.BakeryFoods;
import com.bakery.food.Freshness;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import com.bakery.kitchen.ChurnBlockEntity;
import com.bakery.kitchen.JamPotBlockEntity;
import com.bakery.kitchen.JuicerBlockEntity;
import com.bakery.network.BakeResultPayload;
import com.bakery.network.OpenRollingPayload;
import com.bakery.network.OrdersPayload;
import com.bakery.network.ReviewPayload;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryEntities;
import com.bakery.registry.BakeryItems;
import com.bakery.tree.WoodSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class BakeryClient implements ClientModInitializer {
	private static final int OPAQUE = 0xFF000000;
	private static final int MOLD_TINT = 0xFF9DB070;

	@Override
	public void onInitializeClient() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> DecoratingHud.render(context));
		// Dyed furniture: the painted parts take the dye's (pastel) colour.
		net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
				(state, world, pos, tint) -> tint == 0 ? com.bakery.deco.Dyeable.tint(state) : -1,
				BakeryBlocks.CAFE_TABLE, BakeryBlocks.CAFE_CHAIR, BakeryBlocks.PINK_AWNING, BakeryBlocks.MINT_AWNING, BakeryBlocks.BAKERY_COUNTER,
				BakeryBlocks.DISPLAY_CASE, BakeryBlocks.MIXING_BOWL, BakeryBlocks.BAKERY_CATALOG, BakeryBlocks.PARASOL_TABLE, BakeryBlocks.PASTEL_BENCH);
		// Where an ingredient or bake came from: catalog stock, or grown and made at home.
		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (Boolean.TRUE.equals(stack.get(com.bakery.registry.BakeryComponents.STORE_BOUGHT))) {
				lines.add(net.minecraft.text.Text.translatable("tooltip.bakery.store_bought").withColor(0xFF9A8C92));
			}
			if (Boolean.TRUE.equals(stack.get(com.bakery.registry.BakeryComponents.HOMEMADE))) {
				lines.add(net.minecraft.text.Text.translatable("tooltip.bakery.homemade").withColor(0xFF5DAA55));
			}
		});
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), BakeryBlocks.GRAND_OVEN, BakeryBlocks.OVEN_HEART, BakeryBlocks.MIXING_BOWL,
				BakeryBlocks.PASTRY_BOARD, BakeryBlocks.BREAD_LOAF, BakeryBlocks.BAGUETTE, BakeryBlocks.CROISSANT, BakeryBlocks.CUPCAKE,
				BakeryBlocks.DONUT, BakeryBlocks.COOKIES, BakeryBlocks.MACARONS, BakeryBlocks.PIE, BakeryBlocks.CAKE, BakeryBlocks.MUG,
				BakeryBlocks.BAKERY_COUNTER, BakeryBlocks.SERVICE_BELL, BakeryBlocks.CAFE_TABLE, BakeryBlocks.CAFE_CHAIR, BakeryBlocks.FLOUR_SACK,
				BakeryBlocks.BREAD_BASKET, BakeryBlocks.CAKE_STAND, BakeryBlocks.MENU_BOARD, BakeryBlocks.BUNTING, BakeryBlocks.JAR_SHELF,
				BakeryBlocks.STRAWBERRY_CRATE, BakeryBlocks.LEMON_CRATE, BakeryBlocks.COCONUT_CRATE, BakeryBlocks.GRAND_MILLSTONE,
				BakeryBlocks.CHOCOLATE_FOUNTAIN, BakeryBlocks.MILLSTONE_CORE,
				BakeryBlocks.ICE_CREAM_CHURN, BakeryBlocks.JAM_POT, BakeryBlocks.PROOFING_BASKET);
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), BakeryBlocks.CAKE_TURNTABLE, BakeryBlocks.BAKERY_CATALOG,
				BakeryBlocks.PINK_HEART_BALLOON, BakeryBlocks.LILAC_HEART_BALLOON, BakeryBlocks.MINT_HEART_BALLOON, BakeryBlocks.FAIRY_LIGHTS,
				BakeryBlocks.CUPCAKE_LAMP, BakeryBlocks.COOKIE_JAR, BakeryBlocks.MACARON_TOWER, BakeryBlocks.PINK_AWNING, BakeryBlocks.MINT_AWNING);
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), BakeryBlocks.COZY_DECOR);
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), BakeryBlocks.PINK_PICKET_FENCE);
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), BakeryBlocks.TIP_JAR, BakeryBlocks.COOKIE_JAR);
		BlockRenderLayerMap.INSTANCE.putBlock(BakeryBlocks.JUICER, RenderLayer.getTranslucent());
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), BakeryBlocks.CROPS);
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), BakeryBlocks.FRUIT_BLOCKS);
		BlockRenderLayerMap.INSTANCE.putBlock(BakeryBlocks.DISPLAY_CASE, RenderLayer.getTranslucent());
		for (WoodSet set : BakeryBlocks.WOODS) {
			BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), set.door, set.trapdoor, set.sapling);
			BlockRenderLayerMap.INSTANCE.putBlock(set.leaves, RenderLayer.getCutoutMipped());
		}

		// Frosting, glaze, fillings and drinks take the color of their flavor.
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (tintIndex != 0) return -1;
			// A cake decorated at the turntable keeps the frosting colour that was chosen there.
			if (state.isOf(BakeryBlocks.CAKE) && world != null && pos != null && world.getBlockEntity(pos) instanceof com.bakery.block.PlacedFoodBlockEntity food) {
				com.bakery.sweet.CakeDesign design = food.getStack().get(BakeryComponents.CAKE_DESIGN);
				if (design != null && design.tiers().getFirst() != -1) return OPAQUE | design.tiers().getFirst();
			}
			if (state.contains(PlacedFoodBlock.FLAVOR)) return OPAQUE | state.get(PlacedFoodBlock.FLAVOR).color();
			if (state.contains(PlacedFoodBlock.DRINK)) return OPAQUE | state.get(PlacedFoodBlock.DRINK).color();
			return -1;
		}, BakeryBlocks.CUPCAKE, BakeryBlocks.DONUT, BakeryBlocks.COOKIES, BakeryBlocks.MACARONS, BakeryBlocks.PIE, BakeryBlocks.CAKE, BakeryBlocks.MUG);

		// Juice in the juicer and jam in the pot take the fruit's color; burnt jam goes dark.
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (tintIndex != 0 || world == null || pos == null) return -1;
			Object be = world.getBlockEntity(pos);
			if (be instanceof JuicerBlockEntity juicer) return OPAQUE | juicer.flavor().color();
			if (be instanceof JamPotBlockEntity pot) return OPAQUE | (pot.stage() == JamPotBlockEntity.BURNT ? 0x3A2420 : pot.flavor().color());
			if (be instanceof ChurnBlockEntity churn) return OPAQUE | churn.flavor().color();
			return -1;
		}, BakeryBlocks.JUICER, BakeryBlocks.JAM_POT, BakeryBlocks.ICE_CREAM_CHURN);

		// Zucchini stems ripen from green to yellow like melon stems.
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
			if (state.contains(net.minecraft.block.StemBlock.AGE)) {
				int age = state.get(net.minecraft.block.StemBlock.AGE);
				return OPAQUE | (age * 32) << 16 | (255 - age * 8) << 8 | age * 4;
			}
			return OPAQUE | 0xE0C71C;
		}, BakeryBlocks.ZUCCHINI_STEM, BakeryBlocks.ATTACHED_ZUCCHINI_STEM);

		// Items: layer 0 turns moldy green when spoiled, layer 1 is the flavor color.
		Item[] tinted = {BakeryItems.BREAD_DOUGH, BakeryItems.SWEET_DOUGH, BakeryItems.PASTRY_DOUGH, BakeryItems.COOKIE_DOUGH,
				BakeryItems.RAW_LOAF, BakeryItems.RAW_BAGUETTE, BakeryItems.RAW_CROISSANT, BakeryItems.RAW_CUPCAKE, BakeryItems.RAW_DONUT,
				BakeryItems.RAW_COOKIES, BakeryItems.RAW_MACARONS, BakeryItems.RAW_CAKE, BakeryItems.RAW_PIE,
				BakeryItems.BREAD_LOAF, BakeryItems.BAGUETTE, BakeryItems.CROISSANT, BakeryItems.CUPCAKE, BakeryItems.DONUT,
				BakeryItems.COOKIES, BakeryItems.MACARONS, BakeryItems.CAKE, BakeryItems.PIE,
				BakeryItems.STRAWBERRY, BakeryItems.BLUEBERRY, BakeryItems.RASPBERRY, BakeryItems.WHIPPED_CREAM, BakeryItems.BUTTER,
				BakeryItems.ROSE_PETALS, BakeryItems.TEA_LEAVES, BakeryItems.MINT_LEAVES, BakeryItems.LEMON, BakeryItems.PINEAPPLE,
				BakeryItems.MATCHA_LATTE, BakeryItems.HOT_CHOCOLATE, BakeryItems.STRAWBERRY_MILK, BakeryItems.LAVENDER_TEA,
				BakeryItems.ROSE_TEA, BakeryItems.STARLIGHT_TEA, BakeryItems.LEMONADE, BakeryItems.COCONUT_MILK, BakeryItems.MINT_TEA,
				BakeryItems.PINEAPPLE_SMOOTHIE, BakeryItems.JUICE, BakeryItems.ICE_CREAM, BakeryItems.FRUIT_JAM, BakeryItems.CHOCOLATE_TREAT,
				BakeryItems.CHERRIES, BakeryItems.ORANGE, BakeryItems.PEACH, BakeryItems.MANGO, BakeryItems.PLUM, BakeryItems.CORN,
				BakeryItems.PEA_POD, BakeryItems.SWEET_POTATO, BakeryItems.BAKED_SWEET_POTATO, BakeryItems.ZUCCHINI, BakeryItems.RHUBARB,
				BakeryItems.SUGAR_BEET, BakeryItems.CRANBERRY};
		ColorProviderRegistry.ITEM.register(BakeryClient::itemColor, tinted);

		// Sweet Life: icing bags show their colour, cookies their icing, gifts their ribbon.
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 1 && stack.getItem() instanceof com.bakery.sweet.IcingBagItem bag
				? OPAQUE | bag.icing().color() : -1, java.util.Arrays.stream(com.bakery.sweet.Icing.values()).map(com.bakery.sweet.Icing::bag)
				.toArray(net.minecraft.item.ItemConvertible[]::new));
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
			if (tintIndex == 0) return itemColor(stack, 0);
			if (tintIndex == 1) return OPAQUE | stack.getOrDefault(BakeryComponents.ICING, 0xFFFFFF);
			return -1;
		}, BakeryItems.SUGAR_COOKIE, BakeryItems.RAW_SUGAR_COOKIE);
		ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 1 ? OPAQUE | stack.getOrDefault(BakeryComponents.GIFT_COLOR, 0xF7A8C6) : -1,
				BakeryItems.WRAPPED_GIFT);
		for (Item cookie : new Item[]{BakeryItems.SUGAR_COOKIE, BakeryItems.RAW_SUGAR_COOKIE}) {
			ModelPredicateProviderRegistry.register(cookie, com.bakery.Bakery.id("shape"),
					(stack, world, entity, seed) -> com.bakery.sweet.CookieShape.of(stack).ordinal() * 0.25f);
			ModelPredicateProviderRegistry.register(cookie, com.bakery.Bakery.id("iced"),
					(stack, world, entity, seed) -> stack.contains(BakeryComponents.ICING) ? 1f : 0f);
		}

		BlockEntityRendererFactories.register(BakeryBlockEntities.GRAND_OVEN, GrandOvenRenderer::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.DISPLAY_CASE, DisplayCaseRenderer::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.TURNTABLE, TurntableRenderer::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.PLACED_FOOD, PlacedCakeRenderer::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.JUICER, KitchenRenderers.Juicer::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.CHURN, KitchenRenderers.Churn::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.JAM_POT, KitchenRenderers.JamPot::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.PROOFING_BASKET, KitchenRenderers.Proofing::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.MIXING_BOWL, KitchenRenderers.Bowl::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.PASTRY_BOARD, KitchenRenderers.Board::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.MILLSTONE, KitchenRenderers.Millstone::new);
		BlockEntityRendererFactories.register(BakeryBlockEntities.FOUNTAIN, KitchenRenderers.Fountain::new);
		PixelBox.registerParts();
		EntityRendererRegistry.register(BakeryEntities.SEAT, EmptyEntityRenderer::new);

		EntityRendererRegistry.register(BakeryEntities.CUSTOMER, CustomerRenderer::new);
		com.bakery.client.dev.Showcase.init();
		PathPreview.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world != null) {
				Freshness.clientTime = client.world.getTime();
			}
			ClientOrders.tick();
			WorldPopups.tick();
		});
		WorldRenderEvents.AFTER_ENTITIES.register(ClientOrders::renderBubbles);
		WorldRenderEvents.AFTER_ENTITIES.register(WorldPopups::render);

		ClientPlayNetworking.registerGlobalReceiver(OpenRollingPayload.ID, (payload, context) -> {
			BakingRecipes.DoughType[] types = BakingRecipes.DoughType.values();
			if (payload.doughType() >= 0 && payload.doughType() < types.length) {
				context.client().setScreen(new RollingScreen(payload.pos(), types[payload.doughType()]));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(ReviewPayload.ID, (payload, context) -> WorldPopups.review(payload));
		ClientPlayNetworking.registerGlobalReceiver(OrdersPayload.ID, (payload, context) -> ClientOrders.receive(payload));
		ClientPlayNetworking.registerGlobalReceiver(BakeResultPayload.ID, (payload, context) -> WorldPopups.bake(payload));
		ClientPlayNetworking.registerGlobalReceiver(com.bakery.network.SweetPayloads.OpenCatalog.ID, (payload, context) ->
				context.client().setScreen(new CatalogScreen(payload.pos(), payload.data())));
		ClientPlayNetworking.registerGlobalReceiver(com.bakery.network.SweetPayloads.OpenCounter.ID, (payload, context) ->
				context.client().setScreen(new CounterScreen(payload.counter())));
		ClientPlayNetworking.registerGlobalReceiver(com.bakery.network.SweetPayloads.OpenMenu.ID, (payload, context) ->
				context.client().setScreen(new MenuScreen(payload.counter())));
		ClientPlayNetworking.registerGlobalReceiver(com.bakery.network.StickerPayload.ID, (payload, context) -> {
			com.bakery.sweet.Sticker[] stickers = com.bakery.sweet.Sticker.values();
			if (payload.sticker() >= 0 && payload.sticker() < stickers.length) {
				context.client().getToastManager().add(new StickerToast(stickers[payload.sticker()]));
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(com.bakery.network.SweetPayloads.GuideData.ID, (payload, context) ->
				context.client().setScreen(new GuideScreen(payload)));
	}

	private static int itemColor(ItemStack stack, int tintIndex) {
		if (tintIndex == 0) {
			boolean spoiled = Freshness.clientTime > 0 && Freshness.isPerishable(stack)
					&& Freshness.stage(stack, Freshness.clientTime) == Freshness.Stage.SPOILED;
			return spoiled ? MOLD_TINT : -1;
		}
		if (tintIndex == 1) {
			return stack.contains(BakeryComponents.FLAVOR) ? OPAQUE | BakeryFoods.flavor(stack).color() : OPAQUE | 0xF6E7C8;
		}
		return -1;
	}
}
