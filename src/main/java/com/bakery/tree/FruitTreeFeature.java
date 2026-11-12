package com.bakery.tree;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.function.Supplier;

/** Places wild palms on beaches and lemon trees in meadows and plains. */
public class FruitTreeFeature extends Feature<DefaultFeatureConfig> {
	private final TreeShapes.Kind kind;
	private final Supplier<WoodSet> wood;
	private final Supplier<Block> fruit;

	public FruitTreeFeature(TreeShapes.Kind kind, Supplier<WoodSet> wood, Supplier<Block> fruit) {
		super(DefaultFeatureConfig.CODEC);
		this.kind = kind;
		this.wood = wood;
		this.fruit = fruit;
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		BlockState ground = world.getBlockState(origin.down());
		boolean ok = kind == TreeShapes.Kind.PALM ? ground.isIn(BlockTags.SAND) : ground.isIn(BlockTags.DIRT);
		if (!ok || !world.getBlockState(origin).isReplaceable()) {
			return false;
		}
		return TreeShapes.grow(kind, world, origin, context.getRandom(), wood.get(), fruit.get(), Block.NOTIFY_LISTENERS);
	}
}
