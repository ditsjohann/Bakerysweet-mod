package com.bakery.registry;

import com.bakery.Bakery;
import com.bakery.tree.FruitTreeFeature;
import com.bakery.tree.TreeShapes;
import com.bakery.tree.WoodSet;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;

import java.util.function.Supplier;

/**
 * Wild fruit trees: palms on beaches, lemons in meadows, cherries in flower forests and cherry groves,
 * oranges in savannas, peaches in plains and forests, mangoes in jungles and plums in dark forests and swamps.
 */
public final class BakeryWorldgen {
	private BakeryWorldgen() {
	}

	@SafeVarargs
	private static void tree(String name, TreeShapes.Kind kind, Supplier<WoodSet> wood, Supplier<Block> fruit, RegistryKey<Biome>... biomes) {
		com.bakery.platform.Platform.get().register(Registries.FEATURE, Bakery.id(name), new FruitTreeFeature(kind, wood, fruit));
		// Which biomes it grows in: each loader adds the placed feature bakery:<name> to them its own way.
		com.bakery.platform.Hooks.WILD_TREES.add(new com.bakery.platform.Hooks.WildTree(Bakery.id(name), java.util.List.of(biomes)));
	}

	public static void init() {
		tree("palm_tree", TreeShapes.Kind.PALM, () -> BakeryBlocks.PALM, () -> BakeryBlocks.COCONUT, BiomeKeys.BEACH);
		tree("lemon_tree", TreeShapes.Kind.LEMON, () -> BakeryBlocks.CITRUS, () -> BakeryBlocks.LEMON,
				BiomeKeys.MEADOW, BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.FLOWER_FOREST);
		tree("cherry_tree", TreeShapes.Kind.CHERRY, () -> BakeryBlocks.BLOSSOM, () -> BakeryBlocks.CHERRIES,
				BiomeKeys.CHERRY_GROVE, BiomeKeys.FLOWER_FOREST, BiomeKeys.MEADOW);
		tree("orange_tree", TreeShapes.Kind.ORANGE, () -> BakeryBlocks.ORANGEWOOD, () -> BakeryBlocks.ORANGE,
				BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU, BiomeKeys.WINDSWEPT_SAVANNA);
		tree("peach_tree", TreeShapes.Kind.PEACH, () -> BakeryBlocks.PEACHWOOD, () -> BakeryBlocks.PEACH,
				BiomeKeys.PLAINS, BiomeKeys.FOREST, BiomeKeys.BIRCH_FOREST);
		tree("mango_tree", TreeShapes.Kind.MANGO, () -> BakeryBlocks.MANGOWOOD, () -> BakeryBlocks.MANGO,
				BiomeKeys.JUNGLE, BiomeKeys.SPARSE_JUNGLE, BiomeKeys.BAMBOO_JUNGLE);
		tree("plum_tree", TreeShapes.Kind.PLUM, () -> BakeryBlocks.PLUMWOOD, () -> BakeryBlocks.PLUM,
				BiomeKeys.DARK_FOREST, BiomeKeys.SWAMP, BiomeKeys.OLD_GROWTH_BIRCH_FOREST);
	}
}
