package com.bakery.client;

import com.bakery.Bakery;
import com.bakery.food.BakeryFoods;
import com.bakery.registry.BakeryComponents;
import com.bakery.sweet.CakeDesign;
import com.bakery.sweet.CakeGeometry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws a hand-decorated cake: chunky pixel tiers in their frosting colours, every piped dot, rosette and drip,
 * and the toppers standing on top. Matrices sit at the cake's bottom centre, one unit per block.
 */
public final class CakeRenderer {
	private static final float PX = 1 / 16f;

	private CakeRenderer() {
	}

	private static Sprite sprite(String name) {
		return MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(Bakery.id("block/" + name));
	}

	public static void render(ItemStack cake, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		CakeDesign design = cake.getOrDefault(BakeryComponents.CAKE_DESIGN, CakeDesign.PLAIN);
		int flavor = BakeryFoods.flavor(cake).color();
		VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		Sprite frosting = sprite("cake_frosting");
		Sprite sponge = sprite("cake_sponge");
		Sprite piping = sprite("cake_piping");
		int tiers = design.tierCount();

		for (int i = 0; i < tiers; i++) {
			int color = design.tiers().get(i) == -1 ? flavor : design.tiers().get(i);
			float r = CakeGeometry.radius(i, tiers);
			float y1 = CakeGeometry.bottom(i, tiers), y2 = CakeGeometry.top(i, tiers);
			boolean frosted = design.tiers().get(i) != -1;
			for (float[] s : CakeGeometry.strips(design.shape(), r)) {
				// Unfrosted tiers show their sponge on the sides; frosted ones are covered all over.
				PixelBox.box(buffer, matrices, s[0], y1, s[1], s[2], y2, s[3], color, frosted ? frosting : sponge, frosting, light, overlay);
			}
		}

		for (CakeDesign.Dab dab : design.dabs()) {
			float x = dab.x() / 2f, y = dab.y() / 2f, z = dab.z() / 2f;
			int c = dab.color();
			if (dab.isDrip()) {
				float len = dab.dripLength() * 0.9f;
				PixelBox.box(buffer, matrices, x - 0.45f, y - len, z - 0.45f, x + 0.45f, y + 0.2f, z + 0.45f, c, piping, piping, light, overlay);
				PixelBox.box(buffer, matrices, x - 0.6f, y - len - 0.6f, z - 0.6f, x + 0.6f, y - len + 0.2f, z + 0.6f, c, piping, piping, light, overlay);
			} else if (dab.kind() == CakeDesign.Dab.ROSETTE) {
				PixelBox.box(buffer, matrices, x - 1.1f, y - 0.4f, z - 1.1f, x + 1.1f, y + 0.5f, z + 1.1f, c, piping, piping, light, overlay);
				matrices.push();
				matrices.translate(x * PX, 0, z * PX);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45));
				PixelBox.box(buffer, matrices, -0.75f, y + 0.4f, -0.75f, 0.75f, y + 1.2f, 0.75f, lighten(c), piping, piping, light, overlay);
				matrices.pop();
				PixelBox.box(buffer, matrices, x - 0.3f, y + 1.1f, z - 0.3f, x + 0.3f, y + 1.6f, z + 0.3f, lighten(c), piping, piping, light, overlay);
			} else {
				PixelBox.box(buffer, matrices, x - 0.55f, y - 0.4f, z - 0.55f, x + 0.55f, y + 0.6f, z + 0.55f, c, piping, piping, light, overlay);
			}
		}

		MinecraftClient client = MinecraftClient.getInstance();
		for (CakeDesign.Topper topper : design.toppers()) {
			ItemStack stack = new ItemStack(topper.item());
			matrices.push();
			matrices.translate(topper.x() / 2f * PX, topper.y() / 2f * PX, topper.z() / 2f * PX);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(topper.yaw()));
			// Sculpted toppers are little 3D figurines; fruit and petals are flat cut-outs, candles are blocks.
			boolean figurine = java.util.Arrays.asList(com.bakery.registry.BakeryItems.TOPPERS).contains(stack.getItem());
			float size = figurine ? 0.42f : stack.getItem() instanceof net.minecraft.item.BlockItem ? 0.42f : 0.28f;
			matrices.translate(0, size * 0.5f - 0.01f, 0);
			matrices.scale(size, size, size);
			client.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, consumers, client.world, 0);
			matrices.pop();
		}
	}

	private static int lighten(int c) {
		int r = Math.min(255, (c >> 16 & 255) + 22), g = Math.min(255, (c >> 8 & 255) + 22), b = Math.min(255, (c & 255) + 22);
		return r << 16 | g << 8 | b;
	}

	/** For the renderers' bounding boxes. */
	public static List<float[]> footprint(CakeDesign design) {
		return CakeGeometry.strips(design.shape(), CakeGeometry.radius(0, design.tierCount()));
	}
}
