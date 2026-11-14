package com.bakery.sweet;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of a sculpted cake, in pixels (1/16 block), centred on x = z = 0 with y = 0 at the bottom.
 * Shared by the turntable (where clicks land) and the renderer (what is drawn), so piping always sits on the icing.
 */
public final class CakeGeometry {
	public static final int ROUND = 0, SQUARE = 1, HEART = 2, STAR = 3, FLOWER = 4, HEXAGON = 5, BUNNY = 6;
	public static final int SHAPES = 7;
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
			case SQUARE -> {
				// A soft square: a superellipse, so the corners are gently rounded.
				float h = r * 0.92f, ax = Math.abs(x) / h, az = Math.abs(z) / h;
				yield ax * ax * ax * ax * ax * ax + az * az * az * az * az * az <= 1;
			}
			case HEART -> {
				float lx = x - r * 0.45f, rx = x + r * 0.45f, lz = z + r * 0.25f;
				float lobe = r * 0.6f;
				if (lx * lx + lz * lz <= lobe * lobe || rx * rx + lz * lz <= lobe * lobe) yield true;
				float zt = z + r * 0.25f;
				float half = r * 1.04f * (1 - zt / (r * 1.25f));
				yield zt >= 0 && Math.abs(x) <= half;
			}
			case STAR -> {
				// A plump five-pointed star (soft points, so piping still fits), one point toward the front.
				double a = Math.atan2(x, z);
				double wave = Math.cos(5 * a);
				double edge = r * (0.74 + 0.26 * Math.signum(wave) * Math.pow(Math.abs(wave), 0.6));
				yield x * x + z * z <= edge * edge;
			}
			case FLOWER -> {
				// Six round petals.
				double a = Math.atan2(x, z);
				double edge = r * (0.86 + 0.14 * Math.abs(Math.cos(3 * a)));
				yield x * x + z * z <= edge * edge;
			}
			case HEXAGON -> {
				float ax = Math.abs(x), az = Math.abs(z), h = r * 0.95f;
				yield az <= h * 0.866f && ax * 0.866f + az * 0.5f <= h * 0.866f;
			}
			case BUNNY -> {
				// A round face with two long ears toward the back (-z).
				float face = r * 0.78f, fz = z - r * 0.18f;
				if (x * x + fz * fz <= face * face) yield true;
				for (float ex : new float[]{-r * 0.36f, r * 0.36f}) {
					float dx = (x - ex) / (r * 0.24f), dz = (z + r * 0.55f) / (r * 0.5f);
					if (dx * dx + dz * dz <= 1) yield true;
				}
				yield false;
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

	/**
	 * Follows a ray (cake space, pixels) until it meets the cake's real outline - its round, soft-square or heart
	 * tiers, not their bounding boxes - and returns {x, y, z, tier, 1 if it landed on the tier's top else 0},
	 * or null if it misses. This is what makes piping land exactly under the crosshair.
	 */
	public static float[] raycast(CakeDesign design, float[] origin, float[] dir, float maxDistance) {
		int tiers = design.tierCount();
		float step = 0.06f;
		float px = origin[0], py = origin[1], pz = origin[2];
		for (float t = 0; t <= maxDistance; t += step) {
			float x = origin[0] + dir[0] * t, y = origin[1] + dir[1] * t, z = origin[2] + dir[2] * t;
			for (int i = tiers - 1; i >= 0; i--) {
				int shape = design.shapeOf(i);
				if (y < bottom(i, tiers) || y > top(i, tiers) || !inside(shape, x, z, radius(i, tiers))) continue;
				// Entered through the top if the previous sample was above this tier (and over it).
				boolean fromTop = py > top(i, tiers) - 0.02f && inside(shape, px, pz, radius(i, tiers));
				if (fromTop) return new float[]{x, top(i, tiers), z, i, 1};
				return new float[]{x, y, z, i, 0};
			}
			px = x;
			py = y;
			pz = z;
		}
		return null;
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
