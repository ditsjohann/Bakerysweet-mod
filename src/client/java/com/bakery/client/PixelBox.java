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
