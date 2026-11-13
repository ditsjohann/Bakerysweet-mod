package com.bakery.client;

import com.bakery.Bakery;
import com.bakery.food.BakeryFoods;
import com.bakery.registry.BakeryComponents;
import com.bakery.sweet.CakeDesign;
import com.bakery.sweet.CakeGeometry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws a hand-decorated cake the soft way: smooth round (or rounded-square, or heart) tiers with a pillowy rounded
 * top edge, glossy round dollops of icing, swirled rosettes, teardrop drips, and the toppers standing on top or
 * pressed onto the sides. Matrices sit at the cake's bottom centre, one unit per block.
 */
public final class CakeRenderer {
	private static final float PX = 1 / 16f;
	/** How many points go round each tier's outline. */
	private static final int AROUND = 40;
	/** The rounded top edge of a tier: radius of the bevel and how many steps it is drawn in. */
	private static final float BEVEL = 1.0f;
	private static final int BEVEL_STEPS = 4;

	private CakeRenderer() {
	}

	private static Sprite sprite(String name) {
		return MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(Bakery.id("block/" + name));
	}

	public static void render(ItemStack cake, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		CakeDesign design = cake.getOrDefault(BakeryComponents.CAKE_DESIGN, CakeDesign.PLAIN);
		int flavor = BakeryFoods.flavor(cake).color();
		VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
		Sprite frosting = sprite("cake_frosting");
		Sprite sponge = sprite("cake_sponge");
		Sprite piping = sprite("cake_piping");
		int tiers = design.tierCount();

		for (int i = 0; i < tiers; i++) {
			int color = design.tiers().get(i) == -1 ? flavor : design.tiers().get(i);
			boolean frosted = design.tiers().get(i) != -1;
			tier(buffer, matrices, design.shape(), CakeGeometry.radius(i, tiers), CakeGeometry.bottom(i, tiers), CakeGeometry.top(i, tiers),
					color, frosted ? frosting : sponge, frosting, light, overlay);
		}

		for (CakeDesign.Dab dab : design.dabs()) {
			float x = dab.x() / 2f, y = dab.y() / 2f, z = dab.z() / 2f;
			int c = dab.color();
			if (dab.isDrip()) {
				// A teardrop running down the side: a thin round stream ending in a plump bead.
				float len = dab.dripLength() * 0.9f;
				PixelBox.tube(buffer, matrices, x, z, 0.42f, y - len, y + 0.15f, 8, c, piping, light, overlay);
				PixelBox.blob(buffer, matrices, x, y - len, z, 0.6f, 0.7f, 0.6f, 4, 8, 0, c, piping, light, overlay);
				PixelBox.blob(buffer, matrices, x, y + 0.1f, z, 0.5f, 0.3f, 0.5f, 2, 8, 0.5f, c, piping, light, overlay);
			} else if (dab.kind() == CakeDesign.Dab.ROSETTE) {
				// A piped swirl: three softly stacked rings getting smaller, with a lighter kiss on top.
				PixelBox.blob(buffer, matrices, x, y - 0.35f, z, 1.15f, 0.55f, 1.15f, 3, 10, 0.5f, c, piping, light, overlay);
				PixelBox.blob(buffer, matrices, x + 0.08f, y + 0.1f, z - 0.05f, 0.85f, 0.5f, 0.85f, 3, 10, 0.5f, lighten(c, 10), piping, light, overlay);
				PixelBox.blob(buffer, matrices, x - 0.05f, y + 0.55f, z + 0.06f, 0.55f, 0.45f, 0.55f, 3, 8, 0.5f, lighten(c, 18), piping, light, overlay);
				PixelBox.blob(buffer, matrices, x, y + 0.95f, z, 0.25f, 0.35f, 0.25f, 2, 6, 0.4f, lighten(c, 28), piping, light, overlay);
			} else {
				// A round dollop with a little peak.
				PixelBox.blob(buffer, matrices, x, y - 0.3f, z, 0.7f, 0.65f, 0.7f, 4, 10, 0.45f, c, piping, light, overlay);
				PixelBox.blob(buffer, matrices, x, y + 0.2f, z, 0.25f, 0.3f, 0.25f, 2, 6, 0.4f, lighten(c, 20), piping, light, overlay);
			}
		}

		for (CakeDesign.Topper topper : design.toppers()) {
			topper(new ItemStack(topper.item()), topper.x() / 2f, topper.y() / 2f, topper.z() / 2f, topper.yaw(), onSide(design, topper),
					matrices, consumers, light);
		}
	}

