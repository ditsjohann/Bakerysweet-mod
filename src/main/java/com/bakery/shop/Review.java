package com.bakery.shop;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * A customer's review on the bakery's review board (at the counter): stars for how fast they were served, how good
 * the food was and how charming the place is, an overall rating, and a short comment picked to match.
 *
 * @param speed  1-5, 0 if they never got served (walked out)
 * @param taste  1-5, 0 if they never got anything to eat
 * @param charm  1-5
 * @param line   lang key of the comment
 */
public record Review(String name, String dish, int stars, int speed, int taste, int charm, String line, long day) {
	public static final int KEEP = 40;
	private static final int LINES = 3;

	/** A served customer's review. {@code waited} is the share of their patience left when served (1 = instantly). */
	public static Review served(String name, String dish, int dishStars, float waited, int charmLevel, long day, Random random) {
		int speed = MathHelper.clamp(1 + Math.round(waited * 4.4f), 1, 5);
		int taste = MathHelper.clamp(dishStars, 1, 5);
		int charm = charm(charmLevel);
		int stars = MathHelper.clamp(Math.round(taste * 0.5f + speed * 0.25f + charm * 0.25f), 1, 5);
		// Comment on whatever stood out most: the best part of a good visit, the worst part of a bad one.
		String aspect;
		if (stars >= 4) {
			aspect = taste >= speed && taste >= charm ? "taste" : speed >= charm ? "speed" : "charm";
		} else {
			aspect = taste <= speed && taste <= charm ? "taste" : speed <= charm ? "speed" : "charm";
		}
		String mood = stars >= 4 ? "good" : stars == 3 ? "ok" : "bad";
		String line = "review.bakery." + aspect + "." + mood + "." + random.nextInt(LINES);
		return new Review(name, dish, stars, speed, taste, charm, line, day);
	}

	/** Someone who gave up waiting. */
	public static Review walkout(String name, String dish, int charmLevel, long day, Random random) {
		return new Review(name, dish, 1, 1, 0, charm(charmLevel), "review.bakery.walkout." + random.nextInt(LINES), day);
	}

	private static int charm(int charmLevel) {
		return MathHelper.clamp(1 + Math.round(charmLevel * 0.8f), 1, 5);
	}

	public NbtCompound write() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("name", name);
		nbt.putString("dish", dish);
		nbt.putByte("stars", (byte) stars);
		nbt.putByte("speed", (byte) speed);
		nbt.putByte("taste", (byte) taste);
		nbt.putByte("charm", (byte) charm);
		nbt.putString("line", line);
		nbt.putLong("day", day);
		return nbt;
	}

	public static Review read(NbtCompound nbt) {
		return new Review(nbt.getString("name"), nbt.getString("dish"), nbt.getByte("stars"), nbt.getByte("speed"), nbt.getByte("taste"),
				nbt.getByte("charm"), nbt.getString("line"), nbt.getLong("day"));
	}
}
