package com.bakery.client;

import com.bakery.registry.BakeryItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The Quick Tour: nine picture slides that show the whole bakery before you start - a big picture made of the real
 * items, a title and one short line each. It plays the first time the catalog opens and can be replayed from the
 * guide's first page.
 */
public class TourScreen extends Screen {
	/** A piece of a slide's picture: an item (with a count), or a little sign between them (+, arrow, heart). */
	private record Bit(ItemConvertible item, int count, String sign) {
		static Bit of(ItemConvertible item) {
			return new Bit(item, 1, null);
		}

		static Bit of(ItemConvertible item, int count) {
			return new Bit(item, count, null);
		}

		static Bit sign(String sign) {
			return new Bit(null, 0, sign);
		}
	}

	private record Slide(String key, int color, List<Bit> picture) {
	}

	private static final Bit PLUS = Bit.sign("+"), THEN = Bit.sign("→"), HEART = Bit.sign("♥");
	private static final List<Slide> SLIDES = List.of(
			new Slide("welcome", 0xFFF7A8C6, List.of(Bit.of(BakeryItems.BAKERY_CATALOG), THEN, Bit.of(BakeryItems.SWEET_COIN, 50))),
			new Slide("kitchen", 0xFFFFE08A, List.of(Bit.of(BakeryItems.MIXING_BOWL), PLUS, Bit.of(BakeryItems.WHISK), PLUS,
					Bit.of(BakeryItems.PASTRY_BOARD), PLUS, Bit.of(BakeryItems.ROLLING_PIN))),
			new Slide("oven", 0xFFF4B183, List.of(Bit.of(Items.BRICKS, 21), PLUS, Bit.of(BakeryItems.OVEN_HEART), PLUS, Bit.of(BakeryItems.SPATULA))),
			new Slide("bake", 0xFFF9C98A, List.of(Bit.of(BakeryItems.FLOUR, 2), PLUS, Bit.of(Items.WATER_BUCKET), THEN, Bit.of(BakeryItems.BREAD_DOUGH),
					THEN, Bit.of(BakeryItems.RAW_LOAF), THEN, Bit.of(BakeryItems.BREAD_LOAF))),
			new Slide("coffee", 0xFFD9B08C, List.of(Bit.of(BakeryItems.ROASTED_COFFEE_BEANS), PLUS, Bit.of(Items.MILK_BUCKET), THEN,
					Bit.of(BakeryItems.COFFEE_MACHINE), THEN, Bit.of(BakeryItems.LATTE), PLUS, Bit.of(BakeryItems.CARAMEL_SYRUP))),
			new Slide("shop", 0xFFA8D8F7, List.of(Bit.of(BakeryItems.PATH_RIBBON), THEN, Bit.of(BakeryItems.BAKERY_COUNTER), THEN, Bit.of(BakeryItems.SERVICE_BELL))),
			new Slide("serve", 0xFFFFC2D6, List.of(Bit.of(BakeryItems.BREAD_LOAF), THEN, HEART, THEN, Bit.of(BakeryItems.SWEET_COIN, 3))),
			new Slide("cozy", 0xFFA8E6CF, List.of(Bit.of(BakeryItems.STRAWBERRY_SEEDS), PLUS, Bit.of(BakeryItems.PINK_COUCH), PLUS,
					Bit.of(BakeryItems.PINK_HEART_BALLOON), PLUS, Bit.of(BakeryItems.KITTEN_BASKET))),
			new Slide("help", 0xFFCDB4F0, List.of(Bit.of(BakeryItems.BREAD_LOAF), PLUS, Bit.of(BakeryItems.BAKERY_GUIDE), THEN, HEART)));

	private final Screen parent;
	private int slide;

	public TourScreen(Screen parent) {
		super(Text.translatable("tour.bakery.title"));
		this.parent = parent;
	}

	private static Path seenFile() {
		return com.bakery.platform.Platform.get().configDir().resolve("bakery_tour_seen");
	}

	/** Whether this player has watched (or skipped) the tour on this computer. */
	public static boolean seen() {
		return Files.exists(seenFile());
	}

	private static void markSeen() {
		try {
			Files.writeString(seenFile(), "The Sweetheart Bakery quick tour has been shown. Delete this file to see it again.\n");
		} catch (IOException ignored) {
		}
	}

	// ------------------------------------------------------------------ layout

	private int panelW() {
		return Math.min(width - 16, 330);
	}

	private int panelH() {
		return Math.min(height - 16, 196);
	}

	private int left() {
		return (width - panelW()) / 2;
	}

	private int top() {
		return (height - panelH()) / 2;
	}

	private boolean last() {
		return slide == SLIDES.size() - 1;
	}

	private Text nextLabel() {
		return last() ? Text.translatable("tour.bakery.start") : Text.translatable("guide.bakery.next").append(" ▶");
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x = left(), y = top(), w = panelW(), h = panelH();
		Slide s = SLIDES.get(slide);
		context.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x40000000);
		SweetUi.card(context, x, y, w, h, SweetUi.CREAM, s.color());

