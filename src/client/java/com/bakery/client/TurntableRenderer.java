package com.bakery.client;

import com.bakery.sweet.TurntableBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.client.MinecraftClient;
import com.bakery.sweet.CakeDesign;
import com.bakery.sweet.IcingBagItem;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;

/**
 * The cake on the Cake Turntable, turned to wherever the plate was last spun. While you hold a topper and look at
 * the cake, a bobbing preview shows exactly where it will land and which way it will face; with an icing bag, a
 * see-through ghost of the icing pulses where the next squeeze will go (a whole ring for borders and drips).
 */
public class TurntableRenderer implements BlockEntityRenderer<TurntableBlockEntity> {
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
		preview(table, tickDelta, matrices, consumers, light);
		matrices.pop();
	}

	private static void preview(TurntableBlockEntity table, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || !(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
				|| !hit.getBlockPos().equals(table.getPos())) {
			return;
		}
		ItemStack held = client.player.getMainHandStack();
		if (held.getItem() instanceof IcingBagItem bag && !client.player.isSneaking()) {
			icingPreview(table, bag, held, hit, tickDelta, matrices, consumers);
			return;
		}
		if (!CakeDesign.isTopper(held) || client.player.isSneaking()) return;
		CakeDesign design = table.design();
		float[] spot = table.topperSpot(design, table.toCake(hit.getPos()), hit.getSide(), client.player);
		if (spot == null) return;
		float time = (client.world == null ? 0 : client.world.getTime()) + tickDelta;
		float bob = 0.6f + MathHelper.sin(time * 0.25f) * 0.35f;
		boolean side = spot[4] > 0.5f;
		CakeRenderer.topper(held, spot[0] + (side ? spot[0] * 0.04f : 0), spot[1] + (side ? 0 : bob), spot[2] + (side ? spot[2] * 0.04f : 0), spot[3], side,
				matrices, consumers, net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE);
		if (client.world != null && client.world.getTime() % 6 == 0 && client.world.random.nextFloat() < 0.5f) {
			double a = Math.toRadians(-table.renderAngle(tickDelta));
			double wx = spot[0] / 16 * Math.cos(a) - spot[2] / 16 * Math.sin(a), wz = spot[0] / 16 * Math.sin(a) + spot[2] / 16 * Math.cos(a);
			client.world.addParticle(net.minecraft.particle.ParticleTypes.WAX_ON, table.getPos().getX() + 0.5 + wx, table.getPos().getY() + (TurntableBlockEntity.PLATE + spot[1] + 1) / 16,
					table.getPos().getZ() + 0.5 + wz, 0, 0.01, 0);
		}
	}

	/** A see-through, gently pulsing ghost of exactly what the next squeeze would pipe. */
	private static void icingPreview(TurntableBlockEntity table, IcingBagItem bag, ItemStack held, BlockHitResult hit, float tickDelta, MatrixStack matrices,
									 VertexConsumerProvider consumers) {
		MinecraftClient client = MinecraftClient.getInstance();
		IcingBagItem.Tip tip = IcingBagItem.tip(held);
		CakeDesign design = table.design();
		TurntableBlockEntity.Plan plan = TurntableBlockEntity.plan(design, table.toCake(hit.getPos()), hit.getSide(), tip, bag.icing().color(), null);
		if (plan == null) return;
		float time = (client.world == null ? 0 : client.world.getTime()) + tickDelta;
		int alpha = 110 + Math.round(MathHelper.sin(time * 0.3f) * 50);
		int ghost = alpha << 24 | (bag.icing().color() & 0xFFFFFF);
		VertexConsumer buffer = PixelBox.translucent(consumers);
		int light = net.minecraft.client.render.LightmapTextureManager.MAX_LIGHT_COORDINATE;
		var sprite = CakeRenderer.pipingSprite();
		for (CakeDesign.Dab dab : plan.dabs()) CakeRenderer.dab(buffer, matrices, dab, ghost, sprite, light, OverlayTexture.DEFAULT_UV);
		// A little white ring marks the exact spot under the crosshair.
		float[] at = plan.at();
		PixelBox.blob(buffer, matrices, at[0], at[1] + 0.2f, at[2], 0.35f, 0.15f, 0.35f, 2, 8, 0.5f, (alpha + 40) << 24 | 0xFFFFFF, sprite, light,
				OverlayTexture.DEFAULT_UV);
	}
}
