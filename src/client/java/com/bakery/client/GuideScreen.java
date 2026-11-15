package com.bakery.client;

import com.bakery.network.SweetPayloads;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryItems;
import com.bakery.sweet.Sticker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bakery Guide, made to be skimmed: chapters down the side with their names, one page at a time, every page a
 * short list of numbered steps with a picture each, and a sticky-note tip. Arrow keys or the mouse wheel turn pages.
 */
public class GuideScreen extends Screen {
	private static final int SIDEBAR = 112;
	private static final int STICKERS_PER_PAGE = 6;
	private static final int STEP_H = 25;

	/** A page: its lang key, the pictures in its header, and one picture per step (steps are .s1, .s2, ...). */
	private record Page(String key, ItemConvertible[] hero, ItemConvertible[] steps) {
		static Page of(String key, ItemConvertible[] hero, ItemConvertible... steps) {
			return new Page(key, hero, steps);
		}
	}

	private record Chapter(String key, Item icon, int color, List<Page> pages) {
	}

	private static ItemConvertible[] hero(ItemConvertible... items) {
		return items;
	}

	private static List<Chapter> chapters;

	private final SweetPayloads.GuideData data;
	private int chapter;
	private int page;

	public GuideScreen(SweetPayloads.GuideData data) {
		super(Text.translatable("item.bakery.bakery_guide"));
		this.data = data;
	}

