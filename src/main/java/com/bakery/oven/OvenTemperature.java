package com.bakery.oven;

import net.minecraft.text.Text;

/**
 * Low bakes slowly but forgives a late peel; High is fast with a narrow golden window.
 */
public enum OvenTemperature {
	LOW("low", 0.75f, 0.85f, 1.25f, 1, 0xFFF6C177, 42),
	MEDIUM("medium", 1f, 0.9f, 1.15f, 1, 0xFFFF9A4D, 60),
	HIGH("high", 1.5f, 0.95f, 1.1f, 2, 0xFFFF5A4D, 78);

	private final String name;
	private final float speed;
	private final float perfectStart;
	private final float perfectEnd;
	private final int fuelPerTick;
	private final int color;
	private final int targetHeat;

	OvenTemperature(String name, float speed, float perfectStart, float perfectEnd, int fuelPerTick, int color, int targetHeat) {
		this.name = name;
		this.speed = speed;
		this.perfectStart = perfectStart;
		this.perfectEnd = perfectEnd;
		this.fuelPerTick = fuelPerTick;
		this.color = color;
		this.targetHeat = targetHeat;
	}

	/** The heat the fire drifts toward on this setting. */
	public int targetHeat() {
		return targetHeat;
	}

	public float speed() {
		return speed;
	}

	public float perfectStart() {
		return perfectStart;
	}

	public float perfectEnd() {
		return perfectEnd;
	}

	public int fuelPerTick() {
		return fuelPerTick;
	}

	public int color() {
		return color;
	}

	public Text displayName() {
		return Text.translatable("oven.bakery.temperature." + name);
	}

	public OvenTemperature next() {
		return values()[(ordinal() + 1) % values().length];
	}

	/** Oven stars for how done a bake is. 0 means it is still raw dough. */
	public int bakeStars(float doneness) {
		if (doneness < 0.5f) return 0;
		if (doneness < perfectStart - 0.15f) return 1;
		if (doneness < perfectStart) return 3;
		if (doneness <= perfectEnd) return 5;
		if (doneness <= perfectEnd + 0.15f) return 3;
		return 1;
	}
}