	/** A topper is on a side when it does not sit at any tier's top. */
	public static boolean onSide(CakeDesign design, CakeDesign.Topper topper) {
		float y = topper.y() / 2f;
		for (int i = 0; i < design.tierCount(); i++) {
			if (Math.abs(y - CakeGeometry.top(i, design.tierCount())) < 0.3f) return false;
		}
		return true;
	}

	/** Draws one topper at (x, y, z) pixels: standing on a tier, or pressed flat against a tier's side. */
	public static void topper(ItemStack stack, float x, float y, float z, float yaw, boolean side, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		MinecraftClient client = MinecraftClient.getInstance();
		matrices.push();
		matrices.translate(x * PX, y * PX, z * PX);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
		// Sculpted toppers are little 3D figurines; fruit and petals are flat cut-outs, candles are blocks.
		boolean figurine = java.util.Arrays.asList(com.bakery.registry.BakeryItems.TOPPERS).contains(stack.getItem());
		boolean block = stack.getItem() instanceof net.minecraft.item.BlockItem;
		float size = side ? 0.24f : figurine || block ? 0.42f : 0.28f;
		if (side) {
			matrices.translate(0, 0, 0.02f);
		} else {
			matrices.translate(0, size * 0.5f - 0.01f, 0);
		}
		matrices.scale(size, size, size);
		client.getItemRenderer().renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, consumers, client.world, 0);
		matrices.pop();
	}

	/**
	 * One tier: smooth sides following the outline of its shape, a rounded top edge, and a flat iced top.
	 * The outline is sampled all the way round, so round, rounded-square and heart cakes all come out soft.
	 */
	private static void tier(VertexConsumer vc, MatrixStack matrices, int shape, float r, float y1, float y2, int color, Sprite side, Sprite top,
							 int light, int overlay) {
		float[] ex = new float[AROUND + 1], ez = new float[AROUND + 1], edge = new float[AROUND + 1];
		for (int k = 0; k <= AROUND; k++) {
			double a = 2 * Math.PI * k / AROUND;
			ex[k] = (float) Math.cos(a);
			ez[k] = (float) Math.sin(a);
			edge[k] = CakeGeometry.edge(shape, ex[k], ez[k], r);
		}
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f m = entry.getPositionMatrix();
		int cr = color >> 16 & 255, cg = color >> 8 & 255, cb = color & 255;
		float bevel = Math.min(BEVEL, (y2 - y1) / 2);
		// The tier's profile from the bottom up: {inset from the outline, y, how much the surface faces up}.
		float[][] profile = new float[3 + BEVEL_STEPS][];
		profile[0] = new float[]{0.15f, y1, 0};
		profile[1] = new float[]{0, y1 + 0.3f, 0};
		profile[2] = new float[]{0, y2 - bevel, 0};
		for (int s = 1; s <= BEVEL_STEPS; s++) {
			double t = Math.PI / 2 * s / BEVEL_STEPS;
			profile[2 + s] = new float[]{bevel * (1 - (float) Math.cos(t)), y2 - bevel + bevel * (float) Math.sin(t), (float) Math.sin(t)};
		}
		for (int p = 0; p + 1 < profile.length; p++) {
			float[] lo = profile[p], hi = profile[p + 1];
			boolean bevelBand = p >= 2;
			Sprite sprite = bevelBand ? top : side;
			float v1 = sprite.getFrameV(Math.max(0, 1 - (hi[1] - y1) / 16f)), v2 = sprite.getFrameV(Math.max(0, 1 - (lo[1] - y1) / 16f));
			for (int k = 0; k < AROUND; k++) {
				float u1 = sprite.getFrameU((k % 8) / 8f), u2 = sprite.getFrameU((k % 8 + 1) / 8f);
				float ny1 = hi[2], ny2 = lo[2];
				float h1 = (float) Math.sqrt(1 - ny1 * ny1), h2 = (float) Math.sqrt(1 - ny2 * ny2);
				vertex(vc, m, entry, ex[k + 1] * (edge[k + 1] - hi[0]), hi[1], ez[k + 1] * (edge[k + 1] - hi[0]), cr, cg, cb, u2, v1, ex[k + 1] * h1, ny1, ez[k + 1] * h1, light, overlay);
				vertex(vc, m, entry, ex[k + 1] * (edge[k + 1] - lo[0]), lo[1], ez[k + 1] * (edge[k + 1] - lo[0]), cr, cg, cb, u2, v2, ex[k + 1] * h2, ny2, ez[k + 1] * h2, light, overlay);
				vertex(vc, m, entry, ex[k] * (edge[k] - lo[0]), lo[1], ez[k] * (edge[k] - lo[0]), cr, cg, cb, u1, v2, ex[k] * h2, ny2, ez[k] * h2, light, overlay);
				vertex(vc, m, entry, ex[k] * (edge[k] - hi[0]), hi[1], ez[k] * (edge[k] - hi[0]), cr, cg, cb, u1, v1, ex[k] * h1, ny1, ez[k] * h1, light, overlay);
			}
		}
		// The flat iced top (a fan from the centre) and the underside.
		float in = bevel;
		float uc = top.getFrameU(0.5f), vc0 = top.getFrameV(0.5f);
		for (int k = 0; k < AROUND; k++) {
			float x1 = ex[k] * (edge[k] - in), z1 = ez[k] * (edge[k] - in), x2 = ex[k + 1] * (edge[k + 1] - in), z2 = ez[k + 1] * (edge[k + 1] - in);
			vertex(vc, m, entry, 0, y2, 0, cr, cg, cb, uc, vc0, 0, 1, 0, light, overlay);
			vertex(vc, m, entry, x2, y2, z2, cr, cg, cb, top.getFrameU(0.5f + x2 / 16), top.getFrameV(0.5f + z2 / 16), 0, 1, 0, light, overlay);
			vertex(vc, m, entry, x1, y2, z1, cr, cg, cb, top.getFrameU(0.5f + x1 / 16), top.getFrameV(0.5f + z1 / 16), 0, 1, 0, light, overlay);
			vertex(vc, m, entry, 0, y2, 0, cr, cg, cb, uc, vc0, 0, 1, 0, light, overlay);
			float bx1 = ex[k] * (edge[k] - 0.15f), bz1 = ez[k] * (edge[k] - 0.15f), bx2 = ex[k + 1] * (edge[k + 1] - 0.15f), bz2 = ez[k + 1] * (edge[k + 1] - 0.15f);
			vertex(vc, m, entry, 0, y1, 0, cr, cg, cb, uc, vc0, 0, -1, 0, light, overlay);
			vertex(vc, m, entry, bx1, y1, bz1, cr, cg, cb, uc, vc0, 0, -1, 0, light, overlay);
			vertex(vc, m, entry, bx2, y1, bz2, cr, cg, cb, uc, vc0, 0, -1, 0, light, overlay);
			vertex(vc, m, entry, 0, y1, 0, cr, cg, cb, uc, vc0, 0, -1, 0, light, overlay);
		}
	}

	private static void vertex(VertexConsumer vc, Matrix4f m, MatrixStack.Entry entry, float x, float y, float z, int r, int g, int b,
							   float u, float v, float nx, float ny, float nz, int light, int overlay) {
		float shade = 0.9f + 0.1f * ny;
		vc.vertex(m, x * PX, y * PX, z * PX).color(Math.round(r * shade), Math.round(g * shade), Math.round(b * shade), 255).texture(u, v)
				.overlay(overlay).light(light).normal(entry, nx, ny, nz);
	}

	private static int lighten(int c, int amount) {
		int r = Math.min(255, (c >> 16 & 255) + amount), g = Math.min(255, (c >> 8 & 255) + amount), b = Math.min(255, (c & 255) + amount);
		return r << 16 | g << 8 | b;
	}

	/** For the renderers' bounding boxes. */
	public static List<float[]> footprint(CakeDesign design) {
		return CakeGeometry.strips(design.shape(), CakeGeometry.radius(0, design.tierCount()));
	}
}
