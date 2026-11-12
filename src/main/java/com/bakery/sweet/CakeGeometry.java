package com.bakery.sweet;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a sculpted cake, in pixels (1/16 block), centred on x = z = 0 with y = 0 at the bottom.
 * Shared by the turntable (where clicks land) and the renderer (what is drawn), so piping always sits on the icing.
 */
public final class CakeGeometry {
	public static final int ROUND = 0, SQUARE = 1, HEART = 2;
	public static final int SHAPES = 3;
	public static final int MAX_TIERS = 3;

	private static final float[][] RADII = {{6.5f}, {6.5f, 4.5f}, {6.5f, 4.5f, 3f}};
	private static final float[][] HEIGHTS = {{6f}, {5f, 4f}, {4.5f, 3.5f, 3f}};

	private CakeGeometry() {
	}

	public static float radius(int tier, int tiers) {
		return RADII[MathHelper.clamp(tiers, 1, MAX_TIERS) - 1][tier];
	}

	public static float height(int tier, int tiers) {
		return HEIGHTS[MathHelper.clamp(tiers, 1, MAX_TIERS) - 1][tier];
	}

	public static float bottom(int tier, int tiers) {
		float y = 0;
		for (int i = 0; i < tier; i++) y += height(i, tiers);
		return y;
	}

	public static float top(int tier, int tiers) {
		return bottom(tier, tiers) + height(tier, tiers);
	}

	public static float totalHeight(int tiers) {
		return top(MathHelper.clamp(tiers, 1, MAX_TIERS) - 1, tiers);
	}

	/** Whether the point (x, z) lies inside a tier of radius r. The heart points towards +z (the front). */
	public static boolean inside(int shape, float x, float z, float r) {
		return switch (shape) {
			case SQUARE -> Math.abs(x) <= r * 0.9f && Math.abs(z) <= r * 0.9f;
			case HEART -> {
				float lx = x - r * 0.45f, rx = x + r * 0.45f, lz = z + r * 0.25f;
				float lobe = r * 0.6f;
				if (lx * lx + lz * lz <= lobe * lobe || rx * rx + lz * lz <= lobe * lobe) yield true;
				float zt = z + r * 0.25f;
				float half = r * 1.04f * (1 - zt / (r * 1.25f));
				yield zt >= 0 && Math.abs(x) <= half;
			}
			default -> x * x + z * z <= r * r;
		};
	}

	/** How far from the centre the edge is, looking in direction (dx, dz) (a unit vector). */
	public static float edge(int shape, float dx, float dz, float r) {
		float lo = 0, hi = r * 1.5f;
		for (int i = 0; i < 12; i++) {
			float mid = (lo + hi) / 2;
			if (inside(shape, dx * mid, dz * mid, r)) lo = mid;
			else hi = mid;
		}
		return lo;
	}

	/** Points around the edge of a tier, roughly {@code spacing} pixels apart: {x, z, dx, dz} each. */
	public static List<float[]> perimeter(int shape, float r, float spacing) {
		List<float[]> points = new ArrayList<>();
		int n = Math.max(8, Math.round((float) (Math.PI * 2 * r / spacing)));
		for (int i = 0; i < n; i++) {
			double a = Math.PI * 2 * i / n;
			float dx = (float) Math.cos(a), dz = (float) Math.sin(a);
			float e = edge(shape, dx, dz, r);
			points.add(new float[]{dx * e, dz * e, dx, dz});
		}
		return points;
	}

	/**
	 * The tier's footprint as horizontal strips one pixel deep: {x1, z1, x2, z2} each (pixels).
	 * Drawing these as boxes gives the chunky, pixel-art cake.
	 */
	public static List<float[]> strips(int shape, float r) {
		List<float[]> strips = new ArrayList<>();
		int extent = MathHelper.ceil(r * 1.3f);
		for (int z = -extent; z < extent; z++) {
			int start = Integer.MIN_VALUE;
			for (int x = -extent; x <= extent; x++) {
				boolean in = x < extent && inside(shape, x + 0.5f, z + 0.5f, r);
				if (in && start == Integer.MIN_VALUE) start = x;
				if (!in && start != Integer.MIN_VALUE) {
					strips.add(new float[]{start, z, x, z + 1});
					start = Integer.MIN_VALUE;
				}
			}
		}
		return strips;
	}

	/** Which tier a point at height y sits on top of, or -1. */
	public static int tierWithTopAt(float y, int tiers, float x, float z, int shape) {
		for (int i = tiers - 1; i >= 0; i--) {
			if (Math.abs(y - top(i, tiers)) <= 0.8f && inside(shape, x, z, radius(i, tiers) + 0.4f)) return i;
		}
		return -1;
	}

	/** Which tier's side a point is on, or -1. */
	public static int tierWithSideAt(float y, int tiers, float x, float z, int shape) {
		for (int i = tiers - 1; i >= 0; i--) {
			float r = radius(i, tiers);
			if (y >= bottom(i, tiers) - 0.3f && y <= top(i, tiers) + 0.3f && inside(shape, x, z, r + 1.2f) && !inside(shape, x, z, r - 1.2f)) return i;
		}
		return -1;
	}
}