	private static List<Chapter> chapters() {
		if (chapters == null) {
			List<Page> stickers = new ArrayList<>();
			for (int i = 0; i * STICKERS_PER_PAGE < Sticker.values().length; i++) stickers.add(Page.of("stickers_" + i, hero()));
			chapters = List.of(
					new Chapter("start", BakeryItems.CUPCAKE, 0xFFF7A8C6, List.of(
							Page.of("welcome", hero(BakeryItems.CUPCAKE, BakeryItems.CAKE, BakeryItems.MATCHA_LATTE),
									BakeryItems.BAKERY_CATALOG, BakeryItems.FLOUR, BakeryItems.BAKERY_COUNTER, BakeryItems.SWEET_COIN),
							Page.of("first_bake", hero(BakeryItems.BREAD_DOUGH, BakeryItems.BREAD_LOAF),
									BakeryItems.MIXING_BOWL, BakeryItems.WHISK, BakeryItems.ROLLING_PIN, BakeryItems.OVEN_HEART, BakeryItems.BREAD_LOAF),
							Page.of("coffee", hero(BakeryItems.COFFEE_MACHINE, BakeryItems.LATTE, BakeryItems.CARAMEL_SYRUP),
									BakeryItems.COFFEE_SEEDS, BakeryItems.ROASTED_COFFEE_BEANS, Items.MILK_BUCKET, BakeryItems.MUG, BakeryItems.SYRUP_RACK),
							Page.of("cafe_cats", hero(BakeryItems.KITTEN_BASKET, BakeryItems.CAT_BED, BakeryItems.CAFE_JUKEBOX),
									BakeryItems.KITTEN_BASKET, BakeryItems.CAT_BED, Items.COD, BakeryItems.CAFE_JUKEBOX),
							Page.of("progress", hero()),
							Page.of("first_day", hero(BakeryItems.BREAD_LOAF, BakeryItems.COOKIES),
									BakeryItems.BAKERY_CATALOG, BakeryItems.OVEN_HEART, BakeryItems.PATH_RIBBON, BakeryItems.BREAD_LOAF),
							Page.of("starter_shopping", hero(BakeryItems.BAKERY_CATALOG, BakeryItems.SWEET_COIN),
									BakeryItems.MIXING_BOWL, Items.BRICKS, BakeryItems.FLOUR, BakeryItems.STRAWBERRY_SEEDS))),
					new Chapter("recipes", BakeryItems.BREAD_LOAF, 0xFFF9C98A, recipePages()),
					new Chapter("garden", BakeryItems.STRAWBERRY, 0xFFA8E6CF, List.of(
							Page.of("homegrown", hero(Items.WHEAT, BakeryItems.STRAWBERRY, BakeryItems.BUTTER),
									Items.WHEAT, BakeryItems.MILLSTONE_CORE, Items.COW_SPAWN_EGG, BakeryItems.SWEET_COIN),
							Page.of("farming", hero(BakeryItems.STRAWBERRY, BakeryItems.BLUEBERRY, BakeryItems.STAR_PETALS),
									Items.SHORT_GRASS, Items.WOODEN_HOE, BakeryItems.STARBLOOM_SEEDS, BakeryItems.STRAWBERRY_SEEDS),
							Page.of("trees", hero(BakeryItems.LEMON, BakeryItems.COCONUT, BakeryItems.PEACH),
									BakeryBlocks.PALM.sapling, BakeryItems.LEMON, BakeryItems.COCONUT, BakeryBlocks.BLOSSOM.planks),
							Page.of("veggies", hero(BakeryItems.CORN, BakeryItems.SWEET_POTATO, BakeryItems.ZUCCHINI),
									BakeryItems.CORN, BakeryItems.PEA_POD, BakeryItems.SWEET_POTATO, BakeryItems.ZUCCHINI),
							Page.of("special_crops", hero(BakeryItems.CRANBERRY, BakeryItems.RHUBARB, BakeryItems.CANDYFLOSS),
									BakeryItems.RHUBARB, Items.WATER_BUCKET, BakeryItems.CANDYFLOSS))),
					new Chapter("kitchen", BakeryItems.WHISK, 0xFFFFE08A, List.of(
							Page.of("pantry", hero(BakeryItems.FLOUR, BakeryItems.BUTTER, Items.SUGAR, Items.EGG),
									BakeryItems.FLOUR, BakeryItems.BUTTER, BakeryItems.WHIPPED_CREAM, BakeryItems.MATCHA_POWDER),
							Page.of("mixing", hero(BakeryItems.MIXING_BOWL, BakeryItems.WHISK),
									BakeryItems.MIXING_BOWL, BakeryItems.WHISK, BakeryItems.SWEET_DOUGH, BakeryItems.STRAWBERRY),
							Page.of("rolling", hero(BakeryItems.PASTRY_BOARD, BakeryItems.ROLLING_PIN),
									BakeryItems.PASTRY_BOARD, BakeryItems.ROLLING_PIN, Items.GOLD_NUGGET, BakeryItems.RAW_CUPCAKE),
							Page.of("proofing", hero(BakeryItems.PROOFING_BASKET, BakeryItems.BREAD_DOUGH),
									BakeryItems.PROOFING_BASKET, BakeryItems.BREAD_DOUGH, Items.CLOCK),
							Page.of("juicer", hero(BakeryItems.JUICER, BakeryItems.ORANGE),
									BakeryItems.ORANGE, Items.LEVER, Items.GLASS_BOTTLE),
							Page.of("churn", hero(BakeryItems.ICE_CREAM_CHURN, BakeryItems.ICE_CREAM),
									BakeryItems.WHIPPED_CREAM, BakeryItems.STRAWBERRY, BakeryItems.ICE_CREAM),
							Page.of("jam_pot", hero(BakeryItems.JAM_POT, BakeryItems.FRUIT_JAM),
									Items.CAMPFIRE, BakeryItems.STRAWBERRY, BakeryItems.WHISK, BakeryItems.GLASS_JAR),
							Page.of("freshness", hero(BakeryItems.WAX_PAPER, BakeryItems.FROST_WRAP, BakeryItems.GLASS_JAR),
									BakeryItems.WAX_PAPER, BakeryItems.FROST_WRAP, BakeryItems.GLASS_JAR),
							Page.of("drinks", hero(BakeryItems.MUG, BakeryItems.HOT_CHOCOLATE, BakeryItems.LEMONADE),
									BakeryItems.MUG, BakeryItems.MATCHA_LATTE, BakeryItems.CAFE_TABLE))),
					new Chapter("oven", BakeryItems.OVEN_HEART, 0xFFF4B183, List.of(
							Page.of("oven_build", hero(BakeryItems.OVEN_HEART, BakeryItems.SPATULA),
									Items.BRICKS, BakeryItems.OVEN_HEART, BakeryItems.SPATULA),
							Page.of("oven_use", hero(Items.COAL, BakeryItems.RAW_LOAF),
									Items.COAL, Items.IRON_DOOR, Items.IRON_BARS, BakeryItems.RAW_PIE),
							Page.of("flipping", hero(BakeryItems.RAW_COOKIES, BakeryItems.COOKIES),
									BakeryItems.RAW_COOKIES, BakeryItems.SPATULA, Items.BELL, BakeryItems.CUPCAKE),
							Page.of("heat", hero(BakeryItems.BELLOWS),
									BakeryItems.BELLOWS, Items.IRON_DOOR, BakeryItems.SPATULA),
							Page.of("millstone_build", hero(BakeryItems.MILLSTONE_CORE, Items.WHEAT, BakeryItems.FLOUR),
									Items.STONE_BRICKS, BakeryItems.MILLSTONE_CORE, Items.WHEAT),
							Page.of("fountain", hero(BakeryItems.CHOCOLATE_FOUNTAIN, BakeryItems.CHOCOLATE_TREAT),
									BakeryItems.CHOCOLATE_FOUNTAIN, BakeryItems.CHOCOLATE_BAR, BakeryItems.STRAWBERRY))),
					new Chapter("sweets", BakeryItems.SUGAR_COOKIE, 0xFFCDB4F0, List.of(
							Page.of("decorating", hero(BakeryItems.SPRINKLES, BakeryItems.WHIPPED_CREAM, BakeryItems.STAR_PETALS),
									BakeryItems.CUPCAKE, BakeryItems.SPRINKLES, BakeryItems.STAR_PETALS),
							Page.of("cookies", hero(BakeryItems.HEART_CUTTER, BakeryItems.STAR_CUTTER, BakeryItems.BUNNY_CUTTER),
									BakeryItems.COOKIE_DOUGH, BakeryItems.HEART_CUTTER, BakeryItems.RAW_SUGAR_COOKIE),
							Page.of("icing", hero(BakeryItems.PINK_ICING, BakeryItems.SKY_ICING, BakeryItems.MINT_ICING),
									BakeryItems.SUGAR_COOKIE, BakeryItems.PINK_ICING),
							Page.of("cake_studio", hero(BakeryItems.CAKE_TURNTABLE, BakeryItems.CAKE),
									BakeryItems.CAKE_TURNTABLE, BakeryItems.CAKE, BakeryItems.HEART_CAKE_MOLD, BakeryItems.CAKE_TURNTABLE),
							Page.of("piping", hero(BakeryItems.PINK_ICING, BakeryItems.CHOCOLATE_ICING, BakeryItems.CAKE_SCRAPER),
									BakeryItems.PINK_ICING, BakeryItems.MINT_ICING, BakeryItems.LILAC_ICING, BakeryItems.CHOCOLATE_ICING),
							Page.of("toppers", hero(BakeryItems.FONDANT_BUNNY, BakeryItems.SUGAR_FLOWER, BakeryItems.GOLDEN_CROWN),
									BakeryItems.FONDANT_BUNNY, BakeryItems.SUGAR_FLOWER, BakeryItems.SPATULA, BakeryItems.GOLDEN_CROWN),
							Page.of("gifts", hero(BakeryItems.GIFT_BOX, BakeryItems.WRAPPED_GIFT),
									BakeryItems.GIFT_BOX, BakeryItems.WRAPPED_GIFT))),
					new Chapter("cafe", BakeryItems.BAKERY_COUNTER, 0xFFA8D8F7, List.of(
							Page.of("shop", hero(BakeryItems.BAKERY_COUNTER, BakeryItems.PATH_RIBBON, BakeryItems.SERVICE_BELL),
									BakeryItems.BAKERY_COUNTER, BakeryItems.PATH_RIBBON, BakeryItems.BAKERY_COUNTER, BakeryItems.SERVICE_BELL),
							Page.of("customers", hero(BakeryItems.CAFE_CHAIR, BakeryItems.CAFE_TABLE),
									BakeryItems.PATH_RIBBON, BakeryItems.CUPCAKE, BakeryItems.CAFE_CHAIR),
							Page.of("levels", hero(BakeryItems.BREAD_LOAF, BakeryItems.CUPCAKE, BakeryItems.CAKE),
									BakeryItems.BREAD_LOAF, BakeryItems.CUPCAKE, BakeryItems.CAKE),
							Page.of("menu", hero(BakeryItems.MENU_BOARD),
									BakeryItems.MENU_BOARD, BakeryItems.COOKIES),
							Page.of("coins", hero(BakeryItems.SWEET_COIN, BakeryItems.DISPLAY_CASE),
									BakeryItems.SWEET_COIN, BakeryItems.DISPLAY_CASE, BakeryItems.BAKERY_COUNTER),
							Page.of("charm", hero(BakeryItems.FAIRY_LIGHTS, BakeryItems.CUPCAKE_LAMP, BakeryItems.HEART_RUG),
									BakeryItems.BUNNY_PLUSH, BakeryItems.FAIRY_LIGHTS, BakeryItems.SWEET_COIN),
							Page.of("friends", hero(BakeryItems.WRAPPED_GIFT, BakeryItems.CAKE),
									BakeryItems.CUPCAKE, BakeryItems.WRAPPED_GIFT, BakeryItems.STAR_PETALS))),
					new Chapter("style", BakeryItems.BUNNY_EARS, 0xFFFFB3C7, List.of(
							Page.of("wardrobe", hero(BakeryItems.CHEF_HAT, BakeryItems.BUNNY_EARS, BakeryItems.FRILLY_APRON),
									BakeryItems.CHEF_HAT, BakeryItems.FRILLY_APRON, BakeryItems.SWEET_COIN),
							Page.of("decor", hero(BakeryItems.PINK_CHECKER_TILES, BakeryItems.STRAWBERRY_WALLPAPER, BakeryItems.PINK_HEART_BALLOON),
									BakeryItems.PINK_CHECKER_TILES, BakeryItems.STRAWBERRY_WALLPAPER, BakeryItems.FAIRY_LIGHTS),
							Page.of("cozy_decor", hero(BakeryItems.PINK_COUCH, BakeryItems.NEON_HEART_SIGN, BakeryItems.TEA_SET),
									BakeryItems.PINK_COUCH, BakeryItems.BUNNY_PLUSH, BakeryItems.TEA_SET),
							Page.of("outdoor", hero(BakeryItems.PARASOL_TABLE, BakeryItems.HEART_LAMP_POST, BakeryItems.PINK_PICKET_FENCE),
									BakeryItems.PINK_AWNING, BakeryItems.PARASOL_TABLE, BakeryItems.PINK_PICKET_FENCE))),
					new Chapter("stickers", BakeryItems.STAR_PETALS, 0xFFFFD27A, stickers));
		}
		return chapters;
	}

