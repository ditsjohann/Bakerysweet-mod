package com.bakery.client;

import com.bakery.registry.BakeryItems;
import com.bakery.sweet.CakeDesign;
import com.bakery.sweet.CakeGeometry;
import com.bakery.sweet.IcingBagItem;
import com.bakery.sweet.TurntableBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.List;

/**
 * The cake on the Cake Turntable, turned to wherever the plate was last spun. While you hold a decorating tool and
 * look at the cake, a see-through preview shows exactly what a click will do, right where you aim (traced onto the
 * real cake shape): the icing it will pipe with a little crosshair ring, the scraper's reach, or the topper bobbing
 * where it will land.
 */
public class TurntableRenderer implements BlockEntityRenderer<TurntableBlockEntity> {
	private static final int FULL = LightmapTextureManager.MAX_LIGHT_COORDINATE;

	public TurntableRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(TurntableBlockEntity table, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		ItemStack cake = table.getCake();
		if (cake.isEmpty()) return;
		matrices.push();
		matrices.translate(0.5, TurntableBlockEntity.PLATE / 16f, 0.5);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(table.renderAngle(tickDelta)));
		CakeRenderer.render(cake, matrices, consumers, light, overlay);
		preview(table, tickDelta, matrices, consumers);
		matrices.pop();
	}

	private static void preview(TurntableBlockEntity table, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || !(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
				|| !hit.getBlockPos().equals(table.getPos())) {
			return;
		}
		ItemStack held = client.player.getMainHandStack();
		float[] aim = table.aim(client.player, tickDelta);
		if (aim == null) return;
		CakeDesign design = table.design();
		boolean top = aim[4] > 0.5f;
		int tier = (int) aim[3];
		boolean sneaking = client.player.isSneaking();
		float time = (client.world == null ? 0 : client.world.getTime()) + tickDelta;
		int pulse = 120 + Math.round(MathHelper.sin(time * 0.3f) * 40);
		Sprite piping = PixelBox.sprite("cake_piping");
		VertexConsumer ghost = PixelBox.translucent(consumers);
		matrices.push();

		if (held.getItem() instanceof IcingBagItem bag) {
			int color = bag.icing().color();
			if (sneaking) {
				ring(ghost, matrices, aim, top, 2.4f, 0xE06070, pulse, piping);
			} else {
				IcingBagItem.Tip tip = IcingBagItem.tip(held);
				float[] at = TurntableBlockEntity.surface(new float[]{aim[0], aim[1], aim[2]}, top, tier, design);
				List<CakeDesign.Dab> dabs = tip == IcingBagItem.Tip.LINE ? List.of(new CakeDesign.Dab(CakeDesign.Dab.DOT, color,
						Math.round(at[0] * 2), Math.round(at[1] * 2), Math.round(at[2] * 2))) : TurntableBlockEntity.piped(tip, color, design, at, tier, top);
				for (CakeDesign.Dab d : dabs) {
					CakeRenderer.dab(ghost, matrices, d.kind(), d.color(), 150, d.x() / 2f, d.y() / 2f, d.z() / 2f, piping, FULL, OverlayTexture.DEFAULT_UV);
				}
				if (tip == IcingBagItem.Tip.FROST) {
					// Frosting covers the whole tier: outline its top edge in the new colour.
					for (float[] e : CakeGeometry.perimeter(design.shape(), CakeGeometry.radius(tier, design.tierCount()) - 0.3f, 1.2f)) {
						PixelBox.blob(ghost, matrices, e[0], CakeGeometry.top(tier, design.tierCount()) + 0.1f, e[1], 0.35f, 0.25f, 0.35f, 2, 6, 0.4f,
								pulse << 24 | color, piping, FULL, OverlayTexture.DEFAULT_UV);
					}
				}
				ring(ghost, matrices, aim, top, 0.9f, 0xFFFFFF, pulse, piping);
			}
		} else if (held.isOf(BakeryItems.CAKE_SCRAPER)) {
			if (sneaking) {
				// The whole tier will be scraped: ring its top edge and its foot.
				int n = design.tierCount();
				for (float[] e : CakeGeometry.perimeter(design.shape(), CakeGeometry.radius(tier, n) + 0.4f, 1.4f)) {
					for (float y : new float[]{CakeGeometry.top(tier, n) + 0.2f, CakeGeometry.bottom(tier, n) + 0.3f}) {
						PixelBox.blob(ghost, matrices, e[0], y, e[1], 0.22f, 0.22f, 0.22f, 2, 5, 0.3f, pulse << 24 | 0xE06070, piping, FULL, OverlayTexture.DEFAULT_UV);
					}
				}
			} else {
				ring(ghost, matrices, aim, top, 2.6f, 0xE06070, pulse, piping);
			}
		} else if (held.isOf(BakeryItems.SPATULA)) {
			ring(ghost, matrices, aim, top, 1.2f, 0xF2C94C, pulse, piping);
		} else if (CakeDesign.isTopper(held) && !sneaking) {
			float[] spot = table.topperSpot(design, new float[]{aim[0], aim[1], aim[2]},
					top ? net.minecraft.util.math.Direction.UP : net.minecraft.util.math.Direction.NORTH, client.player);
			if (spot != null) {
				float bob = 0.6f + MathHelper.sin(time * 0.25f) * 0.35f;
				boolean side = spot[4] > 0.5f;
				CakeRenderer.topper(held, spot[0] + (side ? spot[0] * 0.04f : 0), spot[1] + (side ? 0 : bob), spot[2] + (side ? spot[2] * 0.04f : 0), spot[3],
						CakeDesign.defaultScale(held.getItem()) / 100f, side, matrices, consumers, FULL);
				ring(ghost, matrices, aim, top, 1.2f, 0xFFFFFF, pulse, piping);
			}
		}
		matrices.pop();
	}

	/**
	 * A ring of little beads lying on the cake around where you aim: flat on a tier's top, or standing against its
	 * side. It is the crosshair for piping, and shows how far the scraper reaches.
	 */
	private static void ring(VertexConsumer vc, MatrixStack matrices, float[] aim, boolean top, float radius, int rgb, int alpha, Sprite sprite) {
		int beads = Math.max(10, Math.round(radius * 9));
		float len = (float) Math.sqrt(aim[0] * aim[0] + aim[2] * aim[2]);
		float nx = len < 0.01f ? 1 : aim[0] / len, nz = len < 0.01f ? 0 : aim[2] / len;
		for (int i = 0; i < beads; i++) {
			double a = Math.PI * 2 * i / beads;
			float c = (float) Math.cos(a) * radius, s = (float) Math.sin(a) * radius;
			float x, y, z;
			if (top) {
				x = aim[0] + c;
				y = aim[1] + 0.15f;
				z = aim[2] + s;
			} else {
				x = aim[0] - nz * c + nx * 0.25f;
				y = aim[1] + s;
				z = aim[2] + nx * c + nz * 0.25f;
			}
			PixelBox.blob(vc, matrices, x, y, z, 0.13f, 0.13f, 0.13f, 2, 5, 0.3f, alpha << 24 | rgb, sprite, FULL, OverlayTexture.DEFAULT_UV);
		}
	}
}
