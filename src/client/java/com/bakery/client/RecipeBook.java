package com.bakery.client;

import com.bakery.baking.BakingRecipes;
import com.bakery.oven.GrandOvenBlockEntity;
import com.bakery.oven.OvenTemperature;
import com.bakery.registry.BakeryItems;
import com.bakery.sweet.Catalog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every recipe in the bakery, written out as plain numbered steps for the catalog's Recipes tab.
 * Bakes are built from {@link BakingRecipes} (bowl, board, oven), crafting and smelting are read from the game's own
 * recipe list, and the machines (millstone, juicer, churn, jam pot, fountain) are written out by hand.
 */
public final class RecipeBook {
	public enum Group {
		START(BakeryItems.BAKERY_GUIDE), BREAD(BakeryItems.BREAD_LOAF), SWEETS(BakeryItems.CUPCAKE), PASTRY(BakeryItems.CROISSANT),
		COOKIES(BakeryItems.COOKIES), PANTRY(BakeryItems.FLOUR), TREATS(BakeryItems.ICE_CREAM), DRINKS(BakeryItems.MATCHA_LATTE);

		public final Item icon;

		Group(Item icon) {
			this.icon = icon;
		}

		public Text displayName() {
			return Text.translatable("recipe.bakery.group." + name().toLowerCase());
		}
	}

	/** One step: where you do it, what goes in, what comes out, and what to do in words. */
	public record Step(ItemStack station, List<ItemStack> inputs, ItemStack output, Text text, boolean optional) {
	}

	public record Entry(Group group, ItemStack result, List<Step> steps, Text tip) {
		public Text name() {
			return group == Group.START ? Text.translatable("recipe.bakery.first_bake") : result.getName();
		}
	}

	private static List<Entry> entries;

	private RecipeBook() {
	}

	/** Clears the cache (the crafting recipes come from the world, so they are read again in a new world). */
	public static void reset() {
		entries = null;
	}

	public static List<Entry> entries() {
		if (entries == null) entries = build();
		return entries;
	}

