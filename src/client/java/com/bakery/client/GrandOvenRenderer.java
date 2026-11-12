package com.bakery.client;

import com.bakery.baking.BakingRecipes;
import com.bakery.oven.GrandOvenBlock;
import com.bakery.oven.GrandOvenBlockEntity;
import com.bakery.registry.BakeryComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Everything you need to read the oven, right on the oven: bakes on the floor puffing up and turning golden,
 * logs in the firebox and flames that grow and shrink with the heat, and a heat gauge on the front whose needle
 * swings with the fire (the green notch is the sweet spot for what's inside).
 */
public class GrandOvenRenderer implements BlockEntityRenderer<GrandOvenBlockEntity> {
	public GrandOvenRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(GrandOvenBlockEntity oven, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		Direction facing = oven.getCachedState().get(GrandOvenBlock.FACING);
		boolean lit = oven.getCachedState().get(GrandOvenBlock.LIT);
		int glow = lit ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
		MinecraftClient client = MinecraftClient.getInstance();
		for (int i = 0; i < GrandOvenBlockEntity.SLOTS; i++) {
			ItemStack stack = oven.getSlot(i);
			if (stack.isEmpty()) {
				continue;
			}
			float doneness = oven.doneness(i, tickDelta);
			ItemStack shown = stack;
			BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(stack);
			if (shape != null && doneness >= 0.85f) {
				shown = new ItemStack(shape.baked());
				if (stack.contains(BakeryComponents.FLAVOR)) {
					shown.set(BakeryComponents.FLAVOR, stack.get(BakeryComponents.FLAVOR));
				}
			}
			float puff = 0.36f + Math.min(doneness, 1f) * 0.08f;
			Vec3d spot = GrandOvenBlockEntity.slotOffset(facing, i);
			matrices.push();
			matrices.translate(spot.x, spot.y, spot.z);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - facing.asRotation()));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 25 - 25));
			matrices.scale(puff, puff, puff);
			client.getItemRenderer().renderItem(shown, ModelTransformationMode.FIXED, glow, overlay, matrices, consumers, oven.getWorld(), i);
			matrices.pop();
		}

		matrices.push();
		PixelBox.orient(matrices, facing);
		renderFire(oven, tickDelta, matrices, consumers, light, overlay);
		renderGauge(oven, matrices, consumers, light, overlay);
		matrices.pop();
	}

	/** Logs at the back of the chamber and a fire whose size follows the heat. */
	private void renderFire(GrandOvenBlockEntity oven, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		int logs = oven.logs();
		VertexConsumer vc = PixelBox.solid(consumers);
		Sprite bark = PixelBox.sprite("oven/bark");
		Sprite end = PixelBox.sprite("oven/log_end");
		for (int i = 0; i < logs; i++) {
			float y = i < 2 ? 0.1f : 1.6f;
			float x = i < 2 ? 3.5f + i * 5 : 6 + (i - 2) * 3;
			matrices.push();
			PixelBox.rotateAround(matrices, x + 2, y, 10, RotationAxis.POSITIVE_Y, i % 2 == 0 ? 12 : -16);
			PixelBox.box(vc, matrices, x, y, 8, x + 4, y + 1.6f, 12.5f, 0xFFFFFF, bark, end, light, overlay);
			matrices.pop();
		}
		if (oven.getFuel() <= 0) return;
		float heat = oven.getHeat() / 100f;
		float time = (oven.getWorld() == null ? 0 : oven.getWorld().getTime()) + tickDelta;
		float h = 3 + heat * 7 + MathHelper.sin(time * 0.35f) * 0.6f;
		Sprite fire = MinecraftClient.getInstance().getSpriteAtlas(net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
				.apply(net.minecraft.util.Identifier.ofVanilla("block/fire_0"));
		VertexConsumer flames = consumers.getBuffer(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		float w = 4 + heat * 4;
		crossed(flames, matrices, 8, 1.2f, 10.2f, w, h, fire);
	}

	/** Two crossed upright quads, like vanilla fire, centred on (cx, cz) in pixels. */
	private static void crossed(VertexConsumer vc, MatrixStack matrices, float cx, float y, float cz, float w, float h, Sprite sprite) {
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f m = entry.getPositionMatrix();
		float s = 1 / 16f, half = w / 2;
		float[][] planes = {{cx - half, cz - half, cx + half, cz + half}, {cx - half, cz + half, cx + half, cz - half}};
		for (float[] p : planes) {
			for (int side = 0; side < 2; side++) {
				float x1 = side == 0 ? p[0] : p[2], z1 = side == 0 ? p[1] : p[3], x2 = side == 0 ? p[2] : p[0], z2 = side == 0 ? p[3] : p[1];
				int full = LightmapTextureManager.MAX_LIGHT_COORDINATE;
				vc.vertex(m, x1 * s, (y + h) * s, z1 * s).color(255, 255, 255, 255).texture(sprite.getMinU(), sprite.getMinV()).overlay(OverlayTexture.DEFAULT_UV).light(full).normal(entry, 0, 1, 0);
				vc.vertex(m, x1 * s, y * s, z1 * s).color(255, 255, 255, 255).texture(sprite.getMinU(), sprite.getMaxV()).overlay(OverlayTexture.DEFAULT_UV).light(full).normal(entry, 0, 1, 0);
				vc.vertex(m, x2 * s, y * s, z2 * s).color(255, 255, 255, 255).texture(sprite.getMaxU(), sprite.getMaxV()).overlay(OverlayTexture.DEFAULT_UV).light(full).normal(entry, 0, 1, 0);
				vc.vertex(m, x2 * s, (y + h) * s, z2 * s).color(255, 255, 255, 255).texture(sprite.getMaxU(), sprite.getMinV()).overlay(OverlayTexture.DEFAULT_UV).light(full).normal(entry, 0, 1, 0);
			}
		}
	}

	/** A round heat gauge above the oven mouth: cool blue, sweet-spot green, hot red, and a swinging needle. */
	private void renderGauge(GrandOvenBlockEntity oven, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		VertexConsumer vc = PixelBox.solid(consumers);
		Sprite face = PixelBox.sprite("cake_piping");
		Sprite metal = PixelBox.sprite("metal");
		// Brass rim and cream face, on the lintel to the right of the heart plaque.
		PixelBox.box(vc, matrices, 11.2f, 12.2f, -16.2f, 15.8f, 16.8f, -15.4f, 0xF0C45A, face, face, light, overlay);
		PixelBox.box(vc, matrices, 11.6f, 12.6f, -16.4f, 15.4f, 16.4f, -16.1f, 0xFFF4E2, face, face, light, overlay);
		// The sweet spot for whatever is baking (if anything is).
		int ideal = -1;
		for (int i = 0; i < GrandOvenBlockEntity.SLOTS; i++) {
			if (!oven.getSlot(i).isEmpty() && GrandOvenBlockEntity.bakeTime(oven.getSlot(i)) > 0) {
				ideal = GrandOvenBlockEntity.idealHeat(oven.getSlot(i));
				break;
			}
		}
		float cx = 13.5f, cy = 13.2f;
		if (ideal >= 0) {
			matrices.push();
			PixelBox.rotateAround(matrices, cx, cy, -16.45f, RotationAxis.POSITIVE_Z, -90 + ideal / 100f * 180);
			PixelBox.box(vc, matrices, cx - 0.35f, cy + 2.2f, -16.55f, cx + 0.35f, cy + 3f, -16.4f, 0x5DCB6A, metal, metal, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay);
			matrices.pop();
		}
		// Needle
		matrices.push();
		PixelBox.rotateAround(matrices, cx, cy, -16.5f, RotationAxis.POSITIVE_Z, -90 + oven.getHeat() / 100f * 180);
		PixelBox.box(vc, matrices, cx - 0.2f, cy - 0.2f, -16.6f, cx + 0.2f, cy + 2.6f, -16.45f, 0xE0405A, metal, metal, light, overlay);
		matrices.pop();
		PixelBox.box(vc, matrices, cx - 0.4f, cy - 0.4f, -16.7f, cx + 0.4f, cy + 0.4f, -16.45f, 0x6A4A3A, metal, metal, light, overlay);
	}

	@Override
	public boolean rendersOutsideBoundingBox(GrandOvenBlockEntity oven) {
		return true;
	}
}
