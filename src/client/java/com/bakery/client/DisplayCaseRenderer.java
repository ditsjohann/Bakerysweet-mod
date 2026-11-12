package com.bakery.client;

import com.bakery.shop.DisplayCaseBlock;
import com.bakery.shop.DisplayCaseBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/** Bakes sitting on the two glass shelves of a display case. */
public class DisplayCaseRenderer implements BlockEntityRenderer<DisplayCaseBlockEntity> {
	private static final float[][] SPOTS = {{-0.2f, 0.56f}, {0.2f, 0.56f}, {-0.2f, 0.13f}, {0.2f, 0.13f}};

	public DisplayCaseRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(DisplayCaseBlockEntity display, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		Direction facing = display.getCachedState().get(DisplayCaseBlock.FACING);
		for (int i = 0; i < DisplayCaseBlockEntity.SLOTS; i++) {
			ItemStack stack = display.getSlot(i);
			if (stack.isEmpty()) {
				continue;
			}
			matrices.push();
			matrices.translate(0.5, 0, 0.5);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - facing.asRotation()));
			matrices.translate(SPOTS[i][0], SPOTS[i][1] + 0.14, -0.05);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
			matrices.scale(0.32f, 0.32f, 0.32f);
			MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light, overlay,
					matrices, consumers, display.getWorld(), i);
			matrices.pop();
		}
	}
}
