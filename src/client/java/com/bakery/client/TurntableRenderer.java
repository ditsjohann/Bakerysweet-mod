package com.bakery.client;

import com.bakery.sweet.TurntableBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/** The cake on the Cake Turntable, turned to wherever the plate was last spun. */
public class TurntableRenderer implements BlockEntityRenderer<TurntableBlockEntity> {
	public TurntableRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(TurntableBlockEntity table, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		ItemStack cake = table.getCake();
		if (cake.isEmpty()) return;
		matrices.push();
		matrices.translate(0.5, TurntableBlockEntity.PLATE / 16f, 0.5);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(table.renderAngle(tickDelta)));
		CakeRenderer.render(cake, matrices, consumers, light, overlay);
		matrices.pop();
	}
}
