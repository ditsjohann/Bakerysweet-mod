package com.bakery.client;

import com.bakery.Bakery;
import com.bakery.network.SweetPayloads;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryItems;
import com.bakery.sweet.Sticker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bakery Guide: a cozy picture book with chapter ribbons down the side, a progress page with your next
 * goal, and a sticker book at the back.
 */
public class GuideScreen extends Screen {
	private static final Identifier BOOK = Bakery.id("textures/gui/guide.png");
	private static final int BOOK_W = 256;
	private static final int BOOK_H = 170;
	private static final int PAGE_W = 104;
	private static final int STICKERS_PER_PAGE = 4;

	private record Page(String key, Item... icons) {
	}

	private record Chapter(String key, Item icon, int color, List<Page> pages) {
	}

	private static List<Chapter> chapters;

	private final SweetPayloads.GuideData data;
	private final List<Page> pages = new ArrayList<>();
	private final List<Integer> chapterStarts = new ArrayList<>();
	private int spread;

	public GuideScreen(SweetPayloads.GuideData data) {
		super(Text.translatable("item.bakery.bakery_guide"));
		this.data = data;
		// Every chapter starts on a fresh spread (left page), padding with a blank page when needed.
		for (Chapter chapter : chapters()) {
			if (pages.size() % 2 == 1) pages.add(new Page(""));
			chapterStarts.add(pages.size());
			pages.addAll(chapter.pages());
		}
	}

