package com.bakery.shop;

import com.bakery.cafe.CafeLevel;
import com.bakery.cafe.Regular;
import com.bakery.food.BakeryFoods;
import com.bakery.food.Flavor;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What customers order, what it costs, and how picky they are.
 * Dishes unlock with the bakery's {@link CafeLevel}: a new bakery only gets asked for bread and cookies,
 * and the baker can take dishes off the menu at the menu board.
 */
public final class Menu {
	public record Dish(Item item, int price, int weight, boolean flavored, CafeLevel level, boolean drink) {
		public String id() {
			return Registries.ITEM.getId(item).getPath();
		}
	}

	private static List<Dish> dishes;

	private Menu() {
	}

	public static List<Dish> dishes() {
		if (dishes == null) {
			dishes = List.of(
					new Dish(BakeryItems.BREAD_LOAF, 2, 10, true, CafeLevel.STALL, false),
					new Dish(BakeryItems.COOKIES, 1, 9, true, CafeLevel.STALL, false),
					new Dish(BakeryItems.BAGUETTE, 2, 6, false, CafeLevel.STALL, false),
					new Dish(BakeryItems.CUPCAKE, 3, 10, true, CafeLevel.CORNER, false),
					new Dish(BakeryItems.JUICE, 2, 6, true, CafeLevel.CORNER, true),
					new Dish(BakeryItems.SUGAR_COOKIE, 2, 6, false, CafeLevel.CORNER, false),
					new Dish(BakeryItems.STRAWBERRY_MILK, 2, 4, false, CafeLevel.CORNER, true),
					new Dish(BakeryItems.CROISSANT, 3, 9, true, CafeLevel.CAFE, false),
					new Dish(BakeryItems.DONUT, 3, 8, true, CafeLevel.CAFE, false),
					new Dish(BakeryItems.HOT_CHOCOLATE, 2, 4, false, CafeLevel.CAFE, true),
					new Dish(BakeryItems.MATCHA_LATTE, 3, 4, false, CafeLevel.CAFE, true),
					new Dish(BakeryItems.LEMONADE, 2, 3, false, CafeLevel.CAFE, true),
					new Dish(BakeryItems.MINT_TEA, 2, 3, false, CafeLevel.CAFE, true),
					new Dish(BakeryItems.MACARONS, 4, 6, true, CafeLevel.PATISSERIE, false),
					new Dish(BakeryItems.ICE_CREAM, 4, 5, true, CafeLevel.PATISSERIE, false),
					new Dish(BakeryItems.CHOCOLATE_TREAT, 4, 4, true, CafeLevel.PATISSERIE, false),
					new Dish(BakeryItems.PIE, 6, 3, true, CafeLevel.PATISSERIE, false),
					new Dish(BakeryItems.CAKE, 9, 3, true, CafeLevel.GRAND, false));
		}
		return dishes;
	}

	public static Dish dish(Item item) {
		for (Dish dish : dishes()) {
			if (dish.item() == item) return dish;
		}
		return null;
	}

	/** Dishes customers may currently ask for. Falls back to everything unlocked if the baker switched it all off. */
	public static List<Dish> available(CafeLevel level, Set<String> disabled) {
		List<Dish> list = new ArrayList<>();
		for (Dish dish : dishes()) {
			if (dish.level().ordinal() <= level.ordinal() && !disabled.contains(dish.id())) list.add(dish);
		}
		if (list.isEmpty()) {
			for (Dish dish : dishes()) {
				if (dish.level().ordinal() <= level.ordinal()) list.add(dish);
			}
		}
		return list;
	}

	/** A random order from what's on the menu. Some customers ask for a specific flavour - usually their favourite. */
	public static Order randomOrder(java.util.UUID customer, Regular regular, CafeLevel level, Set<String> disabled, Random random) {
		List<Dish> menu = available(level, disabled);
		int total = menu.stream().mapToInt(Dish::weight).sum();
		int roll = random.nextInt(Math.max(1, total));
		Dish dish = menu.getFirst();
		for (Dish d : menu) {
			roll -= d.weight();
			if (roll < 0) {
				dish = d;
				break;
			}
		}
		ItemStack wanted = new ItemStack(dish.item());
		boolean flavorRequested = dish.flavored() && random.nextFloat() < level.flavorChance();
		if (flavorRequested) {
			Flavor flavor = random.nextFloat() < 0.6f ? regular.favorite() : Flavor.values()[1 + random.nextInt(Flavor.values().length - 1)];
			wanted.set(BakeryComponents.FLAVOR, flavor.asString());
		}
		// Customers wait two and a half to four minutes (longer at a small stall), so there is time to bake to order.
		int patience = Math.round(20 * (150 + random.nextInt(90)) * level.patience());
		boolean dineIn = random.nextFloat() < regular.dineIn();
		return new Order(customer, regular.index(), wanted, flavorRequested, patience, dineIn);
	}

	public static boolean matches(Order order, ItemStack offered) {
		if (!offered.isOf(order.wanted.getItem())) {
			return false;
		}
		return !order.flavorRequested || BakeryFoods.flavor(offered) == BakeryFoods.flavor(order.wanted);
	}

	public static int basePrice(Item item) {
		Dish dish = dish(item);
		return dish == null ? 1 : dish.price();
	}

	private static final Map<Integer, Float> STAR_MULTIPLIER = Map.of(1, 0.5f, 2, 0.75f, 3, 1f, 4, 1.4f, 5, 2f);

	/** Coins for a dish: base price scaled by stars, plus tips for speed and getting the flavour right. */
	public static int price(Order order, int stars, boolean favorite) {
		float pay = basePrice(order.wanted.getItem()) * STAR_MULTIPLIER.get(Math.clamp(stars, 1, 5));
		if (order.patienceFraction() > 0.6f) pay += 1;
		if (order.flavorRequested) pay += 1;
		if (favorite) pay += 1;
		return Math.max(1, Math.round(pay));
	}
}
