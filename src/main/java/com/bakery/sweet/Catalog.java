package com.bakery.sweet;

import com.bakery.cafe.CafeLevel;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryItems;
import com.bakery.review.BakeryReputation;
import com.bakery.tree.WoodSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bakery Catalog: where every baker starts. A little mail-order shop that takes Sweet Coins - kitchen tools,
 * pantry basics, seeds and saplings, decor and outfits. Fancier things unlock as the bakery grows
 * ({@link CafeLevel}). One item a day is the Daily Special at a quarter off.
 */
public final class Catalog {
	public enum Category {
		BAKE, CAFE, PANTRY, GARDEN, DECOR, WARDROBE;

		public Text displayName() {
			return Text.translatable("catalog.bakery." + name().toLowerCase());
		}
	}

	/** {@code stock}: how many times a day each baker can buy it (0 = as often as they like). */
	public record Entry(Category category, Item item, int count, int price, CafeLevel level, int stock, String sub) {
		public ItemStack stack() {
			ItemStack stack = new ItemStack(item, count);
			if (storeBought()) stack.set(com.bakery.registry.BakeryComponents.STORE_BOUGHT, true);
			return stack;
		}

		/** Pantry ingredients come labelled, so customers can tell a homemade bake from a store-bought one. */
		public boolean storeBought() {
			boolean coffee = item == BakeryItems.ROASTED_COFFEE_BEANS || item == BakeryItems.CINNAMON || item instanceof com.bakery.item.SyrupItem;
			return coffee || category == Category.PANTRY && item != Items.WATER_BUCKET && item != Items.CHARCOAL && item != Items.ICE
					&& item != BakeryItems.SPRINKLES;
		}
	}

	private static List<Entry> entries;
	/** The shelf (sub-tab) entries are being added to while the list is built; null = none. */
	private static String sub;

	/** The shelves of a category, in order (empty when the category has no sub-tabs). */
	public static List<String> subs(Category category) {
		List<String> out = new ArrayList<>();
		for (Entry e : entries()) {
			if (e.category() == category && e.sub() != null && !out.contains(e.sub())) out.add(e.sub());
		}
		return out;
	}

	private Catalog() {
	}

