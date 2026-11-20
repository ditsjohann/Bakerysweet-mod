package com.bakery.platform;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffers;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Game content the mod loaders wire up their own way, collected while the mod registers its things: stripping,
 * flammability, composting, entity attributes, villager trades and the fruit trees that grow in the wild.
 * Each loader's entry point reads these lists once, after {@link com.bakery.Bakery#init()}.
 */
public final class Hooks {
	public record Flammable(Block block, int burn, int spread) {
	}

	/** A batch of trades for one villager level (or, for the wandering trader, 1 = common, 2 = rare). */
	public record Trades(int level, Consumer<List<TradeOffers.Factory>> factories) {
	}

	/** A fruit tree (configured + placed feature {@code id}) that grows in these biomes. */
	public record WildTree(Identifier id, List<RegistryKey<Biome>> biomes) {
	}

	public record Attributes(EntityType<? extends LivingEntity> type, Supplier<DefaultAttributeContainer.Builder> attributes) {
	}

	public static final Map<Block, Block> STRIPPABLE = new LinkedHashMap<>();
	public static final List<Flammable> FLAMMABLE = new ArrayList<>();
	public static final Map<Item, Float> COMPOSTABLE = new LinkedHashMap<>();
	public static final List<Attributes> ATTRIBUTES = new ArrayList<>();
	public static final List<Trades> BAKER_TRADES = new ArrayList<>();
	public static final List<Trades> WANDERER_TRADES = new ArrayList<>();
	public static final List<WildTree> WILD_TREES = new ArrayList<>();

	private Hooks() {
	}

	public static void strippable(Block log, Block stripped) {
		STRIPPABLE.put(log, stripped);
	}

	public static void flammable(Block block, int burn, int spread) {
		FLAMMABLE.add(new Flammable(block, burn, spread));
	}

	public static void compostable(ItemConvertible item, float chance) {
		COMPOSTABLE.put(item.asItem(), chance);
	}

	public static void attributes(EntityType<? extends LivingEntity> type, Supplier<DefaultAttributeContainer.Builder> attributes) {
		ATTRIBUTES.add(new Attributes(type, attributes));
	}

	public static void bakerTrades(int level, Consumer<List<TradeOffers.Factory>> factories) {
		BAKER_TRADES.add(new Trades(level, factories));
	}

	public static void wandererTrades(int level, Consumer<List<TradeOffers.Factory>> factories) {
		WANDERER_TRADES.add(new Trades(level, factories));
	}
}