	/** One page per dough (generated from the real recipes), then the flavours, two pages of them. */
	private static List<Page> recipePages() {
		List<Page> pages = new ArrayList<>();
		for (com.bakery.baking.BakingRecipes.DoughType type : com.bakery.baking.BakingRecipes.DoughType.values()) {
			pages.add(Page.of("recipe_" + type.name().toLowerCase(), hero(type.dough())));
		}
		for (int i = 0; i * FLAVOURS_PER_PAGE < flavourItems().size(); i++) pages.add(Page.of("flavours_" + i, hero()));
		return pages;
	}

	private static final int FLAVOURS_PER_PAGE = 14;

	/** One ingredient per flavour, in the flavour list's order. */
	private static List<Item> flavourItems() {
		Item[] candidates = {BakeryItems.VANILLA_POD, BakeryItems.CHOCOLATE_BAR, BakeryItems.STRAWBERRY, BakeryItems.BLUEBERRY, BakeryItems.MATCHA_POWDER,
				BakeryItems.LAVENDER, BakeryItems.ROSE_PETALS, BakeryItems.STAR_PETALS, BakeryItems.LEMON, BakeryItems.COCONUT_FLAKES, BakeryItems.RASPBERRY,
				BakeryItems.MINT_LEAVES, BakeryItems.PINEAPPLE, BakeryItems.CHERRIES, BakeryItems.ORANGE, BakeryItems.PEACH, BakeryItems.MANGO, BakeryItems.PLUM,
				BakeryItems.CRANBERRY, BakeryItems.PEANUT, BakeryItems.CANDYFLOSS, BakeryItems.HIBISCUS, BakeryItems.SWEET_POTATO, BakeryItems.ZUCCHINI,
				BakeryItems.RHUBARB, BakeryItems.CORN, BakeryItems.PEA_POD};
		return List.of(candidates);
	}