	public static List<Entry> entries() {
		if (entries == null) {
			List<Entry> list = new ArrayList<>();
			CafeLevel l1 = CafeLevel.STALL, l2 = CafeLevel.CORNER, l3 = CafeLevel.CAFE, l4 = CafeLevel.PATISSERIE, l5 = CafeLevel.GRAND;

			// Six shops, each split into shelves by what you are doing; the basics of every shelf come first.
			Category c = Category.BAKE;
			// Bake: the tools in your hands, the oven and mill you build, the kitchen machines, and cake decorating.
			sub = "tools";
			add(list, c, BakeryItems.MIXING_BOWL, 1, 4, l1);
			add(list, c, BakeryItems.WHISK, 1, 3, l1);
			add(list, c, BakeryItems.PASTRY_BOARD, 1, 4, l1);
			add(list, c, BakeryItems.ROLLING_PIN, 1, 3, l1);
			add(list, c, BakeryItems.SPATULA, 1, 2, l1);
			add(list, c, BakeryItems.BELLOWS, 1, 4, l1);
			add(list, c, BakeryItems.BAKERY_GUIDE, 1, 2, l1);
			add(list, c, BakeryItems.HEART_CUTTER, 1, 6, l2);
			add(list, c, BakeryItems.STAR_CUTTER, 1, 6, l2);
			add(list, c, BakeryItems.BUNNY_CUTTER, 1, 6, l2);
			add(list, c, BakeryItems.FLOWER_CUTTER, 1, 6, l2);
			sub = "oven";
			add(list, c, BakeryItems.OVEN_HEART, 1, 8, l1);
			add(list, c, Items.BRICKS, 21, 7, l1);
			add(list, c, BakeryItems.MILLSTONE_CORE, 1, 8, l1);
			add(list, c, Items.STONE_BRICKS, 9, 3, l1);
			add(list, c, Items.COBBLESTONE_WALL, 4, 2, l1);
			sub = "machines";
			add(list, c, BakeryItems.JUICER, 1, 10, l2);
			add(list, c, BakeryItems.PROOFING_BASKET, 1, 10, l3);
			add(list, c, BakeryItems.JAM_POT, 1, 12, l3);
			add(list, c, BakeryItems.ICE_CREAM_CHURN, 1, 16, l4);
			add(list, c, BakeryItems.CHOCOLATE_FOUNTAIN, 1, 22, l4);
			sub = "cakes";
			add(list, c, BakeryItems.CAKE_TURNTABLE, 1, 15, l4);
			add(list, c, BakeryItems.CAKE_SCRAPER, 1, 4, l4);
			add(list, c, BakeryItems.ROUND_CAKE_MOLD, 1, 6, l4);
			add(list, c, BakeryItems.SQUARE_CAKE_MOLD, 1, 8, l4);
			add(list, c, BakeryItems.HEART_CAKE_MOLD, 1, 12, l5);
			add(list, c, BakeryItems.STAR_CAKE_MOLD, 1, 10, l4);
			add(list, c, BakeryItems.FLOWER_CAKE_MOLD, 1, 10, l4);
			add(list, c, BakeryItems.HEXAGON_CAKE_MOLD, 1, 9, l4);
			add(list, c, BakeryItems.BUNNY_CAKE_MOLD, 1, 14, l5);
			for (Icing icing : Icing.values()) add(list, c, icing.bag(), 1, 5, l2);
			// Sugar decals and toppers: a few to start, the fancier ones as the bakery grows.
			int t = 0;
			for (Item topper : BakeryItems.TOPPERS) {
				add(list, c, topper, 4, 3 + t / 6, t < 6 ? l2 : t < 14 ? l3 : l4);
				t++;
			}

			// Café: running the shop - the counter and service, the coffee corner, displays, and wrapping to keep bakes fresh.
			c = Category.CAFE;
			sub = "counter";
			add(list, c, BakeryItems.BAKERY_COUNTER, 1, 6, l1);
			add(list, c, BakeryItems.PATH_RIBBON, 1, 1, l1);
			add(list, c, BakeryItems.SERVICE_BELL, 1, 5, l1);
			add(list, c, BakeryItems.ORDER_NOTEBOOK, 1, 4, l1);
			add(list, c, BakeryItems.MENU_BOARD, 1, 4, l1);
			add(list, c, BakeryItems.CASH_REGISTER, 1, 8, l1);
			add(list, c, BakeryItems.TIP_JAR, 1, 3, l1);
			sub = "coffee";
			add(list, c, BakeryItems.COFFEE_MACHINE, 1, 8, l1);
			add(list, c, BakeryItems.SYRUP_RACK, 1, 3, l1);
			add(list, c, BakeryItems.MUG, 4, 3, l1);
			stock(list, c, BakeryItems.ROASTED_COFFEE_BEANS, 2, 3, l1, 3);
			stock(list, c, BakeryItems.CARAMEL_SYRUP, 1, 5, l1, 1);
			stock(list, c, BakeryItems.VANILLA_SYRUP, 1, 5, l1, 1);
			stock(list, c, BakeryItems.MOCHA_SYRUP, 1, 5, l1, 1);
			stock(list, c, BakeryItems.SPECULOOS_SYRUP, 1, 6, l2, 1);
			stock(list, c, BakeryItems.PUMPKIN_SPICE_SYRUP, 1, 6, l2, 1);
			stock(list, c, BakeryItems.CINNAMON, 2, 4, l1, 1);
			sub = "displays";
			add(list, c, BakeryItems.PASTRY_SHELF, 1, 10, l1);
			add(list, c, BakeryItems.DISPLAY_CASE, 1, 14, l2);
			add(list, c, BakeryItems.TIERED_STAND, 1, 12, l2);
			add(list, c, BakeryItems.CAKE_STAND, 1, 6, l2);
			add(list, c, BakeryItems.GLASS_CLOCHE, 1, 9, l3);
			add(list, c, BakeryItems.MACARON_TOWER, 1, 10, l4);
			sub = "wrapping";
			add(list, c, BakeryItems.GIFT_BOX, 3, 4, l2);
			add(list, c, BakeryItems.GLASS_JAR, 4, 3, l3);
			add(list, c, BakeryItems.WAX_PAPER, 8, 4, l3);
			add(list, c, BakeryItems.FROST_WRAP, 8, 6, l4);

			// Pantry basics are pricey and only a few come in each day: growing, milling and churning your own is the way
			// to keep a bakery stocked (and dishes made only from your own ingredients sell for more).
			c = Category.PANTRY;
			sub = "basics";
			stock(list, c, BakeryItems.FLOUR, 2, 4, l1, 3);
			stock(list, c, Items.WHEAT, 3, 3, l1, 2);
			stock(list, c, Items.SUGAR, 2, 3, l1, 3);
			stock(list, c, BakeryItems.BUTTER, 1, 4, l1, 2);
			stock(list, c, Items.EGG, 2, 3, l1, 3);
			stock(list, c, Items.MILK_BUCKET, 1, 3, l1, 2);
			add(list, c, Items.WATER_BUCKET, 1, 1, l1);
			stock(list, c, Items.CHARCOAL, 8, 3, l1, 4);
			sub = "flavours";
			stock(list, c, BakeryItems.STRAWBERRY, 2, 5, l1, 1);
			stock(list, c, BakeryItems.BLUEBERRY, 2, 5, l2, 1);
			stock(list, c, BakeryItems.LEMON, 1, 5, l2, 1);
			stock(list, c, BakeryItems.VANILLA_POD, 1, 6, l2, 1);
			stock(list, c, BakeryItems.CHOCOLATE_BAR, 1, 6, l2, 2);
			stock(list, c, BakeryItems.COCOA_POWDER, 2, 4, l2, 2);
			stock(list, c, BakeryItems.RASPBERRY, 2, 5, l2, 1);
			stock(list, c, BakeryItems.ORANGE, 1, 5, l2, 1);
			stock(list, c, BakeryItems.WHIPPED_CREAM, 1, 6, l3, 2);
			stock(list, c, BakeryItems.MATCHA_POWDER, 1, 7, l3, 1);
			add(list, c, BakeryItems.SPRINKLES, 4, 4, l3);
			stock(list, c, Items.ICE, 4, 3, l4, 3);
			stock(list, c, BakeryItems.PEACH, 1, 5, l3, 1);
			stock(list, c, BakeryItems.CORNMEAL, 2, 4, l3, 2);
			stock(list, c, BakeryItems.PEANUT_BUTTER, 1, 5, l3, 1);
			stock(list, c, BakeryItems.COCONUT_FLAKES, 2, 5, l3, 1);
			stock(list, c, BakeryItems.CANDIED_PETALS, 2, 6, l4, 1);
			stock(list, c, BakeryItems.STAR_PETALS, 1, 10, l5, 1);

			// Garden: everything outside - seeds and fruit trees, wild plants, the yard, and the shop front.
			c = Category.GARDEN;
			sub = "seeds";
			add(list, c, Items.WHEAT_SEEDS, 8, 1, l1);
			add(list, c, Items.SUGAR_CANE, 4, 2, l1);
			add(list, c, BakeryItems.STRAWBERRY_SEEDS, 4, 3, l1);
			add(list, c, BakeryItems.BLUEBERRY_SEEDS, 4, 3, l1);
			add(list, c, BakeryItems.COFFEE_SEEDS, 4, 3, l1);
			add(list, c, BakeryItems.CINNAMON_SEEDS, 4, 3, l1);
			add(list, c, Items.BONE_MEAL, 8, 3, l1);
			add(list, c, BakeryItems.RASPBERRY_SEEDS, 4, 3, l2);
			add(list, c, BakeryItems.SUGAR_BEET_SEEDS, 4, 3, l2);
			add(list, c, BakeryItems.TEA_SEEDS, 4, 4, l2);
			add(list, c, BakeryItems.MINT_SEEDS, 4, 4, l2);
			add(list, c, BakeryItems.ROSE_SEEDS, 4, 4, l2);
			add(list, c, BakeryItems.LAVENDER_SEEDS, 4, 4, l2);
			add(list, c, BakeryItems.CORN_SEEDS, 4, 3, l2);
			add(list, c, BakeryItems.PEA_SEEDS, 4, 3, l2);
			add(list, c, BakeryItems.SWEET_POTATO_SEEDS, 3, 4, l3);
			add(list, c, BakeryItems.ZUCCHINI_SEEDS, 3, 4, l3);
			add(list, c, BakeryItems.RHUBARB_SEEDS, 3, 4, l3);
			add(list, c, BakeryItems.PEANUT_SEEDS, 3, 4, l3);
			add(list, c, BakeryItems.VANILLA_SEEDS, 2, 6, l3);
			add(list, c, BakeryItems.HIBISCUS_SEEDS, 2, 6, l3);
			add(list, c, BakeryItems.CRANBERRY_SEEDS, 2, 6, l3);
			add(list, c, BakeryItems.PINEAPPLE_SEEDS, 2, 7, l4);
			add(list, c, BakeryItems.CANDYFLOSS_SEEDS, 1, 12, l4);
			add(list, c, BakeryItems.STARBLOOM_SEEDS, 1, 15, l5);
			int sapling = 0;
			for (WoodSet set : BakeryBlocks.WOODS) add(list, c, set.sapling.asItem(), 1, 10, sapling++ < 3 ? l2 : l3);
			sub = "nature";
			add(list, c, BakeryItems.DAISIES, 2, 2, l1);
			add(list, c, BakeryItems.BUTTERCUPS, 2, 2, l1);
			add(list, c, BakeryItems.FORGET_ME_NOTS, 2, 2, l1);
			add(list, c, BakeryItems.CLOVER_PATCH, 2, 2, l1);
			add(list, c, BakeryItems.WILDFLOWER_PATCH, 2, 2, l1);
			add(list, c, BakeryItems.WILD_FERN, 2, 2, l1);
			add(list, c, BakeryItems.PINK_COSMOS, 2, 3, l2);
			add(list, c, BakeryItems.BLUEBELLS, 2, 3, l2);
			add(list, c, BakeryItems.LILY_OF_THE_VALLEY, 2, 3, l2);
			add(list, c, BakeryItems.MOSSY_PEBBLES, 2, 3, l2);
			add(list, c, BakeryItems.WILD_MUSHROOMS, 2, 3, l2);
			add(list, c, BakeryItems.HANGING_IVY, 2, 4, l2);
			add(list, c, BakeryItems.FOXGLOVE, 1, 4, l2);
			add(list, c, BakeryItems.CATTAILS, 2, 4, l2);
			add(list, c, BakeryItems.BLOSSOM_LILY_PAD, 2, 4, l2);
			add(list, c, BakeryItems.ROSE_BUSH, 1, 6, l3);
			add(list, c, BakeryItems.HYDRANGEA_BUSH, 1, 6, l3);
			add(list, c, BakeryItems.MOSSY_LOG, 1, 6, l3);
			add(list, c, BakeryItems.MOSSY_BOULDER, 1, 6, l3);
			sub = "yard";
			add(list, c, BakeryItems.PINK_PICKET_FENCE, 8, 5, l1);
			add(list, c, BakeryItems.TULIP_POT, 1, 4, l1);
			add(list, c, BakeryItems.POTTED_SUCCULENT, 1, 4, l1);
			add(list, c, BakeryItems.STEPPING_STONES, 3, 3, l1);
			add(list, c, BakeryItems.POTTED_FERN, 1, 5, l2);
			add(list, c, BakeryItems.PASTEL_BENCH, 1, 8, l2);
			add(list, c, BakeryItems.STUMP_STOOL, 1, 7, l2);
			add(list, c, BakeryItems.STRAWBERRY_CRATE, 1, 5, l2);
			add(list, c, BakeryItems.LEMON_CRATE, 1, 5, l2);
			add(list, c, BakeryItems.PARASOL_TABLE, 1, 12, l3);
			add(list, c, BakeryItems.PINK_MAILBOX, 1, 8, l3);
			add(list, c, BakeryItems.HEART_LAMP_POST, 1, 10, l3);
			add(list, c, BakeryItems.MUSHROOM_LAMP, 1, 10, l3);
			add(list, c, BakeryItems.BIRD_BATH, 1, 12, l3);
			add(list, c, BakeryItems.COCONUT_CRATE, 1, 5, l3);
			add(list, c, BakeryItems.BUNNY_STATUE, 1, 12, l4);
			add(list, c, BakeryItems.PASTRY_CART, 1, 18, l4);
			sub = "front";
			add(list, c, BakeryItems.WELCOME_MAT, 1, 3, l1);
			add(list, c, BakeryItems.CHALKBOARD_SIGN, 1, 4, l1);
			add(list, c, BakeryItems.OPEN_SIGN, 1, 3, l1);
			add(list, c, BakeryItems.FLOWER_BOX, 2, 5, l1);
			add(list, c, BakeryItems.BUNTING, 4, 4, l1);
			add(list, c, BakeryItems.PINK_AWNING, 4, 5, l2);
			add(list, c, BakeryItems.MINT_AWNING, 4, 5, l2);
			add(list, c, BakeryItems.HANGING_FLOWERS, 2, 6, l2);

			// Decor: inside your bakery - furniture, floors and walls, lights and wall art, cute little things, and the cats.
			c = Category.DECOR;
			sub = "furniture";
			add(list, c, BakeryItems.CAFE_TABLE, 1, 5, l1);
			add(list, c, BakeryItems.CAFE_CHAIR, 2, 5, l1);
			add(list, c, BakeryItems.CAFE_JUKEBOX, 1, 10, l1);
			add(list, c, BakeryItems.MACARON_POUF, 1, 7, l2);
			add(list, c, BakeryItems.PINK_COUCH, 1, 8, l2);
			add(list, c, BakeryItems.JAR_SHELF, 1, 8, l3);
			add(list, c, BakeryItems.CAKE_SHELF, 1, 12, l3);
			sub = "walls";
			add(list, c, BakeryItems.PINK_CHECKER_TILES, 8, 5, l1);
			add(list, c, BakeryItems.MINT_CHECKER_TILES, 8, 5, l1);
			add(list, c, BakeryItems.LILAC_CHECKER_TILES, 8, 5, l1);
			add(list, c, BakeryItems.BUTTER_CHECKER_TILES, 8, 5, l1);
			add(list, c, BakeryItems.PINK_TILES, 8, 4, l1);
			add(list, c, BakeryItems.MINT_TILES, 8, 4, l1);
			add(list, c, BakeryItems.LAVENDER_TILES, 8, 4, l1);
			add(list, c, BakeryItems.CREAM_TILES, 8, 4, l1);
			add(list, c, BakeryItems.GINGHAM_WALLPAPER, 8, 5, l1);
			add(list, c, BakeryItems.STRAWBERRY_WALLPAPER, 8, 5, l2);
			add(list, c, BakeryItems.HEART_WALLPAPER, 8, 5, l2);
			add(list, c, BakeryItems.POLKA_DOT_WALLPAPER, 8, 5, l2);
			add(list, c, BakeryItems.BOW_WALLPAPER, 8, 6, l3);
			add(list, c, BakeryItems.HEART_RUG, 4, 5, l2);
			add(list, c, BakeryItems.CLOUD_RUG, 4, 5, l2);
			add(list, c, BakeryItems.LACE_CURTAINS, 2, 5, l2);
			sub = "lights";
			add(list, c, BakeryItems.FAIRY_LIGHTS, 4, 6, l2);
			add(list, c, BakeryItems.STAR_LIGHTS, 4, 6, l2);
			add(list, c, BakeryItems.CUPCAKE_LAMP, 1, 8, l3);
			add(list, c, BakeryItems.HEART_MIRROR, 1, 10, l3);
			add(list, c, BakeryItems.DONUT_WALL, 1, 10, l3);
			add(list, c, BakeryItems.NEON_HEART_SIGN, 1, 16, l4);
			sub = "touches";
			add(list, c, BakeryItems.TULIP_VASE, 1, 4, l1);
			add(list, c, BakeryItems.FLOUR_SACK, 1, 4, l1);
			add(list, c, BakeryItems.BREAD_BASKET, 1, 5, l1);
			add(list, c, BakeryItems.COOKIE_JAR, 1, 6, l2);
			add(list, c, BakeryItems.TEA_SET, 1, 7, l2);
			add(list, c, BakeryItems.BUNNY_PLUSH, 1, 8, l2);
			add(list, c, BakeryItems.BEAR_PLUSH, 1, 8, l2);
			add(list, c, BakeryItems.PINK_HEART_BALLOON, 1, 4, l3);
			add(list, c, BakeryItems.LILAC_HEART_BALLOON, 1, 4, l3);
			add(list, c, BakeryItems.MINT_HEART_BALLOON, 1, 4, l3);
			sub = "pets";
			add(list, c, BakeryItems.KITTEN_BASKET, 1, 12, l1);
			add(list, c, BakeryItems.CAT_BED, 1, 5, l1);

			// Outfits
			c = Category.WARDROBE;
			sub = null;
			add(list, c, BakeryItems.RIBBON_BOW, 1, 12, l1);
			add(list, c, BakeryItems.CHEF_HAT, 1, 16, l1);
			add(list, c, BakeryItems.FRILLY_APRON, 1, 25, l2);
			add(list, c, BakeryItems.STRAWBERRY_BERET, 1, 25, l2);
			add(list, c, BakeryItems.BUNNY_EARS, 1, 30, l3);
			add(list, c, BakeryItems.CAT_EARS, 1, 30, l3);

			sub = null;
			entries = List.copyOf(list);
		}
		return entries;
	}