		// Title band, with a little "1 / 9" and the skip cross.
		SweetUi.card(context, x + 4, y + 4, w - 8, 26, s.color(), s.color());
		context.getMatrices().push();
		context.getMatrices().translate(x + 12, y + 10, 0);
		context.getMatrices().scale(1.6f, 1.6f, 1);
		context.drawText(textRenderer, Text.translatable("tour.bakery." + s.key() + ".title"), 0, 0, 0xFFFFFFFF, true);
		context.getMatrices().pop();
		String count = (slide + 1) + " / " + SLIDES.size();
		context.drawText(textRenderer, count, x + w - 26 - textRenderer.getWidth(count), y + 13, 0xFFFFFFFF, false);
		boolean overSkip = SweetUi.inside(mouseX, mouseY, x + w - 20, y + 9, 13, 13);
		SweetUi.card(context, x + w - 20, y + 9, 13, 13, overSkip ? 0xFFFFE0EC : 0xFFFFFFFF, 0xFFFFFFFF);
		context.drawText(textRenderer, "x", x + w - 16, y + 11, SweetUi.DEEP_PINK, false);

		// The picture: big items with signs between them, on a soft shelf.
		int shelfY = y + 38, shelfH = 70;
		SweetUi.card(context, x + 10, shelfY, w - 20, shelfH, (s.color() & 0x00FFFFFF) | 0x38000000, (s.color() & 0x00FFFFFF) | 0x90000000);
		int itemW = 34, signW = 14;
		int total = 0;
		for (Bit b : s.picture()) total += b.item() != null ? itemW + (b.count() > 1 ? 10 : 0) : signW;
		float scale = Math.min(2f, (w - 32f) / Math.max(1, total) * 2f);
		float unit = scale / 2f;
		float bx = x + w / 2f - total * unit / 2f;
		float by = shelfY + shelfH / 2f - 16 * unit;
		for (Bit b : s.picture()) {
			if (b.item() != null) {
				SweetUi.item(context, new ItemStack(b.item()), bx + 1 * unit, by, scale);
				if (b.count() > 1) {
					// The count sits just right of the picture, like a little price tag.
					context.drawText(textRenderer, "×" + b.count(), (int) (bx + 33 * unit), (int) (by + 22 * unit), SweetUi.DEEP_PINK, false);
					bx += 10 * unit;
				}
				bx += itemW * unit;
			} else {
				context.getMatrices().push();
				context.getMatrices().translate(bx + signW * unit / 2f - 4 * unit, by + 10 * unit, 0);
				context.getMatrices().scale(1.6f * unit, 1.6f * unit, 1);
				context.drawText(textRenderer, b.sign(), 0, 0, b.sign().equals("♥") ? 0xFFE0507E : 0xFFB07A8A, false);
				context.getMatrices().pop();
				bx += signW * unit;
			}
		}

		// One short line (two at most), big and centred.
		List<OrderedText> lines = textRenderer.wrapLines(Text.translatable("tour.bakery." + s.key() + ".line"), (int) ((w - 30) / 1.15f));
		int ty = shelfY + shelfH + 10;
		for (OrderedText line : lines) {
			context.getMatrices().push();
			context.getMatrices().translate(x + w / 2f - textRenderer.getWidth(line) * 1.15f / 2f, ty, 0);
			context.getMatrices().scale(1.15f, 1.15f, 1);
			context.drawText(textRenderer, line, 0, 0, SweetUi.INK, false);
			context.getMatrices().pop();
			ty += 13;
		}

		// Footer: back, the dots, next (or "Let's bake!").
		int fy = y + h - 22;
		SweetUi.button(context, textRenderer, Text.literal("◀ ").append(Text.translatable("guide.bakery.prev")), x + 10, fy, 56, 15, mouseX, mouseY, slide > 0);
		Text next = nextLabel();
		int nw = Math.max(56, textRenderer.getWidth(next) + 12);
		SweetUi.button(context, textRenderer, next, x + w - 10 - nw, fy, nw, 15, mouseX, mouseY, true);
		int dotsW = SLIDES.size() * 9;
		for (int i = 0; i < SLIDES.size(); i++) {
			int dx = x + w / 2 - dotsW / 2 + i * 9;
			context.fill(dx + 1, fy + 5, dx + 7, fy + 11, i == slide ? SweetUi.DEEP_PINK : 0xFFEBD3DC);
		}
	}

	// ------------------------------------------------------------------ input

	private void go(int direction) {
		int to = slide + direction;
		if (to >= SLIDES.size()) {
			close();
			return;
		}
		if (to < 0 || to == slide) return;
		slide = to;
		if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.1f));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = left(), y = top(), w = panelW(), h = panelH();
		int fy = y + h - 22;
		if (SweetUi.inside(mouseX, mouseY, x + w - 20, y + 9, 13, 13)) {
			close();
			return true;
		}
		if (SweetUi.inside(mouseX, mouseY, x + 10, fy, 56, 15)) {
			go(-1);
			return true;
		}
		Text next = nextLabel();
		int nw = Math.max(56, textRenderer.getWidth(next) + 12);
		if (SweetUi.inside(mouseX, mouseY, x + w - 10 - nw, fy, nw, 15)) {
			go(1);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
			go(1);
			return true;
		}
		if (keyCode == GLFW.GLFW_KEY_LEFT) {
			go(-1);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		go(vertical < 0 ? 1 : -1);
		return true;
	}

	@Override
	public void close() {
		markSeen();
		if (client != null) client.setScreen(parent);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