	private static List<Chapter> chapters() {
		if (chapters == null) {
			List<Page> stickers = new ArrayList<>();
			for (int i = 0; i * STICKERS_PER_PAGE < Sticker.values().length; i++) stickers.add(new Page("stickers_" + i));
			chapters = List.of(
					new Chapter("start", BakeryItems.CUPCAKE, 0xFFF7A8C6, List.of(
							new Page("welcome", BakeryItems.CUPCAKE, BakeryItems.CAKE, BakeryItems.MATCHA_LATTE),
							new Page("cozy_cafe", BakeryItems.BAKERY_CATALOG, BakeryItems.CAKE_TURNTABLE, BakeryItems.BUNNY_PLUSH),
							new Page("sweet_life", BakeryItems.SWEET_COIN, BakeryItems.CAKE_TURNTABLE, BakeryItems.BUNNY_EARS),
							new Page("progress"),
							new Page("first_day", BakeryItems.FLOUR, BakeryItems.MIXING_BOWL, BakeryItems.BAKERY_COUNTER),
							new Page("starter_shopping", BakeryItems.BAKERY_CATALOG, BakeryItems.OVEN_HEART, Items.BRICKS))),
					new Chapter("garden", BakeryItems.STRAWBERRY, 0xFFA8E6CF, List.of(
							new Page("farming", BakeryItems.STRAWBERRY_SEEDS, BakeryItems.TEA_SEEDS, BakeryItems.STARBLOOM_SEEDS),
							new Page("trees", BakeryBlocks.PALM.sapling.asItem(), BakeryItems.COCONUT, BakeryItems.LEMON),
							new Page("veggies", BakeryItems.CORN, BakeryItems.SWEET_POTATO, BakeryItems.ZUCCHINI),
							new Page("special_crops", BakeryItems.CRANBERRY, BakeryItems.RHUBARB, BakeryItems.CANDYFLOSS))),
					new Chapter("kitchen", BakeryItems.WHISK, 0xFFFFEE99, List.of(
							new Page("pantry", BakeryItems.FLOUR, BakeryItems.BUTTER, Items.SUGAR, Items.EGG),
							new Page("mixing", BakeryItems.MIXING_BOWL, BakeryItems.WHISK, BakeryItems.SWEET_DOUGH),
							new Page("rolling", BakeryItems.PASTRY_BOARD, BakeryItems.ROLLING_PIN, BakeryItems.RAW_CUPCAKE),
							new Page("proofing", BakeryItems.PROOFING_BASKET, BakeryItems.BREAD_DOUGH),
							new Page("juicer", BakeryItems.JUICER, BakeryItems.ORANGE, Items.GLASS_BOTTLE),
							new Page("churn", BakeryItems.ICE_CREAM_CHURN, Items.ICE, BakeryItems.ICE_CREAM),
							new Page("jam_pot", BakeryItems.JAM_POT, Items.CAMPFIRE, BakeryItems.FRUIT_JAM),
							new Page("freshness", BakeryItems.WAX_PAPER, BakeryItems.FROST_WRAP, BakeryItems.GLASS_JAR),
							new Page("drinks", BakeryItems.MUG, BakeryItems.LEMONADE, BakeryItems.HOT_CHOCOLATE))),
					new Chapter("oven", BakeryItems.OVEN_HEART, 0xFFF4B183, List.of(
							new Page("oven", BakeryItems.OVEN_HEART, BakeryItems.SPATULA, Items.BRICKS),
							new Page("oven_build"),
							new Page("baking", Items.COAL, BakeryItems.SPATULA, BakeryItems.BREAD_LOAF),
							new Page("tending", BakeryItems.BELLOWS, BakeryItems.SPATULA, BakeryItems.CUPCAKE),
							new Page("millstone", BakeryItems.MILLSTONE_CORE, Items.WHEAT, BakeryItems.FLOUR),
							new Page("millstone_build"),
							new Page("fountain", BakeryItems.FOUNTAIN_CORE, BakeryItems.CHOCOLATE_BAR, BakeryItems.CHOCOLATE_TREAT),
							new Page("fountain_build"))),
					new Chapter("sweets", BakeryItems.SUGAR_COOKIE, 0xFFCDB4F0, List.of(
							new Page("decorating", BakeryItems.SPRINKLES, BakeryItems.WHIPPED_CREAM, BakeryItems.STAR_PETALS),
							new Page("cookies", BakeryItems.HEART_CUTTER, BakeryItems.COOKIE_DOUGH, BakeryItems.RAW_SUGAR_COOKIE),
							new Page("icing", BakeryItems.PINK_ICING, BakeryItems.SUGAR_COOKIE, BakeryItems.SKY_ICING),
							new Page("cake_studio", BakeryItems.CAKE_TURNTABLE, BakeryItems.CAKE, BakeryItems.HEART_CAKE_MOLD),
							new Page("piping", BakeryItems.PINK_ICING, BakeryItems.MINT_ICING, BakeryItems.LILAC_ICING),
							new Page("toppers", BakeryItems.FONDANT_BUNNY, BakeryItems.SUGAR_FLOWER, BakeryItems.GOLDEN_CROWN),
							new Page("gifts", BakeryItems.GIFT_BOX, BakeryItems.WRAPPED_GIFT, BakeryItems.MACARONS))),
					new Chapter("cafe", BakeryItems.BAKERY_COUNTER, 0xFFA8D8F7, List.of(
							new Page("shop", BakeryItems.BAKERY_COUNTER, BakeryItems.DISPLAY_CASE, BakeryItems.SERVICE_BELL),
							new Page("customers", BakeryItems.CAFE_CHAIR, BakeryItems.CAFE_TABLE, BakeryItems.CUPCAKE),
							new Page("levels", BakeryItems.BREAD_LOAF, BakeryItems.CUPCAKE, BakeryItems.CAKE),
							new Page("menu", BakeryItems.MENU_BOARD, BakeryItems.COOKIES, BakeryItems.CROISSANT),
							new Page("reviews", BakeryItems.SWEET_COIN, BakeryItems.MACARONS, BakeryItems.CAFE_CHAIR),
							new Page("coins", BakeryItems.SWEET_COIN, BakeryItems.BAKERY_CATALOG),
							new Page("catalog", BakeryItems.BAKERY_CATALOG, BakeryItems.PINK_HEART_BALLOON, BakeryItems.CHEF_HAT),
							new Page("charm", BakeryItems.FAIRY_LIGHTS, BakeryItems.CUPCAKE_LAMP, BakeryItems.HEART_RUG),
							new Page("friends", BakeryItems.WRAPPED_GIFT, BakeryItems.CAKE, BakeryItems.CAFE_CHAIR))),
					new Chapter("style", BakeryItems.BUNNY_EARS, 0xFFFFB3C7, List.of(
							new Page("wardrobe", BakeryItems.CHEF_HAT, BakeryItems.BUNNY_EARS, BakeryItems.FRILLY_APRON),
							new Page("outfit", BakeryItems.CAT_EARS, BakeryItems.RIBBON_BOW, BakeryItems.STRAWBERRY_BERET),
							new Page("decor", BakeryItems.PINK_CHECKER_TILES, BakeryItems.STRAWBERRY_WALLPAPER, BakeryItems.PINK_HEART_BALLOON),
							new Page("decor_more", BakeryItems.MACARON_TOWER, BakeryItems.COOKIE_JAR, BakeryItems.PINK_AWNING),
							new Page("cozy_decor", BakeryItems.PINK_LOVESEAT, BakeryItems.NEON_HEART_SIGN, BakeryItems.TEA_SET),
							new Page("outdoor", BakeryItems.PARASOL_TABLE, BakeryItems.HEART_LAMP_POST, BakeryItems.PINK_PICKET_FENCE))),
					new Chapter("stickers", BakeryItems.STAR_PETALS, 0xFFFFD27A, stickers));
		}
		return chapters;
	}

