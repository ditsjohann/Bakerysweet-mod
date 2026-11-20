package com.bakery.tree;

import com.bakery.Bakery;
import com.bakery.platform.Hooks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WoodType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.List;
import java.util.function.Supplier;

/** A full set of building blocks for one fruit tree's wood. */
public final class WoodSet {
	public final String name;
	public final BlockSetType setType;
	public final WoodType woodType;
	public final Block log, wood, strippedLog, strippedWood, planks, stairs, slab, fence, fenceGate, door, trapdoor, leaves, sapling;

	public WoodSet(String name, MapColor planksColor, MapColor barkColor, TreeShapes.Kind kind, Supplier<WoodSet> self, Supplier<Block> fruit) {
		this.name = name;
		// Same sounds and behaviour as oak (what BlockSetType.OAK and WoodType.OAK are made with).
		this.setType = new BlockSetType(Bakery.id(name).toString());
		this.woodType = new WoodType(Bakery.id(name).toString(), setType);

		log = register(name + "_log", new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_LOG).mapColor(barkColor)));
		wood = register(name + "_wood", new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_WOOD).mapColor(barkColor)));
		strippedLog = register("stripped_" + name + "_log", new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_LOG).mapColor(planksColor)));
		strippedWood = register("stripped_" + name + "_wood", new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_WOOD).mapColor(planksColor)));
		planks = register(name + "_planks", new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).mapColor(planksColor)));
		stairs = register(name + "_stairs", new StairsBlock(planks.getDefaultState(), AbstractBlock.Settings.copy(Blocks.OAK_STAIRS).mapColor(planksColor)));
		slab = register(name + "_slab", new SlabBlock(AbstractBlock.Settings.copy(Blocks.OAK_SLAB).mapColor(planksColor)));
		fence = register(name + "_fence", new FenceBlock(AbstractBlock.Settings.copy(Blocks.OAK_FENCE).mapColor(planksColor)));
		fenceGate = register(name + "_fence_gate", new FenceGateBlock(woodType, AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE).mapColor(planksColor)));
		door = register(name + "_door", new DoorBlock(setType, AbstractBlock.Settings.copy(Blocks.OAK_DOOR).mapColor(planksColor)));
		trapdoor = register(name + "_trapdoor", new TrapdoorBlock(setType, AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).mapColor(planksColor)));
		leaves = register(name + "_leaves", new LeavesBlock(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES)));
		sapling = register(name + "_sapling", new FruitSaplingBlock(AbstractBlock.Settings.copy(Blocks.OAK_SAPLING), kind, self, fruit));

		Hooks.strippable(log, strippedLog);
		Hooks.strippable(wood, strippedWood);
		for (Block b : List.of(log, wood, strippedLog, strippedWood)) Hooks.flammable(b, 5, 5);
		for (Block b : List.of(planks, stairs, slab, fence, fenceGate)) Hooks.flammable(b, 5, 20);
		Hooks.flammable(leaves, 30, 60);
	}

	public List<Block> all() {
		return List.of(log, wood, strippedLog, strippedWood, planks, stairs, slab, fence, fenceGate, door, trapdoor, leaves, sapling);
	}

	private static Block register(String name, Block block) {
		return Registry.register(Registries.BLOCK, Bakery.id(name), block);
	}
}
