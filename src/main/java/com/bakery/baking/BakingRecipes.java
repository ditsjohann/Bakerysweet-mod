package com.bakery.baking;

import com.bakery.food.Flavor;
import com.bakery.registry.BakeryItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The whole bakery chain in one place:
 * mixing bowl (ingredients -> dough), pastry board (dough -> shaped raw bake), oven (raw -> baked).
 */
public final class BakingRecipes {
	public static final int STIRS_NEEDED = 8;
	public static final int MAX_BOWL_ITEMS = 6;

	public enum Shape {
		LOAF("loaf", () -> BakeryItems.RAW_LOAF, () -> BakeryItems.BREAD_LOAF, 600, 1, 60),
		BAGUETTE("baguette", () -> BakeryItems.RAW_BAGUETTE, () -> BakeryItems.BAGUETTE, 500, 1, 72),
		CUPCAKE("cupcake", () -> BakeryItems.RAW_CUPCAKE, () -> BakeryItems.CUPCAKE, 400, 4, 55),
		CAKE("cake", () -> BakeryItems.RAW_CAKE, () -> BakeryItems.CAKE, 900, 1, 50),
		DONUT("donut", () -> BakeryItems.RAW_DONUT, () -> BakeryItems.DONUT, 400, 3, 65),
		CROISSANT("croissant", () -> BakeryItems.RAW_CROISSANT, () -> BakeryItems.CROISSANT, 500, 2, 70),
		PIE("pie", () -> BakeryItems.RAW_PIE, () -> BakeryItems.PIE, 900, 1, 60),
		COOKIES("cookies", () -> BakeryItems.RAW_COOKIES, () -> BakeryItems.COOKIES, 300, 6, 55),
		MACARONS("macarons", () -> BakeryItems.RAW_MACARONS, () -> BakeryItems.MACARONS, 400, 4, 42),
		/** Cut with a cookie cutter on the pastry board instead of rolled; one tray bakes into four cookies. */
		SUGAR_COOKIE("sugar_cookie", () -> BakeryItems.RAW_SUGAR_COOKIE, () -> BakeryItems.SUGAR_COOKIE, 300, 4, 50);

		private final String name;
		private final Supplier<Item> raw;
		private final Supplier<Item> baked;
		private final int bakeTicks;
		private final int count;
		private final int idealHeat;

		Shape(String name, Supplier<Item> raw, Supplier<Item> baked, int bakeTicks, int count, int idealHeat) {
			this.name = name;
			this.raw = raw;
			this.baked = baked;
			this.bakeTicks = bakeTicks;
			this.count = count;
			this.idealHeat = idealHeat;
		}

		/** The oven heat (0-100) this bake likes best. Keep the needle within 12 of it. */
		public int idealHeat() {
			return idealHeat;
		}

		public Item raw() {
			return raw.get();
		}

		public Item baked() {
			return baked.get();
		}

		public int bakeTicks() {
			return bakeTicks;
		}

		public int count() {
			return count;
		}

		public Text displayName() {
			return Text.translatable("shape.bakery." + name);
		}
	}

	public enum DoughType {
		BREAD(() -> BakeryItems.BREAD_DOUGH, List.of(Shape.LOAF, Shape.BAGUETTE)),
		SWEET(() -> BakeryItems.SWEET_DOUGH, List.of(Shape.CUPCAKE, Shape.CAKE, Shape.DONUT)),
		PASTRY(() -> BakeryItems.PASTRY_DOUGH, List.of(Shape.CROISSANT, Shape.PIE)),
		COOKIE(() -> BakeryItems.COOKIE_DOUGH, List.of(Shape.COOKIES, Shape.MACARONS));

		private final Supplier<Item> dough;
		private final List<Shape> shapes;

		DoughType(Supplier<Item> dough, List<Shape> shapes) {
			this.dough = dough;
			this.shapes = shapes;
		}

		public Item dough() {
			return dough.get();
		}

		public List<Shape> shapes() {
			return shapes;
		}

		public static DoughType of(ItemStack stack) {
			for (DoughType type : values()) {
				if (stack.isOf(type.dough())) {
					return type;
				}
			}
			return null;
		}
	}