	private int spreads() {
		return (pages.size() + 1) / 2;
	}

	private int left() {
		return (width - BOOK_W) / 2 - 8;
	}

	private int top() {
		return (height - BOOK_H) / 2;
	}

	@Override
	protected void init() {
		int x = left();
		int y = top();
		addDrawableChild(new PageTurnWidget(x + 18, y + BOOK_H - 22, false, b -> turn(-1), true));
		addDrawableChild(new PageTurnWidget(x + BOOK_W - 42, y + BOOK_H - 22, true, b -> turn(1), true));
	}

	private void turn(int direction) {
		spread = Math.clamp(spread + direction, 0, spreads() - 1);
	}

	private int chapterAt(int spread) {
		int chapter = 0;
		for (int i = 0; i < chapterStarts.size(); i++) {
			if (chapterStarts.get(i) <= spread * 2) chapter = i;
		}
		return chapter;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		int x = left();
		int y = top();
		// Chapter ribbons peeking out on the right, drawn first so the book covers their roots.
		int current = chapterAt(spread);
		for (int i = 0; i < chapters().size(); i++) {
			Chapter chapter = chapters().get(i);
			int rx = x + BOOK_W - 10, ry = y + 8 + i * 19;
			boolean hover = SweetUi.inside(mouseX, mouseY, x + BOOK_W - 2, ry, 26, 17);
			int w = i == current ? 34 : hover ? 31 : 28;
			SweetUi.card(context, rx, ry, w, 17, chapter.color(), 0xFFFFFFFF);
			SweetUi.item(context, new ItemStack(chapter.icon()), rx + w - 15, ry + 2, 0.8f);
		}
		context.drawTexture(BOOK, x, y, 0, 0, BOOK_W, BOOK_H, 256, 256);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x = left();
		int y = top();
		for (int side = 0; side < 2; side++) {
			int index = spread * 2 + side;
			if (index >= pages.size()) continue;
			int px = x + (side == 0 ? 20 : 134);
			drawPage(context, pages.get(index), px, y + 14);
			if (!pages.get(index).key().isEmpty()) {
				Text number = Text.literal(String.valueOf(index + 1));
				context.drawText(textRenderer, number, px + PAGE_W / 2 - textRenderer.getWidth(number) / 2, y + BOOK_H - 18, 0xFFB98A9A, false);
			}
		}
		for (int i = 0; i < chapters().size(); i++) {
			if (SweetUi.inside(mouseX, mouseY, x + BOOK_W - 2, y + 8 + i * 19, 26, 17)) {
				context.drawTooltip(textRenderer, Text.translatable("guide.bakery.chapter." + chapters().get(i).key()), mouseX, mouseY);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = left(), y = top();
		for (int i = 0; i < chapters().size(); i++) {
			// Only the part of the ribbon that sticks out past the book's edge is clickable.
			if (SweetUi.inside(mouseX, mouseY, x + BOOK_W - 2, y + 8 + i * 19, 26, 17)) {
				spread = chapterStarts.get(i) / 2;
				client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f));
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void drawPage(DrawContext context, Page page, int x, int y) {
		if (page.key().isEmpty()) {
			drawDoodle(context, x, y);
			return;
		}
		if (page.key().equals("progress")) {
			drawProgress(context, x, y);
			return;
		}
		if (page.key().startsWith("stickers_")) {
			drawStickers(context, Integer.parseInt(page.key().substring(9)), x, y);
			return;
		}
		Text title = Text.translatable("guide.bakery." + page.key() + ".title");
		context.drawText(textRenderer, title, x + PAGE_W / 2 - textRenderer.getWidth(title) / 2, y, 0xFFD0567E, false);
		int cy = y + 12;
		if (page.key().endsWith("_build")) {
			drawDiagram(context, page.key(), x, cy);
			cy += 52;
		} else if (page.icons().length > 0) {
			int iconsW = page.icons().length * 20 - 4;
			for (int i = 0; i < page.icons().length; i++) {
				context.drawItem(new ItemStack(page.icons()[i]), x + PAGE_W / 2 - iconsW / 2 + i * 20, cy);
			}
			cy += 20;
		}
		List<OrderedText> lines = textRenderer.wrapLines(Text.translatable("guide.bakery." + page.key() + ".text"), PAGE_W);
		for (OrderedText line : lines) {
			context.drawText(textRenderer, line, x, cy, 0xFF5A3A46, false);
			cy += 9;
		}
	}

	/** A blank page gets a little heart doodle so it doesn't look empty. */
	private void drawDoodle(DrawContext context, int x, int y) {
		Text heart = Text.literal("♥");
		context.getMatrices().push();
		context.getMatrices().translate(x + PAGE_W / 2f, y + 60, 0);
		context.getMatrices().scale(3, 3, 1);
		context.drawText(textRenderer, heart, -textRenderer.getWidth(heart) / 2, 0, 0x40D0567E, false);
		context.getMatrices().pop();
	}

	private void drawProgress(DrawContext context, int x, int y) {
		Text title = Text.translatable("guide.bakery.progress.title");
		context.drawText(textRenderer, title, x + PAGE_W / 2 - textRenderer.getWidth(title) / 2, y, 0xFFD0567E, false);
		int cy = y + 13;
		Text rank = Text.translatable("rank.bakery." + data.rank());
		SweetUi.card(context, x, cy, PAGE_W, 14, 0xFFFFEAF1, SweetUi.PINK);
		context.drawText(textRenderer, rank, x + PAGE_W / 2 - textRenderer.getWidth(rank) / 2, cy + 3, 0xFFD0567E, false);
		cy += 19;
		Object[][] rows = {
				{BakeryItems.BREAD_LOAF, Text.translatable("guide.bakery.progress.recipes", data.recipes(), data.totalRecipes())},
				{BakeryItems.SWEET_COIN, Text.translatable("guide.bakery.progress.coins", data.coinsEarned())},
				{BakeryItems.BAKERY_COUNTER, Text.translatable("guide.bakery.progress.served", data.served())},
				{BakeryItems.WRAPPED_GIFT, Text.translatable("guide.bakery.progress.friends", data.friends())},
				{BakeryItems.STAR_PETALS, Text.translatable("guide.bakery.progress.stickers", data.stickers().size(), Sticker.values().length)}};
		for (Object[] row : rows) {
			SweetUi.item(context, new ItemStack((Item) row[0]), x, cy - 3, 0.75f);
			context.drawText(textRenderer, (Text) row[1], x + 15, cy, 0xFF5A3A46, false);
			cy += 12;
		}
		cy += 4;
		Sticker next = null;
		for (Sticker s : Sticker.values()) {
			if (!data.stickers().contains(s.id())) {
				next = s;
				break;
			}
		}
		context.drawText(textRenderer, Text.translatable("guide.bakery.progress.next"), x, cy, 0xFFD0567E, false);
		cy += 10;
		Text goal = next == null ? Text.translatable("guide.bakery.progress.all_done") : next.description();
		for (OrderedText line : textRenderer.wrapLines(goal, PAGE_W)) {
			context.drawText(textRenderer, line, x, cy, 0xFF5A3A46, false);
			cy += 9;
		}
	}

	private void drawStickers(DrawContext context, int part, int x, int y) {
		Text title = Text.translatable("guide.bakery.stickers.title");
		context.drawText(textRenderer, title, x + PAGE_W / 2 - textRenderer.getWidth(title) / 2, y, 0xFFD0567E, false);
		int cy = y + 13;
		for (int k = 0; k < STICKERS_PER_PAGE; k++) {
			int index = part * STICKERS_PER_PAGE + k;
			if (index >= Sticker.values().length) break;
			Sticker sticker = Sticker.values()[index];
			boolean earned = data.stickers().contains(sticker.id());
			context.getMatrices().push();
			context.getMatrices().translate(x + 11, cy + 12, 0);
			context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees((index % 3 - 1) * 7));
			if (earned) {
				SweetUi.card(context, -11, -11, 22, 22, 0xFFFFFFFF, 0xFFF0D2DC);
				context.drawItem(new ItemStack(sticker.icon()), -8, -8);
			} else {
				SweetUi.card(context, -11, -11, 22, 22, 0x20B98A9A, 0x40B98A9A);
				context.drawText(textRenderer, "?", -textRenderer.getWidth("?") / 2, -4, 0x80B98A9A, false);
			}
			context.getMatrices().pop();
			context.drawText(textRenderer, sticker.title(), x + 26, cy + 1, earned ? 0xFFD0567E : 0xFFB98A9A, false);
			context.getMatrices().push();
			context.getMatrices().translate(x + 26, cy + 11, 0);
			context.getMatrices().scale(0.7f, 0.7f, 1);
			int ly = 0;
			for (OrderedText line : textRenderer.wrapLines(sticker.description(), (int) ((PAGE_W - 26) / 0.7f))) {
				context.drawText(textRenderer, line, 0, ly, 0xFF7A5A66, false);
				ly += 9;
				if (ly >= 27) break;
			}
			context.getMatrices().pop();
			cy += 33;
		}
	}

	/** Three little layer maps of a buildable machine, front row at the top. */
	private void drawDiagram(DrawContext context, String key, int x, int y) {
		String[][] layers = switch (key) {
			case "millstone_build" -> new String[][]{{"SSS", "SSS", "SSS"}, {".W.", "WCW", ".W."}, {"...", "...", "..."}};
			case "fountain_build" -> new String[][]{{"TTT", "TTT", "TTT"}, {"...", ".F.", "..."}, {"...", ".G.", "..."}};
			default -> new String[][]{{"BBB", "BBB", "BBB"}, {"BHB", "B.B", "BBB"}, {".B.", "BBB", ".B."}};
		};
		java.util.Map<Character, ItemStack> blocks = java.util.Map.of(
				'B', new ItemStack(Items.BRICKS), 'H', new ItemStack(BakeryItems.OVEN_HEART),
				'S', new ItemStack(Items.STONE_BRICKS), 'W', new ItemStack(Items.COBBLESTONE_WALL), 'C', new ItemStack(BakeryItems.MILLSTONE_CORE),
				'T', new ItemStack(Items.WHITE_TERRACOTTA), 'F', new ItemStack(BakeryItems.FOUNTAIN_CORE), 'G', new ItemStack(Items.GOLD_BLOCK));
		for (int l = 0; l < 3; l++) {
			int lx = x + 2 + l * 35;
			for (int row = 0; row < 3; row++) {
				for (int col = 0; col < 3; col++) {
					char c = layers[l][row].charAt(col);
					int ix = lx + col * 10;
					int iy = y + 2 + row * 10;
					context.fill(ix, iy, ix + 9, iy + 9, 0x30B98A9A);
					if (c == '.') continue;
					context.getMatrices().push();
					context.getMatrices().translate(ix, iy, 0);
					context.getMatrices().scale(0.56f, 0.56f, 1);
					context.drawItem(blocks.get(c), 0, 0);
					context.getMatrices().pop();
				}
			}
			Text label = Text.translatable("guide.bakery.layer" + (l + 1));
			context.getMatrices().push();
			context.getMatrices().translate(lx, y + 36, 0);
			context.getMatrices().scale(0.7f, 0.7f, 1);
			context.drawText(textRenderer, label, 0, 0, 0xFFB98A9A, false);
			context.getMatrices().pop();
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
