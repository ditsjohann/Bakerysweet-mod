package com.bakery.registry;

import com.bakery.Bakery;
import com.bakery.block.MixingBowlBlockEntity;
import com.bakery.block.PastryBoardBlockEntity;
import com.bakery.block.PlacedFoodBlockEntity;
import com.bakery.kitchen.ChurnBlockEntity;
import com.bakery.kitchen.JamPotBlockEntity;
import com.bakery.kitchen.JuicerBlockEntity;
import com.bakery.kitchen.ProofingBasketBlockEntity;
import com.bakery.oven.FountainBlockEntity;
import com.bakery.oven.GrandOvenBlockEntity;
import com.bakery.oven.MillstoneBlockEntity;
import com.bakery.shop.CounterBlockEntity;
import com.bakery.shop.DisplayCaseBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class BakeryBlockEntities {
	public static final BlockEntityType<GrandOvenBlockEntity> GRAND_OVEN = register("grand_oven",
			BlockEntityType.Builder.create(GrandOvenBlockEntity::new, BakeryBlocks.GRAND_OVEN).build(null));
	public static final BlockEntityType<MillstoneBlockEntity> MILLSTONE = register("grand_millstone",
			BlockEntityType.Builder.create(MillstoneBlockEntity::new, BakeryBlocks.GRAND_MILLSTONE).build(null));
	public static final BlockEntityType<FountainBlockEntity> FOUNTAIN = register("chocolate_fountain",
			BlockEntityType.Builder.create(FountainBlockEntity::new, BakeryBlocks.CHOCOLATE_FOUNTAIN).build(null));
	public static final BlockEntityType<JuicerBlockEntity> JUICER = register("juicer",
			BlockEntityType.Builder.create(JuicerBlockEntity::new, BakeryBlocks.JUICER).build(null));
	public static final BlockEntityType<ChurnBlockEntity> CHURN = register("ice_cream_churn",
			BlockEntityType.Builder.create(ChurnBlockEntity::new, BakeryBlocks.ICE_CREAM_CHURN).build(null));
	public static final BlockEntityType<JamPotBlockEntity> JAM_POT = register("jam_pot",
			BlockEntityType.Builder.create(JamPotBlockEntity::new, BakeryBlocks.JAM_POT).build(null));
	public static final BlockEntityType<ProofingBasketBlockEntity> PROOFING_BASKET = register("proofing_basket",
			BlockEntityType.Builder.create(ProofingBasketBlockEntity::new, BakeryBlocks.PROOFING_BASKET).build(null));
	public static final BlockEntityType<MixingBowlBlockEntity> MIXING_BOWL = register("mixing_bowl",
			BlockEntityType.Builder.create(MixingBowlBlockEntity::new, BakeryBlocks.MIXING_BOWL).build(null));
	public static final BlockEntityType<PastryBoardBlockEntity> PASTRY_BOARD = register("pastry_board",
			BlockEntityType.Builder.create(PastryBoardBlockEntity::new, BakeryBlocks.PASTRY_BOARD).build(null));
	public static final BlockEntityType<CounterBlockEntity> COUNTER = register("bakery_counter",
			BlockEntityType.Builder.create(CounterBlockEntity::new, BakeryBlocks.BAKERY_COUNTER).build(null));
	public static final BlockEntityType<com.bakery.shop.OrderNotebookBlockEntity> ORDER_NOTEBOOK = register("order_notebook",
			BlockEntityType.Builder.create(com.bakery.shop.OrderNotebookBlockEntity::new, BakeryBlocks.ORDER_NOTEBOOK).build(null));
	public static final BlockEntityType<DisplayCaseBlockEntity> DISPLAY_CASE = register("display_case",
			BlockEntityType.Builder.create(DisplayCaseBlockEntity::new, BakeryBlocks.DISPLAY_CASE).build(null));
	public static final BlockEntityType<com.bakery.sweet.TurntableBlockEntity> TURNTABLE = register("cake_turntable",
			BlockEntityType.Builder.create(com.bakery.sweet.TurntableBlockEntity::new, BakeryBlocks.CAKE_TURNTABLE).build(null));
	public static final BlockEntityType<PlacedFoodBlockEntity> PLACED_FOOD = register("placed_food",
			BlockEntityType.Builder.create(PlacedFoodBlockEntity::new, BakeryBlocks.BREAD_LOAF, BakeryBlocks.BAGUETTE,
					BakeryBlocks.CROISSANT, BakeryBlocks.CUPCAKE, BakeryBlocks.DONUT, BakeryBlocks.COOKIES, BakeryBlocks.MACARONS,
					BakeryBlocks.PIE, BakeryBlocks.CAKE, BakeryBlocks.MUG).build(null));

	private BakeryBlockEntities() {
	}

	private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
		return Registry.register(Registries.BLOCK_ENTITY_TYPE, Bakery.id(name), type);
	}

	public static void init() {
	}
}
