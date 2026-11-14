package com.bakery.registry;

import com.bakery.Bakery;
import com.bakery.block.BakeryCropBlock;
import com.bakery.block.DecoratedCakeBlock;
import com.bakery.block.MixingBowlBlock;
import com.bakery.block.MugBlock;
import com.bakery.block.PastryBoardBlock;
import com.bakery.block.SimpleFoodBlock;
import com.bakery.block.SliceFoodBlock;
import com.bakery.block.StackFoodBlock;
import com.bakery.deco.ChairBlock;
import com.bakery.deco.DecoBlock;
import com.bakery.deco.ServiceBellBlock;
import com.bakery.block.SpecialCropBlock;
import com.bakery.block.TallCropBlock;
import com.bakery.kitchen.ChurnBlockEntity;
import com.bakery.kitchen.JamPotBlockEntity;
import com.bakery.kitchen.JuicerBlockEntity;
import com.bakery.kitchen.KitchenBlock;
import com.bakery.kitchen.ProofingBasketBlockEntity;
import com.bakery.oven.CoreBlock;
import com.bakery.oven.FountainBlockEntity;
import com.bakery.oven.GrandOvenBlock;
import com.bakery.oven.MachineBlock;
import com.bakery.oven.MillstoneBlockEntity;
import com.bakery.oven.Multiblock;
import com.bakery.oven.MultiblockPartBlock;
import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import com.bakery.shop.CounterBlock;
import com.bakery.shop.DisplayCaseBlock;
import com.bakery.tree.HangingFruitBlock;
import com.bakery.tree.TreeShapes;
import com.bakery.tree.WoodSet;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.function.Supplier;

