package com.bakery.test;

import com.bakery.baking.BakingRecipes;
import com.bakery.block.BakeryCropBlock;
import com.bakery.block.PlacedFoodBlock;
import com.bakery.block.SpecialCropBlock;
import com.bakery.block.TallCropBlock;
import com.bakery.food.BakeryFoods;
import com.bakery.food.Flavor;
import com.bakery.food.Freshness;
import com.bakery.food.Preservation;
import com.bakery.food.Topping;
import com.bakery.kitchen.JamPotBlockEntity;
import com.bakery.network.BakeryNetworking;
import com.bakery.network.ShapeDoughPayload;
import com.bakery.oven.GrandOvenBlockEntity;
import com.bakery.oven.MillstoneBlockEntity;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import com.bakery.shop.CounterBlock;
import com.bakery.shop.CounterBlockEntity;
import com.bakery.shop.Order;
import com.bakery.shop.ShopManager;
import com.bakery.sweet.CakeDesign;
import com.bakery.sweet.Catalog;
import com.bakery.sweet.Charm;
import com.bakery.sweet.Friendship;
import com.bakery.sweet.GiftBoxItem;
import com.bakery.sweet.Icing;
import com.bakery.sweet.Outfit;
import com.bakery.sweet.Sticker;
import com.bakery.sweet.Stickers;
import com.bakery.sweet.TurntableBlockEntity;
import com.bakery.sweet.WrappedGiftItem;
import net.minecraft.entity.EquipmentSlot;
import com.bakery.tree.HangingFruitBlock;
import com.bakery.tree.WoodSet;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.List;
import java.util.UUID;

/**
 * Plays through every Sweetheart Bakery feature on a real (headless) server with a fake player:
 * right-clicks, sneaking, held items and real ticking time. Run with ./gradlew runGametest.
 */
public class BakeryGameTests implements FabricGameTest {
	private static final String EMPTY = FabricGameTest.EMPTY_STRUCTURE;

	// ---------------------------------------------------------------- helpers

	private static FakePlayer player(TestContext ctx) {
		FakePlayer p = FakePlayer.get(ctx.getWorld(), new GameProfile(UUID.randomUUID(), "Baker"));
		BlockPos at = ctx.getAbsolutePos(new BlockPos(2, 1, 0));
		p.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.changeGameMode(GameMode.SURVIVAL);
		p.getInventory().clear();
		return p;
	}

