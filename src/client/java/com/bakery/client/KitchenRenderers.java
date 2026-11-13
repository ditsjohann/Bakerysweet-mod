package com.bakery.client;

import com.bakery.baking.BakingRecipes;
import com.bakery.block.MixingBowlBlockEntity;
import com.bakery.block.PastryBoardBlockEntity;
import com.bakery.food.BakeryFoods;
import com.bakery.food.Flavor;
import com.bakery.kitchen.ChurnBlockEntity;
import com.bakery.kitchen.JamPotBlockEntity;
import com.bakery.kitchen.JuicerBlockEntity;
import com.bakery.kitchen.KitchenBlock;
import com.bakery.kitchen.ProofingBasketBlockEntity;
import com.bakery.oven.FountainBlockEntity;
import com.bakery.oven.MachineBlock;
import com.bakery.oven.MillstoneBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import java.util.List;

/**
 * What you see inside the kitchen machines: the fruit on the juicer's reamer squashing flat as you pull the
 * lever while the jug fills up, cream rising and turning pink in the churn as the crank goes round, fruit
 * bobbing in bubbling jam, dough swelling in its basket, ingredients swirling into batter in the bowl, the
 * millstone's runner turning, and chocolate pouring down the fountain.
 */
public final class KitchenRenderers {
	private KitchenRenderers() {
	}

	private static float time(World world, float tickDelta) {
		return world == null ? 0 : world.getTime() + tickDelta;
	}

	/** 0 to 1 and back over {@code ticks} after {@code since}; 0 otherwise. */
	private static float pulse(World world, long since, float tickDelta, float ticks) {
		if (world == null) return 0;
		float t = (world.getTime() - since + tickDelta) / ticks;
		return t >= 0 && t < 1 ? MathHelper.sin(t * MathHelper.PI) : 0;
	}

	private static Direction facing(BlockEntity be) {
		BlockState state = be.getCachedState();
		if (state.contains(KitchenBlock.FACING)) return state.get(KitchenBlock.FACING);
		if (state.contains(MachineBlock.FACING)) return state.get(MachineBlock.FACING);
		return Direction.NORTH;
	}