public final class BakeryBlocks {
	// The Grand Brick Oven
	// Buildable machines (built from blocks, then tapped with the Sweetheart Spatula)
	public static final Block OVEN_HEART = register("oven_heart", new CoreBlock(AbstractBlock.Settings.copy(Blocks.BRICKS).strength(2.0f, 6.0f), () -> Multiblock.OVEN));
	public static final Block MILLSTONE_CORE = register("millstone_core", new CoreBlock(AbstractBlock.Settings.copy(Blocks.STONE_BRICKS), () -> Multiblock.MILLSTONE));
	public static final Block GRAND_OVEN = register("grand_oven", new GrandOvenBlock(AbstractBlock.Settings.copy(Blocks.BRICKS)
			.strength(2.0f, 6.0f).nonOpaque().dropsNothing().luminance(state -> state.get(GrandOvenBlock.LIT) ? 14 : 0)));
	public static final Block GRAND_MILLSTONE = register("grand_millstone", new MachineBlock(AbstractBlock.Settings.copy(Blocks.STONE_BRICKS).nonOpaque(),
			() -> Multiblock.MILLSTONE, MillstoneBlockEntity::new, () -> BakeryBlockEntities.MILLSTONE));
	public static final Block CHOCOLATE_FOUNTAIN = register("chocolate_fountain", new com.bakery.oven.FountainBlock(AbstractBlock.Settings.copy(Blocks.WHITE_TERRACOTTA)
			.strength(1.2f).nonOpaque().luminance(state -> 4)));
	public static final Block OVEN_RACK = register("oven_rack", new com.bakery.oven.OvenRackBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS)
			.strength(0.5f).nonOpaque().dropsNothing().noCollision()));
	public static final Block MULTIBLOCK_PART = register("multiblock_part", new MultiblockPartBlock(AbstractBlock.Settings.copy(Blocks.BRICKS)
			.strength(2.0f, 6.0f).nonOpaque().suffocates((s, w, p) -> false).blockVision((s, w, p) -> false)));

	// Kitchen
	public static final Block MIXING_BOWL = register("mixing_bowl", new MixingBowlBlock(AbstractBlock.Settings.create()
			.mapColor(MapColor.WHITE).strength(0.8f).sounds(BlockSoundGroup.DECORATED_POT).nonOpaque()));
	public static final Block PASTRY_BOARD = register("pastry_board", new PastryBoardBlock(AbstractBlock.Settings.create()
			.mapColor(MapColor.OAK_TAN).strength(1.0f).sounds(BlockSoundGroup.WOOD).nonOpaque()));
	public static final Block JUICER = register("juicer", new KitchenBlock(AbstractBlock.Settings.copy(Blocks.WHITE_TERRACOTTA).strength(1f).nonOpaque(),
			box(3, 0, 3, 13, 14, 13), JuicerBlockEntity::new, () -> BakeryBlockEntities.JUICER));
	public static final Block ICE_CREAM_CHURN = register("ice_cream_churn", new KitchenBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(1f).nonOpaque(),
			box(2, 0, 2, 14, 15, 14), ChurnBlockEntity::new, () -> BakeryBlockEntities.CHURN));
	public static final Block JAM_POT = register("jam_pot", new KitchenBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(1.5f).nonOpaque()
			.sounds(BlockSoundGroup.LANTERN), box(2, 0, 2, 14, 10, 14), JamPotBlockEntity::new, () -> BakeryBlockEntities.JAM_POT));
	public static final Block PROOFING_BASKET = register("proofing_basket", new KitchenBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).strength(0.8f).nonOpaque()
			.sounds(BlockSoundGroup.WOOL), box(2, 0, 2, 14, 7, 14), ProofingBasketBlockEntity::new, () -> BakeryBlockEntities.PROOFING_BASKET));

	// Shop
	public static final Block BAKERY_COUNTER = register("bakery_counter", new CounterBlock(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()));
	public static final Block ORDER_NOTEBOOK = register("order_notebook", new com.bakery.shop.OrderNotebookBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
			.nonOpaque().noCollision().strength(0.3f), box(2, 1, 15, 14, 15, 16)));
	public static final Block DISPLAY_CASE = register("display_case", new DisplayCaseBlock(AbstractBlock.Settings.copy(Blocks.GLASS).strength(1f)
			.sounds(BlockSoundGroup.GLASS).nonOpaque()));
	public static final Block SERVICE_BELL = register("service_bell", new ServiceBellBlock(AbstractBlock.Settings.create().mapColor(MapColor.GOLD)
			.strength(0.5f).sounds(BlockSoundGroup.METAL).nonOpaque().pistonBehavior(PistonBehavior.DESTROY), box(5, 0, 5, 11, 5, 11)));

	// Decor
	public static final Block CAFE_TABLE = register("cafe_table", new com.bakery.deco.DyeableDecoBlock(wood(), box(0, 0, 0, 16, 15, 16), false));
	public static final Block CAFE_CHAIR = register("cafe_chair", new com.bakery.deco.DyeableChairBlock(wood(), VoxelShapes.union(box(2, 0, 2, 14, 9, 14), box(2, 9, 12, 14, 18, 14))));
	public static final Block FLOUR_SACK = register("flour_sack", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).nonOpaque(), box(2, 0, 3, 14, 13, 13), false));
	/** Display furniture customers buy from: spots are {x, y, z, size} for the north-facing model. */
	public static final Block TIERED_STAND = register("tiered_stand", new com.bakery.shop.DisplayStandBlock(
			AbstractBlock.Settings.copy(Blocks.WHITE_TERRACOTTA).nonOpaque().strength(0.6f), box(1, 0, 1, 15, 14, 15),
			new float[][]{{4.6f, 2.2f, 4.8f, 0.3f}, {11.4f, 2.2f, 4.8f, 0.3f}, {5.6f, 7.2f, 5.8f, 0.27f}, {10.4f, 7.2f, 5.8f, 0.27f}, {8f, 11.8f, 6.6f, 0.25f}}));
	public static final Block PASTRY_SHELF = register("pastry_shelf", new com.bakery.shop.DisplayStandBlock(wood(), box(0, 0, 3.5, 16, 16, 16),
			new float[][]{{3.6f, 1.2f, 8f, 0.3f}, {8f, 1.2f, 8f, 0.3f}, {12.4f, 1.2f, 8f, 0.3f}, {3.6f, 8.2f, 8f, 0.3f}, {8f, 8.2f, 8f, 0.3f}, {12.4f, 8.2f, 8f, 0.3f}}));
	public static final Block GLASS_CLOCHE = register("glass_cloche", new com.bakery.shop.DisplayStandBlock(
			AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque().strength(0.4f), box(1, 0, 1, 15, 12.5, 15),
			new float[][]{{8f, 1.2f, 8f, 0.5f}}));
	public static final Block BREAD_BASKET = register("bread_basket", new DecoBlock(wood(), box(2, 0, 3, 14, 9, 13), false));
	public static final Block CAKE_STAND = register("cake_stand", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_TERRACOTTA).nonOpaque(), box(2, 0, 2, 14, 15, 14), false));
	public static final Block MENU_BOARD = register("menu_board", new com.bakery.shop.MenuBoardBlock(wood(), box(1, 0, 13, 15, 16, 16)));
	public static final Block BUNTING = register("bunting", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).noCollision().nonOpaque(), box(0, 6, 14, 16, 16, 16), true));
	public static final Block JAR_SHELF = register("jar_shelf", new DecoBlock(wood(), box(0, 4, 10, 16, 14, 16), true));
	public static final Block STRAWBERRY_CRATE = register("strawberry_crate", new DecoBlock(wood(), box(0, 0, 0, 16, 12, 16), false));
	public static final Block LEMON_CRATE = register("lemon_crate", new DecoBlock(wood(), box(0, 0, 0, 16, 12, 16), false));
	public static final Block COCONUT_CRATE = register("coconut_crate", new DecoBlock(wood(), box(0, 0, 0, 16, 12, 16), false));
	public static final Block PINK_TILES = register("pink_tiles", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_GLAZED_TERRACOTTA).mapColor(MapColor.PINK)));
	public static final Block MINT_TILES = register("mint_tiles", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_GLAZED_TERRACOTTA).mapColor(MapColor.EMERALD_GREEN)));
	public static final Block LAVENDER_TILES = register("lavender_tiles", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_GLAZED_TERRACOTTA).mapColor(MapColor.PURPLE)));
	public static final Block CREAM_TILES = register("cream_tiles", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_GLAZED_TERRACOTTA).mapColor(MapColor.PALE_YELLOW)));

	// Sweet Life: the catalog, the cake studio and cozy decor (everything here counts towards bakery Charm)
	public static final Block BAKERY_CATALOG = register("bakery_catalog", new com.bakery.sweet.CatalogBlock(wood(),
			VoxelShapes.union(box(4, 0, 4, 12, 10, 12), box(1, 10, 2, 15, 15, 14))));
	public static final Block CAKE_TURNTABLE = register("cake_turntable", new com.bakery.sweet.TurntableBlock(AbstractBlock.Settings.copy(Blocks.WHITE_TERRACOTTA)
			.strength(1f).nonOpaque()));
	public static final Block PINK_CHECKER_TILES = register("pink_checker_tiles", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_GLAZED_TERRACOTTA).mapColor(MapColor.PINK)));
	public static final Block MINT_CHECKER_TILES = register("mint_checker_tiles", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_GLAZED_TERRACOTTA).mapColor(MapColor.EMERALD_GREEN)));
	public static final Block LILAC_CHECKER_TILES = register("lilac_checker_tiles", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_GLAZED_TERRACOTTA).mapColor(MapColor.PURPLE)));
	public static final Block BUTTER_CHECKER_TILES = register("butter_checker_tiles", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_GLAZED_TERRACOTTA).mapColor(MapColor.YELLOW)));
	public static final Block STRAWBERRY_WALLPAPER = register("strawberry_wallpaper", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).mapColor(MapColor.WHITE)));
	public static final Block GINGHAM_WALLPAPER = register("gingham_wallpaper", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).mapColor(MapColor.PINK)));
	public static final Block HEART_WALLPAPER = register("heart_wallpaper", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).mapColor(MapColor.PINK)));
	public static final Block POLKA_DOT_WALLPAPER = register("polka_dot_wallpaper", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).mapColor(MapColor.LIGHT_BLUE)));
	public static final Block HEART_RUG = register("heart_rug", new net.minecraft.block.CarpetBlock(AbstractBlock.Settings.copy(Blocks.PINK_CARPET)));
	public static final Block CLOUD_RUG = register("cloud_rug", new net.minecraft.block.CarpetBlock(AbstractBlock.Settings.copy(Blocks.WHITE_CARPET)));
	public static final Block PINK_HEART_BALLOON = register("pink_heart_balloon", balloon());
	public static final Block LILAC_HEART_BALLOON = register("lilac_heart_balloon", balloon());
	public static final Block MINT_HEART_BALLOON = register("mint_heart_balloon", balloon());
	public static final Block FAIRY_LIGHTS = register("fairy_lights", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).noCollision().nonOpaque()
			.strength(0.2f).luminance(state -> 12), box(0, 9, 14, 16, 16, 16), true));
	public static final Block CUPCAKE_LAMP = register("cupcake_lamp", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_TERRACOTTA).nonOpaque()
			.strength(0.5f).luminance(state -> 14), box(4, 0, 4, 12, 13, 12), false));
	public static final Block COOKIE_JAR = register("cookie_jar", new DecoBlock(AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque().strength(0.4f),
			box(4, 0, 4, 12, 11, 12), false));
	public static final Block MACARON_TOWER = register("macaron_tower", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_TERRACOTTA).nonOpaque()
			.strength(0.4f), box(3, 0, 3, 13, 15, 13), false));
	public static final Block PINK_AWNING = register("pink_awning", new com.bakery.deco.DyeableDecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).noCollision().nonOpaque(),
			box(0, 9, 6, 16, 16, 16), true));
	public static final Block MINT_AWNING = register("mint_awning", new com.bakery.deco.DyeableDecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).noCollision().nonOpaque(),
			box(0, 9, 6, 16, 16, 16), true));

	// Cozy update: indoor decor
	public static final Block PINK_COUCH = register("pink_couch", new com.bakery.deco.CouchBlock(fabric(), VoxelShapes.union(box(0, 0, 1.5, 16, 9, 15.5), box(0, 9, 11.5, 16, 16.5, 15.5))));
	public static final Block MACARON_POUF = register("macaron_pouf", new ChairBlock(fabric(), box(2, 0, 2, 14, 9, 14)));
	public static final Block HEART_MIRROR = register("heart_mirror", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_TERRACOTTA).nonOpaque().noCollision()
			.strength(0.5f), box(2, 1, 15, 14, 15, 16), true));
	public static final Block NEON_HEART_SIGN = register("neon_heart_sign", new DecoBlock(AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque().noCollision()
			.strength(0.3f).luminance(state -> 11), box(1, 2, 15, 15, 14, 16), true));
	public static final Block BUNNY_PLUSH = register("bunny_plush", new DecoBlock(fabric().strength(0.2f), box(4, 0, 5, 12, 13, 11), false));
	public static final Block BEAR_PLUSH = register("bear_plush", new DecoBlock(fabric().strength(0.2f), box(4, 0, 5, 12, 11, 11), false));
	public static final Block TULIP_VASE = register("tulip_vase", new DecoBlock(AbstractBlock.Settings.copy(Blocks.FLOWER_POT).nonOpaque(), box(5, 0, 5, 11, 15, 11), false));
	public static final Block TEA_SET = register("tea_set", new DecoBlock(AbstractBlock.Settings.copy(Blocks.FLOWER_POT).nonOpaque(), box(2, 0, 3, 14, 7, 13), false));
	public static final Block CAKE_SHELF = register("cake_shelf", new DecoBlock(wood(), box(0, 2, 10, 16, 16, 16), true));
	public static final Block DONUT_WALL = register("donut_wall", new DecoBlock(wood(), box(1, 1, 14, 15, 15, 16), true));
	public static final Block LACE_CURTAINS = register("lace_curtains", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).noCollision().nonOpaque(),
			box(0, 0, 14, 16, 16, 16), true));
	public static final Block STAR_LIGHTS = register("star_lights", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).noCollision().nonOpaque()
			.strength(0.2f).luminance(state -> 12), box(0, 9, 14, 16, 16, 16), true));
	public static final Block BOW_WALLPAPER = register("bow_wallpaper", new Block(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).mapColor(MapColor.PINK)));
	public static final Block CASH_REGISTER = register("cash_register", new DecoBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(1f).nonOpaque()
			.sounds(BlockSoundGroup.LANTERN), box(2, 0, 3, 14, 11, 14), false));
	public static final Block TIP_JAR = register("tip_jar", new DecoBlock(AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque().strength(0.3f), box(5, 0, 5, 11, 9, 11), false));
	public static final Block OPEN_SIGN = register("open_sign", new DecoBlock(wood().noCollision(), box(2, 2, 15, 14, 14, 16), true));
	// Cozy update: outdoor decor
	public static final Block CHALKBOARD_SIGN = register("chalkboard_sign", new DecoBlock(wood(), box(2, 0, 4, 14, 16, 12), false));
	public static final Block PASTEL_BENCH = register("pastel_bench", new com.bakery.deco.DyeableChairBlock(wood(), VoxelShapes.union(box(0, 0, 3, 16, 8, 13), box(0, 8, 11, 16, 16, 13))));
	public static final Block FLOWER_BOX = register("flower_box", new DecoBlock(wood(), box(0, 0, 9, 16, 9, 16), true));
	public static final Block PINK_PICKET_FENCE = register("pink_picket_fence", new net.minecraft.block.FenceBlock(AbstractBlock.Settings.copy(Blocks.OAK_FENCE).mapColor(MapColor.PINK)));
	public static final Block HEART_LAMP_POST = register("heart_lamp_post", new DecoBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(1f).nonOpaque()
			.sounds(BlockSoundGroup.LANTERN).luminance(state -> 15), box(5, 0, 5, 11, 16, 11), false));
	public static final Block PARASOL_TABLE = register("parasol_table", new com.bakery.deco.DyeableDecoBlock(wood(), box(0, 0, 0, 16, 15, 16), false));
	public static final Block TULIP_POT = register("tulip_pot", new DecoBlock(AbstractBlock.Settings.copy(Blocks.FLOWER_POT).nonOpaque(), box(3, 0, 3, 13, 13, 13), false));
	public static final Block HANGING_FLOWERS = register("hanging_flowers", new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).noCollision().nonOpaque()
			.strength(0.2f).sounds(BlockSoundGroup.GRASS), box(3, 2, 3, 13, 16, 13), false));
	public static final Block BUNNY_STATUE = register("bunny_statue", new DecoBlock(AbstractBlock.Settings.copy(Blocks.STONE).nonOpaque(), box(3, 0, 4, 13, 15, 12), false));
	public static final Block PINK_MAILBOX = register("pink_mailbox", new DecoBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(1f).nonOpaque()
			.sounds(BlockSoundGroup.LANTERN), box(5, 0, 4, 11, 16, 12), false));
	public static final Block PASTRY_CART = register("pastry_cart", new DecoBlock(wood(), box(0, 0, 2, 16, 16, 14), false));
	public static final Block WELCOME_MAT = register("welcome_mat", new DecoBlock(AbstractBlock.Settings.copy(Blocks.PINK_CARPET).nonOpaque(), box(0, 0, 1, 16, 1, 15), false));
	// Nature decorations
	public static final Block MUSHROOM_LAMP = register("mushroom_lamp", new DecoBlock(AbstractBlock.Settings.copy(Blocks.RED_MUSHROOM_BLOCK).strength(0.5f).nonOpaque()
			.luminance(state -> 12), box(2.5, 0, 2.5, 13.5, 12, 13.5), false));
	public static final Block POTTED_SUCCULENT = register("potted_succulent", new DecoBlock(AbstractBlock.Settings.copy(Blocks.FLOWER_POT).nonOpaque(), box(4, 0, 4, 12, 9, 12), false));
	public static final Block POTTED_FERN = register("potted_fern", new DecoBlock(AbstractBlock.Settings.copy(Blocks.FLOWER_POT).nonOpaque(), box(3, 0, 3, 13, 14, 13), false));
	public static final Block BIRD_BATH = register("bird_bath", new DecoBlock(AbstractBlock.Settings.copy(Blocks.STONE).strength(1f).nonOpaque(), box(1.5, 0, 1.5, 14.5, 11.5, 14.5), false));
	public static final Block WILDFLOWER_PATCH = register("wildflower_patch", new DecoBlock(AbstractBlock.Settings.copy(Blocks.PINK_PETALS).noCollision().nonOpaque()
			.strength(0.1f), box(0, 0, 0, 16, 3, 16), false));
	public static final Block HANGING_IVY = register("hanging_ivy", new DecoBlock(AbstractBlock.Settings.copy(Blocks.VINE).noCollision().nonOpaque()
			.strength(0.2f), box(0, 0, 15, 16, 16, 16), true));
	public static final Block STEPPING_STONES = register("stepping_stones", new DecoBlock(AbstractBlock.Settings.copy(Blocks.STONE).strength(1f).nonOpaque(), box(0, 0, 0, 16, 1.5, 16), false));
	public static final Block STUMP_STOOL = register("stump_stool", new ChairBlock(wood(), box(2.5, 0, 2.5, 13.5, 9, 13.5)));
	/** Every Cozy update decor piece (render layers, Charm, catalog). */
	public static final Block[] COZY_DECOR = {PINK_COUCH, MACARON_POUF, HEART_MIRROR, NEON_HEART_SIGN, BUNNY_PLUSH, BEAR_PLUSH, TULIP_VASE, TEA_SET,
			CAKE_SHELF, DONUT_WALL, LACE_CURTAINS, STAR_LIGHTS, CASH_REGISTER, TIP_JAR, OPEN_SIGN, CHALKBOARD_SIGN, PASTEL_BENCH, FLOWER_BOX,
			HEART_LAMP_POST, PARASOL_TABLE, TULIP_POT, HANGING_FLOWERS, BUNNY_STATUE, PINK_MAILBOX, PASTRY_CART, WELCOME_MAT,
			MUSHROOM_LAMP, POTTED_SUCCULENT, POTTED_FERN, BIRD_BATH, WILDFLOWER_PATCH, HANGING_IVY, STEPPING_STONES, STUMP_STOOL};

	// Placed food
	public static final Block BREAD_LOAF = register("bread_loaf", new SimpleFoodBlock(food(), box(2, 0, 3, 14, 7, 13)));
	public static final Block BAGUETTE = register("baguette", new SimpleFoodBlock(food(), box(1, 0, 3, 15, 4, 13)));
	public static final Block CROISSANT = register("croissant", new SimpleFoodBlock(food(), box(2, 0, 3, 14, 4, 13)));
	public static final Block CUPCAKE = register("cupcake", new StackFoodBlock(food(), box(2, 0, 2, 14, 8, 14), 4));
	public static final Block DONUT = register("donut", new StackFoodBlock(food(), box(2, 0, 2, 14, 10, 14), 3));
	public static final Block COOKIES = register("cookies", new StackFoodBlock(food(), box(1, 0, 1, 15, 3, 15), 1));
	public static final Block MACARONS = register("macarons", new StackFoodBlock(food(), box(3, 0, 3, 13, 11, 13), 1));
	public static final Block PIE = register("pie", new SliceFoodBlock(food(), box(1, 0, 1, 15, 5, 15),
			new FoodComponent.Builder().nutrition(4).saturationModifier(0.5f).build()));
	public static final Block CAKE = register("cake", new DecoratedCakeBlock(food(), box(1, 0, 1, 15, 10, 15),
			new FoodComponent.Builder().nutrition(3).saturationModifier(0.4f).build()));
	public static final Block MUG = register("mug", new MugBlock(AbstractBlock.Settings.create().mapColor(MapColor.WHITE)
			.strength(0.3f).sounds(BlockSoundGroup.DECORATED_POT).nonOpaque().pistonBehavior(PistonBehavior.DESTROY),
			box(3, 0, 3, 13, 7, 13)));

	// Crops
	public static final Block STRAWBERRY_CROP = crop("strawberry_crop", () -> BakeryItems.STRAWBERRY_SEEDS, 0);
	public static final Block BLUEBERRY_CROP = crop("blueberry_crop", () -> BakeryItems.BLUEBERRY_SEEDS, 0);
	public static final Block TEA_CROP = crop("tea_crop", () -> BakeryItems.TEA_SEEDS, 0);
	public static final Block VANILLA_CROP = crop("vanilla_crop", () -> BakeryItems.VANILLA_SEEDS, 0);
	public static final Block LAVENDER_CROP = crop("lavender_crop", () -> BakeryItems.LAVENDER_SEEDS, 0);
	public static final Block ROSE_CROP = crop("rose_crop", () -> BakeryItems.ROSE_SEEDS, 0);
	public static final Block STARBLOOM_CROP = crop("starbloom_crop", () -> BakeryItems.STARBLOOM_SEEDS, 11);
	public static final Block RASPBERRY_CROP = crop("raspberry_crop", () -> BakeryItems.RASPBERRY_SEEDS, 0);
	public static final Block MINT_CROP = crop("mint_crop", () -> BakeryItems.MINT_SEEDS, 0);
	public static final Block PINEAPPLE_CROP = crop("pineapple_crop", () -> BakeryItems.PINEAPPLE_SEEDS, 0);

	// Vegetables and special crops: each grows its own way
	public static final Block CORN_CROP = register("corn_crop", new TallCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT), 3, () -> BakeryItems.CORN));
	public static final Block PEA_CROP = register("pea_crop", new TallCropBlock(AbstractBlock.Settings.copy(Blocks.WHEAT), 2, () -> BakeryItems.PEA_POD));
	public static final Block SWEET_POTATO_CROP = special("sweet_potato_crop", () -> BakeryItems.SWEET_POTATO_SEEDS, () -> BakeryItems.SWEET_POTATO, SpecialCropBlock.Style.PULL, 0);
	public static final Block PEANUT_CROP = special("peanut_crop", () -> BakeryItems.PEANUT_SEEDS, () -> BakeryItems.PEANUT, SpecialCropBlock.Style.PULL, 0);
	public static final Block RHUBARB_CROP = special("rhubarb_crop", () -> BakeryItems.RHUBARB_SEEDS, () -> BakeryItems.RHUBARB, SpecialCropBlock.Style.REGROW, 0);
	public static final Block HIBISCUS_CROP = special("hibiscus_crop", () -> BakeryItems.HIBISCUS_SEEDS, () -> BakeryItems.HIBISCUS, SpecialCropBlock.Style.REGROW, 0);
	public static final Block CRANBERRY_CROP = special("cranberry_crop", () -> BakeryItems.CRANBERRY_SEEDS, () -> BakeryItems.CRANBERRY, SpecialCropBlock.Style.BOG, 0);
	public static final Block CANDYFLOSS_CROP = special("candyfloss_crop", () -> BakeryItems.CANDYFLOSS_SEEDS, () -> BakeryItems.CANDYFLOSS, SpecialCropBlock.Style.DREAM, 9);
	public static final Block SUGAR_BEET_CROP = crop("sugar_beet_crop", () -> BakeryItems.SUGAR_BEET_SEEDS, 0);
	public static final Block ZUCCHINI = register("zucchini_block", new DecoBlock(AbstractBlock.Settings.copy(Blocks.MELON).strength(0.5f).nonOpaque()
			.pistonBehavior(PistonBehavior.DESTROY), box(3, 0, 3, 13, 6, 13), false));
	public static final Block ZUCCHINI_STEM = register("zucchini_stem", new StemBlock(key("zucchini_block"), key("attached_zucchini_stem"),
			RegistryKey.of(RegistryKeys.ITEM, Bakery.id("zucchini_seeds")), AbstractBlock.Settings.copy(Blocks.MELON_STEM)));
	public static final Block ATTACHED_ZUCCHINI_STEM = register("attached_zucchini_stem", new AttachedStemBlock(key("zucchini_stem"), key("zucchini_block"),
			RegistryKey.of(RegistryKeys.ITEM, Bakery.id("zucchini_seeds")), AbstractBlock.Settings.copy(Blocks.ATTACHED_MELON_STEM)));

	public static final Block[] CROPS = {STRAWBERRY_CROP, BLUEBERRY_CROP, TEA_CROP, VANILLA_CROP, LAVENDER_CROP, ROSE_CROP, STARBLOOM_CROP,
			RASPBERRY_CROP, MINT_CROP, PINEAPPLE_CROP, CORN_CROP, PEA_CROP, SWEET_POTATO_CROP, PEANUT_CROP, RHUBARB_CROP, HIBISCUS_CROP,
			CRANBERRY_CROP, CANDYFLOSS_CROP, SUGAR_BEET_CROP, ZUCCHINI_STEM, ATTACHED_ZUCCHINI_STEM, ZUCCHINI};

	// Fruit trees: each with its own shape, wood and fruit
	public static final Block COCONUT = fruitBlock("coconut", () -> BakeryItems.COCONUT);
	public static final Block LEMON = fruitBlock("lemon", () -> BakeryItems.LEMON);
	public static final Block CHERRIES = fruitBlock("cherries", () -> BakeryItems.CHERRIES);
	public static final Block ORANGE = fruitBlock("orange", () -> BakeryItems.ORANGE);
	public static final Block PEACH = fruitBlock("peach", () -> BakeryItems.PEACH);
	public static final Block MANGO = fruitBlock("mango", () -> BakeryItems.MANGO);
	public static final Block PLUM = fruitBlock("plum", () -> BakeryItems.PLUM);
	public static final WoodSet PALM = new WoodSet("palm", MapColor.TERRACOTTA_WHITE, MapColor.BROWN, TreeShapes.Kind.PALM, () -> BakeryBlocks.PALM, () -> COCONUT);
	public static final WoodSet CITRUS = new WoodSet("citrus", MapColor.PALE_YELLOW, MapColor.TERRACOTTA_BROWN, TreeShapes.Kind.LEMON, () -> BakeryBlocks.CITRUS, () -> LEMON);
	public static final WoodSet BLOSSOM = new WoodSet("blossom", MapColor.PINK, MapColor.TERRACOTTA_BLACK, TreeShapes.Kind.CHERRY, () -> BakeryBlocks.BLOSSOM, () -> CHERRIES);
	public static final WoodSet ORANGEWOOD = new WoodSet("orangewood", MapColor.ORANGE, MapColor.TERRACOTTA_GRAY, TreeShapes.Kind.ORANGE, () -> BakeryBlocks.ORANGEWOOD, () -> ORANGE);
	public static final WoodSet PEACHWOOD = new WoodSet("peachwood", MapColor.TERRACOTTA_PINK, MapColor.TERRACOTTA_BROWN, TreeShapes.Kind.PEACH, () -> BakeryBlocks.PEACHWOOD, () -> PEACH);
	public static final WoodSet MANGOWOOD = new WoodSet("mangowood", MapColor.TERRACOTTA_YELLOW, MapColor.DARK_GREEN, TreeShapes.Kind.MANGO, () -> BakeryBlocks.MANGOWOOD, () -> MANGO);
	public static final WoodSet PLUMWOOD = new WoodSet("plumwood", MapColor.PURPLE, MapColor.TERRACOTTA_PURPLE, TreeShapes.Kind.PLUM, () -> BakeryBlocks.PLUMWOOD, () -> PLUM);
	public static final WoodSet[] WOODS = {PALM, CITRUS, BLOSSOM, ORANGEWOOD, PEACHWOOD, MANGOWOOD, PLUMWOOD};
	public static final Block[] FRUIT_BLOCKS = {COCONUT, LEMON, CHERRIES, ORANGE, PEACH, MANGO, PLUM};

	private BakeryBlocks() {
	}

	private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
		return Block.createCuboidShape(x1, y1, z1, x2, y2, z2);
	}

	private static AbstractBlock.Settings food() {
		return AbstractBlock.Settings.create().mapColor(MapColor.YELLOW).strength(0.4f).sounds(BlockSoundGroup.WOOL)
				.nonOpaque().pistonBehavior(PistonBehavior.DESTROY);
	}

	private static AbstractBlock.Settings wood() {
		return AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque();
	}

	private static AbstractBlock.Settings fabric() {
		return AbstractBlock.Settings.copy(Blocks.PINK_WOOL).nonOpaque();
	}

	/** A heart-shaped balloon on a string: floats wherever you put it. */
	private static Block balloon() {
		return new DecoBlock(AbstractBlock.Settings.copy(Blocks.WHITE_WOOL).noCollision().nonOpaque().strength(0.1f)
				.sounds(BlockSoundGroup.WOOL).pistonBehavior(PistonBehavior.DESTROY), box(3, 0, 3, 13, 16, 13), false);
	}

	private static AbstractBlock.Settings fruit() {
		return AbstractBlock.Settings.create().mapColor(MapColor.YELLOW).strength(0.3f).sounds(BlockSoundGroup.WOOD)
				.noCollision().nonOpaque().ticksRandomly().pistonBehavior(PistonBehavior.DESTROY);
	}

	private static Block crop(String name, Supplier<ItemConvertible> seeds, int glow) {
		AbstractBlock.Settings settings = AbstractBlock.Settings.copy(Blocks.WHEAT);
		if (glow > 0) {
			settings.luminance(state -> state.get(BakeryCropBlock.AGE) == 3 ? glow : 2);
		}
		return register(name, new BakeryCropBlock(settings, seeds));
	}

	private static Block special(String name, Supplier<ItemConvertible> seeds, Supplier<net.minecraft.item.Item> produce, SpecialCropBlock.Style style, int glow) {
		AbstractBlock.Settings settings = AbstractBlock.Settings.copy(Blocks.WHEAT);
		if (glow > 0) {
			settings.luminance(state -> state.get(BakeryCropBlock.AGE) == 3 ? glow : 1);
		}
		return register(name, new SpecialCropBlock(settings, seeds, produce, style));
	}

	private static Block fruitBlock(String name, Supplier<net.minecraft.item.Item> item) {
		return register(name, new HangingFruitBlock(fruit(), item));
	}

	private static RegistryKey<Block> key(String name) {
		return RegistryKey.of(RegistryKeys.BLOCK, Bakery.id(name));
	}

	private static Block register(String name, Block block) {
		return Registry.register(Registries.BLOCK, Bakery.id(name), block);
	}

	public static void init() {
	}
}