	private static ActionResult use(TestContext ctx, FakePlayer p, BlockPos rel, ItemStack stack, boolean sneak) {
		// Whatever the last click put in our hand moves to the backpack first, so it is not overwritten.
		ItemStack previous = p.getMainHandStack();
		if (!previous.isEmpty()) {
			p.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
			for (int slot = 9; slot < 36 && !previous.isEmpty(); slot++) {
				p.getInventory().insertStack(slot, previous);
			}
		}
		p.setStackInHand(Hand.MAIN_HAND, stack);
		p.setSneaking(sneak);
		BlockPos abs = ctx.getAbsolutePos(rel);
		BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(abs).add(0, 0.5, 0), Direction.UP, abs, false);
		ActionResult result = p.interactionManager.interactBlock(p, ctx.getWorld(), p.getStackInHand(Hand.MAIN_HAND), Hand.MAIN_HAND, hit);
		p.setSneaking(false);
		return result;
	}

	private static ActionResult use(TestContext ctx, FakePlayer p, BlockPos rel, Item item) {
		return use(ctx, p, rel, new ItemStack(item), false);
	}

	private static ActionResult click(TestContext ctx, FakePlayer p, BlockPos rel) {
		return use(ctx, p, rel, ItemStack.EMPTY, false);
	}

	private static ItemStack find(FakePlayer p, Item item) {
		for (int i = 0; i < p.getInventory().size(); i++) {
			ItemStack s = p.getInventory().getStack(i);
			if (s.isOf(item)) return s;
		}
		return ItemStack.EMPTY;
	}

	private static int dropped(TestContext ctx, BlockPos rel, Item item) {
		List<ItemEntity> items = ctx.getWorld().getEntitiesByClass(ItemEntity.class, new Box(ctx.getAbsolutePos(rel)).expand(4), e -> e.getStack().isOf(item));
		return items.stream().mapToInt(e -> e.getStack().getCount()).sum();
	}

	private static void fill(TestContext ctx, int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
		for (BlockPos p : BlockPos.iterate(x1, y1, z1, x2, y2, z2)) ctx.setBlockState(p, state);
	}

	private static ItemStack flavored(Item item, Flavor flavor) {
		ItemStack s = new ItemStack(item);
		s.set(BakeryComponents.FLAVOR, flavor.asString());
		return s;
	}

	private static void check(TestContext ctx, boolean ok, String what) {
		ctx.assertTrue(ok, what);
	}

	// ---------------------------------------------------------------- kitchen basics

	@GameTest(templateName = EMPTY)
	public void mixingBowlMakesFlavoredDough(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos bowl = new BlockPos(1, 1, 1);
		ctx.setBlockState(bowl, BakeryBlocks.MIXING_BOWL.getDefaultState());
		for (Item i : new Item[]{BakeryItems.FLOUR, Items.EGG, Items.SUGAR, BakeryItems.BUTTER, BakeryItems.STRAWBERRY}) use(ctx, p, bowl, i);
		for (int i = 0; i < BakingRecipes.STIRS_NEEDED; i++) use(ctx, p, bowl, new ItemStack(BakeryItems.WHISK), false);
		ItemStack dough = find(p, BakeryItems.SWEET_DOUGH);
		check(ctx, !dough.isEmpty(), "whisking flour+egg+sugar+butter makes sweet dough");
		check(ctx, BakeryFoods.flavor(dough) == Flavor.STRAWBERRY, "the strawberry flavors the dough");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void homemadeDoughOnlyFromYourOwnIngredients(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos bowl = new BlockPos(1, 1, 1);
		ctx.setBlockState(bowl, BakeryBlocks.MIXING_BOWL.getDefaultState());
		for (Item i : new Item[]{BakeryItems.FLOUR, Items.EGG, Items.SUGAR, BakeryItems.BUTTER}) use(ctx, p, bowl, i);
		for (int i = 0; i < BakingRecipes.STIRS_NEEDED; i++) use(ctx, p, bowl, new ItemStack(BakeryItems.WHISK), false);
		ItemStack own = find(p, BakeryItems.SWEET_DOUGH);
		check(ctx, Boolean.TRUE.equals(own.get(com.bakery.registry.BakeryComponents.HOMEMADE)), "dough from your own ingredients is homemade");
		p.getInventory().clear();
		ItemStack bought = new ItemStack(BakeryItems.FLOUR);
		bought.set(com.bakery.registry.BakeryComponents.STORE_BOUGHT, true);
		use(ctx, p, bowl, bought, false);
		for (Item i : new Item[]{Items.EGG, Items.SUGAR, BakeryItems.BUTTER}) use(ctx, p, bowl, i);
		for (int i = 0; i < BakingRecipes.STIRS_NEEDED; i++) use(ctx, p, bowl, new ItemStack(BakeryItems.WHISK), false);
		ItemStack store = find(p, BakeryItems.SWEET_DOUGH);
		check(ctx, !store.isEmpty() && store.get(com.bakery.registry.BakeryComponents.HOMEMADE) == null, "store-bought flour makes it not homemade");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void catalogPantryHasDailyStock(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos at = new BlockPos(2, 1, 1);
		ctx.setBlockState(at, BakeryBlocks.BAKERY_CATALOG.getDefaultState());
		int index = -1;
		for (int i = 0; i < Catalog.entries().size(); i++) {
			if (Catalog.entries().get(i).item() == BakeryItems.FLOUR) index = i;
		}
		int stock = Catalog.entries().get(index).stock();
		check(ctx, stock > 0, "flour is stocked, not unlimited");
		p.getInventory().insertStack(new ItemStack(BakeryItems.SWEET_COIN, 64));
		for (int i = 0; i < stock; i++) check(ctx, Catalog.buy(p, ctx.getAbsolutePos(at), index), "flour sells while in stock");
		check(ctx, !Catalog.buy(p, ctx.getAbsolutePos(at), index), "then it is sold out for the day");
		ItemStack flour = find(p, BakeryItems.FLOUR);
		check(ctx, Boolean.TRUE.equals(flour.get(com.bakery.registry.BakeryComponents.STORE_BOUGHT)), "catalog flour is labelled store-bought");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void mixingBowlWrongRecipeGivesIngredientsBack(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos bowl = new BlockPos(1, 1, 1);
		ctx.setBlockState(bowl, BakeryBlocks.MIXING_BOWL.getDefaultState());
		use(ctx, p, bowl, BakeryItems.FLOUR);
		use(ctx, p, bowl, Items.EGG);
		for (int i = 0; i < BakingRecipes.STIRS_NEEDED; i++) use(ctx, p, bowl, new ItemStack(BakeryItems.WHISK), false);
		check(ctx, find(p, BakeryItems.SWEET_DOUGH).isEmpty() && find(p, BakeryItems.BREAD_DOUGH).isEmpty(), "a wrong recipe makes no dough");
		use(ctx, p, bowl, ItemStack.EMPTY, true);
		check(ctx, !find(p, BakeryItems.FLOUR).isEmpty() && !find(p, Items.EGG).isEmpty(), "sneak-click gives the ingredients back");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void pastryBoardShapesDoughWithProofingBonus(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos board = new BlockPos(1, 1, 1);
		ctx.setBlockState(board, BakeryBlocks.PASTRY_BOARD.getDefaultState());
		ItemStack dough = flavored(BakeryItems.SWEET_DOUGH, Flavor.MATCHA);
		dough.set(BakeryComponents.PROOFED, true);
		use(ctx, p, board, dough, false);
		p.setStackInHand(Hand.MAIN_HAND, new ItemStack(BakeryItems.ROLLING_PIN));
		BakeryNetworking.shapeDough(p, new ShapeDoughPayload(ctx.getAbsolutePos(board), BakingRecipes.Shape.CUPCAKE.ordinal(), 8));
		ItemStack raw = find(p, BakeryItems.RAW_CUPCAKE);
		check(ctx, !raw.isEmpty(), "rolling sweet dough as cupcakes gives cupcake batter");
		check(ctx, BakeryFoods.flavor(raw) == Flavor.MATCHA, "flavor carries over to the batter");
		check(ctx, raw.getOrDefault(BakeryComponents.QUALITY, 0) == 5, "8/10 rolling (4 stars) + proofing bonus = 5 stars, got " + raw.get(BakeryComponents.QUALITY));
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void placedCupcakesStackEatAndPickUp(TestContext ctx) {
		FakePlayer p = player(ctx);
		ctx.setBlockState(new BlockPos(1, 0, 1), Blocks.OAK_PLANKS.getDefaultState());
		ItemStack cupcake = flavored(BakeryItems.CUPCAKE, Flavor.ROSE);
		cupcake.setCount(3);
		use(ctx, p, new BlockPos(1, 0, 1), cupcake, false);
		BlockPos at = new BlockPos(1, 1, 1);
		check(ctx, ctx.getBlockState(at).isOf(BakeryBlocks.CUPCAKE), "a cupcake can be placed");
		check(ctx, ctx.getBlockState(at).get(PlacedFoodBlock.FLAVOR) == Flavor.ROSE, "the placed cupcake keeps its flavor");
		use(ctx, p, at, flavored(BakeryItems.CUPCAKE, Flavor.ROSE), false);
		check(ctx, ctx.getBlockState(at).get(PlacedFoodBlock.COUNT) == 2, "a second cupcake stacks on the first");
		p.getHungerManager().setFoodLevel(6);
		click(ctx, p, at);
		check(ctx, p.getHungerManager().getFoodLevel() > 6, "right-click eats one");
		check(ctx, ctx.getBlockState(at).get(PlacedFoodBlock.COUNT) == 1, "eating removes one cupcake");
		p.getInventory().clear();
		use(ctx, p, at, ItemStack.EMPTY, true);
		check(ctx, ctx.getBlockState(at).isAir(), "sneak-click picks it up");
		check(ctx, !find(p, BakeryItems.CUPCAKE).isEmpty(), "the picked-up cupcake is in the inventory");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void cakeDecoratesAndGetsEatenInSlices(TestContext ctx) {
		FakePlayer p = player(ctx);
		ctx.setBlockState(new BlockPos(1, 0, 1), Blocks.OAK_PLANKS.getDefaultState());
		ItemStack cake = flavored(BakeryItems.CAKE, Flavor.STRAWBERRY);
		cake.set(BakeryComponents.QUALITY, 3);
		use(ctx, p, new BlockPos(1, 0, 1), cake, false);
		BlockPos at = new BlockPos(1, 1, 1);
		check(ctx, ctx.getBlockState(at).isOf(BakeryBlocks.CAKE), "a cake can be placed");
		use(ctx, p, at, new ItemStack(BakeryItems.STRAWBERRY), false);
		check(ctx, ctx.getBlockState(at).get(PlacedFoodBlock.TOPPING) == Topping.STRAWBERRIES, "strawberries decorate the cake");
		for (int i = 0; i < 4; i++) {
			p.getHungerManager().setFoodLevel(2);
			click(ctx, p, at);
		}
		check(ctx, ctx.getBlockState(at).isAir(), "four slices finish the cake");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void waxPaperKeepsBreadFreshLonger(TestContext ctx) {
		FakePlayer p = player(ctx);
		ServerWorld world = ctx.getWorld();
		ItemStack bread = new ItemStack(BakeryItems.BREAD_LOAF);
		Freshness.stampFresh(bread, world.getTime());
		long before = Freshness.remaining(bread, world.getTime());
		p.setStackInHand(Hand.OFF_HAND, bread);
		p.setStackInHand(Hand.MAIN_HAND, new ItemStack(BakeryItems.WAX_PAPER));
		p.interactionManager.interactItem(p, world, p.getMainHandStack(), Hand.MAIN_HAND);
		ItemStack wrapped = p.getOffHandStack();
		check(ctx, Freshness.preservation(wrapped) == Preservation.WRAPPED, "wax paper wraps the bread");
		check(ctx, Freshness.remaining(wrapped, world.getTime()) >= before * 2 - 1, "wrapped bread lasts twice as long");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void spoiledFoodMakesYouSick(TestContext ctx) {
		FakePlayer p = player(ctx);
		ItemStack bread = new ItemStack(BakeryItems.BREAD_LOAF);
		bread.set(BakeryComponents.EXPIRES, ctx.getWorld().getTime() - 10);
		check(ctx, Freshness.stage(bread, ctx.getWorld().getTime()) == Freshness.Stage.SPOILED, "expired bread counts as spoiled");
		BakeryFoods.afterEating(bread, ctx.getWorld(), p);
		check(ctx, p.hasStatusEffect(StatusEffects.POISON), "eating moldy bread poisons you");
		ctx.complete();
	}

	// ---------------------------------------------------------------- the Grand Brick Oven

	private static void buildOven(TestContext ctx, boolean complete) {
		fill(ctx, 2, 0, 2, 4, 0, 4, Blocks.BRICKS.getDefaultState());
		fill(ctx, 2, 1, 2, 4, 1, 4, Blocks.BRICKS.getDefaultState());
		ctx.setBlockState(new BlockPos(3, 1, 3), Blocks.AIR.getDefaultState());
		ctx.setBlockState(new BlockPos(3, 1, 2), BakeryBlocks.OVEN_HEART.getDefaultState().with(com.bakery.oven.CoreBlock.FACING, Direction.NORTH));
		if (complete) {
			for (BlockPos t : new BlockPos[]{new BlockPos(3, 2, 3), new BlockPos(2, 2, 3), new BlockPos(4, 2, 3), new BlockPos(3, 2, 2), new BlockPos(3, 2, 4)}) {
				ctx.setBlockState(t, Blocks.BRICKS.getDefaultState());
			}
		}
	}

	@GameTest(templateName = EMPTY)
	public void ovenNeedsAllItsBricks(TestContext ctx) {
		FakePlayer p = player(ctx);
		buildOven(ctx, false);
		use(ctx, p, new BlockPos(3, 1, 2), BakeryItems.SPATULA);
		check(ctx, !ctx.getBlockState(new BlockPos(3, 1, 3)).isOf(BakeryBlocks.GRAND_OVEN), "an unfinished oven does not form");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY, tickLimit = 700)
	public void ovenFormsBakesAndComesApart(TestContext ctx) {
		FakePlayer p = player(ctx);
		buildOven(ctx, true);
		BlockPos heart = new BlockPos(3, 1, 2), chamber = new BlockPos(3, 1, 3), rack = new BlockPos(3, 1, 1);
		use(ctx, p, heart, BakeryItems.SPATULA);
		check(ctx, ctx.getBlockState(chamber).isOf(BakeryBlocks.GRAND_OVEN), "the spatula lights the Grand Brick Oven");
		use(ctx, p, heart, new ItemStack(Items.COAL, 4), false);
		GrandOvenBlockEntity oven = ctx.getBlockEntity(chamber);
		ItemStack batter = flavored(BakeryItems.RAW_CUPCAKE, Flavor.VANILLA);
		batter.set(BakeryComponents.QUALITY, 5);
		use(ctx, p, heart, batter.copy(), false);
		check(ctx, oven.getBakes().isEmpty(), "bakes can't go in through a shut door");
		click(ctx, p, heart);
		check(ctx, oven.isDoorOpen(), "an empty-hand click swings the door open");
		click(ctx, p, heart);
		check(ctx, oven.isRackOut() && ctx.getBlockState(rack).isOf(BakeryBlocks.OVEN_RACK), "a second click slides the rack out in front");
		use(ctx, p, rack, batter, false);
		check(ctx, oven.getBakes().size() == 1, "the batter goes on the rack");
		click(ctx, p, heart);
		check(ctx, !oven.isRackOut() && ctx.getBlockState(rack).isAir(), "clicking the oven slides the rack back in");
		use(ctx, p, heart, ItemStack.EMPTY, true);
		check(ctx, !oven.isDoorOpen(), "sneak-clicking shuts the door");
		check(ctx, oven.getFuel() > 0, "coal fuels the oven");
		ctx.waitAndRun(410, () -> {
			check(ctx, oven.getHeat() > 30, "the fire warms up, heat " + oven.getHeat());
			click(ctx, p, heart);
			click(ctx, p, heart);
			click(ctx, p, rack);
			ItemStack cupcakes = find(p, BakeryItems.CUPCAKE);
			check(ctx, cupcakes.getCount() == 4, "a perfect peel gives 4 cupcakes, got " + cupcakes.getCount());
			check(ctx, BakeryFoods.quality(cupcakes) >= 4, "well rolled + perfect timing + good heat = 4-5 stars, got " + BakeryFoods.quality(cupcakes));
			check(ctx, com.bakery.review.BakeryReputation.get(ctx.getWorld().getServer()).of(p.getUuid()).recipes.contains("cupcake:vanilla"),
					"the vanilla cupcake joins the recipe collection");
			ctx.getWorld().breakBlock(ctx.getAbsolutePos(new BlockPos(2, 0, 2)), true);
			check(ctx, ctx.getBlockState(chamber).isAir(), "breaking a brick takes the oven apart");
			check(ctx, ctx.getBlockState(rack).isAir(), "the rack goes with it");
			check(ctx, ctx.getBlockState(heart).isOf(BakeryBlocks.OVEN_HEART), "the heart comes back");
			check(ctx, ctx.getBlockState(new BlockPos(4, 1, 4)).isOf(Blocks.BRICKS), "the other bricks come back");
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY, tickLimit = 500)
	public void ovenBigBakesTakeRoomAndCookiesNeedFlipping(TestContext ctx) {
		FakePlayer p = player(ctx);
		buildOven(ctx, true);
		BlockPos heart = new BlockPos(3, 1, 2), chamber = new BlockPos(3, 1, 3), rack = new BlockPos(3, 1, 1);
		use(ctx, p, heart, BakeryItems.SPATULA);
		use(ctx, p, heart, new ItemStack(Items.COAL, 4), false);
		GrandOvenBlockEntity oven = ctx.getBlockEntity(chamber);
		click(ctx, p, heart);
		click(ctx, p, heart);
		for (int i = 0; i < 2; i++) use(ctx, p, rack, new ItemStack(BakeryItems.RAW_CAKE), false);
		check(ctx, oven.getBakes().size() == 1, "a cake takes a 2x2 block: only one fits the 3x3 rack, got " + oven.getBakes().size());
		use(ctx, p, rack, new ItemStack(BakeryItems.RAW_LOAF), false);
		check(ctx, oven.getBakes().size() == 2, "a loaf still fits down the free side");
		oven.getBakes().clear();
		ItemStack dough = flavored(BakeryItems.RAW_COOKIES, Flavor.VANILLA);
		dough.set(BakeryComponents.QUALITY, 5);
		use(ctx, p, rack, dough, false);
		click(ctx, p, heart);
		use(ctx, p, heart, ItemStack.EMPTY, true);
		ctx.waitAndRun(130, () -> {
			GrandOvenBlockEntity.Bake cookies = oven.getBakes().get(0);
			check(ctx, oven.wantsFlip(cookies), "halfway through, the cookies sizzle for a flip");
			click(ctx, p, heart);
			click(ctx, p, heart);
			click(ctx, p, rack);
			check(ctx, cookies.flipped, "clicking the sizzling cookies flips them");
			click(ctx, p, heart);
			use(ctx, p, heart, ItemStack.EMPTY, true);
			ctx.waitAndRun(185, () -> {
				click(ctx, p, heart);
				click(ctx, p, heart);
				click(ctx, p, rack);
				ItemStack baked = find(p, BakeryItems.COOKIES);
				check(ctx, baked.getCount() == 6, "a tray of cookies gives 6, got " + baked.getCount());
				check(ctx, BakeryFoods.quality(baked) >= 3, "flipped cookies bake evenly, got " + BakeryFoods.quality(baked));
				ctx.complete();
			});
		});
	}

	@GameTest(templateName = EMPTY, tickLimit = 200)
	public void ovenBellowsHeatAndOpenDoorCools(TestContext ctx) {
		FakePlayer p = player(ctx);
		buildOven(ctx, true);
		use(ctx, p, new BlockPos(3, 1, 2), BakeryItems.SPATULA);
		use(ctx, p, new BlockPos(3, 1, 2), new ItemStack(Items.COAL, 4), false);
		GrandOvenBlockEntity oven = ctx.getBlockEntity(new BlockPos(3, 1, 3));
		float before = oven.getHeat();
		use(ctx, p, new BlockPos(3, 1, 2), BakeryItems.BELLOWS);
		check(ctx, oven.getHeat() > before, "pumping the bellows raises the heat");
		check(ctx, find(p, BakeryItems.BELLOWS).getDamage() == 1 || p.getMainHandStack().getDamage() == 1, "the bellows wear a little");
		for (int i = 0; i < 16; i++) use(ctx, p, new BlockPos(3, 1, 2), BakeryItems.BELLOWS);
		click(ctx, p, new BlockPos(3, 1, 2));
		ctx.waitAndRun(40, () -> {
			check(ctx, oven.getHeat() < 60, "an open door lets the heat out, heat " + oven.getHeat());
			ctx.complete();
		});
	}

	// ---------------------------------------------------------------- buildable machines

	@GameTest(templateName = EMPTY)
	public void millstoneGrindsWheatIntoFlour(TestContext ctx) {
		FakePlayer p = player(ctx);
		fill(ctx, 2, 0, 2, 4, 0, 4, Blocks.STONE_BRICKS.getDefaultState());
		BlockPos core = new BlockPos(3, 1, 3);
		ctx.setBlockState(core, BakeryBlocks.MILLSTONE_CORE.getDefaultState().with(com.bakery.oven.CoreBlock.FACING, Direction.NORTH));
		for (BlockPos w : new BlockPos[]{new BlockPos(2, 1, 3), new BlockPos(4, 1, 3), new BlockPos(3, 1, 2), new BlockPos(3, 1, 4)}) {
			ctx.setBlockState(w, Blocks.COBBLESTONE_WALL.getDefaultState());
		}
		use(ctx, p, core, BakeryItems.SPATULA);
		check(ctx, ctx.getBlockState(core).isOf(BakeryBlocks.GRAND_MILLSTONE), "the spatula builds the Grand Millstone");
		use(ctx, p, core, new ItemStack(Items.WHEAT, 3), false);
		MillstoneBlockEntity mill = ctx.getBlockEntity(core);
		check(ctx, mill.getInput().getCount() == 3, "wheat loads into the millstone");
		for (int i = 0; i < 12; i++) click(ctx, p, core);
		check(ctx, dropped(ctx, core, BakeryItems.FLOUR) == 3, "twelve turns grind three wheat into flour, got " + dropped(ctx, core, BakeryItems.FLOUR));
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void chocolateFountainDipsFruit(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos fountain = new BlockPos(3, 1, 3);
		ctx.setBlockState(fountain.down(), Blocks.OAK_PLANKS.getDefaultState());
		check(ctx, use(ctx, p, fountain.down(), BakeryItems.CHOCOLATE_FOUNTAIN).isAccepted(), "the fountain is placed like any block");
		check(ctx, ctx.getBlockState(fountain).isOf(BakeryBlocks.CHOCOLATE_FOUNTAIN), "it stands on the table, no building needed");
		use(ctx, p, fountain, BakeryItems.CHOCOLATE_BAR);
		use(ctx, p, fountain, BakeryItems.STRAWBERRY);
		ItemStack treat = find(p, BakeryItems.CHOCOLATE_TREAT);
		check(ctx, treat.getCount() == 2, "dipping a strawberry gives two chocolate treats");
		check(ctx, BakeryFoods.flavor(treat) == Flavor.STRAWBERRY, "the treat is strawberry flavored");
		ctx.getWorld().breakBlock(ctx.getAbsolutePos(fountain), true);
		check(ctx, dropped(ctx, fountain, BakeryItems.CHOCOLATE_FOUNTAIN) == 1, "breaking it drops the fountain as an item");
		ctx.complete();
	}

	// ---------------------------------------------------------------- kitchen machines

	@GameTest(templateName = EMPTY)
	public void juicerSqueezesOrangeJuice(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos at = new BlockPos(1, 1, 1);
		ctx.setBlockState(at, BakeryBlocks.JUICER.getDefaultState());
		use(ctx, p, at, BakeryItems.ORANGE);
		for (int i = 0; i < 5; i++) click(ctx, p, at);
		use(ctx, p, at, Items.GLASS_BOTTLE);
		use(ctx, p, at, Items.GLASS_BOTTLE);
		check(ctx, p.getInventory().count(BakeryItems.JUICE) == 2, "one orange makes two glasses of juice");
		check(ctx, BakeryFoods.flavor(find(p, BakeryItems.JUICE)) == Flavor.ORANGE, "it is orange juice");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void kitchenMachinesIgnoreUnrelatedItems(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos at = new BlockPos(1, 1, 1);
		ctx.setBlockState(at, BakeryBlocks.JUICER.getDefaultState());
		use(ctx, p, at, BakeryItems.ORANGE);
		use(ctx, p, at, Items.IRON_SWORD);
		com.bakery.kitchen.JuicerBlockEntity juicer = ctx.getBlockEntity(at);
		check(ctx, juicer.presses() == 0, "clicking with a sword does not press the juicer");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void churnMakesIceCream(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos at = new BlockPos(1, 1, 1);
		ctx.setBlockState(at, BakeryBlocks.ICE_CREAM_CHURN.getDefaultState());
		for (Item i : new Item[]{BakeryItems.WHIPPED_CREAM, Items.SUGAR, Items.ICE, BakeryItems.MANGO}) use(ctx, p, at, i);
		for (int i = 0; i < 12; i++) click(ctx, p, at);
		ItemStack ice = find(p, BakeryItems.ICE_CREAM);
		check(ctx, ice.getCount() == 3, "twelve turns make three scoops");
		check(ctx, BakeryFoods.flavor(ice) == Flavor.MANGO, "mango ice cream");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY, tickLimit = 700)
	public void jamPotCooksWhenStirred(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos fire = new BlockPos(1, 1, 1), pot = new BlockPos(1, 2, 1);
		ctx.setBlockState(fire, Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		ctx.setBlockState(pot, BakeryBlocks.JAM_POT.getDefaultState());
		for (int i = 0; i < 3; i++) use(ctx, p, pot, BakeryItems.PEACH);
		use(ctx, p, pot, Items.SUGAR);
		JamPotBlockEntity jam = ctx.getBlockEntity(pot);
		check(ctx, jam.stage() == JamPotBlockEntity.COOKING, "three peaches + sugar over a fire starts cooking");
		for (int t = 150; t < 520; t += 150) ctx.runAtTick(t, () -> use(ctx, p, pot, BakeryItems.WHISK));
		ctx.runAtTick(560, () -> {
			check(ctx, jam.stage() == JamPotBlockEntity.DONE, "stirred jam finishes cooking, stage " + jam.stage());
			use(ctx, p, pot, BakeryItems.GLASS_JAR);
			ItemStack jar = find(p, BakeryItems.FRUIT_JAM);
			check(ctx, !jar.isEmpty() && BakeryFoods.flavor(jar) == Flavor.PEACH, "a jar of peach jam");
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY, tickLimit = 400)
	public void jamPotScorchesIfForgotten(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos pot = new BlockPos(1, 2, 1);
		ctx.setBlockState(new BlockPos(1, 1, 1), Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.LIT, true));
		ctx.setBlockState(pot, BakeryBlocks.JAM_POT.getDefaultState());
		for (int i = 0; i < 3; i++) use(ctx, p, pot, BakeryItems.STRAWBERRY);
		use(ctx, p, pot, Items.SUGAR);
		JamPotBlockEntity jam = ctx.getBlockEntity(pot);
		ctx.runAtTick(300, () -> {
			check(ctx, jam.stage() == JamPotBlockEntity.BURNT, "unstirred jam scorches");
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY, tickLimit = 700)
	public void proofingBasketRisesDough(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos at = new BlockPos(1, 1, 1);
		ctx.setBlockState(at, BakeryBlocks.PROOFING_BASKET.getDefaultState());
		use(ctx, p, at, BakeryItems.BREAD_DOUGH);
		ctx.runAtTick(620, () -> {
			click(ctx, p, at);
			ItemStack dough = find(p, BakeryItems.BREAD_DOUGH);
			check(ctx, Boolean.TRUE.equals(dough.get(BakeryComponents.PROOFED)), "risen dough comes out proofed");
			ctx.complete();
		});
	}

	// ---------------------------------------------------------------- restaurant

	@GameTest(templateName = EMPTY, tickLimit = 200)
	public void customerOrdersAndPaysWhenServed(TestContext ctx) {
		FakePlayer p = player(ctx);
		ctx.getWorld().setTimeOfDay(1000);
		fill(ctx, 0, 0, 0, 6, 0, 6, Blocks.STONE.getDefaultState());
		BlockPos counterPos = new BlockPos(3, 1, 2);
		ctx.setBlockState(counterPos, BakeryBlocks.BAKERY_COUNTER.getDefaultState().with(CounterBlock.FACING, Direction.SOUTH).with(CounterBlock.OPEN, true));
		ctx.setBlockState(new BlockPos(4, 1, 2), BakeryBlocks.SERVICE_BELL.getDefaultState());
		CounterBlockEntity counter = ctx.getBlockEntity(counterPos);
		counter.setOwner(p.getUuid());
		click(ctx, p, new BlockPos(4, 1, 2));
		check(ctx, counter.getOrders().isEmpty(), "without a walking path nobody can come in");
		// Lay the path: the entrance at the far corner, a turn, then tie it to the counter.
		ItemStack ribbon = new ItemStack(BakeryItems.PATH_RIBBON);
		use(ctx, p, new BlockPos(6, 0, 6), ribbon, false);
		ribbon = p.getMainHandStack().copy();
		use(ctx, p, new BlockPos(3, 0, 6), ribbon, false);
		ribbon = p.getMainHandStack().copy();
		check(ctx, com.bakery.shop.PathRibbonItem.points(ribbon).size() == 2, "the ribbon remembers the entrance and the turn");
		use(ctx, p, counterPos, ribbon, false);
		check(ctx, counter.hasPath() && counter.getPath().getLast().equals(ctx.getAbsolutePos(new BlockPos(3, 1, 3))),
				"tying the ribbon to the counter links the path, ending in front of it");
		click(ctx, p, new BlockPos(4, 1, 2));
		check(ctx, counter.getOrders().size() == 1, "ringing the bell calls a customer");
		Order order = counter.getOrders().getFirst();
		com.bakery.cafe.CustomerEntity customer = (com.bakery.cafe.CustomerEntity) ctx.getWorld().getEntity(order.customer);
		check(ctx, customer != null && customer.hasCustomName(), "a named customer walks in (not a villager)");
		check(ctx, customer.getBlockPos().getSquaredDistance(ctx.getAbsolutePos(new BlockPos(6, 1, 6))) <= 2 * 2 * 5 * 5,
				"they come in at the entrance, got " + customer.getBlockPos());
		check(ctx, customer.spot().equals(ctx.getAbsolutePos(new BlockPos(3, 1, 3))), "the first in line heads for the spot in front of the counter");
		check(ctx, com.bakery.shop.Menu.dish(order.wanted.getItem()).level() == com.bakery.cafe.CafeLevel.STALL,
				"a brand new bakery only gets asked for the simple things, got " + order.wanted.getName().getString());
		ItemStack wanted = order.wanted.copy();
		Item dish = wanted.getItem();
		wanted.set(BakeryComponents.QUALITY, 5);
		Freshness.stampFresh(wanted, ctx.getWorld().getTime());
		check(ctx, ShopManager.serve(p, customer, new ItemStack(Items.DIRT)), "a wrong item is handled (they say what they want)");
		check(ctx, counter.getOrders().size() == 1, "the wrong item does not complete the order");
		check(ctx, ShopManager.serve(p, customer, wanted), "the right item serves the customer");
		check(ctx, p.getInventory().count(BakeryItems.SWEET_COIN) > 0, "the customer pays in Sweet Coins");
		check(ctx, counter.getOrders().isEmpty() && counter.getServed() == 1, "the order is done");
		check(ctx, ShopManager.streak(p.getUuid()) == 1, "a 5-star serve starts a serving streak");
		check(ctx, Friendship.hearts(p, customer.regular().id()) == 1, "a 5-star serve earns a friendship heart");
		check(ctx, Stickers.has(p, Sticker.FIRST_CUSTOMER), "the first customer earns a sticker");
		check(ctx, customer.getMainHandStack().isOf(dish), "they walk off holding their treat");
		check(ctx, customer.state() == com.bakery.cafe.CustomerEntity.LEAVING || customer.state() == com.bakery.cafe.CustomerEntity.TO_SEAT,
				"then they head home or to a seat");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY, tickLimit = 200)
	public void customersQueueAlongThePath(TestContext ctx) {
		ServerWorld world = ctx.getWorld();
		fill(ctx, 0, 0, 0, 7, 0, 7, Blocks.STONE.getDefaultState());
		BlockPos counterPos = new BlockPos(3, 1, 1);
		ctx.setBlockState(counterPos, BakeryBlocks.BAKERY_COUNTER.getDefaultState().with(CounterBlock.FACING, Direction.SOUTH));
		// In at the far corner, along the bottom, then straight up to the counter.
		List<BlockPos> path = List.of(ctx.getAbsolutePos(new BlockPos(7, 1, 6)), ctx.getAbsolutePos(new BlockPos(3, 1, 6)), ctx.getAbsolutePos(new BlockPos(3, 1, 2)));
		check(ctx, com.bakery.shop.CustomerPath.blocks(path).size() == 9, "the path is walked block by block: " + com.bakery.shop.CustomerPath.blocks(path).size());
		check(ctx, com.bakery.shop.CustomerPath.queueSpot(path, 0).equals(ctx.getAbsolutePos(new BlockPos(3, 1, 2))), "first in line stands at the counter");
		check(ctx, com.bakery.shop.CustomerPath.queueSpot(path, 2).equals(ctx.getAbsolutePos(new BlockPos(3, 1, 4))), "the line forms back along the path");
		check(ctx, com.bakery.shop.CustomerPath.routeTo(path, 0).size() == 2, "the first customer walks through the entrance and the turn");
		com.bakery.cafe.CustomerEntity second = com.bakery.cafe.CustomerEntity.create(world, ctx.getAbsolutePos(counterPos), path, 1, com.bakery.cafe.Regular.get(1));
		check(ctx, second.spot().equals(ctx.getAbsolutePos(new BlockPos(3, 1, 3))), "second in line waits one step back");
		check(ctx, second.getBlockPos().getX() >= ctx.getAbsolutePos(new BlockPos(7, 1, 6)).getX(), "customers arrive from outside the entrance");
		second.queueAt(0, path);
		check(ctx, second.spot().equals(ctx.getAbsolutePos(new BlockPos(3, 1, 2))), "when the first is served, the next moves up to the counter");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY, tickLimit = 300)
	public void dinersSitDownToEat(TestContext ctx) {
		ServerWorld world = ctx.getWorld();
		fill(ctx, 0, 0, 0, 6, 0, 6, Blocks.STONE.getDefaultState());
		BlockPos counterPos = ctx.getAbsolutePos(new BlockPos(1, 1, 1));
		BlockPos chair = new BlockPos(4, 1, 4);
		ctx.setBlockState(chair, BakeryBlocks.CAFE_CHAIR.getDefaultState().with(com.bakery.deco.DecoBlock.FACING, Direction.NORTH));
		ctx.setBlockState(new BlockPos(4, 1, 3), BakeryBlocks.CAFE_TABLE.getDefaultState());
		com.bakery.cafe.CustomerEntity customer = com.bakery.cafe.CustomerEntity.create(world, counterPos, ctx.getAbsolutePos(new BlockPos(1, 1, 2)),
				com.bakery.cafe.Regular.get(0));
		BlockPos at = ctx.getAbsolutePos(new BlockPos(4, 1, 5));
		customer.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		world.spawnEntity(customer);
		customer.served(world, new Order(customer.getUuid(), 0, new ItemStack(BakeryItems.CUPCAKE), false, 1000, true), new ItemStack(BakeryItems.CUPCAKE), 2);
		check(ctx, customer.state() == com.bakery.cafe.CustomerEntity.TO_SEAT, "a dine-in customer looks for a free chair");
		ctx.runAtTick(120, () -> {
			check(ctx, customer.state() == com.bakery.cafe.CustomerEntity.EATING && customer.hasVehicle(), "they sit down and eat");
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY, tickLimit = 900)
	public void dinersLingerAndChatAtTheirTable(TestContext ctx) {
		ServerWorld world = ctx.getWorld();
		fill(ctx, 0, 0, 0, 6, 0, 6, Blocks.STONE.getDefaultState());
		BlockPos counterPos = ctx.getAbsolutePos(new BlockPos(1, 1, 1));
		// Two chairs facing each other across a table.
		ctx.setBlockState(new BlockPos(4, 1, 2), BakeryBlocks.CAFE_CHAIR.getDefaultState().with(com.bakery.deco.DecoBlock.FACING, Direction.SOUTH));
		ctx.setBlockState(new BlockPos(4, 1, 3), BakeryBlocks.CAFE_TABLE.getDefaultState());
		ctx.setBlockState(new BlockPos(4, 1, 4), BakeryBlocks.CAFE_CHAIR.getDefaultState().with(com.bakery.deco.DecoBlock.FACING, Direction.NORTH));
		com.bakery.cafe.CustomerEntity a = com.bakery.cafe.CustomerEntity.create(world, counterPos, ctx.getAbsolutePos(new BlockPos(1, 1, 2)), com.bakery.cafe.Regular.get(0));
		com.bakery.cafe.CustomerEntity b = com.bakery.cafe.CustomerEntity.create(world, counterPos, ctx.getAbsolutePos(new BlockPos(1, 1, 2)), com.bakery.cafe.Regular.get(1));
		world.spawnEntity(a);
		world.spawnEntity(b);
		a.demoSit(world, ctx.getAbsolutePos(new BlockPos(4, 1, 2)), new ItemStack(BakeryItems.CUPCAKE));
		b.demoSit(world, ctx.getAbsolutePos(new BlockPos(4, 1, 4)), new ItemStack(BakeryItems.HOT_CHOCOLATE));
		a.chatWith(b, 200);
		check(ctx, a.chatPartnerId() == b.getId() && b.chatPartnerId() == a.getId(), "diners at the same table chat with each other");
		check(ctx, a.isTalking() != b.isTalking(), "they take turns talking");
		ctx.runAtTick(800, () -> {
			check(ctx, a.isEating() && a.hasVehicle() && b.isEating(), "they stay at the table for a good while, not just a few seconds");
			check(ctx, a.getMainHandStack().isEmpty(), "the treat is long finished; they linger anyway");
			ctx.complete();
		});
	}

	@GameTest(templateName = EMPTY)
	public void smallBlocksAreNotOpaque(TestContext ctx) {
		// A block that is opaque but not a full cube blacks out the light inside it and renders dark.
		List<String> bad = new java.util.ArrayList<>();
		for (net.minecraft.block.Block block : net.minecraft.registry.Registries.BLOCK) {
			net.minecraft.util.Identifier id = net.minecraft.registry.Registries.BLOCK.getId(block);
			if (!id.getNamespace().equals("bakery")) continue;
			// Vanilla-style shapes light themselves by their shape, just like oak stairs or carpet.
			if (block instanceof net.minecraft.block.StairsBlock || block instanceof net.minecraft.block.SlabBlock || block instanceof net.minecraft.block.FenceBlock
					|| block instanceof net.minecraft.block.FenceGateBlock || block instanceof net.minecraft.block.CarpetBlock) continue;
			for (BlockState state : block.getStateManager().getStates()) {
				boolean full = net.minecraft.block.Block.isShapeFullCube(state.getOutlineShape(ctx.getWorld(), BlockPos.ORIGIN));
				if (state.isOpaque() && !full) {
					bad.add(id.getPath());
					break;
				}
			}
		}
		check(ctx, bad.isEmpty(), "opaque blocks that are not full cubes: " + bad);
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void menuGrowsWithTheBakery(TestContext ctx) {
		java.util.Set<String> none = java.util.Set.of();
		check(ctx, com.bakery.shop.Menu.available(com.bakery.cafe.CafeLevel.STALL, none).stream().noneMatch(d -> d.item() == BakeryItems.CAKE),
				"no cake orders on day one");
		check(ctx, com.bakery.shop.Menu.available(com.bakery.cafe.CafeLevel.GRAND, none).stream().anyMatch(d -> d.item() == BakeryItems.CAKE),
				"a grand bakery gets cake orders");
		check(ctx, com.bakery.cafe.CafeLevel.of(0) == com.bakery.cafe.CafeLevel.STALL && com.bakery.cafe.CafeLevel.of(70) == com.bakery.cafe.CafeLevel.GRAND,
				"levels follow customers served");
		java.util.Set<String> noBread = java.util.Set.of("bread_loaf", "baguette");
		check(ctx, com.bakery.shop.Menu.available(com.bakery.cafe.CafeLevel.STALL, noBread).stream().allMatch(d -> d.item() == BakeryItems.COOKIES),
				"taking dishes off the menu board leaves only the rest");
		ctx.complete();
	}

	// ---------------------------------------------------------------- sweet life

	@GameTest(templateName = EMPTY)
	public void cookieCutterAndIcing(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos board = new BlockPos(1, 1, 1);
		ctx.setBlockState(board, BakeryBlocks.PASTRY_BOARD.getDefaultState());
		use(ctx, p, board, BakeryItems.COOKIE_DOUGH);
		use(ctx, p, board, BakeryItems.STAR_CUTTER);
		ItemStack tray = find(p, BakeryItems.RAW_SUGAR_COOKIE);
		check(ctx, !tray.isEmpty() && "star".equals(tray.get(BakeryComponents.COOKIE_SHAPE)), "a cutter turns cookie dough into star cookies");
		check(ctx, BakingRecipes.shapeForRaw(tray) == BakingRecipes.Shape.SUGAR_COOKIE, "the oven knows how to bake them");

		ItemStack cookie = new ItemStack(BakeryItems.SUGAR_COOKIE, 2);
		cookie.set(BakeryComponents.COOKIE_SHAPE, "heart");
		cookie.set(BakeryComponents.QUALITY, 3);
		p.getInventory().clear();
		p.setStackInHand(Hand.OFF_HAND, cookie);
		p.setStackInHand(Hand.MAIN_HAND, new ItemStack(BakeryItems.PINK_ICING));
		p.getMainHandStack().use(ctx.getWorld(), p, Hand.MAIN_HAND);
		ItemStack iced = ItemStack.EMPTY;
		for (int i = 0; i < p.getInventory().size(); i++) {
			ItemStack s = p.getInventory().getStack(i);
			if (s.isOf(BakeryItems.SUGAR_COOKIE) && s.contains(BakeryComponents.ICING)) iced = s;
		}
		check(ctx, !iced.isEmpty() && iced.get(BakeryComponents.ICING) == Icing.PINK.color(), "the icing bag ices one cookie pink");
		check(ctx, BakeryFoods.quality(iced) == 4, "icing adds a star");
		check(ctx, p.getOffHandStack().getCount() == 1, "only one cookie is iced at a time");
		check(ctx, p.getMainHandStack().getDamage() == 1, "the icing bag wears a little");
		check(ctx, Stickers.has(p, Sticker.COOKIE_ARTIST), "icing earns the Cookie Artist sticker");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void turntableDecoratesACake(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos at = new BlockPos(1, 1, 1);
		ctx.setBlockState(at, BakeryBlocks.CAKE_TURNTABLE.getDefaultState());
		ItemStack cake = new ItemStack(BakeryItems.CAKE);
		cake.set(BakeryComponents.QUALITY, 3);
		use(ctx, p, at, cake, false);
		TurntableBlockEntity table = ctx.getBlockEntity(at);
		check(ctx, table.getCake().isOf(BakeryItems.CAKE), "a cake sits on the turntable");

		BlockPos abs = ctx.getAbsolutePos(at);
		// Where the top of a one-tier cake is, and a point on its side.
		Vec3d top = new Vec3d(abs.getX() + 0.5 + 2 / 16.0, abs.getY() + (TurntableBlockEntity.PLATE + 6) / 16.0, abs.getZ() + 0.5);
		Vec3d side = new Vec3d(abs.getX() + 0.5 + 6.5 / 16.0, abs.getY() + (TurntableBlockEntity.PLATE + 3) / 16.0, abs.getZ() + 0.5);

		ItemStack bag = new ItemStack(BakeryItems.PINK_ICING);
		check(ctx, table.use(p, Hand.MAIN_HAND, bag, top), "piping on the top of the cake works");
		check(ctx, table.design().dabs().size() == 1, "one dot of icing exactly where we clicked");
		check(ctx, bag.getDamage() == 1, "the icing bag wears a little");
		table.use(p, Hand.MAIN_HAND, bag, side);
		check(ctx, table.design().dabs().size() == 2, "piping on the side works too");
		bag.set(BakeryComponents.PIPING_TIP, com.bakery.sweet.IcingBagItem.Tip.BORDER.ordinal());
		table.use(p, Hand.MAIN_HAND, bag, top);
		check(ctx, table.design().dabs().size() > 12, "the border tip pipes a whole ring of rosettes");
		bag.set(BakeryComponents.PIPING_TIP, com.bakery.sweet.IcingBagItem.Tip.FROST.ordinal());
		table.use(p, Hand.MAIN_HAND, bag, top);
		check(ctx, table.design().tiers().getFirst() == Icing.PINK.color(), "the frost tip covers the tier in pink");

		ItemStack berries = new ItemStack(BakeryItems.STRAWBERRY, 2);
		table.use(p, Hand.MAIN_HAND, berries, top);
		check(ctx, table.design().toppers().size() == 1 && berries.getCount() == 1, "a strawberry stands where we clicked");
		p.setSneaking(true);
		table.use(p, Hand.MAIN_HAND, berries, top);
		p.setSneaking(false);
		check(ctx, table.design().toppers().isEmpty() && p.getInventory().count(BakeryItems.STRAWBERRY) == 1, "sneak-clicking lifts the topper back off");
		table.use(p, Hand.MAIN_HAND, new ItemStack(BakeryItems.FONDANT_BUNNY), top);
		check(ctx, table.design().toppers().getFirst().item() == BakeryItems.FONDANT_BUNNY, "fancy toppers too");

		table.use(p, Hand.MAIN_HAND, new ItemStack(BakeryItems.CAKE), top);
		check(ctx, table.design().tierCount() == 2, "another cake stacks on as a second tier");
		table.use(p, Hand.MAIN_HAND, new ItemStack(BakeryItems.HEART_CAKE_MOLD), top);
		check(ctx, table.design().shape() == com.bakery.sweet.CakeGeometry.HEART, "the heart mould makes a heart cake");

		click(ctx, p, at);
		check(ctx, table.getAngle() == 45, "an empty hand gives the turntable a spin");
		use(ctx, p, at, ItemStack.EMPTY, true);
		ItemStack done = find(p, BakeryItems.CAKE);
		check(ctx, table.getCake().isEmpty() && !done.isEmpty(), "sneak-clicking takes the cake back");
		check(ctx, done.get(BakeryComponents.CAKE_DESIGN) != null && done.get(BakeryComponents.CAKE_DESIGN).tierCount() == 2, "the cake keeps its design");
		check(ctx, BakeryFoods.quality(done) > 3, "a cute cake earns stars, got " + BakeryFoods.quality(done));
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void catalogSellsForCoins(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos at = new BlockPos(2, 1, 1);
		ctx.setBlockState(at, BakeryBlocks.BAKERY_CATALOG.getDefaultState());
		int index = -1;
		for (int i = 0; i < Catalog.entries().size(); i++) {
			if (Catalog.entries().get(i).item() == BakeryItems.CHEF_HAT) index = i;
		}
		long day = Catalog.day(ctx.getWorld());
		int price = Catalog.price(index, day);
		check(ctx, !Catalog.buy(p, ctx.getAbsolutePos(at), index), "no coins, no hat");
		p.getInventory().insertStack(new ItemStack(BakeryItems.SWEET_COIN, price + 3));
		check(ctx, Catalog.buy(p, ctx.getAbsolutePos(at), index), "with enough coins the hat is bought");
		check(ctx, p.getInventory().count(BakeryItems.CHEF_HAT) == 1, "the hat arrives");
		check(ctx, p.getInventory().count(BakeryItems.SWEET_COIN) == 3 + Sticker.SHOPPER.reward(), "the right number of coins is taken (plus the sticker reward)");
		check(ctx, Stickers.has(p, Sticker.SHOPPER), "first purchase earns a sticker");
		int featured = Catalog.featured(day);
		check(ctx, Catalog.price(featured, day) < Catalog.entries().get(featured).price() || Catalog.entries().get(featured).price() == 1,
				"the daily special is cheaper");
		int ears = -1;
		for (int i = 0; i < Catalog.entries().size(); i++) {
			if (Catalog.entries().get(i).item() == BakeryItems.BUNNY_EARS) ears = i;
		}
		p.getInventory().insertStack(new ItemStack(BakeryItems.SWEET_COIN, 64));
		check(ctx, !Catalog.buy(p, ctx.getAbsolutePos(at), ears), "fancier things stay locked until the bakery grows");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void giftsMakeFriends(TestContext ctx) {
		FakePlayer p = player(ctx);
		p.setStackInHand(Hand.OFF_HAND, new ItemStack(BakeryItems.STRAWBERRY, 3));
		p.setStackInHand(Hand.MAIN_HAND, new ItemStack(BakeryItems.GIFT_BOX));
		p.getMainHandStack().use(ctx.getWorld(), p, Hand.MAIN_HAND);
		ItemStack gift = find(p, BakeryItems.WRAPPED_GIFT);
		check(ctx, !gift.isEmpty() && WrappedGiftItem.contents(gift).getCount() == 3, "the gift box wraps the strawberries");
		check(ctx, p.getOffHandStack().isEmpty(), "the wrapped things leave your hand");

		com.bakery.cafe.CustomerEntity friend = ctx.spawnEntity(com.bakery.registry.BakeryEntities.CUSTOMER, new BlockPos(3, 1, 3));
		friend.setRegular(3);
		ItemStack second = gift.copy();
		check(ctx, Friendship.giveGift(p, friend, gift), "the regular accepts the gift");
		check(ctx, Friendship.hearts(p, friend.regular().id()) >= 2, "a gift is worth friendship hearts");
		check(ctx, p.getInventory().count(BakeryItems.SWEET_COIN) > 0, "they say thank you with coins");
		check(ctx, !Friendship.giveGift(p, friend, second), "one gift per regular per day");
		check(ctx, Stickers.has(p, Sticker.GIFT_GIVER), "giving earns a sticker");

		ItemStack toOpen = GiftBoxItem.wrap(new ItemStack(BakeryItems.CUPCAKE), "Test", GiftBoxItem.RIBBONS[0]);
		p.getInventory().clear();
		p.setStackInHand(Hand.MAIN_HAND, toOpen);
		p.getMainHandStack().use(ctx.getWorld(), p, Hand.MAIN_HAND);
		check(ctx, p.getInventory().count(BakeryItems.CUPCAKE) == 1, "unwrapping gives what was inside");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void charmComesFromCozyDecor(TestContext ctx) {
		BlockPos counterPos = new BlockPos(3, 1, 3);
		ctx.setBlockState(counterPos, BakeryBlocks.BAKERY_COUNTER.getDefaultState());
		CounterBlockEntity counter = ctx.getBlockEntity(counterPos);
		counter.updateCharm();
		check(ctx, counter.getCharm() == 0, "a bare counter has no charm");
		Block[] decor = {BakeryBlocks.PINK_HEART_BALLOON, BakeryBlocks.CUPCAKE_LAMP, BakeryBlocks.COOKIE_JAR, BakeryBlocks.MACARON_TOWER,
				BakeryBlocks.CAFE_TABLE, BakeryBlocks.LILAC_HEART_BALLOON};
		for (int i = 0; i < decor.length; i++) ctx.setBlockState(new BlockPos(1 + i, 1, 5), decor[i].getDefaultState());
		ctx.setBlockState(new BlockPos(1, 1, 1), BakeryBlocks.PINK_CHECKER_TILES.getDefaultState());
		counter.updateCharm();
		check(ctx, counter.getCozyCount() == 6, "six pieces of decor are counted, tiles are not (counted " + counter.getCozyCount() + ")");
		check(ctx, counter.getCharm() == Charm.level(6) && counter.getCharm() >= 2, "decor gives charm hearts");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void outfitAndStickers(TestContext ctx) {
		FakePlayer p = player(ctx);
		check(ctx, !Outfit.sweetBaker(p), "no outfit to start with");
		p.setStackInHand(Hand.MAIN_HAND, new ItemStack(BakeryItems.BUNNY_EARS));
		p.getMainHandStack().use(ctx.getWorld(), p, Hand.MAIN_HAND);
		check(ctx, p.getEquippedStack(EquipmentSlot.HEAD).isOf(BakeryItems.BUNNY_EARS), "right-clicking a hat puts it on");
		p.equipStack(EquipmentSlot.CHEST, new ItemStack(BakeryItems.FRILLY_APRON));
		check(ctx, Outfit.sweetBaker(p), "hat + apron is the Sweet Baker outfit");

		Stickers.award(p, Sticker.ORCHARD);
		int coins = p.getInventory().count(BakeryItems.SWEET_COIN);
		Stickers.award(p, Sticker.ORCHARD);
		check(ctx, coins == Sticker.ORCHARD.reward() && p.getInventory().count(BakeryItems.SWEET_COIN) == coins, "a sticker pays out only once");
		ctx.complete();
	}

	// ---------------------------------------------------------------- crops

	@GameTest(templateName = EMPTY)
	public void cornGrowsThreeTallAndRegrows(TestContext ctx) {
		FakePlayer p = player(ctx);
		BlockPos soil = new BlockPos(1, 0, 1), at = new BlockPos(1, 1, 1);
		ctx.setBlockState(soil, Blocks.FARMLAND.getDefaultState());
		use(ctx, p, soil, BakeryItems.CORN_SEEDS);
		check(ctx, ctx.getBlockState(at).isOf(BakeryBlocks.CORN_CROP), "corn kernels plant corn");
		TallCropBlock corn = (TallCropBlock) BakeryBlocks.CORN_CROP;
		for (int i = 0; i < 3; i++) corn.grow(ctx.getWorld(), ctx.getWorld().random, ctx.getAbsolutePos(at), ctx.getBlockState(at));
		check(ctx, ctx.getBlockState(at.up(2)).isOf(BakeryBlocks.CORN_CROP), "ripe corn is three blocks tall");
		click(ctx, p, at.up(2));
		check(ctx, dropped(ctx, at, BakeryItems.CORN) >= 3, "harvesting drops corn cobs");
		check(ctx, ctx.getBlockState(at.up()).isAir() && ctx.getBlockState(at).isOf(BakeryBlocks.CORN_CROP), "the stalk is cut back and grows again");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void cranberriesNeedWater(TestContext ctx) {
		BlockPos soil = new BlockPos(1, 0, 1), at = new BlockPos(1, 1, 1);
		ctx.setBlockState(soil, Blocks.FARMLAND.getDefaultState());
		ctx.setBlockState(at, BakeryBlocks.CRANBERRY_CROP.getDefaultState());
		SpecialCropBlock crop = (SpecialCropBlock) BakeryBlocks.CRANBERRY_CROP;
		check(ctx, !crop.conditionsMet(ctx.getWorld(), ctx.getAbsolutePos(at)), "dry cranberries do not grow");
		ctx.setBlockState(new BlockPos(2, 0, 1), Blocks.WATER.getDefaultState());
		check(ctx, crop.conditionsMet(ctx.getWorld(), ctx.getAbsolutePos(at)), "with water next to them they grow");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void peanutsArePulledAndRhubarbRegrows(TestContext ctx) {
		FakePlayer p = player(ctx);
		ctx.setBlockState(new BlockPos(1, 0, 1), Blocks.FARMLAND.getDefaultState());
		ctx.setBlockState(new BlockPos(3, 0, 1), Blocks.FARMLAND.getDefaultState());
		BlockPos peanut = new BlockPos(1, 1, 1), rhubarb = new BlockPos(3, 1, 1);
		ctx.setBlockState(peanut, BakeryBlocks.PEANUT_CROP.getDefaultState().with(BakeryCropBlock.AGE, 3));
		ctx.setBlockState(rhubarb, BakeryBlocks.RHUBARB_CROP.getDefaultState().with(BakeryCropBlock.AGE, 3));
		click(ctx, p, peanut);
		check(ctx, ctx.getBlockState(peanut).isAir() && dropped(ctx, peanut, BakeryItems.PEANUT) > 0, "ripe peanuts are pulled up");
		click(ctx, p, rhubarb);
		check(ctx, ctx.getBlockState(rhubarb).get(BakeryCropBlock.AGE) == 1 && dropped(ctx, rhubarb, BakeryItems.RHUBARB) > 0, "rhubarb is picked and regrows");
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void zucchiniStemGrowsAZucchini(TestContext ctx) {
		fill(ctx, 0, 0, 0, 3, 0, 3, Blocks.DIRT.getDefaultState());
		BlockPos stem = new BlockPos(1, 1, 1);
		ctx.setBlockState(new BlockPos(1, 0, 1), Blocks.FARMLAND.getDefaultState());
		ctx.setBlockState(stem, BakeryBlocks.ZUCCHINI_STEM.getDefaultState().with(StemBlock.AGE, 7));
		ServerWorld world = ctx.getWorld();
		for (int i = 0; i < 200 && !ctx.getBlockState(stem).isOf(BakeryBlocks.ATTACHED_ZUCCHINI_STEM); i++) {
			ctx.getBlockState(stem).randomTick(world, ctx.getAbsolutePos(stem), world.random);
		}
		check(ctx, ctx.getBlockState(stem).isOf(BakeryBlocks.ATTACHED_ZUCCHINI_STEM), "a ripe stem grows a zucchini next to it");
		ctx.complete();
	}

	// ---------------------------------------------------------------- trees

	/** Trees are bigger than the barrier-walled test box, so they grow in open air high above it (and get cleaned up). */
	private void growTree(TestContext ctx, WoodSet wood, Block ground) {
		ServerWorld world = ctx.getWorld();
		BlockPos at = ctx.getAbsolutePos(new BlockPos(3, 40, 3));
		world.setBlockState(at.down(), ground.getDefaultState());
		world.setBlockState(at, wood.sapling.getDefaultState());
		com.bakery.tree.FruitSaplingBlock sapling = (com.bakery.tree.FruitSaplingBlock) wood.sapling;
		for (int i = 0; i < 2; i++) sapling.grow(world, world.random, at, world.getBlockState(at));
		boolean grown = world.getBlockState(at).isOf(wood.log);
		int leaves = 0;
		for (BlockPos p : BlockPos.iterate(at.add(-13, -2, -13), at.add(13, 22, 13))) {
			if (world.getBlockState(p).isOf(wood.leaves)) leaves++;
		}
		for (BlockPos p : BlockPos.iterate(at.add(-13, -2, -13), at.add(13, 22, 13))) {
			world.setBlockState(p, Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS | net.minecraft.block.Block.FORCE_STATE);
		}
		check(ctx, grown, wood.name + " sapling grows a tree (blocked by " + com.bakery.tree.TreeShapes.lastBlocker + ")");
		check(ctx, leaves >= 8, wood.name + " tree has a canopy, leaves " + leaves);
		ctx.complete();
	}

	/** Grows one of every tree high in the sky and writes their blocks to tree_dump.json, for previewing them in Blockbench. */
	@GameTest(templateName = EMPTY)
	public void dumpTreeShapes(TestContext ctx) {
		ServerWorld world = ctx.getWorld();
		StringBuilder json = new StringBuilder("{");
		WoodSet[] woods = BakeryBlocks.WOODS;
		for (int n = 0; n < woods.length * 3; n++) {
			int t = n / 3, v = n % 3;
			WoodSet wood = woods[t];
			BlockPos at = ctx.getAbsolutePos(new BlockPos(3, 60, 3)).add(t * 30, 0, v * 30);
			world.setBlockState(at.down(), (wood == BakeryBlocks.PALM ? Blocks.SAND : Blocks.GRASS_BLOCK).getDefaultState());
			world.setBlockState(at, wood.sapling.getDefaultState());
			com.bakery.tree.FruitSaplingBlock sapling = (com.bakery.tree.FruitSaplingBlock) wood.sapling;
			for (int i = 0; i < 2; i++) sapling.grow(world, net.minecraft.util.math.random.Random.create(7 + n * 31L), at, world.getBlockState(at));
			json.append(n == 0 ? "" : ",").append('"').append(wood.name).append('_').append(v).append("\":[");
			boolean first = true;
			for (BlockPos p : BlockPos.iterate(at.add(-13, -1, -13), at.add(13, 22, 13))) {
				BlockState s = world.getBlockState(p);
				if (s.isAir()) continue;
				String id = net.minecraft.registry.Registries.BLOCK.getId(s.getBlock()).getPath();
				json.append(first ? "" : ",").append("[").append(p.getX() - at.getX()).append(",").append(p.getY() - at.getY()).append(",")
						.append(p.getZ() - at.getZ()).append(",\"").append(id).append("\"]");
				first = false;
				world.setBlockState(p, Blocks.AIR.getDefaultState(), net.minecraft.block.Block.NOTIFY_LISTENERS | net.minecraft.block.Block.FORCE_STATE);
			}
			json.append("]");
		}
		json.append("}");
		try {
			java.nio.file.Files.writeString(java.nio.file.Path.of("tree_dump.json"), json.toString());
		} catch (java.io.IOException e) {
			ctx.throwGameTestException("could not write tree dump: " + e);
		}
		ctx.complete();
	}

	@GameTest(templateName = EMPTY)
	public void palmGrowsOnSand(TestContext ctx) {
		growTree(ctx, BakeryBlocks.PALM, Blocks.SAND);
	}

	@GameTest(templateName = EMPTY)
	public void lemonTreeGrows(TestContext ctx) {
		growTree(ctx, BakeryBlocks.CITRUS, Blocks.GRASS_BLOCK);
	}

	@GameTest(templateName = EMPTY)
	public void cherryTreeGrows(TestContext ctx) {
		growTree(ctx, BakeryBlocks.BLOSSOM, Blocks.GRASS_BLOCK);
	}

	@GameTest(templateName = EMPTY)
	public void orangeTreeGrows(TestContext ctx) {
		growTree(ctx, BakeryBlocks.ORANGEWOOD, Blocks.GRASS_BLOCK);
	}

	@GameTest(templateName = EMPTY)
	public void peachTreeGrows(TestContext ctx) {
		growTree(ctx, BakeryBlocks.PEACHWOOD, Blocks.GRASS_BLOCK);
	}

	@GameTest(templateName = EMPTY)
	public void mangoTreeGrows(TestContext ctx) {
		growTree(ctx, BakeryBlocks.MANGOWOOD, Blocks.GRASS_BLOCK);
	}

	@GameTest(templateName = EMPTY)
	public void plumTreeGrows(TestContext ctx) {
		growTree(ctx, BakeryBlocks.PLUMWOOD, Blocks.GRASS_BLOCK);
	}

	@GameTest(templateName = EMPTY)
	public void ripeFruitIsPickedAndGrowsBack(TestContext ctx) {
		FakePlayer p = player(ctx);
		ctx.setBlockState(new BlockPos(1, 3, 1), BakeryBlocks.CITRUS.leaves.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT, true));
		BlockPos fruit = new BlockPos(1, 2, 1);
		ctx.setBlockState(fruit, BakeryBlocks.LEMON.getDefaultState().with(HangingFruitBlock.AGE, 2));
		click(ctx, p, fruit);
		check(ctx, dropped(ctx, fruit, BakeryItems.LEMON) > 0, "picking a ripe lemon drops lemons");
		check(ctx, ctx.getBlockState(fruit).get(HangingFruitBlock.AGE) == 0, "the lemon grows back from small");
		ctx.complete();
	}
}