	private record MixRecipe(DoughType result, Map<Item, Integer> needs) {
	}

	private static List<MixRecipe> recipes;

	private BakingRecipes() {
	}

	private static List<MixRecipe> recipes() {
		if (recipes == null) {
			recipes = List.of(
					new MixRecipe(DoughType.BREAD, Map.of(BakeryItems.FLOUR, 2, Items.WATER_BUCKET, 1)),
					new MixRecipe(DoughType.SWEET, Map.of(BakeryItems.FLOUR, 1, Items.EGG, 1, Items.SUGAR, 1, BakeryItems.BUTTER, 1)),
					new MixRecipe(DoughType.PASTRY, Map.of(BakeryItems.FLOUR, 2, BakeryItems.BUTTER, 2)),
					new MixRecipe(DoughType.COOKIE, Map.of(BakeryItems.FLOUR, 1, Items.SUGAR, 2, BakeryItems.BUTTER, 1)));
		}
		return recipes;
	}

	/** Same as {@link #mixNeeds}, for the guide's recipe pages. */
	public static Map<Item, Integer> needs(DoughType type) {
		return mixNeeds(type);
	}

	/** What goes into the bowl for this dough (item to count), for the recipe book. */
	public static Map<Item, Integer> mixNeeds(DoughType type) {
		for (MixRecipe recipe : recipes()) {
			if (recipe.result() == type) return recipe.needs();
		}
		return Map.of();
	}

	/** The dough a shape is rolled from (sugar cookies are cut from cookie dough). */
	public static DoughType doughFor(Shape shape) {
		if (shape == Shape.SUGAR_COOKIE) return DoughType.COOKIE;
		for (DoughType type : DoughType.values()) {
			if (type.shapes().contains(shape)) return type;
		}
		return null;
	}

	/** Base ingredients (not flavorings) that the mixing bowl accepts. */
	public static boolean isBaseIngredient(ItemStack stack) {
		for (MixRecipe recipe : recipes()) {
			if (recipe.needs().containsKey(stack.getItem())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isBowlIngredient(ItemStack stack) {
		return isBaseIngredient(stack) || Flavor.fromIngredient(stack) != null;
	}

	/** The dough these bowl contents would make, or null if it is not a recipe (yet). At most one flavoring is allowed. */
	public static DoughType match(List<ItemStack> contents) {
		Map<Item, Integer> base = new HashMap<>();
		Flavor flavor = null;
		for (ItemStack stack : contents) {
			Flavor f = Flavor.fromIngredient(stack);
			if (f != null && !isBaseIngredient(stack)) {
				if (flavor != null) {
					return null;
				}
				flavor = f;
			} else {
				base.merge(stack.getItem(), 1, Integer::sum);
			}
		}
		for (MixRecipe recipe : recipes()) {
			if (recipe.needs().equals(base)) {
				return recipe.result();
			}
		}
		return null;
	}

	public static Flavor flavorOf(List<ItemStack> contents) {
		for (ItemStack stack : contents) {
			Flavor f = Flavor.fromIngredient(stack);
			if (f != null && !isBaseIngredient(stack)) {
				return f;
			}
		}
		return Flavor.PLAIN;
	}

	public static Shape shapeForRaw(ItemStack stack) {
		for (Shape shape : Shape.values()) {
			if (stack.isOf(shape.raw())) {
				return shape;
			}
		}
		return null;
	}

	/** Rolling minigame score (0-10) to prep stars. */
	public static int prepStars(int score) {
		if (score >= 10) return 5;
		if (score >= 8) return 4;
		if (score >= 5) return 3;
		if (score >= 3) return 2;
		return 1;
	}

	/** How done a bake is (progress / bake time) to oven stars. 0 means still raw. */
	public static int bakeStars(float doneness) {
		if (doneness < 0.5f) return 0;
		if (doneness < 0.75f) return 1;
		if (doneness < 0.9f) return 3;
		if (doneness <= 1.15f) return 5;
		if (doneness <= 1.3f) return 3;
		return 1;
	}

	public static final float PERFECT_START = 0.9f;
	public static final float PERFECT_END = 1.15f;
	public static final float BURNT = 1.6f;
}
