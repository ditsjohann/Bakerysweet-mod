package com.bakery.food;

import com.bakery.registry.BakeryComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Expiry dates. Every perishable stack gets stamped with the world time it spoils at.
 * Nothing ticks while food sits in a chest, the clock just keeps running.
 */
public final class Freshness {
	public static final int DAY = 24000;

	/** World time on the client, kept up to date by the client tick so tooltips can count down. */
	public static long clientTime;

	public enum Stage {
		FRESH(Formatting.GREEN),
		GOOD(Formatting.YELLOW),
		STALE(Formatting.GOLD),
		SPOILED(Formatting.DARK_GREEN);

		private final Formatting color;

		Stage(Formatting color) {
			this.color = color;
		}

		public Formatting color() {
			return color;
		}

		public Text displayName() {
			return Text.translatable("freshness.bakery." + name().toLowerCase()).formatted(color);
		}
	}

	private Freshness() {
	}

	public static int shelfLife(ItemStack stack) {
		return stack.getItem() instanceof Perishable perishable ? perishable.shelfLife() : 0;
	}

	public static boolean isPerishable(ItemStack stack) {
		return shelfLife(stack) > 0;
	}

	/** Gives a fresh expiry date to stacks that do not have one yet. */
	public static void stamp(ItemStack stack, World world) {
		int life = shelfLife(stack);
		if (life > 0 && !stack.contains(BakeryComponents.EXPIRES)) {
			stack.set(BakeryComponents.EXPIRES, world.getTime() + life);
		}
	}

	public static void stampFresh(ItemStack stack, long now) {
		int life = shelfLife(stack);
		if (life > 0) {
			stack.set(BakeryComponents.EXPIRES, now + life);
		}
	}

	public static long remaining(ItemStack stack, long now) {
		Long expires = stack.get(BakeryComponents.EXPIRES);
		if (expires == null) {
			return shelfLife(stack);
		}
		return Math.max(0, expires - now);
	}

	public static Stage stage(ItemStack stack, long now) {
		int life = shelfLife(stack);
		if (life <= 0) {
			return Stage.FRESH;
		}
		long left = remaining(stack, now);
		if (left <= 0) return Stage.SPOILED;
		float ratio = Math.min(1f, left / (float) life);
		if (ratio > 0.6f) return Stage.FRESH;
		if (ratio > 0.25f) return Stage.GOOD;
		return Stage.STALE;
	}

	public static Preservation preservation(ItemStack stack) {
		return Preservation.byName(stack.getOrDefault(BakeryComponents.PRESERVATION, "none"));
	}

	/** Stretches the freshness that is left. Returns false if it was already preserved this way or better. */
	public static boolean preserve(ItemStack stack, Preservation method, long now) {
		Preservation current = preservation(stack);
		if (current.multiplier() >= method.multiplier() || stage(stack, now) == Stage.SPOILED) {
			return false;
		}
		long left = remaining(stack, now);
		long stretched = left * method.multiplier() / current.multiplier();
		stack.set(BakeryComponents.EXPIRES, now + stretched);
		stack.set(BakeryComponents.PRESERVATION, method.id());
		return true;
	}

	/** "3d 4h left" in Minecraft days and hours. */
	public static Text timeLeft(long ticks) {
		long hours = ticks / 1000;
		long days = hours / 24;
		hours %= 24;
		if (days > 0) {
			return Text.translatable("freshness.bakery.days_left", days, hours);
		}
		if (hours > 0) {
			return Text.translatable("freshness.bakery.hours_left", hours);
		}
		return Text.translatable("freshness.bakery.almost_gone");
	}
}
