package com.bakery.client;

import com.bakery.network.OrdersPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The latest orders from nearby counters, drawn as floating wishes above each customer's head. */
public final class ClientOrders {
	private record Batch(List<OrdersPayload.OrderView> orders, long received) {
	}

	private static final Map<BlockPos, Batch> BATCHES = new HashMap<>();
	private static long ticks;

	private ClientOrders() {
	}

	public static void receive(OrdersPayload payload) {
		BATCHES.put(payload.counter(), new Batch(payload.orders(), ticks));
	}

	public static void tick() {
		ticks++;
		BATCHES.values().removeIf(b -> ticks - b.received() > 60);
	}

	public static List<OrdersPayload.OrderView> all() {
		List<OrdersPayload.OrderView> list = new ArrayList<>();
		BATCHES.values().forEach(b -> list.addAll(b.orders()));
		return list;
	}

	public static void renderBubbles(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, float delta) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || BATCHES.isEmpty() || consumers == null) {
			return;
		}
		Vec3d cam = camera.getPos();
		TextRenderer text = client.textRenderer;

		for (OrdersPayload.OrderView order : all()) {
			Entity entity = client.world.getEntityById(order.entityId());
			if (entity == null || entity.squaredDistanceTo(cam) > 32 * 32) {
				continue;
			}
			Vec3d pos = entity.getLerpedPos(delta);
			float bob = MathHelper.sin((ticks + delta) * 0.12f + order.entityId()) * 0.04f;
			matrices.push();
			// Above the name tag (which floats half a block over the head): the wish, with the patience hearts under it.
			matrices.translate(pos.x - cam.x, pos.y - cam.y + entity.getHeight() + 1.18 + bob, pos.z - cam.z);
			matrices.multiply(camera.getRotation());

			// The wished-for item, gently spinning.
			matrices.push();
			matrices.scale(0.42f, 0.42f, 0.42f);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin((ticks + delta) * 0.05f) * 18f));
			client.getItemRenderer().renderItem(order.wanted(), ModelTransformationMode.FIXED, LightmapTextureManager.MAX_LIGHT_COORDINATE,
					OverlayTexture.DEFAULT_UV, matrices, consumers, client.world, order.entityId());
			matrices.pop();

			// Patience as little hearts underneath, still clear of the name tag.
			matrices.translate(0, -0.27, 0);
			matrices.scale(0.02f, -0.02f, 0.02f);
			int hearts = MathHelper.ceil(order.patience() * 5);
			boolean urgent = order.patience() < 0.3f && (ticks / 5) % 2 == 0;
			StringBuilder full = new StringBuilder();
			for (int i = 0; i < 5; i++) full.append('♥');
			float x = -text.getWidth(full.toString()) / 2f;
			for (int i = 0; i < 5; i++) {
				int color = i < hearts ? (urgent ? 0xFFFFD34D : 0xFFFF6F91) : 0x80FFFFFF;
				text.draw("♥", x + i * text.getWidth("♥"), 0, color, true, matrices.peek().getPositionMatrix(), consumers,
						TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
			}
			matrices.pop();
		}
	}
}