	private static void item(ItemStack stack, MatrixStack matrices, VertexConsumerProvider consumers, World world, int light, int seed) {
		MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
				matrices, consumers, world, seed);
	}

	private static int mix(int a, int b, float t) {
		t = MathHelper.clamp(t, 0, 1);
		int r = Math.round(MathHelper.lerp(t, a >> 16 & 255, b >> 16 & 255));
		int g = Math.round(MathHelper.lerp(t, a >> 8 & 255, b >> 8 & 255));
		int bl = Math.round(MathHelper.lerp(t, a & 255, b & 255));
		return r << 16 | g << 8 | bl;
	}

	// ------------------------------------------------------------------ juicer

	public static class Juicer implements BlockEntityRenderer<JuicerBlockEntity> {
		public Juicer(BlockEntityRendererFactory.Context context) {
		}

		@Override
		public void render(JuicerBlockEntity juicer, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
			World world = juicer.getWorld();
			float push = pulse(world, juicer.pressedAt(), tickDelta, 7);
			matrices.push();
			PixelBox.orient(matrices, facing(juicer));

			// The lever dips down with every press.
			matrices.push();
			PixelBox.rotateAround(matrices, 11.5f, 10.5f, 8, RotationAxis.POSITIVE_Z, -32 * push);
			PixelBox.part("juicer_lever", juicer.getCachedState(), matrices, consumers, light, overlay);
			matrices.pop();

			int color = juicer.flavor().color();
			// Juice in the jug, rising two glasses per fruit.
			if (juicer.juice() > 0) {
				float top = 3.2f + juicer.juice() * 1.55f;
				VertexConsumer glass = PixelBox.translucent(consumers);
				Sprite liquid = PixelBox.sprite("juice_liquid");
				PixelBox.tube(glass, matrices, 8, 8, 3.55f, 3.1f, top, 16, 0xD0000000 | color, liquid, light, overlay);
				PixelBox.disc(glass, matrices, 8, top, 8, 3.55f, 16, 0xD0000000 | color, liquid, true, light, overlay);
			}
			// A trickle of juice for a moment after each press.
			if (juicer.loaded() && push > 0) {
				float bottom = 3.2f + juicer.juice() * 1.55f;
				PixelBox.box(PixelBox.translucent(consumers), matrices, 7.6f, bottom, 7.6f, 8.4f, 10.5f, 8.4f, 0xE0000000 | color,
						PixelBox.sprite("juice_liquid"), PixelBox.sprite("juice_liquid"), light, overlay);
			}
			// The fruit on the reamer, squashing flatter with each press.
			if (juicer.loaded() && !juicer.fruit().isEmpty()) {
				float squash = juicer.presses() / 5f;
				matrices.push();
				matrices.translate(0.5, 13.2 / 16, 0.5);
				matrices.scale(1 + squash * 0.35f, 1 - squash * 0.55f - push * 0.1f, 1 + squash * 0.35f);
				matrices.translate(0, 0.17, 0);
				matrices.scale(0.34f, 0.34f, 0.34f);
				item(juicer.fruit(), matrices, consumers, world, light, 0);
				matrices.pop();
			}
			matrices.pop();
		}
	}

	// ------------------------------------------------------------------ ice cream churn

	public static class Churn implements BlockEntityRenderer<ChurnBlockEntity> {
		public Churn(BlockEntityRendererFactory.Context context) {
		}

		@Override
		public void render(ChurnBlockEntity churn, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
			World world = churn.getWorld();
			matrices.push();
			PixelBox.orient(matrices, facing(churn));
			float since = world == null ? 99 : world.getTime() - churn.churnedAt() + tickDelta;
			float turn = churn.churns() * 90 + (since < 8 ? -90 * (1 - since / 8f) : 0);
			matrices.push();
			PixelBox.rotateAround(matrices, 14, 8.5f, 8, RotationAxis.POSITIVE_X, turn);
			PixelBox.part("churn_crank", churn.getCachedState(), matrices, consumers, light, overlay);
			matrices.pop();

			VertexConsumer vc = PixelBox.solid(consumers);
			Sprite cream = PixelBox.sprite("ice_cream_block");
			float progress = churn.churns() / 12f;
			if (churn.hasCream() || churn.churns() > 0) {
				int color = mix(0xFFF8EE, churn.flavor().color(), progress * 0.9f + 0.1f);
				float top = 2.5f + progress * 5.5f;
				PixelBox.box(vc, matrices, 4, 1, 4, 12, top, 12, color, cream, cream, light, overlay);
				// A soft swirl on top that turns with the crank.
				matrices.push();
				PixelBox.rotateAround(matrices, 8, 0, 8, RotationAxis.POSITIVE_Y, turn * 0.5f);
				PixelBox.box(vc, matrices, 6, top, 6, 10, top + 0.8f + progress, 10, mix(color, 0xFFFFFF, 0.25f), cream, cream, light, overlay);
				matrices.pop();
			}
			if (churn.hasIce() && churn.churns() < 4) {
				Sprite ice = PixelBox.sprite("minecraft:block/ice");
				VertexConsumer glass = PixelBox.translucent(consumers);
				float bob = MathHelper.sin(time(world, tickDelta) * 0.1f) * 0.2f;
				PixelBox.box(glass, matrices, 5, 2.4f + bob, 5, 7, 4.2f + bob, 7, 0xC0FFFFFF, ice, ice, light, overlay);
				PixelBox.box(glass, matrices, 9, 2.2f - bob, 8.5f, 10.8f, 3.9f - bob, 10.3f, 0xC0FFFFFF, ice, ice, light, overlay);
			}
			if (churn.hasSugar() && churn.churns() < 4) {
				Sprite sugar = PixelBox.sprite("cake_piping");
				float y = churn.hasCream() ? 2.6f : 1.1f;
				for (int i = 0; i < 6; i++) {
					float x = 5.5f + (i * 37 % 50) / 10f, z = 5.5f + (i * 23 % 50) / 10f;
					PixelBox.box(vc, matrices, x, y, z, x + 0.6f, y + 0.4f, z + 0.6f, 0xFFFFFF, sugar, sugar, light, overlay);
				}
			}
			matrices.pop();
		}
	}

	// ------------------------------------------------------------------ jam pot

	public static class JamPot implements BlockEntityRenderer<JamPotBlockEntity> {
		public JamPot(BlockEntityRendererFactory.Context context) {
		}

		@Override
		public void render(JamPotBlockEntity pot, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
			World world = pot.getWorld();
			float time = time(world, tickDelta);
			matrices.push();
			PixelBox.orient(matrices, facing(pot));

			// The spoon sweeps round the pot after each stir, then rests.
			float since = world == null ? 99 : world.getTime() - pot.stirredAt() + tickDelta;
			float sweep = (pot.stirredAt() * 47 % 360) + Math.min(since, 14) / 14f * 140;
			matrices.push();
			PixelBox.rotateAround(matrices, 8, 0, 8, RotationAxis.POSITIVE_Y, sweep);
			PixelBox.part("jam_spoon", pot.getCachedState(), matrices, consumers, light, overlay);
			matrices.pop();

			VertexConsumer vc = PixelBox.solid(consumers);
			Sprite jam = PixelBox.sprite("jam_surface");
			int color = pot.flavor().color();
			switch (pot.stage()) {
				case JamPotBlockEntity.FILLING -> {
					// Fruit bobbing in the bottom of the pot.
					for (int i = 0; i < pot.fruit() && !pot.fruitType().isEmpty(); i++) {
						matrices.push();
						float bob = MathHelper.sin(time * 0.08f + i * 2) * 0.02f;
						matrices.translate((6 + i * 2.2f) / 16f, 2.4f / 16 + bob, (7 + (i % 2) * 2.5f) / 16f);
						matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 70));
						matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70));
						matrices.scale(0.3f, 0.3f, 0.3f);
						item(pot.fruitType(), matrices, consumers, world, light, i);
						matrices.pop();
					}
					if (pot.sugar()) {
						Sprite sugar = PixelBox.sprite("cake_piping");
						PixelBox.blob(vc, matrices, 8, 1.0f, 8, 3.6f, 0.9f, 3.6f, 3, 14, 0.5f, 0xFFFAF4, sugar, light, overlay);
					}
				}
				case JamPotBlockEntity.COOKING -> {
					// Jam cooking down: it thickens and darkens if you forget to stir.
					int c = mix(color, 0x5A2A20, pot.scorch() * 0.55f);
					float level = 3.5f + pot.cookProgress() * 2.5f + MathHelper.sin(time * 0.4f) * 0.15f;
					PixelBox.disc(vc, matrices, 8, level, 8, 5.05f, 20, c, jam, true, LightmapTextureManager.pack(Math.max(6, LightmapTextureManager.getBlockLightCoordinates(light)), LightmapTextureManager.getSkyLightCoordinates(light)), overlay);
					// Big slow bubbles.
					for (int i = 0; i < 3; i++) {
						float phase = (time * 0.05f + i * 0.33f) % 1f;
						float size = phase * 1.6f;
						float x = 5.5f + (i * 31 % 40) / 10f, z = 5.5f + (i * 17 % 40) / 10f;
						PixelBox.blob(vc, matrices, x, level - 0.1f, z, size / 2 + 0.05f, size * 0.45f + 0.05f, size / 2 + 0.05f, 3, 8, 0.5f, mix(c, 0xFFFFFF, 0.2f), jam, light, overlay);
					}
				}
				case JamPotBlockEntity.DONE -> {
					float level = 2.5f + pot.servings() * 1.3f;
					PixelBox.disc(vc, matrices, 8, level, 8, 5.05f, 20, mix(color, 0xFFFFFF, 0.1f), jam, true, light, overlay);
				}
				case JamPotBlockEntity.BURNT -> PixelBox.disc(vc, matrices, 8, 4.5f, 8, 5.05f, 20, 0x2E1A16, jam, true, light, overlay);
				default -> {
				}
			}
			matrices.pop();
		}
	}

	// ------------------------------------------------------------------ proofing basket

	public static class Proofing implements BlockEntityRenderer<ProofingBasketBlockEntity> {
		public Proofing(BlockEntityRendererFactory.Context context) {
		}

		@Override
		public void render(ProofingBasketBlockEntity basket, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
			ItemStack dough = basket.dough();
			if (dough.isEmpty()) return;
			long elapsed = basket.elapsed();
			boolean collapsed = elapsed > ProofingBasketBlockEntity.COLLAPSE;
			float rise = collapsed ? 0.15f : MathHelper.clamp((elapsed + tickDelta) / ProofingBasketBlockEntity.RISEN, 0, 1);
			// Breathing gently while it rises.
			float breathe = collapsed ? 0 : MathHelper.sin(((basket.getWorld() == null ? 0 : basket.getWorld().getTime()) + tickDelta) * 0.05f) * 0.08f;
			int color = BakeryFoods.flavor(dough) == Flavor.PLAIN ? 0xF6E3C0 : mix(0xF6E3C0, BakeryFoods.flavor(dough).color(), 0.45f);
			Sprite sprite = PixelBox.sprite("proof_dough");
			VertexConsumer vc = PixelBox.solid(consumers);
			float grow = rise * 1.2f;
			float h = 2.2f + rise * 2.2f + breathe;
			PixelBox.box(vc, matrices, 4 - grow, 1, 4 - grow, 12 + grow, 1 + h, 12 + grow, color, sprite, sprite, light, overlay);
			float d = 1.5f - grow * 0.5f;
			PixelBox.box(vc, matrices, 4 + d, 1 + h, 4 + d, 12 - d, 1.6f + h + rise * 0.9f, 12 - d, mix(color, 0xFFFFFF, 0.12f), sprite, sprite, light, overlay);
		}
	}

	// ------------------------------------------------------------------ mixing bowl

	public static class Bowl implements BlockEntityRenderer<MixingBowlBlockEntity> {
		public Bowl(BlockEntityRendererFactory.Context context) {
		}

		@Override
		public void render(MixingBowlBlockEntity bowl, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
			List<ItemStack> contents = bowl.getContents();
			if (contents.isEmpty()) return;
			World world = bowl.getWorld();
			float since = world == null ? 99 : world.getTime() - bowl.stirredAt() + tickDelta;
			float progress = bowl.getStirs() / (float) BakingRecipes.STIRS_NEEDED;
			float swirl = bowl.getStirs() * 50 + Math.min(since, 10) / 10f * 50;
			Flavor flavor = BakingRecipes.flavorOf(contents);
			int batter = flavor == Flavor.PLAIN ? 0xF4E6C8 : mix(0xF4E6C8, flavor.color(), 0.6f);
			Sprite sprite = PixelBox.sprite("proof_dough");
			// Batter forming as you stir.
			float h = 1.6f + progress * 2.6f;
			if (progress > 0) {
				PixelBox.box(PixelBox.solid(consumers), matrices, 3.5f, 1.4f, 3.5f, 12.5f, h, 12.5f, batter, sprite, sprite, light, overlay);
			}
			// Ingredients swirling round, sinking into the batter.
			float scale = 0.3f * (1 - progress * 0.75f);
			for (int i = 0; i < contents.size(); i++) {
				matrices.push();
				matrices.translate(0.5, (h + 0.3f) / 16, 0.5);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(swirl + i * 360f / contents.size()));
				matrices.translate(0, 0, 3.2 / 16);
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(80));
				matrices.scale(scale, scale, scale);
				item(contents.get(i), matrices, consumers, world, light, i);
				matrices.pop();
			}
		}
	}

	// ------------------------------------------------------------------ pastry board

	public static class Board implements BlockEntityRenderer<PastryBoardBlockEntity> {
		public Board(BlockEntityRendererFactory.Context context) {
		}

		@Override
		public void render(PastryBoardBlockEntity board, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
			ItemStack dough = board.getDough();
			if (dough.isEmpty()) return;
			Flavor flavor = BakeryFoods.flavor(dough);
			int color = flavor == Flavor.PLAIN ? 0xF4E2BE : mix(0xF4E2BE, flavor.color(), 0.5f);
			Sprite sprite = PixelBox.sprite("proof_dough");
			VertexConsumer vc = PixelBox.solid(consumers);
			PixelBox.box(vc, matrices, 4, 1, 5, 12, 2.6f, 11, color, sprite, sprite, light, overlay);
			PixelBox.box(vc, matrices, 5, 2.6f, 6, 11, 3.4f, 10, mix(color, 0xFFFFFF, 0.1f), sprite, sprite, light, overlay);
			// A dusting of flour around it.
			Sprite flour = PixelBox.sprite("cake_piping");
			PixelBox.box(vc, matrices, 2.5f, 1, 3.5f, 13.5f, 1.05f, 12.5f, 0xFFFFFF, flour, flour, light, overlay);
		}
	}

	// ------------------------------------------------------------------ grand millstone

	public static class Millstone implements BlockEntityRenderer<MillstoneBlockEntity> {
		public Millstone(BlockEntityRendererFactory.Context context) {
		}

		@Override
		public void render(MillstoneBlockEntity mill, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
			matrices.push();
			PixelBox.orient(matrices, facing(mill));
			float angle = mill.renderAngle(tickDelta);
			matrices.push();
			PixelBox.rotateAround(matrices, 8, 0, 8, RotationAxis.POSITIVE_Y, angle);
			PixelBox.part("millstone_runner", mill.getCachedState(), matrices, consumers, light, overlay);
			// Whatever is being ground sits in a little heap on the runner, going round with it.
			ItemStack input = mill.getInput();
			int shown = Math.min(5, (input.getCount() + 3) / 4);
			for (int i = 0; i < shown; i++) {
				matrices.push();
				matrices.translate(0.5, 10.2 / 16, 0.5);
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 72 + 20));
				matrices.translate(0, 0, 5.5 / 16);
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
				matrices.scale(0.42f, 0.42f, 0.42f);
				item(input, matrices, consumers, mill.getWorld(), light, i);
				matrices.pop();
			}
			matrices.pop();
			matrices.pop();
		}

		@Override
		public boolean rendersOutsideBoundingBox(MillstoneBlockEntity mill) {
			return true;
		}
	}

	// ------------------------------------------------------------------ chocolate fountain

	public static class Fountain implements BlockEntityRenderer<FountainBlockEntity> {
		public Fountain(BlockEntityRendererFactory.Context context) {
		}

		@Override
		public void render(FountainBlockEntity fountain, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
			int choc = fountain.getChocolate();
			if (choc <= 0) return;
			float time = time(fountain.getWorld(), tickDelta);
			VertexConsumer vc = PixelBox.solid(consumers);
			Sprite flow = PixelBox.sprite("fountain/chocolate_flow");
			Sprite pool = PixelBox.sprite("fountain/chocolate_pool");
			int c = 0xFFFFFF;
			float full = choc / (float) FountainBlockEntity.MAX_CHOCOLATE;
			// The basin fills up with melted chocolate.
			float level = 3.05f + full * 1.4f;
			PixelBox.disc(vc, matrices, 8, level, 8, 6.7f, 24, c, pool, true, light, overlay);
			// Chocolate wells up out of the crown, coats each tier and pours over its gold lip in a smooth round curtain.
			float well = MathHelper.sin(time * 0.3f) * 0.25f;
			PixelBox.blob(vc, matrices, 8, 21.6f + well, 8, 1.35f, 0.9f, 1.35f, 4, 12, 0.5f, c, flow, light, overlay);
			PixelBox.tube(vc, matrices, 8, 8, 1.3f, 19.0f, 21.8f + well, 12, c, flow, light, overlay);
			// Each tier: a glossy pool inside its gold lip, and a curtain pouring from just under the lip.
			sheet(vc, matrices, 2.1f, 19.05f, 2.85f, 18.8f, 14.95f, flow, pool, light, overlay, time, 0);
			sheet(vc, matrices, 3.6f, 14.75f, 4.45f, 14.4f, 9.45f, flow, pool, light, overlay, time, 1);
			sheet(vc, matrices, 5.2f, 9.25f, 6.15f, 8.8f, level, flow, pool, light, overlay, time, 2);
		}

		/**
		 * A tier holding a glossy pool of chocolate (radius {@code poolR} at {@code poolY}, inside the gold lip) and the
		 * round curtain pouring off it (radius {@code curtainR}) from {@code top} down to {@code to}.
		 */
		private static void sheet(VertexConsumer vc, MatrixStack matrices, float poolR, float poolY, float curtainR, float top, float to, Sprite flow, Sprite pool,
								  int light, int overlay, float time, int tier) {
			int c = 0xFFFFFF;
			PixelBox.disc(vc, matrices, 8, poolY, 8, poolR, 20, c, pool, true, light, overlay);
			// The curtain flares out a touch as it falls, with a gentle ripple.
			float ripple = MathHelper.sin(time * 0.25f + tier) * 0.08f;
			float mid = (top + to) / 2;
			PixelBox.tube(vc, matrices, 8, 8, curtainR + ripple, mid, top, 20, c, flow, light, overlay);
			PixelBox.tube(vc, matrices, 8, 8, curtainR + 0.25f + ripple, to, mid + 0.05f, 20, c, flow, light, overlay);
		}

		@Override
		public boolean rendersOutsideBoundingBox(FountainBlockEntity fountain) {
			return true;
		}
	}

}