	// ------------------------------------------------------------------ layout

	private int panelW() {
		return Math.min(width - 16, 420);
	}

	private int panelH() {
		return Math.min(height - 16, 252);
	}

	private int left() {
		return (width - panelW()) / 2;
	}

	private int top() {
		return (height - panelH()) / 2;
	}

	private Chapter current() {
		return chapters().get(chapter);
	}

	private Page currentPage() {
		return current().pages().get(page);
	}

	private void turn(int direction) {
		int before = chapter * 100 + page;
		page += direction;
		if (page < 0) {
			if (chapter > 0) {
				chapter--;
				page = current().pages().size() - 1;
			} else {
				page = 0;
			}
		} else if (page >= current().pages().size()) {
			if (chapter < chapters().size() - 1) {
				chapter++;
				page = 0;
			} else {
				page = current().pages().size() - 1;
			}
		}
		if (before != chapter * 100 + page) sound(1f);
	}

	private void sound(float pitch) {
		if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, pitch));
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x = left(), y = top(), w = panelW(), h = panelH();
		Chapter ch = current();
		// Soft shadow, then the page.
		context.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x40000000);
		SweetUi.card(context, x, y, w, h, SweetUi.CREAM, ch.color());

		// Sidebar: the book's title and every chapter by name.
		SweetUi.card(context, x + 4, y + 4, SIDEBAR - 4, h - 8, 0xFFFFEEF4, 0xFFF6D3E0);
		SweetUi.item(context, new ItemStack(BakeryItems.BAKERY_GUIDE), x + 9, y + 9, 1f);
		context.drawText(textRenderer, Text.translatable("guide.bakery.book_title"), x + 28, y + 13, SweetUi.DEEP_PINK, false);
		int rowH = Math.max(16, Math.min(22, (h - 44) / chapters().size()));
		for (int i = 0; i < chapters().size(); i++) {
			Chapter c = chapters().get(i);
			int ry = y + 32 + i * rowH;
			boolean active = i == chapter;
			boolean hover = SweetUi.inside(mouseX, mouseY, x + 7, ry, SIDEBAR - 10, rowH - 2);
			if (active || hover) SweetUi.card(context, x + 7, ry, SIDEBAR - 10, rowH - 2, active ? c.color() : 0xFFFFF7FA, active ? 0xFFFFFFFF : c.color());
			SweetUi.item(context, new ItemStack(c.icon()), x + 10, ry + (rowH - 2 - 13) / 2f, 0.8f);
			context.drawText(textRenderer, Text.translatable("guide.bakery.chapter." + c.key()), x + 26, ry + (rowH - 2 - 8) / 2, active ? SweetUi.INK : 0xFF8A6A76, false);
		}

		// Page area
		int px = x + SIDEBAR + 8, pw = w - SIDEBAR - 16;
		Page pg = currentPage();
		int cy = y + 10;
		if (pg.key().startsWith("stickers_")) {
			cy = header(context, Text.translatable("guide.bakery.stickers.title"), pg, px, cy, pw, ch.color());
			drawStickers(context, Integer.parseInt(pg.key().substring(9)), px, cy, pw);
		} else if (pg.key().startsWith("recipe_")) {
			com.bakery.baking.BakingRecipes.DoughType type = com.bakery.baking.BakingRecipes.DoughType.valueOf(pg.key().substring(7).toUpperCase());
			cy = header(context, Text.translatable("guide.bakery." + pg.key() + ".title"), pg, px, cy, pw, ch.color());
			cy = drawRecipe(context, type, px, cy, pw);
			drawTip(context, pg, px, cy + 3, pw, y + h - 26);
		} else if (pg.key().startsWith("flavours_")) {
			cy = header(context, Text.translatable("guide.bakery.flavours.title"), pg, px, cy, pw, ch.color());
			drawFlavours(context, Integer.parseInt(pg.key().substring(9)), px, cy, pw);
		} else if (pg.key().equals("progress")) {
			cy = header(context, Text.translatable("guide.bakery.progress.title"), pg, px, cy, pw, ch.color());
			drawProgress(context, px, cy, pw);
		} else {
			boolean shelf = pg.hero().length > 0 && !pg.key().endsWith("_build") && room(pg, pw) >= 48;
			cy = header(context, Text.translatable("guide.bakery." + pg.key() + ".title"), shelf ? Page.of("", hero(ch.icon())) : pg, px, cy, pw, ch.color());
			cy = drawSteps(context, pg, px, cy, pw);
			if (pg.key().endsWith("_build")) cy = drawDiagram(context, pg.key(), px, cy + 2, pw);
			cy = drawTip(context, pg, px, cy + 3, pw, y + h - 26);
			if (shelf) drawShelf(context, pg, px, cy + 4, pw, y + h - 24, ch.color());
		}

		// Footer: previous / page dots / next
		int fy = y + h - 18;
		boolean first = chapter == 0 && page == 0, last = chapter == chapters().size() - 1 && page == current().pages().size() - 1;
		SweetUi.button(context, textRenderer, Text.literal("◀ ").append(Text.translatable("guide.bakery.prev")), px, fy, 54, 13, mouseX, mouseY, !first);
		SweetUi.button(context, textRenderer, Text.translatable("guide.bakery.next").append(" ▶"), px + pw - 54, fy, 54, 13, mouseX, mouseY, !last);
		int pages = current().pages().size();
		int dotsW = pages * 8;
		for (int i = 0; i < pages; i++) {
			int dx = px + pw / 2 - dotsW / 2 + i * 8;
			context.fill(dx + 1, fy + 4, dx + 6, fy + 9, i == page ? SweetUi.DEEP_PINK : 0xFFEBD3DC);
		}
	}

	/** The page title on a band of the chapter's colour, with the header pictures on the right. Returns the next y. */
	private int header(DrawContext context, Text title, Page pg, int x, int y, int w, int color) {
		SweetUi.card(context, x, y, w, 26, color, color);
		context.getMatrices().push();
		context.getMatrices().translate(x + 8, y + 7, 0);
		context.getMatrices().scale(1.5f, 1.5f, 1);
		context.drawText(textRenderer, title, 0, 0, 0xFFFFFFFF, true);
		context.getMatrices().pop();
		int ix = x + w - 4 - pg.hero().length * 20;
		for (ItemConvertible item : pg.hero()) {
			context.drawItem(new ItemStack(item), ix, y + 5);
			ix += 20;
		}
		return y + 32;
	}

	/** Numbered steps, each with its picture and one or two short lines. Returns the next y. */
	private int drawSteps(DrawContext context, Page pg, int x, int y, int w) {
		for (int i = 0; i < pg.steps().length; i++) {
			String key = "guide.bakery." + pg.key() + ".s" + (i + 1);
			if (!Language.getInstance().hasTranslation(key)) break;
			List<OrderedText> lines = textRenderer.wrapLines(Text.translatable(key), w - 46);
			int rowH = Math.max(STEP_H, lines.size() * 10 + 8);
			SweetUi.card(context, x, y, w, rowH - 3, i % 2 == 0 ? 0xFFFFFFFF : 0xFFFFF6F9, 0xFFF3DDE6);
			// Number bubble
			int bx = x + 5, by = y + (rowH - 3) / 2 - 6;
			SweetUi.card(context, bx, by, 12, 12, SweetUi.DEEP_PINK, SweetUi.DEEP_PINK);
			String n = String.valueOf(i + 1);
			context.drawText(textRenderer, n, bx + 6 - textRenderer.getWidth(n) / 2, by + 2, 0xFFFFFFFF, false);
			context.drawItem(new ItemStack(pg.steps()[i]), x + 21, y + (rowH - 3) / 2 - 8);
			int ty = y + (rowH - 3) / 2 - lines.size() * 5 + 1;
			for (OrderedText line : lines) {
				context.drawText(textRenderer, line, x + 41, ty, SweetUi.INK, false);
				ty += 10;
			}
			y += rowH;
		}
		return y;
	}

	/** Height left under the header, steps and tip, to decide whether the big picture shelf fits. */
	private int room(Page pg, int w) {
		int used = 32;
		for (int i = 0; i < pg.steps().length; i++) {
			String key = "guide.bakery." + pg.key() + ".s" + (i + 1);
			if (!Language.getInstance().hasTranslation(key)) break;
			used += Math.max(STEP_H, textRenderer.wrapLines(Text.translatable(key), w - 46).size() * 10 + 8);
		}
		String tip = "guide.bakery." + pg.key() + ".tip";
		if (Language.getInstance().hasTranslation(tip)) used += textRenderer.wrapLines(Text.translatable(tip), w - 30).size() * 10 + 11;
		return panelH() - 10 - 26 - used;
	}

	/** The page's pictures, big, on a soft shelf at the bottom of the page. */
	private void drawShelf(DrawContext context, Page pg, int x, int y, int w, int maxY, int color) {
		int hgt = Math.min(44, maxY - y);
		if (hgt < 36) return;
		SweetUi.card(context, x, y, w, hgt, (color & 0x00FFFFFF) | 0x40000000, (color & 0x00FFFFFF) | 0x90000000);
		int n = pg.hero().length;
		int gap = Math.min(54, (w - 8) / n);
		int sx = x + w / 2 - (n * gap) / 2 + gap / 2 - 16;
		for (int i = 0; i < n; i++) {
			SweetUi.item(context, new ItemStack(pg.hero()[i]), sx + i * gap, y + hgt / 2f - 16, 2f);
		}
	}

	/** A yellow sticky note with the page's tip, if it has one. Returns the next y. */
	private int drawTip(DrawContext context, Page pg, int x, int y, int w, int maxY) {
		String key = "guide.bakery." + pg.key() + ".tip";
		if (!Language.getInstance().hasTranslation(key)) return y;
		List<OrderedText> lines = textRenderer.wrapLines(Text.translatable(key), w - 30);
		int hgt = lines.size() * 10 + 8;
		y = Math.min(y, maxY - hgt);
		SweetUi.card(context, x, y, w, hgt, 0xFFFFF3C4, 0xFFF2C94C);
		context.drawText(textRenderer, Text.literal("✿"), x + 6, y + 5, 0xFFE0A020, false);
		int ty = y + 5;
		for (OrderedText line : lines) {
			context.drawText(textRenderer, line, x + 18, ty, 0xFF8A5A10, false);
			ty += 10;
		}
		return y + hgt;
	}

	/** A recipe card: what goes in the bowl, the dough it makes, then every shape it rolls into and what that bakes into. */
	private int drawRecipe(DrawContext context, com.bakery.baking.BakingRecipes.DoughType type, int x, int y, int w) {
		SweetUi.card(context, x, y, w, 30, 0xFFFFFFFF, 0xFFF3DDE6);
		context.drawText(textRenderer, Text.translatable("guide.bakery.recipe.bowl"), x + 6, y + 3, 0xFFB07A3C, false);
		int ix = x + 6;
		for (var e : com.bakery.baking.BakingRecipes.needs(type).entrySet()) {
			context.drawItem(new ItemStack(e.getKey()), ix, y + 12);
			if (e.getValue() > 1) context.drawText(textRenderer, "×" + e.getValue(), ix + 16, y + 18, SweetUi.INK, false);
			ix += e.getValue() > 1 ? 34 : 20;
		}
		context.drawText(textRenderer, Text.literal("+"), ix + 1, y + 16, 0xFFB0A0A6, false);
		context.getMatrices().push();
		context.drawItem(new ItemStack(BakeryItems.STRAWBERRY), ix + 9, y + 12);
		context.getMatrices().pop();
		context.drawText(textRenderer, Text.literal("?"), ix + 22, y + 18, 0xFFB0A0A6, false);
		int ax = x + w - 60;
		context.drawText(textRenderer, Text.translatable("guide.bakery.recipe.whisk"), ax - 4, y + 3, 0xFFB0A0A6, false);
		context.drawText(textRenderer, Text.literal("→"), ax + 4, y + 16, SweetUi.DEEP_PINK, false);
		context.drawItem(new ItemStack(type.dough()), ax + 18, y + 12);
		y += 34;
		context.drawText(textRenderer, Text.translatable("guide.bakery.recipe.shapes"), x + 2, y, 0xFFB07A3C, false);
		y += 11;
		int colW = w / 2;
		int i = 0;
		for (com.bakery.baking.BakingRecipes.Shape shape : type.shapes()) {
			int cx = x + (i % 2) * colW, cy = y + (i / 2) * 26;
			SweetUi.card(context, cx, cy, colW - 3, 23, i % 2 == 0 ? 0xFFFFFFFF : 0xFFFFF6F9, 0xFFF3DDE6);
			context.drawItem(new ItemStack(shape.raw()), cx + 3, cy + 3);
			context.drawText(textRenderer, Text.literal("→"), cx + 21, cy + 8, SweetUi.DEEP_PINK, false);
			ItemStack baked = new ItemStack(shape.baked(), shape.count());
			context.drawItem(baked, cx + 30, cy + 3);
			context.drawItemInSlot(textRenderer, baked, cx + 30, cy + 3);
			context.drawText(textRenderer, shape.baked().getName(), cx + 54, cy + 3, SweetUi.INK, false);
			context.drawText(textRenderer, Text.translatable("guide.bakery.recipe.bake", shape.bakeTicks() / 20, shape.idealHeat()), cx + 54, cy + 13, 0xFF9C7C88, false);
			i++;
		}
		return y + ((i + 1) / 2) * 26;
	}

	/** Every flavour and what to put in the bowl for it. */
	private void drawFlavours(DrawContext context, int part, int x, int y, int w) {
		List<Item> items = flavourItems();
		int colW = w / 2;
		for (int k = 0; k < FLAVOURS_PER_PAGE; k++) {
			int index = part * FLAVOURS_PER_PAGE + k;
			if (index >= items.size()) break;
			ItemStack stack = new ItemStack(items.get(index));
			com.bakery.food.Flavor flavor = com.bakery.food.Flavor.fromIngredient(stack);
			int cx = x + (k % 2) * colW, cy = y + (k / 2) * 21;
			SweetUi.card(context, cx, cy, colW - 3, 19, 0xFFFFFFFF, 0xFFF3DDE6);
			context.fill(cx + 2, cy + 2, cx + 5, cy + 17, 0xFF000000 | flavor.color());
			context.drawItem(stack, cx + 7, cy + 1);
			context.drawText(textRenderer, flavor.displayName(), cx + 26, cy + 6, SweetUi.INK, false);
		}
	}

	private void drawProgress(DrawContext context, int x, int y, int w) {
		Text rank = Text.translatable("rank.bakery." + data.rank());
		SweetUi.card(context, x, y, w, 18, 0xFFFFEAF1, SweetUi.PINK);
		SweetUi.centered(context, textRenderer, rank, x + w / 2, y + 5, SweetUi.DEEP_PINK);
		y += 24;
		Object[][] rows = {
				{BakeryItems.BREAD_LOAF, Text.translatable("guide.bakery.progress.recipes", data.recipes(), data.totalRecipes())},
				{BakeryItems.SWEET_COIN, Text.translatable("guide.bakery.progress.coins", data.coinsEarned())},
				{BakeryItems.BAKERY_COUNTER, Text.translatable("guide.bakery.progress.served", data.served())},
				{BakeryItems.WRAPPED_GIFT, Text.translatable("guide.bakery.progress.friends", data.friends())},
				{BakeryItems.STAR_PETALS, Text.translatable("guide.bakery.progress.stickers", data.stickers().size(), Sticker.values().length)}};
		for (int i = 0; i < rows.length; i++) {
			int cx = x + (i % 2) * (w / 2), cy = y + (i / 2) * 22;
			SweetUi.card(context, cx, cy, w / 2 - 3, 19, 0xFFFFFFFF, 0xFFF3DDE6);
			context.drawItem(new ItemStack((Item) rows[i][0]), cx + 2, cy + 1);
			context.drawText(textRenderer, (Text) rows[i][1], cx + 21, cy + 6, SweetUi.INK, false);
		}
		y += 70;
		Sticker next = null;
		for (Sticker s : Sticker.values()) {
			if (!data.stickers().contains(s.id())) {
				next = s;
				break;
			}
		}
		Text goal = next == null ? Text.translatable("guide.bakery.progress.all_done") : next.description();
		List<OrderedText> lines = textRenderer.wrapLines(goal, w - 30);
		SweetUi.card(context, x, y, w, lines.size() * 10 + 20, 0xFFFFF3C4, 0xFFF2C94C);
		context.drawText(textRenderer, Text.translatable("guide.bakery.progress.next"), x + 8, y + 5, 0xFFD0567E, false);
		int ty = y + 16;
		for (OrderedText line : lines) {
			context.drawText(textRenderer, line, x + 8, ty, 0xFF8A5A10, false);
			ty += 10;
		}
	}

	private void drawStickers(DrawContext context, int part, int x, int y, int w) {
		int colW = w / 2;
		for (int k = 0; k < STICKERS_PER_PAGE; k++) {
			int index = part * STICKERS_PER_PAGE + k;
			if (index >= Sticker.values().length) break;
			Sticker sticker = Sticker.values()[index];
			boolean earned = data.stickers().contains(sticker.id());
			int cx = x + (k % 2) * colW, cy = y + (k / 2) * 46;
			SweetUi.card(context, cx, cy, colW - 4, 42, earned ? 0xFFFFFFFF : 0xFFF6EEF1, earned ? 0xFFF2C94C : 0xFFE8D6DD);
			if (earned) {
				context.drawItem(new ItemStack(sticker.icon()), cx + 4, cy + 4);
			} else {
				context.drawText(textRenderer, "?", cx + 10, cy + 8, 0xFFB98A9A, false);
			}
			context.drawText(textRenderer, sticker.title(), cx + 24, cy + 8, earned ? 0xFFD0567E : 0xFFB98A9A, false);
			int ly = cy + 22;
			for (OrderedText line : textRenderer.wrapLines(sticker.description(), colW - 12)) {
				if (ly > cy + 34) break;
				context.drawText(textRenderer, line, cx + 5, ly, 0xFF7A5A66, false);
				ly += 10;
			}
		}
	}

	/** Three little layer maps of a buildable machine, front row at the top. Returns the next y. */
	private int drawDiagram(DrawContext context, String key, int x, int y, int w) {
		String[][] layers = key.equals("millstone_build")
				? new String[][]{{"SSS", "SSS", "SSS"}, {".W.", "WCW", ".W."}, {"...", "...", "..."}}
				: new String[][]{{"BBB", "BBB", "BBB"}, {"BHB", "B.B", "BBB"}, {".B.", "BBB", ".B."}};
		java.util.Map<Character, ItemStack> blocks = java.util.Map.of(
				'B', new ItemStack(Items.BRICKS), 'H', new ItemStack(BakeryItems.OVEN_HEART),
				'S', new ItemStack(Items.STONE_BRICKS), 'W', new ItemStack(Items.COBBLESTONE_WALL), 'C', new ItemStack(BakeryItems.MILLSTONE_CORE));
		int cell = 13, gap = 14, total = 3 * (3 * cell) + 2 * gap;
		int sx = x + (w - total) / 2;
		for (int l = 0; l < 3; l++) {
			int lx = sx + l * (3 * cell + gap);
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					char c = layers[l][row].charAt(col);
					int ix = lx + col * cell, iy = y + row * cell;
					context.fill(ix, iy, ix + cell - 1, iy + cell - 1, 0x30B98A9A);
					if (c == '.') continue;
					SweetUi.item(context, blocks.get(c), ix, iy, (cell - 1) / 16f);
				}
			}
			SweetUi.centered(context, textRenderer, Text.translatable("guide.bakery.layer" + (l + 1)), lx + 3 * cell / 2, y + 3 * cell + 2, 0xFFB98A9A);
		}
		return y + 3 * cell + 12;
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = left(), y = top(), w = panelW(), h = panelH();
		int rowH = Math.max(16, Math.min(22, (h - 44) / chapters().size()));
		for (int i = 0; i < chapters().size(); i++) {
			if (SweetUi.inside(mouseX, mouseY, x + 7, y + 32 + i * rowH, SIDEBAR - 10, rowH - 2)) {
				if (chapter != i) sound(1.1f);
				chapter = i;
				page = 0;
				return true;
			}
		}
		int px = x + SIDEBAR + 8, pw = w - SIDEBAR - 16, fy = y + h - 18;
		if (SweetUi.inside(mouseX, mouseY, px, fy, 54, 13)) {
			turn(-1);
			return true;
		}
		if (SweetUi.inside(mouseX, mouseY, px + pw - 54, fy, 54, 13)) {
			turn(1);
			return true;
		}
		int pages = current().pages().size();
		for (int i = 0; i < pages; i++) {
			int dx = px + pw / 2 - pages * 4 + i * 8;
			if (SweetUi.inside(mouseX, mouseY, dx, fy, 8, 13)) {
				if (page != i) sound(1f);
				page = i;
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (vertical != 0) turn(vertical < 0 ? 1 : -1);
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
			turn(1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
			turn(-1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
			chapter = MathHelper.clamp(chapter + (keyCode == GLFW.GLFW_KEY_UP ? -1 : 1), 0, chapters().size() - 1);
			page = 0;
			sound(1.1f);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
