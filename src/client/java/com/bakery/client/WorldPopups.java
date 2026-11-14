package com.bakery.client;

import com.bakery.food.Flavor;
import com.bakery.network.BakeResultPayload;
import com.bakery.network.ReviewPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Little floating cards in the world instead of a HUD: a customer's review pops up over their head,
 * and a fresh bake's grade and stars rise out of the oven mouth.
 */
public final class WorldPopups {
	private static final int DURATION = 140;

	private record Popup(int entity, Vec3d pos, ItemStack item, int stars, Text title, List<Text> lines, long born) {
	}

	private static final List<Popup> POPUPS = new ArrayList<>();
	private static long ticks;

	private WorldPopups() {
	}

	public static void tick() {
		ticks++;
		POPUPS.removeIf(p -> ticks - p.born() > DURATION);
	}

	public static void review(ReviewPayload review) {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.translatable(review.comment()).formatted(Formatting.ITALIC));
		if (review.coins() > 0) lines.add(Text.translatable("popup.bakery.coins", review.coins()).withColor(0xE0568A));
		if (!"loved".equals(review.favorite())) {
			lines.add(Text.translatable("popup.bakery.hint", Flavor.byName(review.favorite()).displayName()).withColor(0x9C7C88));
		} else {
			lines.add(Text.translatable("popup.bakery.loved").withColor(0xE0568A));
		}
		if (review.hearts() >= 0) lines.add(SweetUi.hearts(review.hearts(), com.bakery.sweet.Friendship.MAX));
		Text title = review.customer().isEmpty() ? review.food().getName() : Text.literal(review.customer()).withColor(0xD0567E);
		POPUPS.removeIf(p -> p.entity() == review.entity());
		POPUPS.add(new Popup(review.entity(), null, review.food(), review.stars(), title, lines, ticks));
	}

	public static void bake(BakeResultPayload result) {
		List<Text> lines = new ArrayList<>();
		lines.add(Text.translatable("screen.bakery.bake.breakdown", com.bakery.food.BakeryFoods.stars(result.prep()),
				com.bakery.food.BakeryFoods.stars(result.timing()), com.bakery.food.BakeryFoods.stars(result.heat())));
		Text title = Text.translatable("grade.bakery." + result.stars()).withColor(switch (result.stars()) {
			case 5 -> 0xF0A800;
			case 4 -> 0xE0568A;
			case 3 -> 0x5DAA55;
			default -> 0xB07060;
		});
		POPUPS.add(new Popup(-1, Vec3d.ofBottomCenter(result.pos()).add(0, 0.9, 0), result.result(), result.stars(), title, lines, ticks));
	}

	public static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || POPUPS.isEmpty() || context.consumers() == null) return;
		Camera camera = context.camera();
		Vec3d cam = camera.getPos();
		MatrixStack matrices = context.matrixStack();
		VertexConsumerProvider consumers = context.consumers();
		float delta = context.tickCounter().getTickDelta(false);
		TextRenderer text = client.textRenderer;

		Iterator<Popup> it = POPUPS.iterator();
		while (it.hasNext()) {
			Popup popup = it.next();
			Vec3d at;
			if (popup.entity() >= 0) {
				Entity entity = client.world.getEntityById(popup.entity());
				if (entity == null) continue;
				// Lift the card so its lowest line still clears the customer's head (the lines hang below the anchor).
				double card = (14 + popup.lines().size() * 10 + 4) * 0.022;
				at = entity.getLerpedPos(delta).add(0, entity.getHeight() + 0.2 + card, 0);
			} else {
				at = popup.pos();
			}
			float t = ticks - popup.born() + delta;
			float alpha = MathHelper.clamp(Math.min(t / 6f, (DURATION - t) / 15f), 0, 1);
			float rise = Math.min(t, 20) * 0.012f;
			int a = Math.max(5, (int) (alpha * 255)) << 24;

			matrices.push();
			matrices.translate(at.x - cam.x, at.y - cam.y + rise, at.z - cam.z);
			matrices.multiply(camera.getRotation());

			// The dish itself, bobbing.
			matrices.push();
			matrices.translate(0, 0.62, 0);
			float pop = t < 6 ? t / 6f : 1;
			matrices.scale(0.38f * pop, 0.38f * pop, 0.38f * pop);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin(t * 0.08f) * 20));
			client.getItemRenderer().renderItem(popup.item(), ModelTransformationMode.FIXED, LightmapTextureManager.MAX_LIGHT_COORDINATE,
					OverlayTexture.DEFAULT_UV, matrices, consumers, client.world, 0);
			matrices.pop();

			matrices.scale(0.022f, -0.022f, 0.022f);
			// Stars popping in one at a time.
			int shown = Math.min(popup.stars(), (int) ((t - 4) / 4));
			float sx = -5 * 9 / 2f;
			for (int i = 0; i < 5; i++) {
				boolean lit = i < shown;
				float s = lit && (int) ((t - 4) / 4) == i ? 1.4f : 1f;
				matrices.push();
				matrices.translate(sx + i * 9 + 4, 4, 0);
				matrices.scale(s * 1.3f, s * 1.3f, 1);
				text.draw("★", -text.getWidth("★") / 2f, -4, a | (lit ? 0xFFC83D : 0x806070), true, matrices.peek().getPositionMatrix(), consumers,
						TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
				matrices.pop();
			}
			int y = -12;
			drawCentered(text, popup.title(), y, a, 0x99FFF4F8, matrices, consumers);
			y = 14;
			if (shown >= popup.stars()) {
				for (Text line : popup.lines()) {
					drawCentered(text, line, y, a, 0x88FFF4F8, matrices, consumers);
					y += 10;
				}
			}
			matrices.pop();
		}
	}

	private static void drawCentered(TextRenderer text, Text line, int y, int alpha, int background, MatrixStack matrices, VertexConsumerProvider consumers) {
		MutableText copy = line.copy();
		int color = alpha | 0x5A3A48;
		float x = -text.getWidth(copy) / 2f;
		int bg = ((int) (((background >>> 24) & 0xFF) * ((alpha >>> 24) / 255f)) << 24) | (background & 0xFFFFFF);
		text.draw(copy, x, y, color, false, matrices.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, bg,
				LightmapTextureManager.MAX_LIGHT_COORDINATE);
	}
}
