package com.bakery.client;

import com.bakery.Bakery;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Drawing helpers for the block entity renderers: boxes measured in pixels with a block-atlas sprite and soft
 * shading (for liquids, dough, icing...), plus the separately-loaded "part" models that move (levers, cranks,
 * the millstone's runner, the jam spoon).
 */
public final class PixelBox {
	private static final float PX = 1 / 16f;
	/** Models in assets/bakery/models/block/part/ that are drawn by renderers rather than by blockstates. */
	public static final String[] PARTS = {"juicer_lever", "churn_crank", "jam_spoon", "millstone_runner"};

	private PixelBox() {
	}

	public static void registerParts() {
		ModelLoadingPlugin.register(context -> {
			for (String part : PARTS) context.addModels(Bakery.id("block/part/" + part));
		});
	}

	public static Sprite sprite(String path) {
		Identifier id = path.contains(":") ? Identifier.of(path) : Bakery.id("block/" + path);
		return MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(id);
	}

	public static VertexConsumer solid(VertexConsumerProvider consumers) {
		return consumers.getBuffer(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
	}

	public static VertexConsumer translucent(VertexConsumerProvider consumers) {
		return consumers.getBuffer(RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
	}

	/** Draws a part model; matrices at the block corner as for a block model. */
	public static void part(String name, BlockState state, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		MinecraftClient client = MinecraftClient.getInstance();
		BakedModel model = client.getBakedModelManager().getModel(Bakery.id("block/part/" + name));
		if (model == null) return;
		client.getBlockRenderManager().getModelRenderer().render(matrices.peek(), consumers.getBuffer(TexturedRenderLayers.getEntityCutout()),
				state, model, 1f, 1f, 1f, light, overlay);
	}

	/** Turns the matrices the way a blockstate with "y" rotation for this facing would turn a model. */
	public static void orient(MatrixStack matrices, Direction facing) {
		matrices.translate(0.5, 0, 0.5);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-(facing.asRotation() + 180)));
		matrices.translate(-0.5, 0, -0.5);
	}

	/** Rotates about a pivot given in pixels. */
	public static void rotateAround(MatrixStack matrices, float px, float py, float pz, RotationAxis axis, float degrees) {
		matrices.translate(px * PX, py * PX, pz * PX);
		matrices.multiply(axis.rotationDegrees(degrees));
		matrices.translate(-px * PX, -py * PX, -pz * PX);
	}

	/** A box in pixels. {@code argb} with alpha 0 counts as opaque. */
	public static void box(VertexConsumer vc, MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, int argb,
						   Sprite side, Sprite top, int light, int overlay) {
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f m = entry.getPositionMatrix();
		int a = argb >>> 24 == 0 ? 255 : argb >>> 24;
		int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
		float ax = x1 * PX, ay = y1 * PX, az = z1 * PX, bx = x2 * PX, by = y2 * PX, bz = z2 * PX;
		quad(vc, m, entry, top, r, g, b, a, 1f, ax, by, az, ax, by, bz, bx, by, bz, bx, by, az, 0, 1, 0, x1, z1, x2, z2, light, overlay);
		quad(vc, m, entry, top, r, g, b, a, 0.6f, ax, ay, bz, ax, ay, az, bx, ay, az, bx, ay, bz, 0, -1, 0, x1, z1, x2, z2, light, overlay);
		quad(vc, m, entry, side, r, g, b, a, 0.86f, bx, by, az, bx, ay, az, ax, ay, az, ax, by, az, 0, 0, -1, x1, 16 - y2, x2, 16 - y1, light, overlay);
		quad(vc, m, entry, side, r, g, b, a, 0.86f, ax, by, bz, ax, ay, bz, bx, ay, bz, bx, by, bz, 0, 0, 1, x1, 16 - y2, x2, 16 - y1, light, overlay);
		quad(vc, m, entry, side, r, g, b, a, 0.76f, ax, by, az, ax, ay, az, ax, ay, bz, ax, by, bz, -1, 0, 0, z1, 16 - y2, z2, 16 - y1, light, overlay);
		quad(vc, m, entry, side, r, g, b, a, 0.76f, bx, by, bz, bx, ay, bz, bx, ay, az, bx, by, az, 1, 0, 0, z1, 16 - y2, z2, 16 - y1, light, overlay);
	}

	/** Just the top face of a box: a liquid surface. */
	public static void surface(VertexConsumer vc, MatrixStack matrices, float x1, float y, float z1, float x2, float z2, int argb, Sprite sprite, int light, int overlay) {
		MatrixStack.Entry entry = matrices.peek();
		int a = argb >>> 24 == 0 ? 255 : argb >>> 24;
		float ay = y * PX;
		quad(vc, entry.getPositionMatrix(), entry, sprite, argb >> 16 & 255, argb >> 8 & 255, argb & 255, a, 1f,
				x1 * PX, ay, z1 * PX, x1 * PX, ay, z2 * PX, x2 * PX, ay, z2 * PX, x2 * PX, ay, z1 * PX, 0, 1, 0, x1, z1, x2, z2, light, overlay);
	}

	// ------------------------------------------------------------------ round shapes

	private static void vertex(VertexConsumer vc, Matrix4f m, MatrixStack.Entry entry, float x, float y, float z, int r, int g, int b, int a,
							   float u, float v, float nx, float ny, float nz, int light, int overlay) {
		float shade = 0.88f + 0.12f * ny;
		vc.vertex(m, x * PX, y * PX, z * PX).color(Math.round(r * shade), Math.round(g * shade), Math.round(b * shade), a).texture(u, v)
				.overlay(overlay).light(light).normal(entry, nx, ny, nz);
	}

	/** The side of an upright cylinder in pixels, drawn from both sides (it may be seen from inside a bowl). */
	public static void tube(VertexConsumer vc, MatrixStack matrices, float cx, float cz, float r, float y1, float y2, int segments, int argb,
							Sprite sprite, int light, int overlay) {
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f m = entry.getPositionMatrix();
		int a = argb >>> 24 == 0 ? 255 : argb >>> 24, cr = argb >> 16 & 255, cg = argb >> 8 & 255, cb = argb & 255;
		float arc = Math.min(16, (float) (2 * Math.PI * r / segments));
		float v1 = sprite.getFrameV(wrap(16 - Math.min(16, y2 - y1)) / 16f), v2 = sprite.getFrameV(1);
		for (int i = 0; i < segments; i++) {
			double a1 = 2 * Math.PI * i / segments, a2 = 2 * Math.PI * (i + 1) / segments;
			float x1 = cx + (float) Math.cos(a1) * r, z1 = cz + (float) Math.sin(a1) * r;
			float x2 = cx + (float) Math.cos(a2) * r, z2 = cz + (float) Math.sin(a2) * r;
			float n1x = (float) Math.cos(a1), n1z = (float) Math.sin(a1), n2x = (float) Math.cos(a2), n2z = (float) Math.sin(a2);
			float us = (i * arc) % 16;
			if (us + arc > 16) us = 16 - arc;
			float u1 = sprite.getFrameU(us / 16f), u2 = sprite.getFrameU((us + arc) / 16f);
			// Outside (counter-clockwise seen from outside), then the inside.
			vertex(vc, m, entry, x2, y2, z2, cr, cg, cb, a, u2, v1, n2x, 0, n2z, light, overlay);
			vertex(vc, m, entry, x2, y1, z2, cr, cg, cb, a, u2, v2, n2x, 0, n2z, light, overlay);
			vertex(vc, m, entry, x1, y1, z1, cr, cg, cb, a, u1, v2, n1x, 0, n1z, light, overlay);
			vertex(vc, m, entry, x1, y2, z1, cr, cg, cb, a, u1, v1, n1x, 0, n1z, light, overlay);
			vertex(vc, m, entry, x1, y2, z1, cr, cg, cb, a, u1, v1, -n1x, 0, -n1z, light, overlay);
			vertex(vc, m, entry, x1, y1, z1, cr, cg, cb, a, u1, v2, -n1x, 0, -n1z, light, overlay);
			vertex(vc, m, entry, x2, y1, z2, cr, cg, cb, a, u2, v2, -n2x, 0, -n2z, light, overlay);
			vertex(vc, m, entry, x2, y2, z2, cr, cg, cb, a, u2, v1, -n2x, 0, -n2z, light, overlay);
		}
	}

	/** A flat round disc facing up (or down), in pixels. */
	public static void disc(VertexConsumer vc, MatrixStack matrices, float cx, float y, float cz, float r, int segments, int argb, Sprite sprite,
							boolean up, int light, int overlay) {
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f m = entry.getPositionMatrix();
		int a = argb >>> 24 == 0 ? 255 : argb >>> 24, cr = argb >> 16 & 255, cg = argb >> 8 & 255, cb = argb & 255;
		float ny = up ? 1 : -1;
		float uc = sprite.getFrameU(0.5f), vc0 = sprite.getFrameV(0.5f);
		float scale = Math.min(1f, 8f / Math.max(r, 0.01f));
		for (int i = 0; i < segments; i++) {
			double a1 = 2 * Math.PI * i / segments, a2 = 2 * Math.PI * (i + 1) / segments;
			if (!up) {
				double t = a1;
				a1 = a2;
				a2 = t;
			}
			float x1 = (float) Math.cos(a1) * r, z1 = (float) Math.sin(a1) * r, x2 = (float) Math.cos(a2) * r, z2 = (float) Math.sin(a2) * r;
			float u1 = sprite.getFrameU(0.5f + x1 * scale / 16), w1 = sprite.getFrameV(0.5f + z1 * scale / 16);
			float u2 = sprite.getFrameU(0.5f + x2 * scale / 16), w2 = sprite.getFrameV(0.5f + z2 * scale / 16);
			vertex(vc, m, entry, cx, y, cz, cr, cg, cb, a, uc, vc0, 0, ny, 0, light, overlay);
			vertex(vc, m, entry, cx + x2, y, cz + z2, cr, cg, cb, a, u2, w2, 0, ny, 0, light, overlay);
			vertex(vc, m, entry, cx + x1, y, cz + z1, cr, cg, cb, a, u1, w1, 0, ny, 0, light, overlay);
			vertex(vc, m, entry, cx, y, cz, cr, cg, cb, a, uc, vc0, 0, ny, 0, light, overlay);
		}
	}

	/**
	 * A smooth ellipsoid (a squashed or stretched ball) in pixels. {@code bottom} 0..1 cuts off the lower part
	 * (0.5 = a dome sitting on its flat side), which is how dollops and drips sit on a cake.
	 */
	public static void blob(VertexConsumer vc, MatrixStack matrices, float cx, float cy, float cz, float rx, float ry, float rz, int rings, int segments,
							float bottom, int argb, Sprite sprite, int light, int overlay) {
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f m = entry.getPositionMatrix();
		int a = argb >>> 24 == 0 ? 255 : argb >>> 24, cr = argb >> 16 & 255, cg = argb >> 8 & 255, cb = argb & 255;
		double lat0 = -Math.PI / 2 + Math.PI * bottom;
		for (int i = 0; i < rings; i++) {
			double t1 = lat0 + (Math.PI / 2 - lat0) * i / rings, t2 = lat0 + (Math.PI / 2 - lat0) * (i + 1) / rings;
			float y1 = (float) Math.sin(t1), y2 = (float) Math.sin(t2), c1 = (float) Math.cos(t1), c2 = (float) Math.cos(t2);
			float v1 = sprite.getFrameV(1 - (i + 1f) / rings), v2 = sprite.getFrameV(1 - (float) i / rings);
			for (int j = 0; j < segments; j++) {
				double p1 = 2 * Math.PI * j / segments, p2 = 2 * Math.PI * (j + 1) / segments;
				float s1 = (float) Math.sin(p1), k1 = (float) Math.cos(p1), s2 = (float) Math.sin(p2), k2 = (float) Math.cos(p2);
				float u1 = sprite.getFrameU((float) j / segments), u2 = sprite.getFrameU((j + 1f) / segments);
				vertex(vc, m, entry, cx + rx * c2 * k1, cy + ry * y2, cz + rz * c2 * s1, cr, cg, cb, a, u1, v1, c2 * k1, y2, c2 * s1, light, overlay);
				vertex(vc, m, entry, cx + rx * c2 * k2, cy + ry * y2, cz + rz * c2 * s2, cr, cg, cb, a, u2, v1, c2 * k2, y2, c2 * s2, light, overlay);
				vertex(vc, m, entry, cx + rx * c1 * k2, cy + ry * y1, cz + rz * c1 * s2, cr, cg, cb, a, u2, v2, c1 * k2, y1, c1 * s2, light, overlay);
				vertex(vc, m, entry, cx + rx * c1 * k1, cy + ry * y1, cz + rz * c1 * s1, cr, cg, cb, a, u1, v2, c1 * k1, y1, c1 * s1, light, overlay);
			}
		}
		if (bottom > 0) {
			float yb = cy + ry * (float) Math.sin(lat0), rb = (float) Math.cos(lat0);
			matrices.push();
			disc(vc, matrices, cx, yb, cz, Math.max(rx, rz) * rb, segments, argb, sprite, false, light, overlay);
			matrices.pop();
		}
	}

	private static void quad(VertexConsumer vc, Matrix4f m, MatrixStack.Entry entry, Sprite sprite, int r, int g, int b, int a, float shade,
							 float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
							 float nx, float ny, float nz, float u1, float v1, float u2, float v2, int light, int overlay) {
		float w = Math.min(16, Math.abs(u2 - u1)), h = Math.min(16, Math.abs(v2 - v1));
		float su = wrap(Math.min(u1, u2)), sv = wrap(Math.min(v1, v2));
		if (su + w > 16) su = 16 - w;
		if (sv + h > 16) sv = 16 - h;
		float minU = sprite.getFrameU(su / 16f), maxU = sprite.getFrameU((su + w) / 16f);
		float minV = sprite.getFrameV(sv / 16f), maxV = sprite.getFrameV((sv + h) / 16f);
		int sr = Math.round(r * shade), sg = Math.round(g * shade), sb = Math.round(b * shade);
		vc.vertex(m, x1, y1, z1).color(sr, sg, sb, a).texture(minU, minV).overlay(overlay).light(light).normal(entry, nx, ny, nz);
		vc.vertex(m, x2, y2, z2).color(sr, sg, sb, a).texture(minU, maxV).overlay(overlay).light(light).normal(entry, nx, ny, nz);
		vc.vertex(m, x3, y3, z3).color(sr, sg, sb, a).texture(maxU, maxV).overlay(overlay).light(light).normal(entry, nx, ny, nz);
		vc.vertex(m, x4, y4, z4).color(sr, sg, sb, a).texture(maxU, minV).overlay(overlay).light(light).normal(entry, nx, ny, nz);
	}

	private static float wrap(float v) {
		return ((v % 16) + 16) % 16;
	}
}
