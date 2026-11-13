package com.bakery.registry;

import com.bakery.Bakery;
import com.bakery.food.Drink;
import com.bakery.food.Flavor;
import com.bakery.food.Freshness;
import com.bakery.food.Perishable;
import com.bakery.food.Preservation;
import com.bakery.food.Topping;
import com.bakery.item.BakeryItem;
import com.bakery.item.DrinkItem;
import com.bakery.item.GuideItem;
import com.bakery.item.JuiceItem;
import com.bakery.item.PlaceableFoodItem;
import com.bakery.item.PreservingItem;
import com.bakery.item.TooltipItem;
import com.bakery.sweet.CookieShape;
import com.bakery.sweet.GiftBoxItem;
import com.bakery.sweet.HatItem;
import com.bakery.sweet.Icing;
import com.bakery.sweet.IcingBagItem;
import com.bakery.sweet.SugarCookieItem;
import com.bakery.sweet.WrappedGiftItem;
import com.bakery.tree.WoodSet;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import java.util.EnumMap;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.DoorBlock;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TallBlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class BakeryItems {
	private static final int DAY = Freshness.DAY;
	private static final List<Item> KITCHEN = new ArrayList<>();
	private static final List<Item> DECOR = new ArrayList<>();
	private static final List<Item> SWEET = new ArrayList<>();
	private static List<Item> current = KITCHEN;

	// Tools and workstations
	public static final Item BAKERY_GUIDE = register("bakery_guide", new GuideItem(new Item.Settings().maxCount(1)));
	public static final Item PATH_RIBBON = register("path_ribbon", new com.bakery.shop.PathRibbonItem(new Item.Settings().maxCount(1)));
	public static final Item SPATULA = register("sweetheart_spatula", new TooltipItem(new Item.Settings().maxCount(1), "spatula"));
	public static final Item ROLLING_PIN = register("rolling_pin", new TooltipItem(new Item.Settings().maxDamage(128), "rolling_pin"));
	public static final Item WHISK = register("whisk", new TooltipItem(new Item.Settings().maxDamage(128), "whisk"));
	public static final Item OVEN_HEART = register("oven_heart", new BlockItem(BakeryBlocks.OVEN_HEART, new Item.Settings()));
	public static final Item MIXING_BOWL = register("mixing_bowl", new BlockItem(BakeryBlocks.MIXING_BOWL, new Item.Settings()));
	public static final Item PASTRY_BOARD = register("pastry_board", new BlockItem(BakeryBlocks.PASTRY_BOARD, new Item.Settings()));
	public static final Item PROOFING_BASKET = register("proofing_basket", new BlockItem(BakeryBlocks.PROOFING_BASKET, new Item.Settings()));
	public static final Item JUICER = register("juicer", new BlockItem(BakeryBlocks.JUICER, new Item.Settings()));
	public static final Item ICE_CREAM_CHURN = register("ice_cream_churn", new BlockItem(BakeryBlocks.ICE_CREAM_CHURN, new Item.Settings()));
	public static final Item JAM_POT = register("jam_pot", new BlockItem(BakeryBlocks.JAM_POT, new Item.Settings()));
	public static final Item MILLSTONE_CORE = register("millstone_core", new BlockItem(BakeryBlocks.MILLSTONE_CORE, new Item.Settings()));
	public static final Item CHOCOLATE_FOUNTAIN = register("chocolate_fountain", new BlockItem(BakeryBlocks.CHOCOLATE_FOUNTAIN, new Item.Settings()));

	// Seeds
	public static final Item STRAWBERRY_SEEDS = seeds("strawberry_seeds", BakeryBlocks.STRAWBERRY_CROP);
	public static final Item BLUEBERRY_SEEDS = seeds("blueberry_seeds", BakeryBlocks.BLUEBERRY_CROP);
	public static final Item RASPBERRY_SEEDS = seeds("raspberry_seeds", BakeryBlocks.RASPBERRY_CROP);
	public static final Item TEA_SEEDS = seeds("tea_seeds", BakeryBlocks.TEA_CROP);
	public static final Item MINT_SEEDS = seeds("mint_seeds", BakeryBlocks.MINT_CROP);
	public static final Item VANILLA_SEEDS = seeds("vanilla_seeds", BakeryBlocks.VANILLA_CROP);
	public static final Item LAVENDER_SEEDS = seeds("lavender_seeds", BakeryBlocks.LAVENDER_CROP);
	public static final Item ROSE_SEEDS = seeds("rose_seeds", BakeryBlocks.ROSE_CROP);
	public static final Item PINEAPPLE_SEEDS = seeds("pineapple_seeds", BakeryBlocks.PINEAPPLE_CROP);
	public static final Item STARBLOOM_SEEDS = seeds("starbloom_seeds", BakeryBlocks.STARBLOOM_CROP);
	public static final Item CORN_SEEDS = seeds("corn_seeds", BakeryBlocks.CORN_CROP);
	public static final Item PEA_SEEDS = seeds("pea_seeds", BakeryBlocks.PEA_CROP);
	public static final Item SWEET_POTATO_SEEDS = seeds("sweet_potato_seeds", BakeryBlocks.SWEET_POTATO_CROP);
	public static final Item ZUCCHINI_SEEDS = seeds("zucchini_seeds", BakeryBlocks.ZUCCHINI_STEM);
	public static final Item RHUBARB_SEEDS = seeds("rhubarb_seeds", BakeryBlocks.RHUBARB_CROP);
	public static final Item SUGAR_BEET_SEEDS = seeds("sugar_beet_seeds", BakeryBlocks.SUGAR_BEET_CROP);
	public static final Item CRANBERRY_SEEDS = seeds("cranberry_seeds", BakeryBlocks.CRANBERRY_CROP);
	public static final Item PEANUT_SEEDS = seeds("peanut_seeds", BakeryBlocks.PEANUT_CROP);
	public static final Item HIBISCUS_SEEDS = seeds("hibiscus_seeds", BakeryBlocks.HIBISCUS_CROP);
	public static final Item CANDYFLOSS_SEEDS = seeds("candyfloss_seeds", BakeryBlocks.CANDYFLOSS_CROP);

	// Harvest
	public static final Item STRAWBERRY = register("strawberry", new BakeryItem(new Item.Settings().food(snack(2, 0.3f)), 5 * DAY));
	public static final Item BLUEBERRY = register("blueberry", new BakeryItem(new Item.Settings().food(snack(2, 0.3f)), 6 * DAY));
	public static final Item RASPBERRY = register("raspberry", new BakeryItem(new Item.Settings().food(snack(2, 0.3f)), 4 * DAY));
	public static final Item TEA_LEAVES = register("tea_leaves", new BakeryItem(new Item.Settings(), 4 * DAY));
	public static final Item MINT_LEAVES = register("mint_leaves", new BakeryItem(new Item.Settings(), 4 * DAY));
	public static final Item VANILLA_POD = register("vanilla_pod", new Item(new Item.Settings()));
	public static final Item LAVENDER = register("lavender", new Item(new Item.Settings()));
	public static final Item ROSE_PETALS = register("rose_petals", new BakeryItem(new Item.Settings(), 4 * DAY));
	public static final Item PINEAPPLE = register("pineapple", new BakeryItem(new Item.Settings().food(food(4, 0.5f)), 6 * DAY));
	public static final Item STAR_PETALS = register("star_petals", new TooltipItem(new Item.Settings(), "star_petals"));
	public static final Item COCONUT = register("coconut", new TooltipItem(new Item.Settings(), "coconut"));
	public static final Item LEMON = register("lemon", new BakeryItem(new Item.Settings().food(snack(1, 0.2f)), 10 * DAY));
	public static final Item CHERRIES = register("cherries", new BakeryItem(new Item.Settings().food(snack(2, 0.3f)), 4 * DAY));
	public static final Item ORANGE = register("orange", new BakeryItem(new Item.Settings().food(food(3, 0.4f)), 8 * DAY));
	public static final Item PEACH = register("peach", new BakeryItem(new Item.Settings().food(food(3, 0.5f)), 4 * DAY));
	public static final Item MANGO = register("mango", new BakeryItem(new Item.Settings().food(food(4, 0.5f)), 5 * DAY));
	public static final Item PLUM = register("plum", new BakeryItem(new Item.Settings().food(food(3, 0.4f)), 5 * DAY));
	public static final Item CORN = register("corn", new BakeryItem(new Item.Settings().food(food(2, 0.3f)), 8 * DAY));
	public static final Item PEA_POD = register("pea_pod", new BakeryItem(new Item.Settings().food(snack(1, 0.3f)), 4 * DAY));
	public static final Item SWEET_POTATO = register("sweet_potato", new BakeryItem(new Item.Settings().food(food(2, 0.3f)), 14 * DAY));
	public static final Item BAKED_SWEET_POTATO = register("baked_sweet_potato", new BakeryItem(new Item.Settings().food(food(6, 0.7f)), 4 * DAY));
	public static final Item ZUCCHINI = register("zucchini", new BakeryItem(new Item.Settings().food(food(2, 0.3f)), 6 * DAY));
	public static final Item RHUBARB = register("rhubarb", new BakeryItem(new Item.Settings(), 6 * DAY));
	public static final Item SUGAR_BEET = register("sugar_beet", new BakeryItem(new Item.Settings().food(food(1, 0.2f)), 14 * DAY));
	public static final Item CRANBERRY = register("cranberry", new BakeryItem(new Item.Settings().food(snack(1, 0.2f)), 8 * DAY));
	public static final Item PEANUT = register("peanut", new Item(new Item.Settings().food(snack(1, 0.3f))));
	public static final Item HIBISCUS = register("hibiscus", new Item(new Item.Settings()));
	public static final Item CANDYFLOSS = register("candyfloss", new TooltipItem(new Item.Settings().food(snack(2, 0.1f)), "candyfloss"));

	// Pantry
	public static final Item FLOUR = register("flour", new Item(new Item.Settings()));
	public static final Item BUTTER = register("butter", new BakeryItem(new Item.Settings(), 6 * DAY));
	public static final Item WHIPPED_CREAM = register("whipped_cream", new BakeryItem(new Item.Settings().food(snack(2, 0.2f)), 2 * DAY));
	public static final Item CHOCOLATE_BAR = register("chocolate_bar", new Item(new Item.Settings().food(food(3, 0.4f))));
	public static final Item MATCHA_POWDER = register("matcha_powder", new Item(new Item.Settings()));
	public static final Item COCOA_POWDER = register("cocoa_powder", new Item(new Item.Settings()));
	public static final Item CORNMEAL = register("cornmeal", new Item(new Item.Settings()));
	public static final Item PEANUT_BUTTER = register("peanut_butter", new Item(new Item.Settings().maxCount(16).food(new FoodComponent.Builder()
			.nutrition(4).saturationModifier(0.6f).usingConvertsTo(net.minecraft.item.Items.GLASS_BOTTLE).build())));
	public static final Item COCONUT_FLAKES = register("coconut_flakes", new TooltipItem(new Item.Settings(), "topping"));
	public static final Item SPRINKLES = register("sprinkles", new TooltipItem(new Item.Settings(), "topping"));
	public static final Item CANDIED_PETALS = register("candied_petals", new Item(new Item.Settings().food(snack(2, 0.4f))));
	public static final Item GLASS_JAR = register("glass_jar", new PreservingItem(new Item.Settings(), Preservation.JARRED, BakeryItems::isJarrable));
	public static final Item STRAWBERRY_JAM = register("strawberry_jam", new Item(new Item.Settings().maxCount(16).food(jam())));
	public static final Item BLUEBERRY_JAM = register("blueberry_jam", new Item(new Item.Settings().maxCount(16).food(jam())));
	public static final Item RASPBERRY_JAM = register("raspberry_jam", new Item(new Item.Settings().maxCount(16).food(jam())));
	public static final Item FRUIT_JAM = register("fruit_jam", new BakeryItem(new Item.Settings().maxCount(16).food(jam()), 0));
	public static final Item JUICE = register("juice", new JuiceItem(new Item.Settings().maxCount(16).food(new FoodComponent.Builder()
			.nutrition(2).saturationModifier(0.4f).alwaysEdible().usingConvertsTo(net.minecraft.item.Items.GLASS_BOTTLE).build()), 2 * DAY));
	public static final Item ICE_CREAM = register("ice_cream", new BakeryItem(new Item.Settings().maxCount(16).food(snack(3, 0.4f)), DAY, true));
	public static final Item CHOCOLATE_TREAT = register("chocolate_treat", new BakeryItem(new Item.Settings().food(snack(3, 0.5f)), 6 * DAY, true));
	public static final Item WAX_PAPER = register("wax_paper", new PreservingItem(new Item.Settings(), Preservation.WRAPPED, BakeryItems::isWrappable));
	public static final Item FROST_WRAP = register("frost_wrap", new PreservingItem(new Item.Settings(), Preservation.CHILLED, stack -> true));
	public static final Item MUG = register("mug", new Item(new Item.Settings()));

	// Dough
	public static final Item BREAD_DOUGH = dough("bread_dough");
	public static final Item SWEET_DOUGH = dough("sweet_dough");
	public static final Item PASTRY_DOUGH = dough("pastry_dough");
	public static final Item COOKIE_DOUGH = dough("cookie_dough");

	// Shaped, ready for the oven
	public static final Item RAW_LOAF = raw("raw_loaf");
	public static final Item RAW_BAGUETTE = raw("raw_baguette");
	public static final Item RAW_CROISSANT = raw("raw_croissant");
	public static final Item RAW_CUPCAKE = raw("raw_cupcake");
	public static final Item RAW_DONUT = raw("raw_donut");
	public static final Item RAW_COOKIES = raw("raw_cookies");
	public static final Item RAW_MACARONS = raw("raw_macarons");
	public static final Item RAW_CAKE = raw("raw_cake");
	public static final Item RAW_PIE = raw("raw_pie");

	// Baked
	public static final Item BREAD_LOAF = bake("bread_loaf", BakeryBlocks.BREAD_LOAF, food(6, 0.7f), 5 * DAY, 16);
	public static final Item BAGUETTE = bake("baguette", BakeryBlocks.BAGUETTE, food(7, 0.6f), 3 * DAY, 16);
	public static final Item CROISSANT = bake("croissant", BakeryBlocks.CROISSANT, food(5, 0.6f), 3 * DAY, 64);
	public static final Item CUPCAKE = bake("cupcake", BakeryBlocks.CUPCAKE, food(4, 0.5f), 4 * DAY, 64);
	public static final Item DONUT = bake("donut", BakeryBlocks.DONUT, food(4, 0.4f), 3 * DAY, 64);
	public static final Item COOKIES = bake("cookies", BakeryBlocks.COOKIES, snack(2, 0.2f), 12 * DAY, 64);
	public static final Item MACARONS = bake("macarons", BakeryBlocks.MACARONS, snack(2, 0.4f), 7 * DAY, 64);
	public static final Item CAKE = bake("cake", BakeryBlocks.CAKE, null, 4 * DAY, 1);
	public static final Item PIE = bake("pie", BakeryBlocks.PIE, null, 4 * DAY, 1);
	public static final Item BURNT_PASTRY = register("burnt_pastry", new TooltipItem(new Item.Settings().food(food(1, 0.1f)), "burnt"));

	// Drinks
	public static final Item MATCHA_LATTE = drink("matcha_latte", Drink.MATCHA_LATTE, new StatusEffectInstance(StatusEffects.HASTE, 2400), new StatusEffectInstance(StatusEffects.SPEED, 1200));
	public static final Item HOT_CHOCOLATE = drink("hot_chocolate", Drink.HOT_CHOCOLATE, new StatusEffectInstance(StatusEffects.REGENERATION, 200), new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 1200));
	public static final Item STRAWBERRY_MILK = drink("strawberry_milk", Drink.STRAWBERRY_MILK, new StatusEffectInstance(StatusEffects.ABSORPTION, 1200), new StatusEffectInstance(StatusEffects.JUMP_BOOST, 1200));
	public static final Item LAVENDER_TEA = drink("lavender_tea", Drink.LAVENDER_TEA, new StatusEffectInstance(StatusEffects.REGENERATION, 400), new StatusEffectInstance(StatusEffects.SLOW_FALLING, 1200));
	public static final Item ROSE_TEA = drink("rose_tea", Drink.ROSE_TEA, new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 2400), new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1));
	public static final Item STARLIGHT_TEA = drink("starlight_tea", Drink.STARLIGHT_TEA, new StatusEffectInstance(StatusEffects.NIGHT_VISION, 3600), new StatusEffectInstance(StatusEffects.LUCK, 3600));
	public static final Item LEMONADE = drink("lemonade", Drink.LEMONADE, new StatusEffectInstance(StatusEffects.SPEED, 1800), new StatusEffectInstance(StatusEffects.WATER_BREATHING, 1200));
	public static final Item COCONUT_MILK = drink("coconut_milk", Drink.COCONUT_MILK, new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 1200), new StatusEffectInstance(StatusEffects.RESISTANCE, 600));
	public static final Item MINT_TEA = drink("mint_tea", Drink.MINT_TEA, new StatusEffectInstance(StatusEffects.HASTE, 1200), new StatusEffectInstance(StatusEffects.REGENERATION, 200));
	public static final Item PINEAPPLE_SMOOTHIE = drink("pineapple_smoothie", Drink.PINEAPPLE_SMOOTHIE, new StatusEffectInstance(StatusEffects.SATURATION, 20), new StatusEffectInstance(StatusEffects.JUMP_BOOST, 1800));

	// ---- Sweet Life tab: coins, the catalog, cookie and cake decorating, gifts and outfits ----
	static {
		current = SWEET;
	}

	public static final Item SWEET_COIN = register("sweet_coin", new TooltipItem(new Item.Settings(), "sweet_coin"));
	public static final Item BAKERY_CATALOG = block(BakeryBlocks.BAKERY_CATALOG);
	public static final Item CAKE_TURNTABLE = block(BakeryBlocks.CAKE_TURNTABLE);
	public static final Item HEART_CUTTER = cutter("heart_cutter");
	public static final Item STAR_CUTTER = cutter("star_cutter");
	public static final Item BUNNY_CUTTER = cutter("bunny_cutter");
	public static final Item FLOWER_CUTTER = cutter("flower_cutter");
	public static final Item RAW_SUGAR_COOKIE = register("raw_sugar_cookie", new SugarCookieItem(new Item.Settings().maxCount(16), 2 * DAY, false));
	public static final Item SUGAR_COOKIE = register("sugar_cookie", new SugarCookieItem(new Item.Settings().food(snack(3, 0.4f)), 10 * DAY, true));
	public static final Item PINK_ICING = icing("pink_icing", Icing.PINK);
	public static final Item MINT_ICING = icing("mint_icing", Icing.MINT);
	public static final Item LILAC_ICING = icing("lilac_icing", Icing.LILAC);
	public static final Item LEMON_ICING = icing("lemon_icing", Icing.LEMON);
	public static final Item SKY_ICING = icing("sky_icing", Icing.SKY);
	public static final Item WHITE_ICING = icing("white_icing", Icing.WHITE);
	public static final Item GIFT_BOX = register("gift_box", new GiftBoxItem(new Item.Settings()));
	public static final Item WRAPPED_GIFT = register("wrapped_gift", new WrappedGiftItem(new Item.Settings().maxCount(1)));
	public static final Item CHEF_HAT = hat("chef_hat");
	public static final Item STRAWBERRY_BERET = hat("strawberry_beret");
	public static final Item BUNNY_EARS = hat("bunny_ears");
	public static final Item CAT_EARS = hat("cat_ears");
	public static final Item RIBBON_BOW = hat("ribbon_bow");
	public static final RegistryEntry<ArmorMaterial> APRON_MATERIAL = Registry.registerReference(Registries.ARMOR_MATERIAL, Bakery.id("frilly_apron"),
			new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
				for (ArmorItem.Type type : ArmorItem.Type.values()) map.put(type, 1);
			}), 15, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, () -> Ingredient.ofItems(Items.PINK_WOOL),
					List.of(new ArmorMaterial.Layer(Bakery.id("frilly_apron"))), 0f, 0f));
	public static final Item FRILLY_APRON = register("frilly_apron", new ArmorItem(APRON_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings().maxCount(1)) {
		@Override
		public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
			tooltip.add(Text.translatable("tooltip.bakery.outfit").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
		}
	});
	// Cozy update: oven bellows, cake molds and fancy cake toppers
	public static final Item BELLOWS = register("bellows", new TooltipItem(new Item.Settings().maxDamage(256), "bellows"));
	public static final Item SQUARE_CAKE_MOLD = register("square_cake_mold", new TooltipItem(new Item.Settings().maxCount(1), "cake_mold"));
	public static final Item HEART_CAKE_MOLD = register("heart_cake_mold", new TooltipItem(new Item.Settings().maxCount(1), "cake_mold"));
	public static final Item ROUND_CAKE_MOLD = register("round_cake_mold", new TooltipItem(new Item.Settings().maxCount(1), "cake_mold"));
	public static final Item FONDANT_HEART = topper("fondant_heart");
	public static final Item FONDANT_STAR = topper("fondant_star");
	public static final Item FONDANT_BUNNY = topper("fondant_bunny");
	public static final Item SUGAR_FLOWER = topper("sugar_flower");
	public static final Item MINI_MACARON = topper("mini_macaron");
	public static final Item SUGAR_PEARLS = topper("sugar_pearls");
	public static final Item CHOCOLATE_CURL = topper("chocolate_curl");
	public static final Item SUGAR_BUTTERFLY = topper("sugar_butterfly");
	public static final Item GOLDEN_CROWN = topper("golden_crown");
	public static final Item CAKE_FLAG = topper("cake_flag");
	public static final Item[] TOPPERS = {FONDANT_HEART, FONDANT_STAR, FONDANT_BUNNY, SUGAR_FLOWER, MINI_MACARON, SUGAR_PEARLS, CHOCOLATE_CURL,
			SUGAR_BUTTERFLY, GOLDEN_CROWN, CAKE_FLAG};

	// ---- Decor & garden tab ----
	static {
		current = DECOR;
	}

	public static final Item BAKERY_COUNTER = block(BakeryBlocks.BAKERY_COUNTER);
	public static final Item DISPLAY_CASE = block(BakeryBlocks.DISPLAY_CASE);
	public static final Item SERVICE_BELL = block(BakeryBlocks.SERVICE_BELL);
	public static final Item CAFE_TABLE = block(BakeryBlocks.CAFE_TABLE);
	public static final Item CAFE_CHAIR = block(BakeryBlocks.CAFE_CHAIR);
	public static final Item FLOUR_SACK = block(BakeryBlocks.FLOUR_SACK);
	public static final Item BREAD_BASKET = block(BakeryBlocks.BREAD_BASKET);
	public static final Item CAKE_STAND = block(BakeryBlocks.CAKE_STAND);
	public static final Item MENU_BOARD = block(BakeryBlocks.MENU_BOARD);
	public static final Item BUNTING = block(BakeryBlocks.BUNTING);
	public static final Item JAR_SHELF = block(BakeryBlocks.JAR_SHELF);
	public static final Item STRAWBERRY_CRATE = block(BakeryBlocks.STRAWBERRY_CRATE);
	public static final Item LEMON_CRATE = block(BakeryBlocks.LEMON_CRATE);
	public static final Item COCONUT_CRATE = block(BakeryBlocks.COCONUT_CRATE);
	public static final Item PINK_TILES = block(BakeryBlocks.PINK_TILES);
	public static final Item MINT_TILES = block(BakeryBlocks.MINT_TILES);
	public static final Item LAVENDER_TILES = block(BakeryBlocks.LAVENDER_TILES);
	public static final Item CREAM_TILES = block(BakeryBlocks.CREAM_TILES);
	public static final Item PINK_CHECKER_TILES = block(BakeryBlocks.PINK_CHECKER_TILES);
	public static final Item MINT_CHECKER_TILES = block(BakeryBlocks.MINT_CHECKER_TILES);
	public static final Item LILAC_CHECKER_TILES = block(BakeryBlocks.LILAC_CHECKER_TILES);
	public static final Item BUTTER_CHECKER_TILES = block(BakeryBlocks.BUTTER_CHECKER_TILES);
	public static final Item STRAWBERRY_WALLPAPER = block(BakeryBlocks.STRAWBERRY_WALLPAPER);
	public static final Item GINGHAM_WALLPAPER = block(BakeryBlocks.GINGHAM_WALLPAPER);
	public static final Item HEART_WALLPAPER = block(BakeryBlocks.HEART_WALLPAPER);
	public static final Item POLKA_DOT_WALLPAPER = block(BakeryBlocks.POLKA_DOT_WALLPAPER);
	public static final Item HEART_RUG = block(BakeryBlocks.HEART_RUG);
	public static final Item CLOUD_RUG = block(BakeryBlocks.CLOUD_RUG);
	public static final Item PINK_HEART_BALLOON = block(BakeryBlocks.PINK_HEART_BALLOON);
	public static final Item LILAC_HEART_BALLOON = block(BakeryBlocks.LILAC_HEART_BALLOON);
	public static final Item MINT_HEART_BALLOON = block(BakeryBlocks.MINT_HEART_BALLOON);
	public static final Item FAIRY_LIGHTS = block(BakeryBlocks.FAIRY_LIGHTS);
	public static final Item CUPCAKE_LAMP = block(BakeryBlocks.CUPCAKE_LAMP);
	public static final Item COOKIE_JAR = block(BakeryBlocks.COOKIE_JAR);
	public static final Item MACARON_TOWER = block(BakeryBlocks.MACARON_TOWER);
	public static final Item PINK_AWNING = block(BakeryBlocks.PINK_AWNING);
	public static final Item MINT_AWNING = block(BakeryBlocks.MINT_AWNING);
	public static final Item PINK_COUCH = block(BakeryBlocks.PINK_COUCH);
	public static final Item MACARON_POUF = block(BakeryBlocks.MACARON_POUF);
	public static final Item HEART_MIRROR = block(BakeryBlocks.HEART_MIRROR);
	public static final Item NEON_HEART_SIGN = block(BakeryBlocks.NEON_HEART_SIGN);
	public static final Item BUNNY_PLUSH = block(BakeryBlocks.BUNNY_PLUSH);
	public static final Item BEAR_PLUSH = block(BakeryBlocks.BEAR_PLUSH);
	public static final Item TULIP_VASE = block(BakeryBlocks.TULIP_VASE);
	public static final Item TEA_SET = block(BakeryBlocks.TEA_SET);
	public static final Item CAKE_SHELF = block(BakeryBlocks.CAKE_SHELF);
	public static final Item DONUT_WALL = block(BakeryBlocks.DONUT_WALL);
	public static final Item LACE_CURTAINS = block(BakeryBlocks.LACE_CURTAINS);
	public static final Item STAR_LIGHTS = block(BakeryBlocks.STAR_LIGHTS);
	public static final Item BOW_WALLPAPER = block(BakeryBlocks.BOW_WALLPAPER);
	public static final Item CASH_REGISTER = block(BakeryBlocks.CASH_REGISTER);
	public static final Item TIP_JAR = block(BakeryBlocks.TIP_JAR);
	public static final Item OPEN_SIGN = block(BakeryBlocks.OPEN_SIGN);
	public static final Item CHALKBOARD_SIGN = block(BakeryBlocks.CHALKBOARD_SIGN);
	public static final Item PASTEL_BENCH = block(BakeryBlocks.PASTEL_BENCH);
	public static final Item FLOWER_BOX = block(BakeryBlocks.FLOWER_BOX);
	public static final Item PINK_PICKET_FENCE = block(BakeryBlocks.PINK_PICKET_FENCE);
	public static final Item HEART_LAMP_POST = block(BakeryBlocks.HEART_LAMP_POST);
	public static final Item PARASOL_TABLE = block(BakeryBlocks.PARASOL_TABLE);
	public static final Item TULIP_POT = block(BakeryBlocks.TULIP_POT);
	public static final Item HANGING_FLOWERS = block(BakeryBlocks.HANGING_FLOWERS);
	public static final Item BUNNY_STATUE = block(BakeryBlocks.BUNNY_STATUE);
	public static final Item PINK_MAILBOX = block(BakeryBlocks.PINK_MAILBOX);
	public static final Item PASTRY_CART = block(BakeryBlocks.PASTRY_CART);
	public static final Item WELCOME_MAT = block(BakeryBlocks.WELCOME_MAT);

	static {
		for (WoodSet set : BakeryBlocks.WOODS) {
			woodItems(set);
		}
		current = KITCHEN;
	}

	public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP, Bakery.id("bakery"), FabricItemGroup.builder()
			.icon(() -> new ItemStack(CUPCAKE))
			.displayName(Text.translatable("itemGroup.bakery"))
			.entries((context, entries) -> {
				KITCHEN.forEach(entries::add);
				for (Item showcase : new Item[]{CUPCAKE, CAKE, MACARONS, DONUT}) {
					for (Flavor flavor : Flavor.values()) {
						if (flavor == Flavor.PLAIN) continue;
						ItemStack stack = new ItemStack(showcase);
						stack.set(BakeryComponents.FLAVOR, flavor.asString());
						stack.set(BakeryComponents.QUALITY, 5);
						entries.add(stack);
					}
				}
				ItemStack fancy = new ItemStack(CAKE);
				fancy.set(BakeryComponents.FLAVOR, Flavor.STRAWBERRY.asString());
				fancy.set(BakeryComponents.TOPPING, Topping.STRAWBERRIES.asString());
				fancy.set(BakeryComponents.QUALITY, 5);
				entries.add(fancy);
			})
			.build());

	public static final ItemGroup DECOR_GROUP = Registry.register(Registries.ITEM_GROUP, Bakery.id("decor"), FabricItemGroup.builder()
			.icon(() -> new ItemStack(BAKERY_COUNTER))
			.displayName(Text.translatable("itemGroup.bakery.decor"))
			.entries((context, entries) -> DECOR.forEach(entries::add))
			.build());

	public static final ItemGroup SWEET_GROUP = Registry.register(Registries.ITEM_GROUP, Bakery.id("sweet_life"), FabricItemGroup.builder()
			.icon(() -> new ItemStack(SWEET_COIN))
			.displayName(Text.translatable("itemGroup.bakery.sweet_life"))
			.entries((context, entries) -> {
				for (Item item : SWEET) {
					if (item == RAW_SUGAR_COOKIE || item == SUGAR_COOKIE) {
						for (CookieShape shape : CookieShape.values()) {
							ItemStack cookie = new ItemStack(item);
							cookie.set(BakeryComponents.COOKIE_SHAPE, shape.asString());
							entries.add(cookie);
						}
					} else if (item != WRAPPED_GIFT) {
						entries.add(item);
					}
				}
				for (Icing icing : Icing.values()) {
					ItemStack cookie = new ItemStack(SUGAR_COOKIE);
					cookie.set(BakeryComponents.COOKIE_SHAPE, CookieShape.values()[icing.ordinal() % 4].asString());
					cookie.set(BakeryComponents.ICING, icing.color());
					cookie.set(BakeryComponents.QUALITY, 5);
					entries.add(cookie);
				}
			})
			.build());

	private BakeryItems() {
	}

	private static FoodComponent food(int nutrition, float saturation) {
		return new FoodComponent.Builder().nutrition(nutrition).saturationModifier(saturation).build();
	}

	private static FoodComponent snack(int nutrition, float saturation) {
		return new FoodComponent.Builder().nutrition(nutrition).saturationModifier(saturation).snack().build();
	}

	private static FoodComponent jam() {
		return new FoodComponent.Builder().nutrition(6).saturationModifier(0.6f).usingConvertsTo(GLASS_JAR).build();
	}

	private static Item seeds(String name, Block crop) {
		return register(name, new AliasedBlockItem(crop, new Item.Settings()));
	}

	private static Item dough(String name) {
		return register(name, new BakeryItem(new Item.Settings().maxCount(16), 3 * DAY));
	}

	private static Item raw(String name) {
		return register(name, new BakeryItem(new Item.Settings().maxCount(16), 2 * DAY));
	}

	private static Item bake(String name, Block block, FoodComponent food, int shelfLife, int maxCount) {
		Item.Settings settings = new Item.Settings().maxCount(maxCount);
		if (food != null) {
			settings.food(food);
		}
		return register(name, new PlaceableFoodItem(block, settings, shelfLife));
	}

	private static Item drink(String name, Drink drink, StatusEffectInstance... effects) {
		FoodComponent.Builder food = new FoodComponent.Builder().nutrition(3).saturationModifier(0.4f).alwaysEdible().usingConvertsTo(MUG);
		for (StatusEffectInstance effect : effects) {
			food.statusEffect(effect, 1f);
		}
		return register(name, new DrinkItem(BakeryBlocks.MUG, drink, new Item.Settings().maxCount(16).food(food.build()), 2 * DAY));
	}

	private static Item cutter(String name) {
		return register(name, new TooltipItem(new Item.Settings().maxCount(1), "cookie_cutter"));
	}

	private static Item icing(String name, Icing icing) {
		return register(name, new IcingBagItem(new Item.Settings().maxDamage(64), icing));
	}

	private static Item topper(String name) {
		return register(name, new TooltipItem(new Item.Settings(), "cake_topper"));
	}

	private static Item hat(String name) {
		return register(name, new HatItem(new Item.Settings().maxCount(1)));
	}

	private static Item block(Block block) {
		return register(Registries.BLOCK.getId(block).getPath(), new BlockItem(block, new Item.Settings()));
	}

	private static void woodItems(WoodSet set) {
		for (Block b : set.all()) {
			String name = Registries.BLOCK.getId(b).getPath();
			register(name, b instanceof DoorBlock ? new TallBlockItem(b, new Item.Settings()) : new BlockItem(b, new Item.Settings()));
		}
	}

	private static boolean isJarrable(ItemStack stack) {
		return stack.isOf(STRAWBERRY) || stack.isOf(BLUEBERRY) || stack.isOf(RASPBERRY) || stack.isOf(WHIPPED_CREAM) || stack.isOf(ROSE_PETALS)
				|| stack.isOf(TEA_LEAVES) || stack.isOf(MINT_LEAVES) || stack.isOf(BUTTER) || stack.isOf(LEMON) || stack.isOf(PINEAPPLE);
	}

	private static boolean isWrappable(ItemStack stack) {
		return stack.getItem() instanceof Perishable && !(stack.getItem() instanceof DrinkItem);
	}

	private static Item register(String name, Item item) {
		current.add(item);
		return Registry.register(Registries.ITEM, Bakery.id(name), item);
	}

	public static void init() {
		for (Item compostable : new Item[]{STRAWBERRY_SEEDS, BLUEBERRY_SEEDS, RASPBERRY_SEEDS, TEA_SEEDS, MINT_SEEDS, VANILLA_SEEDS, LAVENDER_SEEDS, ROSE_SEEDS, PINEAPPLE_SEEDS}) {
			CompostingChanceRegistry.INSTANCE.add(compostable, 0.3f);
		}
		for (Item compostable : new Item[]{STRAWBERRY, BLUEBERRY, RASPBERRY, TEA_LEAVES, MINT_LEAVES, LAVENDER, ROSE_PETALS, LEMON, PINEAPPLE, BURNT_PASTRY}) {
			CompostingChanceRegistry.INSTANCE.add(compostable, 0.65f);
		}
		for (Item compostable : new Item[]{BREAD_LOAF, BAGUETTE, CROISSANT, CUPCAKE, DONUT, COOKIES, MACARONS}) {
			CompostingChanceRegistry.INSTANCE.add(compostable, 0.85f);
		}
		for (WoodSet set : BakeryBlocks.WOODS) {
			CompostingChanceRegistry.INSTANCE.add(set.leaves, 0.3f);
			CompostingChanceRegistry.INSTANCE.add(set.sapling, 0.3f);
		}
		for (Item compostable : new Item[]{CORN_SEEDS, PEA_SEEDS, SWEET_POTATO_SEEDS, ZUCCHINI_SEEDS, RHUBARB_SEEDS, SUGAR_BEET_SEEDS,
				CRANBERRY_SEEDS, PEANUT_SEEDS, HIBISCUS_SEEDS, CANDYFLOSS_SEEDS}) {
			CompostingChanceRegistry.INSTANCE.add(compostable, 0.3f);
		}
		for (Item compostable : new Item[]{CHERRIES, ORANGE, PEACH, MANGO, PLUM, CORN, PEA_POD, SWEET_POTATO, ZUCCHINI, RHUBARB, SUGAR_BEET,
				CRANBERRY, HIBISCUS}) {
			CompostingChanceRegistry.INSTANCE.add(compostable, 0.65f);
		}
	}
}
