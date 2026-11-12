package com.bakery.sweet;

import com.bakery.registry.BakeryItems;
import net.minecraft.item.Item;
import net.minecraft.text.Text;

import java.util.function.Supplier;

/** The sticker book: little goals that each give a sticker and a few Sweet Coins. */
public enum Sticker {
	FIRST_BAKE("first_bake", 5, () -> BakeryItems.BREAD_LOAF),
	FIVE_STARS("five_stars", 10, () -> BakeryItems.MACARONS),
	FIRST_CUSTOMER("first_customer", 5, () -> BakeryItems.BAKERY_COUNTER),
	REGULAR("regular", 10, () -> BakeryItems.CAFE_CHAIR),
	BEST_FRIEND("best_friend", 25, () -> BakeryItems.PINK_HEART_BALLOON),
	CAKE_ARTIST("cake_artist", 10, () -> BakeryItems.CAKE_TURNTABLE),
	COOKIE_ARTIST("cookie_artist", 5, () -> BakeryItems.SUGAR_COOKIE),
	GIFT_GIVER("gift_giver", 5, () -> BakeryItems.WRAPPED_GIFT),
	DRESS_UP("dress_up", 10, () -> BakeryItems.FRILLY_APRON),
	COZY_CAFE("cozy_cafe", 15, () -> BakeryItems.FAIRY_LIGHTS),
	SHOPPER("shopper", 5, () -> BakeryItems.BAKERY_CATALOG),
	PIGGY_BANK("piggy_bank", 15, () -> BakeryItems.SWEET_COIN),
	COLLECTOR("collector", 20, () -> BakeryItems.BAKERY_GUIDE),
	ORCHARD("orchard", 5, () -> BakeryItems.CHERRIES),
	DREAMY("dreamy", 10, () -> BakeryItems.CANDYFLOSS),
	STAR_PATISSIER("star_patissier", 50, () -> BakeryItems.STAR_PETALS);

	private final String id;
	private final int reward;
	private final Supplier<Item> icon;

	Sticker(String id, int reward, Supplier<Item> icon) {
		this.id = id;
		this.reward = reward;
		this.icon = icon;
	}

	public String id() {
		return id;
	}

	public int reward() {
		return reward;
	}

	public Item icon() {
		return icon.get();
	}

	public Text title() {
		return Text.translatable("sticker.bakery." + id);
	}

	public Text description() {
		return Text.translatable("sticker.bakery." + id + ".desc");
	}

	public static Sticker byId(String id) {
		for (Sticker s : values()) {
			if (s.id.equals(id)) return s;
		}
		return null;
	}
}
