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
import net.minecraft.client.render.WorldRenderer;
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
import org.joml.Matrix4f;

/**
 * Everything you need to read the oven, right on the oven: the iron door and wire rack sliding in and out, bakes on
 * the rack puffing up, turning golden and flipping over,
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
		MinecraftClient client = MinecraftClient.getInstance();
		float rack = oven.rackAmount(tickDelta);
		float door = oven.doorAmount(tickDelta);
		// Out on the rack the bakes get the light of the kitchen; inside they glow with the fire.
		int outside = oven.getWorld() == null ? light : WorldRenderer.getLightmapCoordinates(oven.getWorld(), oven.rackPos());
		int rackLight = rack > 0.5f ? outside : lit ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light;
		float time = (oven.getWorld() == null ? 0 : oven.getWorld().getTime()) + tickDelta;

		matrices.push();
		PixelBox.orient(matrices, facing);
		float front = MathHelper.lerp(rack, GrandOvenBlockEntity.RACK_FRONT_IN, GrandOvenBlockEntity.RACK_FRONT_OUT);
		renderRack(matrices, consumers, front, rackLight, overlay);
		float cellW = GrandOvenBlockEntity.RACK_W / GrandOvenBlockEntity.COLS, cellD = GrandOvenBlockEntity.RACK_D / GrandOvenBlockEntity.ROWS;
		int index = 0;
		for (GrandOvenBlockEntity.Bake bake : oven.getBakes()) {
			ItemStack stack = bake.stack;
			float doneness = oven.doneness(bake, tickDelta);
			ItemStack shown = stack;
			BakingRecipes.Shape shape = BakingRecipes.shapeForRaw(stack);
			if (shape != null && doneness >= 0.85f) {
				shown = new ItemStack(shape.baked());
				if (stack.contains(BakeryComponents.FLAVOR)) shown.set(BakeryComponents.FLAVOR, stack.get(BakeryComponents.FLAVOR));
				if (stack.contains(BakeryComponents.COOKIE_SHAPE)) shown.set(BakeryComponents.COOKIE_SHAPE, stack.get(BakeryComponents.COOKIE_SHAPE));
			}
			float cx = GrandOvenBlockEntity.RACK_X + (bake.col + bake.w / 2f) * cellW;
			float cz = front + (bake.row + bake.h / 2f) * cellD;
			float size = bake.w == 2 ? 0.56f : bake.h == 3 ? 0.66f : bake.h == 2 ? 0.44f : 0.3f;
			size *= 1 + Math.min(doneness, 1f) * 0.08f;
			float lift = 0, roll = 0;
			float flip = MathHelper.clamp((oven.getWorld() == null ? 0 : oven.getWorld().getTime() - bake.flippedAt + tickDelta) / 10f, 0, 1);
			if (bake.flipped && flip < 1) {
				lift = MathHelper.sin(flip * MathHelper.PI) * 3.5f;
				roll = flip * 180;
			} else if (bake.flipped) {
				roll = 180;
			} else if (oven.wantsFlip(bake)) {
				// Sizzling: a little jitter so you notice from across the kitchen.
				lift = Math.abs(MathHelper.sin(time * 1.3f + index)) * 0.35f;
			}
			matrices.push();
			matrices.translate(cx / 16f, (GrandOvenBlockEntity.RACK_Y + 0.55f + lift) / 16f, cz / 16f);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90 + roll));
			// Long bakes lie along the rack; squares get a small natural twist.
			float twist = bake.h > bake.w ? -45 : (bake.col * 7 + bake.row * 11) % 21 - 10;
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(twist));
			matrices.scale(size, size, size);
			client.getItemRenderer().renderItem(shown, ModelTransformationMode.FIXED, rackLight, overlay, matrices, consumers, oven.getWorld(), index++);
			matrices.pop();
		}
		renderDoor(matrices, consumers, door, lit, light, overlay);
		renderFire(oven, tickDelta, matrices, consumers, light, overlay);
		renderGauge(oven, matrices, consumers, light, overlay);
		renderEmbers(oven, time, matrices, consumers, light, overlay);
		matrices.pop();
	}

	/** The wire rack: a frame, wires running front to back, and cross bars between the squares. */
	private void renderRack(MatrixStack matrices, VertexConsumerProvider consumers, float front, int light, int overlay) {
		VertexConsumer vc = PixelBox.solid(consumers);
		Sprite metal = PixelBox.sprite("metal");
		int steel = 0xC9CCD6, dark = 0x8E92A0;
		float x1 = GrandOvenBlockEntity.RACK_X, x2 = x1 + GrandOvenBlockEntity.RACK_W, y = GrandOvenBlockEntity.RACK_Y;
		float z1 = front, z2 = front + GrandOvenBlockEntity.RACK_D;
		float t = 0.35f;
		// Frame and runners
		PixelBox.box(vc, matrices, x1 - 0.4f, y - 0.5f, z1, x1 + 0.2f, y + 0.3f, z2, steel, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, x2 - 0.2f, y - 0.5f, z1, x2 + 0.4f, y + 0.3f, z2, steel, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, x1, y - t, z1, x2, y + t, z1 + 0.5f, steel, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, x1, y - t, z2 - 0.5f, x2, y + t, z2, steel, metal, metal, light, overlay);
		// Handle on the front edge
		PixelBox.box(vc, matrices, x1 + 3, y + 0.3f, z1 - 1.2f, x1 + 3.5f, y + 0.8f, z1, dark, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, x2 - 3.5f, y + 0.3f, z1 - 1.2f, x2 - 3, y + 0.8f, z1, dark, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, x1 + 3, y + 0.5f, z1 - 1.6f, x2 - 3, y + 1f, z1 - 1.1f, 0xE8C46A, metal, metal, light, overlay);
		// Wires
		for (float x = x1 + 1; x < x2 - 0.5f; x += 1.5f) {
			PixelBox.box(vc, matrices, x - 0.15f, y - 0.15f, z1, x + 0.15f, y + 0.15f, z2, steel, metal, metal, light, overlay);
		}
		float cellD = GrandOvenBlockEntity.RACK_D / GrandOvenBlockEntity.ROWS, cellW = GrandOvenBlockEntity.RACK_W / GrandOvenBlockEntity.COLS;
		for (int r = 1; r < GrandOvenBlockEntity.ROWS; r++) {
			float z = z1 + r * cellD;
			PixelBox.box(vc, matrices, x1, y - 0.2f, z - 0.2f, x2, y + 0.25f, z + 0.2f, dark, metal, metal, light, overlay);
		}
		for (int c = 1; c < GrandOvenBlockEntity.COLS; c++) {
			float x = x1 + c * cellW;
			PixelBox.box(vc, matrices, x - 0.2f, y - 0.2f, z1, x + 0.2f, y + 0.25f, z2, dark, metal, metal, light, overlay);
		}
	}

	/** The iron door, hinged at the bottom of the mouth, with a little window so you can peek at the bakes. */
	private void renderDoor(MatrixStack matrices, VertexConsumerProvider consumers, float open, boolean lit, int light, int overlay) {
		VertexConsumer vc = PixelBox.solid(consumers);
		Sprite metal = PixelBox.sprite("metal");
		Sprite glass = PixelBox.sprite("case_glass");
		int iron = 0x4A4650, rim = 0x6A6470, brass = 0xE8C46A;
		matrices.push();
		PixelBox.rotateAround(matrices, 8, 1.5f, -16.7f, RotationAxis.POSITIVE_X, -90 * open);
		float z1 = -16.7f, z2 = -15.7f;
		PixelBox.box(vc, matrices, 1.2f, 1.5f, z1, 3.4f, 10.2f, z2, iron, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 12.6f, 1.5f, z1, 14.8f, 10.2f, z2, iron, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 3.4f, 1.5f, z1, 12.6f, 3.6f, z2, iron, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 3.4f, 7.8f, z1, 12.6f, 10.2f, z2, iron, metal, metal, light, overlay);
		// Rivets and a pink heart on the bottom panel
		for (float x : new float[]{2.3f, 13.7f}) {
			for (float y : new float[]{2.4f, 9.3f}) {
				PixelBox.box(vc, matrices, x - 0.3f, y - 0.3f, z1 - 0.3f, x + 0.3f, y + 0.3f, z1, rim, metal, metal, light, overlay);
			}
		}
		PixelBox.box(vc, matrices, 7.2f, 2.0f, z1 - 0.2f, 8.8f, 3.0f, z1, 0xF06A98, metal, metal, light, overlay);
		// Brass handle
		PixelBox.box(vc, matrices, 5f, 8.6f, z1 - 1.2f, 5.6f, 9.2f, z1, brass, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 10.4f, 8.6f, z1 - 1.2f, 11f, 9.2f, z1, brass, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 5f, 8.7f, z1 - 1.7f, 11f, 9.3f, z1 - 1.1f, brass, metal, metal, light, overlay);
		// Window glass, warm when the fire is going.
		PixelBox.box(PixelBox.translucent(consumers), matrices, 3.4f, 3.6f, -16.3f, 12.6f, 7.8f, -16.1f, lit ? 0x90FFB070 : 0x70A0B4C0, glass, glass,
				lit ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light, overlay);
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
		for (GrandOvenBlockEntity.Bake bake : oven.getBakes()) {
			if (GrandOvenBlockEntity.bakeTime(bake.stack) > 0) {
				ideal = GrandOvenBlockEntity.idealHeat(bake.stack);
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

	/**
	 * The ash-pit grate under the mouth: four coal lumps that glow while there's fuel, going dark one by one as it
	 * burns down, and blinking red when it's almost out.
	 */
	private void renderEmbers(GrandOvenBlockEntity oven, float time, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		VertexConsumer vc = PixelBox.solid(consumers);
		Sprite metal = PixelBox.sprite("metal");
		Sprite coal = PixelBox.sprite("cake_piping");
		float z = -16.35f, y1 = -5.6f, y2 = -1.6f;
		// Iron grate frame with bars
		PixelBox.box(vc, matrices, 3.6f, y1, z, 12.4f, y1 + 0.6f, -15.9f, 0x3E3A44, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 3.6f, y2 - 0.6f, z, 12.4f, y2, -15.9f, 0x3E3A44, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 3.6f, y1, z, 4.2f, y2, -15.9f, 0x3E3A44, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 11.8f, y1, z, 12.4f, y2, -15.9f, 0x3E3A44, metal, metal, light, overlay);
		PixelBox.box(vc, matrices, 4.2f, y1 + 0.6f, -16.0f, 11.8f, y2 - 0.6f, -15.95f, 0x1C1A20, metal, metal, light, overlay);
		int fuel = oven.getFuel();
		boolean low = fuel > 0 && fuel < GrandOvenBlockEntity.LOW_FUEL;
		float pulse = 0.6f + 0.4f * MathHelper.sin(time * (low ? 0.5f : 0.12f));
		int lumps = fuel <= 0 ? 0 : low ? 1 : 1 + Math.min(3, fuel * 4 / GrandOvenBlockEntity.MAX_FUEL);
		for (int i = 0; i < 4; i++) {
			float x = 4.8f + i * 1.8f;
			boolean on = i < lumps;
			int glow = !on ? 0x2A2628 : low ? lerpColor(0x8A2010, 0xFF4030, pulse) : lerpColor(0xC8501C, 0xFFB040, pulse);
			PixelBox.box(vc, matrices, x, y1 + 1.1f, -16.25f, x + 1.3f, y2 - 1.1f, -15.95f, glow, coal, coal,
					on ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light, overlay);
		}
		// Bars in front of the coals
		for (float x = 5.9f; x < 11.5f; x += 1.8f) {
			PixelBox.box(vc, matrices, x, y1 + 0.6f, -16.4f, x + 0.4f, y2 - 0.6f, -16.25f, 0x4A4650, metal, metal, light, overlay);
		}
	}

	private static int lerpColor(int a, int b, float t) {
		int r = (int) MathHelper.lerp(t, a >> 16 & 255, b >> 16 & 255);
		int g = (int) MathHelper.lerp(t, a >> 8 & 255, b >> 8 & 255);
		int bl = (int) MathHelper.lerp(t, a & 255, b & 255);
		return r << 16 | g << 8 | bl;
	}

	@Override
	public boolean rendersOutsideBoundingBox(GrandOvenBlockEntity oven) {
		return true;
	}
}
