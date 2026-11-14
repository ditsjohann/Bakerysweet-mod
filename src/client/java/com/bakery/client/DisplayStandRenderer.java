package com.bakery.client;

import com.bakery.deco.DecoBlock;
import com.bakery.shop.DisplayStandBlock;
import com.bakery.shop.DisplayStandBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/** The bakes set out on a display stand, shelf or cloche, standing at the block's spots facing the front. */
public class DisplayStandRenderer implements BlockEntityRenderer<DisplayStandBlockEntity> {
	public DisplayStandRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(DisplayStandBlockEntity display, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		if (!(display.getCachedState().getBlock() instanceof DisplayStandBlock stand)) return;
		Direction facing = display.getCachedState().get(DecoBlock.FACING);
		float[][] spots = stand.spots();
		for (int i = 0; i < spots.length; i++) {
			ItemStack stack = display.getSlot(i);
			if (stack.isEmpty()) continue;
			float[] s = spots[i];
			matrices.push();
			matrices.translate(0.5, 0, 0.5);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - facing.asRotation()));
			// Model space (north-facing, pixels) to around the block centre; the front is at low z, so flip x and z.
			matrices.translate((8 - s[0]) / 16f, s[1] / 16f + s[3] / 2, (8 - s[2]) / 16f);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 + (i % 2 == 0 ? -8 : 8)));
			matrices.scale(s[3], s[3], s[3]);
			MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light, overlay, matrices, consumers,
					display.getWorld(), i);
			matrices.pop();
		}
	}
}