	/** The recipe that makes {@code item}, if the book has one. */
	public static int indexOf(Item item) {
		List<Entry> all = entries();
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).group() != Group.START && all.get(i).result().isOf(item)) return i;
		}
		return -1;
	}

	// ------------------------------------------------------------------ building

	private static List<Entry> build() {
		List<Entry> list = new ArrayList<>();
		list.add(firstBake());
		for (BakingRecipes.Shape shape : BakingRecipes.Shape.values()) list.add(bake(shape));
		pantry(list);
		treats(list);
		for (Item drink : new Item[]{BakeryItems.MATCHA_LATTE, BakeryItems.HOT_CHOCOLATE, BakeryItems.STRAWBERRY_MILK, BakeryItems.LAVENDER_TEA,
				BakeryItems.ROSE_TEA, BakeryItems.MINT_TEA, BakeryItems.STARLIGHT_TEA, BakeryItems.LEMONADE, BakeryItems.COCONUT_MILK,
				BakeryItems.PINEAPPLE_SMOOTHIE}) {
			Step craft = crafted(drink);
			if (craft != null) list.add(new Entry(Group.DRINKS, new ItemStack(drink), List.of(craft), Text.translatable("recipe.bakery.tip.drink")));
		}
		return List.copyOf(list);
	}

	private static Entry firstBake() {
		List<Step> steps = List.of(
				step(BakeryItems.BAKERY_CATALOG, List.of(st(BakeryItems.MIXING_BOWL), st(BakeryItems.WHISK), st(BakeryItems.PASTRY_BOARD), st(BakeryItems.ROLLING_PIN)),
						ItemStack.EMPTY, Text.translatable("recipe.bakery.start.tools")),
				step(BakeryItems.BAKERY_CATALOG, List.of(st(BakeryItems.OVEN_HEART), st(Items.BRICKS, 21), st(BakeryItems.SPATULA)),
						ItemStack.EMPTY, Text.translatable("recipe.bakery.start.oven")),
				step(Items.CRAFTING_TABLE, List.of(st(Items.WHEAT, 3)), st(BakeryItems.FLOUR, 2), Text.translatable("recipe.bakery.start.flour")),
				step(BakeryItems.MIXING_BOWL, List.of(st(BakeryItems.FLOUR, 2), st(Items.WATER_BUCKET)), st(BakeryItems.BREAD_LOAF),
						Text.translatable("recipe.bakery.start.bread")),
				step(BakeryItems.BAKERY_COUNTER, List.of(st(BakeryItems.BAKERY_COUNTER), st(BakeryItems.PATH_RIBBON)), st(BakeryItems.SWEET_COIN),
						Text.translatable("recipe.bakery.start.shop")));
		return new Entry(Group.START, st(BakeryItems.BAKERY_GUIDE), steps, Text.translatable("recipe.bakery.start.tip"));
	}

	private static Entry bake(BakingRecipes.Shape shape) {
		BakingRecipes.DoughType dough = BakingRecipes.doughFor(shape);
		List<Step> steps = new ArrayList<>();
		List<ItemStack> mix = new ArrayList<>();
		for (Map.Entry<Item, Integer> need : BakingRecipes.mixNeeds(dough).entrySet()) mix.add(st(need.getKey(), need.getValue()));
		mix.sort((a, b) -> Integer.compare(b.getCount(), a.getCount()));
		steps.add(step(BakeryItems.MIXING_BOWL, mix, st(dough.dough()), Text.translatable("recipe.bakery.step.mix", BakingRecipes.STIRS_NEEDED)));
		if (shape == BakingRecipes.Shape.SUGAR_COOKIE) {
			steps.add(step(BakeryItems.PASTRY_BOARD, List.of(st(dough.dough()), st(BakeryItems.HEART_CUTTER)), st(shape.raw()),
					Text.translatable("recipe.bakery.step.cut")));
		} else {
			steps.add(step(BakeryItems.PASTRY_BOARD, List.of(st(dough.dough()), st(BakeryItems.ROLLING_PIN)), st(shape.raw()),
					Text.translatable("recipe.bakery.step.roll", shape.displayName())));
		}
		if (dough == BakingRecipes.DoughType.BREAD) {
			steps.add(new Step(st(BakeryItems.PROOFING_BASKET), List.of(st(shape.raw())), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.proof"), true));
		}
		OvenTemperature heat = heatFor(shape);
		int seconds = Math.round(shape.bakeTicks() / heat.speed() / 20f);
		ItemStack baked = st(shape.baked(), shape.count());
		if (GrandOvenBlockEntity.needsFlip(st(shape.raw()))) {
			steps.add(step(BakeryItems.OVEN_HEART, List.of(st(shape.raw())), ItemStack.EMPTY,
					Text.translatable("recipe.bakery.step.bake_flip", heat.displayName(), seconds, Math.round(seconds * GrandOvenBlockEntity.FLIP_FROM),
							Math.round(seconds * GrandOvenBlockEntity.FLIP_UNTIL))));
			steps.add(step(BakeryItems.SPATULA, List.of(), baked, Text.translatable("recipe.bakery.step.flip_take")));
		} else {
			steps.add(step(BakeryItems.OVEN_HEART, List.of(st(shape.raw())), baked, Text.translatable("recipe.bakery.step.bake", heat.displayName(), seconds)));
		}
		if (shape == BakingRecipes.Shape.CAKE) {
			steps.add(new Step(st(BakeryItems.CAKE_TURNTABLE), List.of(st(shape.baked()), st(BakeryItems.PINK_ICING)), ItemStack.EMPTY,
					Text.translatable("recipe.bakery.step.cake_studio"), true));
		} else if (shape == BakingRecipes.Shape.SUGAR_COOKIE) {
			steps.add(new Step(st(BakeryItems.PINK_ICING), List.of(st(shape.baked())), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.ice_cookie"), true));
		} else if (shape != BakingRecipes.Shape.LOAF && shape != BakingRecipes.Shape.BAGUETTE && shape != BakingRecipes.Shape.PIE) {
			steps.add(new Step(st(BakeryItems.SPRINKLES), List.of(st(shape.baked())), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.decorate"), true));
		}
		Group group = switch (dough) {
			case BREAD -> Group.BREAD;
			case SWEET -> Group.SWEETS;
			case PASTRY -> Group.PASTRY;
			case COOKIE -> Group.COOKIES;
		};
		return new Entry(group, st(shape.baked(), 1), steps, Text.translatable("recipe.bakery.tip.flavor"));
	}

	/** The oven setting closest to the heat this bake likes. */
	public static OvenTemperature heatFor(BakingRecipes.Shape shape) {
		OvenTemperature best = OvenTemperature.MEDIUM;
		for (OvenTemperature t : OvenTemperature.values()) {
			if (Math.abs(t.targetHeat() - shape.idealHeat()) < Math.abs(best.targetHeat() - shape.idealHeat())) best = t;
		}
		return best;
	}

	private static void pantry(List<Entry> list) {
		// Flour: crafting or the millstone.
		List<Step> flour = new ArrayList<>();
		Step craft = crafted(BakeryItems.FLOUR);
		if (craft != null) flour.add(craft);
		flour.add(mill(Items.WHEAT, BakeryItems.FLOUR, 1));
		list.add(new Entry(Group.PANTRY, st(BakeryItems.FLOUR), flour, Text.translatable("recipe.bakery.tip.buy_pantry")));
		addCrafted(list, Group.PANTRY, BakeryItems.BUTTER, null);
		List<Step> sugar = new ArrayList<>();
		Step sugarCraft = crafted(Items.SUGAR);
		if (sugarCraft != null) sugar.add(sugarCraft);
		sugar.add(mill(Items.SUGAR_CANE, Items.SUGAR, 2));
		sugar.add(mill(BakeryItems.SUGAR_BEET, Items.SUGAR, 3));
		list.add(new Entry(Group.PANTRY, st(Items.SUGAR), sugar, Text.translatable("recipe.bakery.tip.buy_pantry")));
		addCrafted(list, Group.PANTRY, BakeryItems.WHIPPED_CREAM, null);
		addCrafted(list, Group.PANTRY, BakeryItems.CHOCOLATE_BAR, null);
		List<Step> matcha = new ArrayList<>();
		Step smelt = crafted(BakeryItems.MATCHA_POWDER);
		if (smelt != null) matcha.add(smelt);
		matcha.add(mill(BakeryItems.TEA_LEAVES, BakeryItems.MATCHA_POWDER, 1));
		list.add(new Entry(Group.PANTRY, st(BakeryItems.MATCHA_POWDER), matcha, Text.empty()));
		List<Step> cocoa = new ArrayList<>();
		Step cocoaCraft = crafted(BakeryItems.COCOA_POWDER);
		if (cocoaCraft != null) cocoa.add(cocoaCraft);
		cocoa.add(mill(Items.COCOA_BEANS, BakeryItems.COCOA_POWDER, 1));
		list.add(new Entry(Group.PANTRY, st(BakeryItems.COCOA_POWDER), cocoa, Text.empty()));
		addCrafted(list, Group.PANTRY, BakeryItems.SPRINKLES, null);
		addCrafted(list, Group.PANTRY, BakeryItems.CANDIED_PETALS, null);
		list.add(new Entry(Group.PANTRY, st(BakeryItems.CORNMEAL), List.of(mill(BakeryItems.CORN, BakeryItems.CORNMEAL, 2)), Text.empty()));
		list.add(new Entry(Group.PANTRY, st(BakeryItems.PEANUT_BUTTER), List.of(mill(BakeryItems.PEANUT, BakeryItems.PEANUT_BUTTER, 1)), Text.empty()));
		List<Step> flakes = new ArrayList<>();
		Step flakeCraft = crafted(BakeryItems.COCONUT_FLAKES);
		if (flakeCraft != null) flakes.add(flakeCraft);
		flakes.add(mill(BakeryItems.COCONUT, BakeryItems.COCONUT_FLAKES, 4));
		list.add(new Entry(Group.PANTRY, st(BakeryItems.COCONUT_FLAKES), flakes, Text.empty()));
	}

	private static void treats(List<Entry> list) {
		list.add(new Entry(Group.TREATS, st(BakeryItems.JUICE), List.of(
				step(BakeryItems.JUICER, List.of(st(BakeryItems.ORANGE)), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.juicer")),
				step(Items.GLASS_BOTTLE, List.of(st(Items.GLASS_BOTTLE, 2)), st(BakeryItems.JUICE, 2), Text.translatable("recipe.bakery.step.bottle"))),
				Text.translatable("recipe.bakery.tip.juice")));
		list.add(new Entry(Group.TREATS, st(BakeryItems.ICE_CREAM), List.of(
				step(BakeryItems.ICE_CREAM_CHURN, List.of(st(BakeryItems.WHIPPED_CREAM), st(Items.SUGAR), st(Items.ICE)), ItemStack.EMPTY,
						Text.translatable("recipe.bakery.step.churn_fill")),
				new Step(st(BakeryItems.ICE_CREAM_CHURN), List.of(st(BakeryItems.STRAWBERRY)), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.churn_flavor"), true),
				step(BakeryItems.ICE_CREAM_CHURN, List.of(), st(BakeryItems.ICE_CREAM, 3), Text.translatable("recipe.bakery.step.churn_turn"))),
				Text.translatable("recipe.bakery.tip.ice_cream")));
		list.add(new Entry(Group.TREATS, st(BakeryItems.STRAWBERRY_JAM), List.of(
				step(BakeryItems.JAM_POT, List.of(st(Items.CAMPFIRE)), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.jam_place")),
				step(BakeryItems.JAM_POT, List.of(st(BakeryItems.STRAWBERRY, 3), st(Items.SUGAR)), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.jam_fill")),
				step(BakeryItems.WHISK, List.of(), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.jam_stir")),
				step(BakeryItems.JAM_POT, List.of(st(BakeryItems.GLASS_JAR)), st(BakeryItems.STRAWBERRY_JAM), Text.translatable("recipe.bakery.step.jam_jar"))),
				Text.translatable("recipe.bakery.tip.jam")));
		list.add(new Entry(Group.TREATS, st(BakeryItems.CHOCOLATE_TREAT), List.of(
				step(BakeryItems.CHOCOLATE_FOUNTAIN, List.of(st(BakeryItems.CHOCOLATE_BAR)), ItemStack.EMPTY, Text.translatable("recipe.bakery.step.fountain_fill")),
				step(BakeryItems.CHOCOLATE_FOUNTAIN, List.of(st(BakeryItems.STRAWBERRY)), st(BakeryItems.CHOCOLATE_TREAT, 2),
						Text.translatable("recipe.bakery.step.fountain_dip"))),
				Text.empty()));
	}

	private static void addCrafted(List<Entry> list, Group group, Item item, Text tip) {
		Step craft = crafted(item);
		if (craft != null) list.add(new Entry(group, st(item), List.of(craft), tip == null ? Text.empty() : tip));
	}

	private static Step mill(Item in, Item out, int count) {
		return step(BakeryItems.MILLSTONE_CORE, List.of(st(in)), st(out, count), Text.translatable("recipe.bakery.step.mill"));
	}

	/** A step read from the game's own crafting / smelting recipes, or null if the item has none. */
	private static Step crafted(Item item) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null) return null;
		var registries = client.world.getRegistryManager();
		for (RecipeEntry<?> entry : client.world.getRecipeManager().values()) {
			Recipe<?> recipe = entry.value();
			ItemStack result = recipe.getResult(registries);
			if (!result.isOf(item)) continue;
			Map<Item, Integer> counts = new LinkedHashMap<>();
			for (Ingredient ingredient : recipe.getIngredients()) {
				ItemStack[] options = ingredient.getMatchingStacks();
				if (options.length > 0) counts.merge(options[0].getItem(), 1, Integer::sum);
			}
			List<ItemStack> inputs = new ArrayList<>();
			counts.forEach((k, v) -> inputs.add(st(k, v)));
			RecipeType<?> type = recipe.getType();
			if (type == RecipeType.SMELTING) {
				return step(Items.FURNACE, inputs, result.copy(), Text.translatable("recipe.bakery.step.smelt"));
			}
			if (type == RecipeType.CRAFTING) {
				return step(Items.CRAFTING_TABLE, inputs, result.copy(), Text.translatable("recipe.bakery.step.craft"));
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ where ingredients come from

	private static final Item[] TREE_FRUIT = {BakeryItems.LEMON, BakeryItems.ORANGE, BakeryItems.PEACH, BakeryItems.MANGO, BakeryItems.PLUM,
			BakeryItems.CHERRIES, BakeryItems.COCONUT};

	/** Short lines saying where to get an ingredient: grow it, buy it, or see its own recipe. */
	public static List<Text> sources(Item item) {
		List<Text> lines = new ArrayList<>();
		Identifier id = Registries.ITEM.getId(item);
		Item seeds = Registries.ITEM.get(Identifier.of(id.getNamespace(), id.getPath() + "_seeds"));
		if (seeds != Items.AIR && seeds != item) lines.add(Text.translatable("recipe.bakery.source.grow", seeds.getName()));
		for (Item fruit : TREE_FRUIT) {
			if (fruit == item) lines.add(Text.translatable("recipe.bakery.source.tree"));
		}
		if (item == Items.EGG) lines.add(Text.translatable("recipe.bakery.source.egg"));
		if (item == Items.WATER_BUCKET) lines.add(Text.translatable("recipe.bakery.source.water"));
		if (item == Items.MILK_BUCKET) lines.add(Text.translatable("recipe.bakery.source.milk"));
		if (item == Items.WHEAT) lines.add(Text.translatable("recipe.bakery.source.wheat"));
		for (Catalog.Entry entry : Catalog.entries()) {
			if (entry.item() == item) {
				lines.add(Text.translatable("recipe.bakery.source.buy", entry.category().displayName(), entry.level().number()));
				break;
			}
		}
		if (indexOf(item) >= 0) lines.add(Text.translatable("recipe.bakery.source.click"));
		return lines;
	}

	// ------------------------------------------------------------------ helpers

	private static ItemStack st(ItemConvertible item) {
		return new ItemStack(item);
	}

	private static ItemStack st(ItemConvertible item, int count) {
		return new ItemStack(item, count);
	}

	private static Step step(ItemConvertible station, List<ItemStack> inputs, ItemStack output, Text text) {
		return new Step(new ItemStack(station), inputs, output, text, false);
	}
}
