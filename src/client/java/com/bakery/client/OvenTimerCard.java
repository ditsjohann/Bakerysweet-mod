package com.bakery.client;

import com.bakery.baking.BakingRecipes;
import com.bakery.oven.GrandOvenBlock;
import com.bakery.oven.GrandOvenBlockEntity;
import com.bakery.oven.OvenTemperature;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/**
 * A little timer board floating above the oven mouth while you are near it. One row per bake: what it is, a bar from
 * raw to burnt with the flip window (orange) and the golden window marked, a needle for how done it is right now, and
 * plain words for what to do next with a countdown ("Flip in 6s", "FLIP NOW!", "Ready in 12s", "Take it out!").
 */
final class OvenTimerCard {
	private static final double SHOW_DISTANCE = 9;
	private static final int BAR_W = 70, ROW_H = 20;
	private static final float BAR_END = BakingRecipes.BURNT;

	private OvenTimerCard() {
	}

	static void render(GrandOvenBlockEntity oven, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers) {
		MinecraftClient client = MinecraftClient.getInstance();
		List<GrandOvenBlockEntity.Bake> bakes = oven.getBakes();
		if (client.player == null || oven.getWorld() == null || bakes.isEmpty()) return;
		Direction facing = oven.getCachedState().get(GrandOvenBlock.FACING);
		// Just above the oven's front, over the heart (the oven is two blocks tall).
		Vec3d at = Vec3d.ofCenter(oven.getPos()).add(facing.getOffsetX() * 1.1, 1.55, facing.getOffsetZ() * 1.1);
		if (client.player.getPos().squaredDistanceTo(at) > SHOW_DISTANCE * SHOW_DISTANCE) return;

		TextRenderer text = client.textRenderer;
		long time = oven.getWorld().getTime();
		int rows = Math.min(bakes.size(), 6);
		int w = 24 + BAR_W + 62, h = rows * ROW_H + 14;

		matrices.push();
		matrices.translate(at.x - oven.getPos().getX(), at.y - oven.getPos().getY(), at.z - oven.getPos().getZ());
		matrices.multiply(client.gameRenderer.getCamera().getRotation());
		final float scale = 0.0135f;
		// The card's bottom edge sits at the anchor, so more bakes make it grow upward, not into the oven.
		matrices.translate(0, h / 2f * scale, 0);
		matrices.scale(scale, -scale, scale);
		Matrix4f m = matrices.peek().getPositionMatrix();
		// Item and text rendering switch buffers, so quads always ask for theirs fresh.
		Quads quads = new Quads(consumers);
		int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		float left = -w / 2f, top = -h / 2f;

		// Card: cream with a pink rim, and a header with the oven setting.
		rect(quads, m, left - 1, top - 1, left + w + 1, top + h + 1, 0xF0F7B6CF, 0.02f);
		rect(quads, m, left, top, left + w, top + h, 0xE8FFF7F0, 0.01f);
		OvenTemperature setting = oven.getTemperature();
		Text header = Text.translatable("oven.bakery.card.header", setting.displayName());
		if (oven.isRackOut()) header = Text.translatable("oven.bakery.card.rack_out");
		else if (oven.getFuel() <= 0) header = Text.translatable("oven.bakery.card.no_fuel");
		text.draw(header, left + 4, top + 3, 0xFFD0567E, false, m, consumers, TextRenderer.TextLayerType.NORMAL, 0, light);

		for (int i = 0; i < rows; i++) {
			GrandOvenBlockEntity.Bake bake = bakes.get(i);
			float y = top + 13 + i * ROW_H;
			Status status = status(oven, bake, tickDelta, time);
			// Item icon
			matrices.push();
			matrices.translate(left + 10, y + 8, -0.02f);
			matrices.scale(14, -14, 0.01f);
			client.getItemRenderer().renderItem(bake.stack, ModelTransformationMode.GUI, light, OverlayTexture.DEFAULT_UV, matrices, consumers, oven.getWorld(), i);
			matrices.pop();
			m = matrices.peek().getPositionMatrix();
			// Bar
			float bx = left + 20, by = y + 2;
			drawBar(quads, m, oven, bake, bx, by, tickDelta);
			// Words under the bar
			int color = status.urgent && (time / 6) % 2 == 0 ? 0xFFE0405A : status.color;
			text.draw(status.label, bx, by + 8, color, false, m, consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
		}
		matrices.pop();
	}

	private record Status(Text label, int color, boolean urgent) {
	}

	/** What to do next with this bake, and how long until it matters. */
	private static Status status(GrandOvenBlockEntity oven, GrandOvenBlockEntity.Bake bake, float tickDelta, long time) {
		int bakeTime = GrandOvenBlockEntity.bakeTime(bake.stack);
		if (bakeTime == 0) return new Status(Text.translatable("oven.bakery.card.burnt"), 0xFF6A4A3A, false);
		OvenTemperature t = oven.getTemperature();
		float d = oven.doneness(bake, tickDelta);
		boolean baking = oven.isBaking(bake);
		boolean flip = GrandOvenBlockEntity.needsFlip(bake.stack) && !bake.flipped;
		if (flip && d >= GrandOvenBlockEntity.FLIP_FROM && d <= GrandOvenBlockEntity.FLIP_UNTIL) {
			return new Status(Text.translatable("oven.bakery.card.flip_now", seconds(GrandOvenBlockEntity.FLIP_UNTIL - d, bakeTime, t)), 0xFFE07A20, true);
		}
		if (flip && d < GrandOvenBlockEntity.FLIP_FROM) {
			return paused(baking, Text.translatable("oven.bakery.card.flip_in", seconds(GrandOvenBlockEntity.FLIP_FROM - d, bakeTime, t)), 0xFFE07A20);
		}
		if (d < t.perfectStart()) {
			Text label = Text.translatable(bake.flipped ? "oven.bakery.card.flipped_ready_in" : "oven.bakery.card.ready_in",
					seconds(t.perfectStart() - d, bakeTime, t));
			return paused(baking, label, 0xFF5A3A46);
		}
		if (d <= t.perfectEnd()) {
			return new Status(Text.translatable("oven.bakery.card.take_out", seconds(t.perfectEnd() - d, bakeTime, t)), 0xFFC89400, true);
		}
		return new Status(Text.translatable("oven.bakery.card.burning"), 0xFFB04030, true);
	}

	private static Status paused(boolean baking, Text label, int color) {
		return baking ? new Status(label, color, false) : new Status(Text.translatable("oven.bakery.card.paused", label), 0xFF9C8C94, false);
	}

	/** Real seconds for a stretch of doneness, at this oven setting. */
	private static int seconds(float doneness, int bakeTime, OvenTemperature t) {
		return Math.max(1, Math.round(doneness * bakeTime / t.speed() / 20f));
	}

	private static void drawBar(Quads quads, Matrix4f m, GrandOvenBlockEntity oven, GrandOvenBlockEntity.Bake bake, float x, float y, float tickDelta) {
		OvenTemperature t = oven.getTemperature();
		float h = 6;
		rect(quads, m, x - 1, y - 1, x + BAR_W + 1, y + h + 1, 0xFF8A6A76, 0.006f);
		// raw (cream) -> golden window -> darker -> burnt
		zone(quads, m, x, y, h, 0, t.perfectStart(), 0xFFF3E4D0);
		zone(quads, m, x, y, h, t.perfectStart(), t.perfectEnd(), 0xFFF2C94C);
		zone(quads, m, x, y, h, t.perfectEnd(), BAR_END - 0.15f, 0xFFB07850);
		zone(quads, m, x, y, h, BAR_END - 0.15f, BAR_END, 0xFF4A3028);
		if (GrandOvenBlockEntity.needsFlip(bake.stack)) {
			zone(quads, m, x, y + 1, h - 2, GrandOvenBlockEntity.FLIP_FROM, GrandOvenBlockEntity.FLIP_UNTIL, bake.flipped ? 0xFF9AD08A : 0xFFFF9A4D);
		}
		// Needle
		float d = Math.min(oven.doneness(bake, tickDelta), BAR_END);
		float nx = x + d / BAR_END * BAR_W;
		rect(quads, m, nx - 0.8f, y - 2.5f, nx + 0.8f, y + h + 2.5f, 0xFFD0304A, 0.004f);
	}

	private static void zone(Quads quads, Matrix4f m, float x, float y, float h, float from, float to, int color) {
		rect(quads, m, x + from / BAR_END * BAR_W, y, x + to / BAR_END * BAR_W, y + h, color, 0.005f);
	}

	private record Quads(VertexConsumerProvider consumers) {
	}

	/** A flat coloured quad (z pulls it toward the viewer, so later layers sit on top). */
	private static void rect(Quads quads, Matrix4f m, float x1, float y1, float x2, float y2, int argb, float z) {
		VertexConsumer vc = quads.consumers().getBuffer(RenderLayer.getTextBackground());
		int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		// Both windings, so the card shows whichever way the matrices ended up facing.
		vc.vertex(m, x1, y1, -z).color(argb).light(light);
		vc.vertex(m, x1, y2, -z).color(argb).light(light);
		vc.vertex(m, x2, y2, -z).color(argb).light(light);
		vc.vertex(m, x2, y1, -z).color(argb).light(light);
		vc.vertex(m, x2, y1, -z).color(argb).light(light);
		vc.vertex(m, x2, y2, -z).color(argb).light(light);
		vc.vertex(m, x1, y2, -z).color(argb).light(light);
		vc.vertex(m, x1, y1, -z).color(argb).light(light);
	}
}
