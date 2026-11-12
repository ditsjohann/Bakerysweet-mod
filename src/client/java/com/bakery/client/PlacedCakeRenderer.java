package com.bakery.client;

import com.bakery.block.PlacedFoodBlock;
import com.bakery.block.PlacedFoodBlockEntity;
import com.bakery.registry.BakeryBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

/** A hand-decorated cake set down on a cake plate: drawn exactly as it was made on the turntable. */
public class PlacedCakeRenderer implements BlockEntityRenderer<PlacedFoodBlockEntity> {
	public PlacedCakeRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(PlacedFoodBlockEntity food, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		BlockState state = food.getCachedState();
		if (!state.isOf(BakeryBlocks.CAKE) || !state.get(PlacedFoodBlock.CUSTOM) || food.getStack().isEmpty()) return;
		matrices.push();
		matrices.translate(0.5, 1 / 16f, 0.5);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-state.get(PlacedFoodBlock.FACING).asRotation()));
		// Each slice eaten makes the cake a little smaller - a gentle stand-in for cutting it.
		float s = 1 - state.get(PlacedFoodBlock.BITES) * 0.14f;
		matrices.scale(s, s, s);
		CakeRenderer.render(food.getStack(), matrices, consumers, light, overlay);
		matrices.pop();
	}
}
