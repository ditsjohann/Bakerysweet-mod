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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The client side of Sweetheart Bakery. {@link #init()} registers everything into {@link ClientContent} (plus the
 * item model predicates, which are plain Minecraft); each mod loader's client entry point calls it and hands the
 * collected content to the game.
 */
public final class BakeryClient {
	private static final int OPAQUE = 0xFF000000;
	private static final int MOLD_TINT = 0xFF9DB070;

	private static boolean initialized;

	private BakeryClient() {
	}

	public static void init() {
		if (initialized) return;
		initialized = true;
		ClientContent.HUD.add(DecoratingHud::render);
		// Dyed furniture: the painted parts take the dye's (pastel) colour.
		ClientContent.blockTint(
				(state, world, pos, tint) -> tint == 0 ? com.bakery.deco.Dyeable.tint(state) : -1,
				BakeryBlocks.CAFE_TABLE, BakeryBlocks.CAFE_CHAIR, BakeryBlocks.PINK_AWNING, BakeryBlocks.MINT_AWNING, BakeryBlocks.BAKERY_COUNTER,
				BakeryBlocks.DISPLAY_CASE, BakeryBlocks.MIXING_BOWL, BakeryBlocks.BAKERY_CATALOG, BakeryBlocks.PARASOL_TABLE, BakeryBlocks.PASTEL_BENCH);
		// Where an ingredient or bake came from: catalog stock, or grown and made at home.
		ClientContent.TOOLTIP.add((stack, lines) -> {
			if (Boolean.TRUE.equals(stack.get(com.bakery.registry.BakeryComponents.STORE_BOUGHT))) {
				lines.add(net.minecraft.text.Text.translatable("tooltip.bakery.store_bought").withColor(0xFF9A8C92));
			}
			if (Boolean.TRUE.equals(stack.get(com.bakery.registry.BakeryComponents.HOMEMADE))) {
				lines.add(net.minecraft.text.Text.translatable("tooltip.bakery.homemade").withColor(0xFF5DAA55));
			}
		});
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.GRAND_OVEN, BakeryBlocks.OVEN_HEART, BakeryBlocks.MIXING_BOWL,
				BakeryBlocks.PASTRY_BOARD, BakeryBlocks.BREAD_LOAF, BakeryBlocks.BAGUETTE, BakeryBlocks.CROISSANT, BakeryBlocks.CUPCAKE,
				BakeryBlocks.DONUT, BakeryBlocks.COOKIES, BakeryBlocks.MACARONS, BakeryBlocks.PIE, BakeryBlocks.CAKE, BakeryBlocks.MUG,
				BakeryBlocks.BAKERY_COUNTER, BakeryBlocks.SERVICE_BELL, BakeryBlocks.CAFE_TABLE, BakeryBlocks.CAFE_CHAIR, BakeryBlocks.FLOUR_SACK,
				BakeryBlocks.BREAD_BASKET, BakeryBlocks.CAKE_STAND, BakeryBlocks.MENU_BOARD, BakeryBlocks.BUNTING, BakeryBlocks.JAR_SHELF,
				BakeryBlocks.STRAWBERRY_CRATE, BakeryBlocks.LEMON_CRATE, BakeryBlocks.COCONUT_CRATE, BakeryBlocks.GRAND_MILLSTONE,
				BakeryBlocks.CHOCOLATE_FOUNTAIN, BakeryBlocks.MILLSTONE_CORE,
				BakeryBlocks.ICE_CREAM_CHURN, BakeryBlocks.JAM_POT, BakeryBlocks.PROOFING_BASKET);
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.CAKE_TURNTABLE, BakeryBlocks.BAKERY_CATALOG,
				BakeryBlocks.PINK_HEART_BALLOON, BakeryBlocks.LILAC_HEART_BALLOON, BakeryBlocks.MINT_HEART_BALLOON, BakeryBlocks.FAIRY_LIGHTS,
				BakeryBlocks.CUPCAKE_LAMP, BakeryBlocks.COOKIE_JAR, BakeryBlocks.MACARON_TOWER, BakeryBlocks.PINK_AWNING, BakeryBlocks.MINT_AWNING);
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.COZY_DECOR);
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.NATURE);
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.COFFEE_MACHINE, BakeryBlocks.SYRUP_RACK);
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.PINK_PICKET_FENCE);
		ClientContent.layer(RenderLayer.getTranslucent(), BakeryBlocks.TIP_JAR, BakeryBlocks.COOKIE_JAR);
		ClientContent.layer(RenderLayer.getTranslucent(), BakeryBlocks.JUICER);
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.CROPS);
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.FRUIT_BLOCKS);
		ClientContent.layer(RenderLayer.getTranslucent(), BakeryBlocks.DISPLAY_CASE);
		ClientContent.layer(RenderLayer.getTranslucent(), BakeryBlocks.GLASS_CLOCHE);
		ClientContent.layer(RenderLayer.getCutout(), BakeryBlocks.TIERED_STAND, BakeryBlocks.PASTRY_SHELF);
		for (WoodSet set : BakeryBlocks.WOODS) {
			ClientContent.layer(RenderLayer.getCutout(), set.trapdoor, set.sapling);
			// Doors are translucent so their window panes are real see-through glass.
			ClientContent.layer(RenderLayer.getTranslucent(), set.door);
			ClientContent.layer(RenderLayer.getCutoutMipped(), set.leaves);
		}

		// Frosting, glaze, fillings and drinks take the color of their flavor.
		ClientContent.blockTint((state, world, pos, tintIndex) -> {
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
		ClientContent.blockTint((state, world, pos, tintIndex) -> {
			if (tintIndex != 0 || world == null || pos == null) return -1;
			Object be = world.getBlockEntity(pos);
			if (be instanceof JuicerBlockEntity juicer) return OPAQUE | juicer.flavor().color();
			if (be instanceof JamPotBlockEntity pot) return OPAQUE | (pot.stage() == JamPotBlockEntity.BURNT ? 0x3A2420 : pot.flavor().color());
			if (be instanceof ChurnBlockEntity churn) return OPAQUE | churn.flavor().color();
			return -1;
		}, BakeryBlocks.JUICER, BakeryBlocks.JAM_POT, BakeryBlocks.ICE_CREAM_CHURN);

		// Zucchini stems ripen from green to yellow like melon stems.
		ClientContent.blockTint((state, world, pos, tintIndex) -> {
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
		ClientContent.itemTint(BakeryClient::itemColor, tinted);
		// Coffee: layer 1 is the syrup drizzle, hidden in the drink's own colour until a syrup is pumped in.
		ClientContent.itemTint((stack, tintIndex) -> {
			if (tintIndex != 1) return itemColor(stack, tintIndex);
			if (stack.contains(BakeryComponents.FLAVOR)) return OPAQUE | BakeryFoods.flavor(stack).color();
			return OPAQUE | (stack.isOf(BakeryItems.LATTE) ? 0xF4E6D2 : 0x4A2C1C);
		}, BakeryItems.COFFEE, BakeryItems.LATTE);

		// Sweet Life: icing bags show their colour, cookies their icing, gifts their ribbon.
		ClientContent.itemTint((stack, tintIndex) -> tintIndex == 1 && stack.getItem() instanceof com.bakery.sweet.IcingBagItem bag
				? OPAQUE | bag.icing().color() : -1, java.util.Arrays.stream(com.bakery.sweet.Icing.values()).map(com.bakery.sweet.Icing::bag)
				.toArray(net.minecraft.item.ItemConvertible[]::new));
		ClientContent.itemTint((stack, tintIndex) -> {
			if (tintIndex == 0) return itemColor(stack, 0);
			if (tintIndex == 1) return OPAQUE | stack.getOrDefault(BakeryComponents.ICING, 0xFFFFFF);
			return -1;
		}, BakeryItems.SUGAR_COOKIE, BakeryItems.RAW_SUGAR_COOKIE);
		ClientContent.itemTint((stack, tintIndex) -> tintIndex == 1 ? OPAQUE | stack.getOrDefault(BakeryComponents.GIFT_COLOR, 0xF7A8C6) : -1,
				BakeryItems.WRAPPED_GIFT);
		for (Item cookie : new Item[]{BakeryItems.SUGAR_COOKIE, BakeryItems.RAW_SUGAR_COOKIE}) {
			ModelPredicateProviderRegistry.register(cookie, com.bakery.Bakery.id("shape"),
					(stack, world, entity, seed) -> com.bakery.sweet.CookieShape.of(stack).ordinal() * 0.25f);
			ModelPredicateProviderRegistry.register(cookie, com.bakery.Bakery.id("iced"),
					(stack, world, entity, seed) -> stack.contains(BakeryComponents.ICING) ? 1f : 0f);
		}

		ClientContent.blockEntityRenderer(BakeryBlockEntities.GRAND_OVEN, GrandOvenRenderer::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.DISPLAY_CASE, DisplayCaseRenderer::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.DISPLAY_STAND, DisplayStandRenderer::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.ORDER_NOTEBOOK, OrderNotebookRenderer::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.TURNTABLE, TurntableRenderer::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.PLACED_FOOD, PlacedCakeRenderer::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.JUICER, KitchenRenderers.Juicer::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.CHURN, KitchenRenderers.Churn::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.JAM_POT, KitchenRenderers.JamPot::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.PROOFING_BASKET, KitchenRenderers.Proofing::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.MIXING_BOWL, KitchenRenderers.Bowl::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.PASTRY_BOARD, KitchenRenderers.Board::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.MILLSTONE, KitchenRenderers.Millstone::new);
		ClientContent.blockEntityRenderer(BakeryBlockEntities.FOUNTAIN, KitchenRenderers.Fountain::new);
		PixelBox.registerParts();
		ClientContent.entityRenderer(BakeryEntities.SEAT, EmptyEntityRenderer::new);

		ClientContent.entityRenderer(BakeryEntities.CUSTOMER, CustomerRenderer::new);
		JukeboxMusic.init();
		ClientContent.entityRenderer(BakeryEntities.CAFE_CAT, net.minecraft.client.render.entity.CatEntityRenderer::new);
		net.minecraft.client.item.ModelPredicateProviderRegistry.register(BakeryItems.KITTEN_BASKET, com.bakery.Bakery.id("empty"),
				(stack, world, entity, seed) -> com.bakery.pets.KittenBasketItem.isEmpty(stack) ? 1f : 0f);
		com.bakery.client.dev.Showcase.init();
		PathPreview.init();

		ClientContent.TICK.add(client -> {
			if (client.world != null) {
				Freshness.clientTime = client.world.getTime();
			}
			ClientOrders.tick();
			WorldPopups.tick();
		});
		ClientContent.WORLD_RENDER.add(ClientOrders::renderBubbles);
		ClientContent.WORLD_RENDER.add(WorldPopups::render);

		ClientContent.receive(OpenRollingPayload.ID, payload -> {
			BakingRecipes.DoughType[] types = BakingRecipes.DoughType.values();
			if (payload.doughType() >= 0 && payload.doughType() < types.length) {
				MinecraftClient.getInstance().setScreen(new RollingScreen(payload.pos(), types[payload.doughType()]));
			}
		});
		ClientContent.receive(ReviewPayload.ID, payload -> WorldPopups.review(payload));
		ClientContent.receive(OrdersPayload.ID, payload -> ClientOrders.receive(payload));
		ClientContent.receive(BakeResultPayload.ID, payload -> WorldPopups.bake(payload));
		ClientContent.receive(com.bakery.network.AppraisalPayload.ID, payload -> WorldPopups.appraisal(payload));
		ClientContent.receive(com.bakery.network.SweetPayloads.OpenCatalog.ID, payload ->
				MinecraftClient.getInstance().setScreen(new CatalogScreen(payload.pos(), payload.data())));
		ClientContent.receive(com.bakery.network.SweetPayloads.OpenCounter.ID, payload ->
				MinecraftClient.getInstance().setScreen(new CounterScreen(payload.counter())));
		ClientContent.receive(com.bakery.network.SweetPayloads.OpenMenu.ID, payload ->
				MinecraftClient.getInstance().setScreen(new MenuScreen(payload.counter())));
		ClientContent.receive(com.bakery.network.SweetPayloads.OpenCounter.ID, payload ->
				MinecraftClient.getInstance().setScreen(new CounterScreen(payload.counter())));
		ClientContent.receive(com.bakery.network.SweetPayloads.OpenNotebook.ID, payload ->
				MinecraftClient.getInstance().setScreen(new OrderNotebookScreen(payload.pos())));
		ClientContent.receive(com.bakery.network.StickerPayload.ID, payload -> {
			com.bakery.sweet.Sticker[] stickers = com.bakery.sweet.Sticker.values();
			if (payload.sticker() >= 0 && payload.sticker() < stickers.length) {
				MinecraftClient.getInstance().getToastManager().add(new StickerToast(stickers[payload.sticker()]));
			}
		});
		ClientContent.receive(com.bakery.network.SweetPayloads.GuideData.ID, payload ->
				MinecraftClient.getInstance().setScreen(new GuideScreen(payload)));
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
