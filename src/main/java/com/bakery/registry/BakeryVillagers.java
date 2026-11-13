package com.bakery.registry;

import com.bakery.Bakery;
import com.google.common.collect.ImmutableSet;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.List;

/** The Baker: a villager who works at the oven and trades seeds, tools and pastries. */
public final class BakeryVillagers {
	public static final RegistryKey<PointOfInterestType> BAKER_POI_KEY = RegistryKey.of(RegistryKeys.POINT_OF_INTEREST_TYPE, Bakery.id("baker"));
	public static final PointOfInterestType BAKER_POI = PointOfInterestHelper.register(Bakery.id("baker"), 1, 1, BakeryBlocks.BAKERY_COUNTER);
	public static final VillagerProfession BAKER = Registry.register(Registries.VILLAGER_PROFESSION, Bakery.id("baker"),
			new VillagerProfession("baker", entry -> entry.matchesKey(BAKER_POI_KEY), entry -> entry.matchesKey(BAKER_POI_KEY),
					ImmutableSet.of(Items.WHEAT, BakeryItems.STRAWBERRY), ImmutableSet.of(), SoundEvents.ENTITY_VILLAGER_WORK_BUTCHER));

	private BakeryVillagers() {
	}

	public static void init() {
		TradeOfferHelper.registerVillagerOffers(BAKER, 1, offers -> {
			buy(offers, Items.WHEAT, 20, 2);
			buy(offers, Items.SUGAR_CANE, 18, 2);
			sell(offers, BakeryItems.FLOUR, 4, 1, 1);
			sell(offers, BakeryItems.STRAWBERRY_SEEDS, 2, 1, 1);
		});
		TradeOfferHelper.registerVillagerOffers(BAKER, 2, offers -> {
			buy(offers, Items.EGG, 12, 10);
			sell(offers, BakeryItems.WHISK, 1, 3, 5);
			sell(offers, BakeryItems.ROLLING_PIN, 1, 3, 5);
			sell(offers, BakeryItems.BLUEBERRY_SEEDS, 2, 1, 5);
		});
		TradeOfferHelper.registerVillagerOffers(BAKER, 3, offers -> {
			buy(offers, BakeryItems.STRAWBERRY, 10, 20);
			sell(offers, BakeryItems.TEA_SEEDS, 2, 2, 10);
			sell(offers, BakeryItems.VANILLA_SEEDS, 2, 2, 10);
			sell(offers, BakeryItems.WAX_PAPER, 4, 1, 10);
		});
		TradeOfferHelper.registerVillagerOffers(BAKER, 4, offers -> {
			sell(offers, BakeryItems.LAVENDER_SEEDS, 2, 3, 15);
			sell(offers, BakeryItems.ROSE_SEEDS, 2, 3, 15);
			sell(offers, BakeryItems.MATCHA_LATTE, 1, 4, 15);
			sell(offers, BakeryBlocks.PALM.sapling.asItem(), 1, 6, 15);
			sell(offers, BakeryBlocks.CITRUS.sapling.asItem(), 1, 6, 15);
		});
		TradeOfferHelper.registerVillagerOffers(BAKER, 5, offers -> {
			sell(offers, BakeryItems.STARBLOOM_SEEDS, 1, 12, 30);
			sell(offers, BakeryItems.STAR_PETALS, 1, 8, 30);
			sell(offers, BakeryItems.OVEN_HEART, 1, 10, 30);
		});
		TradeOfferHelper.registerVillagerOffers(BAKER, 3, offers -> {
			sell(offers, BakeryItems.CORN_SEEDS, 3, 1, 10);
			sell(offers, BakeryItems.SUGAR_BEET_SEEDS, 3, 1, 10);
			sell(offers, BakeryItems.MILLSTONE_CORE, 1, 6, 10);
		});
		TradeOfferHelper.registerVillagerOffers(BAKER, 4, offers -> {
			sell(offers, BakeryItems.CHOCOLATE_FOUNTAIN, 1, 12, 15);
			sell(offers, BakeryBlocks.BLOSSOM.sapling.asItem(), 1, 6, 15);
			sell(offers, BakeryBlocks.PEACHWOOD.sapling.asItem(), 1, 6, 15);
		});
		TradeOfferHelper.registerWanderingTraderOffers(1, offers -> {
			for (com.bakery.tree.WoodSet set : BakeryBlocks.WOODS) {
				sell(offers, set.sapling.asItem(), 1, 5, 1);
			}
			sell(offers, BakeryItems.CANDYFLOSS_SEEDS, 1, 6, 1);
			sell(offers, BakeryItems.CRANBERRY_SEEDS, 2, 2, 1);
			sell(offers, BakeryItems.PINEAPPLE_SEEDS, 2, 2, 1);
			sell(offers, BakeryItems.STARBLOOM_SEEDS, 1, 8, 1);
		});
	}

	private static void buy(List<TradeOffers.Factory> offers, ItemConvertible item, int count, int xp) {
		offers.add((entity, random) -> new TradeOffer(new TradedItem(item, count), new ItemStack(Items.EMERALD), 16, xp, 0.05f));
	}

	private static void sell(List<TradeOffers.Factory> offers, Item item, int count, int emeralds, int xp) {
		offers.add((entity, random) -> new TradeOffer(new TradedItem(Items.EMERALD, emeralds), new ItemStack(item, count), 12, xp, 0.05f));
	}
}