	private static void add(List<Entry> list, Category category, ItemConvertible item, int count, int price, CafeLevel level) {
		list.add(new Entry(category, item.asItem(), count, price, level, 0, sub));
	}

	private static void stock(List<Entry> list, Category category, ItemConvertible item, int count, int price, CafeLevel level, int perDay) {
		list.add(new Entry(category, item.asItem(), count, price, level, perDay, sub));
	}

	/** How many more of entry {@code index} this baker can buy today, or -1 for no limit. */
	public static int left(BakeryReputation.Stats stats, int index, long day) {
		int stock = entries().get(index).stock();
		if (stock <= 0) return -1;
		if (stats.stockDay != day) return stock;
		return Math.max(0, stock - stats.bought.getOrDefault(index, 0));
	}

	/** Today's stock for every entry (-1 = unlimited), for the catalog screen. */
	public static List<Integer> stockLeft(BakeryReputation.Stats stats, long day) {
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < entries().size(); i++) out.add(left(stats, i, day));
		return out;
	}

	public static long day(World world) {
		return world.getTimeOfDay() / 24000L;
	}

	/** Today's Daily Special (always something a new bakery can buy). */
	public static int featured(long day) {
		List<Entry> all = entries();
		int start = (int) Math.floorMod(day * 7919L + 13, all.size());
		for (int i = 0; i < all.size(); i++) {
			int index = (start + i) % all.size();
			// Today's special is always something cute to decorate with (or wear), never a bag of bricks.
			Entry e = all.get(index);
			boolean cute = e.category() == Category.DECOR || e.category() == Category.WARDROBE
					|| e.category() == Category.GARDEN && ("yard".equals(e.sub()) || "front".equals(e.sub()) || "nature".equals(e.sub()));
			if (cute && e.level() == CafeLevel.STALL && e.price() > 2) return index;
		}
		return start;
	}

	public static int price(int index, long day) {
		int price = entries().get(index).price();
		return index == featured(day) ? Math.max(1, price * 3 / 4) : price;
	}

	/** Buys entry {@code index} from the catalog at {@code pos}. */
	public static boolean buy(ServerPlayerEntity player, BlockPos pos, int index) {
		ServerWorld world = player.getServerWorld();
		if (index < 0 || index >= entries().size() || player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64
				|| !world.getBlockState(pos).isOf(BakeryBlocks.BAKERY_CATALOG)) {
			return false;
		}
		Entry entry = entries().get(index);
		CafeLevel level = CafeLevel.of(BakeryReputation.get(player.getServer()).of(player.getUuid()).served);
		if (entry.level().ordinal() > level.ordinal() && !player.getAbilities().creativeMode) {
			player.sendMessage(Text.translatable("message.bakery.catalog.locked", entry.level().displayName()).formatted(Formatting.GOLD), true);
			world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 0.6f, 1.3f);
			return false;
		}
		BakeryReputation reputation = BakeryReputation.get(player.getServer());
		BakeryReputation.Stats stats = reputation.of(player.getUuid());
		long today = day(world);
		if (left(stats, index, today) == 0 && !player.getAbilities().creativeMode) {
			player.sendMessage(Text.translatable("message.bakery.catalog.sold_out").formatted(Formatting.GOLD), true);
			world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 0.6f, 1.3f);
			return false;
		}
		int price = price(index, today);
		if (!Coins.take(player, price)) {
			player.sendMessage(Text.translatable("message.bakery.catalog.poor", price).formatted(Formatting.RED), true);
			world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 0.6f, 1.3f);
			return false;
		}
		if (entry.stock() > 0) {
			if (stats.stockDay != today) {
				stats.stockDay = today;
				stats.bought.clear();
			}
			stats.bought.merge(index, 1, Integer::sum);
			reputation.markDirty();
		}
		player.getInventory().offerOrDrop(entry.stack());
		player.currentScreenHandler.sendContentUpdates();
		world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.7f, 1.5f);
		world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1.2f);
		player.sendMessage(Text.translatable("message.bakery.catalog.bought", entry.stack().getName(), price).formatted(Formatting.LIGHT_PURPLE), true);
		Stickers.award(player, Sticker.SHOPPER);
		return true;
	}
}
