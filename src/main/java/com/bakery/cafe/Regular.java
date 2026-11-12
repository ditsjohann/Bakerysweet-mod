package com.bakery.cafe;

import com.bakery.food.Flavor;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The town's café-goers. There is a fixed cast of them, so the same faces keep coming back: each has a name,
 * a look, a favourite (and a disliked) flavour and a habit - some like to sit down and eat, some take it to go.
 * Friendship hearts are kept per player, keyed by {@link #id()}.
 */
public record Regular(int index, String name, int skin, boolean slim, Flavor favorite, Flavor disliked, float dineIn, boolean sweetTooth) {
	/** Skins in assets/bakery/textures/entity/customer/; the first {@link #SLIM_SKINS} use slim arms. */
	public static final int SKINS = 12;
	public static final int SLIM_SKINS = 8;

	private static final String[] NAMES = {"Mabel", "Pip", "Clementine", "Biscuit", "Honey", "Juniper", "Poppy", "Waffles", "Maple", "Daisy",
			"Truffle", "Peaches", "Sprout", "Bramble", "Caramel", "Nutmeg", "Pudding", "Rosie", "Fig", "Coco", "Mochi", "Toffee", "Willow", "Bun"};
	private static final Regular[] ALL = new Regular[NAMES.length];

	static {
		Flavor[] flavors = Flavor.values();
		for (int i = 0; i < NAMES.length; i++) {
			// Spread skins so neighbours in the list look different; most regulars are cute girls in slim skins.
			int skin = (i * 5) % SKINS;
			Flavor favorite = flavors[1 + Math.floorMod(i * 7 + 3, flavors.length - 1)];
			Flavor disliked = flavors[1 + Math.floorMod(i * 11 + 9, flavors.length - 1)];
			if (disliked == favorite) disliked = Flavor.PLAIN;
			float dine = 0.25f + (Math.floorMod(i * 37, 10) / 10f) * 0.6f;
			ALL[i] = new Regular(i, NAMES[i], skin, skin < SLIM_SKINS, favorite, disliked, dine, i % 3 == 0);
		}
	}

	public static int count() {
		return ALL.length;
	}

	public static Regular get(int index) {
		return ALL[Math.floorMod(index, ALL.length)];
	}

	public UUID id() {
		return UUID.nameUUIDFromBytes(("bakery:regular:" + name).getBytes(StandardCharsets.UTF_8));
	}

	public Text displayName() {
		return Text.literal(name);
	}
}
