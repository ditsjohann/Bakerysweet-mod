package com.bakery.client;

import com.bakery.cafe.CafeLevel;
import com.bakery.network.SweetPayloads;
import com.bakery.registry.BakeryItems;
import com.bakery.sweet.Catalog;
import com.bakery.sweet.Coins;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bakery Catalog: a pastel shop window. Labelled tabs along the top, the Daily Special on its own card, roomy
 * cards with each goodie's name and price, a scrolling list, and a bar showing how close the next bakery level is.
 * Fancier goodies show a little lock until the bakery reaches their level.
 */
public class CatalogScreen extends Screen {
	private static final Item[] TAB_ICONS = {BakeryItems.MIXING_BOWL, BakeryItems.FLOUR, BakeryItems.STRAWBERRY_SEEDS, BakeryItems.PINK_HEART_BALLOON,
			BakeryItems.TULIP_POT, BakeryItems.BUNNY_EARS};
	private static final int[] TAB_COLORS = {0xFFFFE08A, 0xFFFFD6A8, 0xFFA8E6CF, 0xFFF7A8C6, 0xFFA8D8F7, 0xFFCDB4F0};
	private static final int CARD_H = 34;

	private final BlockPos pos;
	private final SweetPayloads.GuideData data;
	private int tab;
	/** The shelf inside the current tab (Kitchen: tools, stations, shop, cakes ...). */
	private int shelf;
	private int scroll;
	private int flash = -1;
	private long flashAt;
	/** Today's stock per entry (-1 = unlimited), counted down locally as the baker buys. */
	private final List<Integer> stock;

	public CatalogScreen(BlockPos pos, SweetPayloads.GuideData data) {
		super(Text.translatable("screen.bakery.catalog"));
		this.pos = pos;
		this.data = data;
		this.stock = new ArrayList<>(data.stockLeft());
	}

	private int left(int index) {
		return index < stock.size() ? stock.get(index) : -1;
	}

	private boolean soldOut(int index) {
		return left(index) == 0 && !creative();
	}

	// ------------------------------------------------------------------ layout

	private int panelW() {
		return Math.min(width - 12, 460);
	}

	private int panelH() {
		return Math.min(height - 12, 270);
	}

	private int left() {
		return (width - panelW()) / 2;
	}

	private int top() {
		return (height - panelH()) / 2;
	}

	/** Whether there is room for the Daily Special card on the left. */
	private boolean wide() {
		return panelW() >= 360;
	}

	private int gridX() {
		return left() + (wide() ? 112 : 10);
	}

	private List<String> shelves() {
		return tab < Catalog.Category.values().length ? Catalog.subs(Catalog.Category.values()[tab]) : List.of();
	}

	private int shelfY() {
		return top() + 52;
	}

	private int gridY() {
		return top() + 52 + (shelves().isEmpty() ? 0 : 19);
	}

	private int gridW() {
		return left() + panelW() - 16 - gridX();
	}

	private int gridH() {
		return panelH() - (gridY() - top()) - 24;
	}

	private int cols() {
		return Math.max(2, gridW() / 84);
	}

	private int cardW() {
		return gridW() / cols();
	}

	private int visibleRows() {
		return Math.max(1, gridH() / CARD_H);
	}

	private int rows() {
		return (shown().size() + cols() - 1) / cols();
	}

	private int maxScroll() {
		return Math.max(0, rows() - visibleRows());
	}

	private long day() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.world == null ? 0 : Catalog.day(client.world);
	}

	private CafeLevel level() {
		return CafeLevel.of(data.served());
	}

	private boolean creative() {
		return client != null && client.player != null && client.player.getAbilities().creativeMode;
	}

	/** Indices (into Catalog.entries) shown on the current tab: unlocked first, then the locked teasers. */
	private List<Integer> shown() {
		List<Integer> open = new ArrayList<>(), locked = new ArrayList<>();
		List<Catalog.Entry> all = Catalog.entries();
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).category().ordinal() != tab) continue;
			List<String> shelves = shelves();
			if (!shelves.isEmpty() && !shelves.get(Math.min(shelf, shelves.size() - 1)).equals(all.get(i).sub())) continue;
			(isLocked(i) ? locked : open).add(i);
		}
		open.addAll(locked);
		return open;
	}

	private boolean isLocked(int index) {
		return !creative() && Catalog.entries().get(index).level().ordinal() > level().ordinal();
	}

	private int coins() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player == null ? 0 : Coins.count(client.player);
	}

	private int tabCount() {
		return TAB_ICONS.length + 1;
	}

	private int tabW() {
		return (panelW() - 20) / tabCount();
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x = left(), y = top(), w = panelW(), h = panelH();
		context.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x40000000);
		SweetUi.card(context, x, y, w, h, SweetUi.CREAM, SweetUi.PINK);

		// Header: title, bakery level, coin purse.
		SweetUi.card(context, x + 4, y + 4, w - 8, 22, 0xFFF7A8C6, 0xFFF7A8C6);
		SweetUi.item(context, new ItemStack(BakeryItems.BAKERY_CATALOG), x + 8, y + 7, 1f);
		context.getMatrices().push();
		context.getMatrices().translate(x + 27, y + 9, 0);
		context.getMatrices().scale(1.3f, 1.3f, 1);
		context.drawText(textRenderer, title, 0, 0, 0xFFFFFFFF, true);
		context.getMatrices().pop();
		CafeLevel level = level();
		Text badge = Text.translatable("screen.bakery.catalog.level", level.number(), level.displayName());
		Text balance = Text.literal(String.valueOf(coins()));
		int purseW = textRenderer.getWidth(balance) + 26;
		int purseX = x + w - 8 - purseW;
		SweetUi.card(context, purseX, y + 7, purseW, 16, 0xFFFFFFFF, 0xFFF2C94C);
		SweetUi.item(context, new ItemStack(BakeryItems.SWEET_COIN), purseX + 3, y + 8, 0.85f);
		context.drawText(textRenderer, balance, purseX + 20, y + 11, 0xFF9A6A10, false);
		int badgeW = textRenderer.getWidth(badge) + 10;
		if (purseX - badgeW - 4 > x + 27 + textRenderer.getWidth(title) * 1.3f + 6) {
			SweetUi.card(context, purseX - badgeW - 4, y + 7, badgeW, 16, 0xFFFFEAF1, 0xFFFFFFFF);
			context.drawText(textRenderer, badge, purseX - badgeW + 1, y + 11, SweetUi.DEEP_PINK, false);
		}

		// Tabs with names (icons only if it gets too narrow).
		int tw = tabW();
		for (int t = 0; t < tabCount(); t++) {
			int tx = x + 10 + t * tw, ty = y + 30;
			boolean read = t == TAB_ICONS.length;
			boolean active = !read && tab == t;
			boolean hover = SweetUi.inside(mouseX, mouseY, tx, ty, tw - 3, 18);
			int color = read ? 0xFFFFF3C4 : TAB_COLORS[t];
			SweetUi.card(context, tx, ty, tw - 3, 18, active ? color : hover ? 0xFFFFF7FA : 0xFFFFFFFF, active ? SweetUi.DEEP_PINK : color);
			Text name = read ? Text.translatable("screen.bakery.catalog.guide_tab") : Catalog.Category.values()[t].displayName();
			boolean label = tw >= 56;
			int iconX = label ? tx + 3 : tx + (tw - 3) / 2 - 7;
			SweetUi.item(context, new ItemStack(read ? BakeryItems.BAKERY_GUIDE : TAB_ICONS[t]), iconX, ty + 2, 0.85f);
			if (label) {
				Text fitted = Text.literal(textRenderer.trimToWidth(name.getString(), tw - 24));
				context.drawText(textRenderer, fitted, tx + 19, ty + 5, active ? SweetUi.INK : 0xFF8A6A76, false);
			}
		}

		// Daily Special card
		int featured = Catalog.featured(day());
		if (wide()) drawSpecial(context, featured, x + 10, shelfY(), 96, panelH() - 52 - 24, mouseX, mouseY);

		// Shelves: little chips that split a big tab into sections.
		List<String> shelves = shelves();
		if (!shelves.isEmpty()) {
			int sx = gridX();
			for (int i = 0; i < shelves.size(); i++) {
				Text name = Text.translatable("catalog.bakery.shelf." + shelves.get(i));
				int cw = textRenderer.getWidth(name) + 12;
				boolean active = i == Math.min(shelf, shelves.size() - 1);
				boolean hover = SweetUi.inside(mouseX, mouseY, sx, shelfY(), cw, 15);
				SweetUi.card(context, sx, shelfY(), cw, 15, active ? TAB_COLORS[tab] : hover ? 0xFFFFF7FA : 0xFFFFFFFF, active ? SweetUi.DEEP_PINK : SweetUi.LINE);
				context.drawText(textRenderer, name, sx + 6, shelfY() + 4, active ? SweetUi.INK : 0xFF8A6A76, false);
				sx += cw + 3;
			}
		}

		// The goods
		List<Integer> shown = shown();
		scroll = MathHelper.clamp(scroll, 0, maxScroll());
		int cols = cols(), cw = cardW();
		Integer hovered = null;
		int first = scroll * cols;
		for (int k = 0; k < visibleRows() * cols; k++) {
			int n = first + k;
			if (n >= shown.size()) break;
			int index = shown.get(n);
			int cx = gridX() + (k % cols) * cw, cy = gridY() + (k / cols) * CARD_H;
			if (drawCard(context, index, index == featured, cx, cy, cw - 4, CARD_H - 4, mouseX, mouseY)) hovered = index;
		}
		if (shown.isEmpty()) {
			SweetUi.centered(context, textRenderer, Text.translatable("screen.bakery.catalog.nothing"), gridX() + gridW() / 2, gridY() + 20, SweetUi.SOFT);
		}

		// Scrollbar
		if (maxScroll() > 0) {
			int sx = gridX() + gridW() + 4, sh = gridH() - 4;
			context.fill(sx, gridY(), sx + 4, gridY() + sh, 0xFFF3DDE6);
			int thumb = Math.max(14, sh * visibleRows() / rows());
			int ty = gridY() + (sh - thumb) * scroll / maxScroll();
			context.fill(sx, ty, sx + 4, ty + thumb, SweetUi.DEEP_PINK);
		}

		// Footer: progress to the next level.
		CafeLevel next = level.next();
		int fy = y + h - 18;
		if (next == null) {
			context.drawText(textRenderer, Text.translatable("screen.bakery.catalog.max_level"), x + 12, fy + 3, SweetUi.DEEP_PINK, false);
		} else {
			Text footer = Text.translatable("screen.bakery.catalog.next_level", next.served() - data.served(), next.displayName());
			int barX = x + 12, barW = Math.min(140, w / 3);
			float progress = MathHelper.clamp((data.served() - level.served()) / (float) Math.max(1, next.served() - level.served()), 0, 1);
			SweetUi.card(context, barX, fy + 2, barW, 9, 0xFFFFFFFF, 0xFFF3DDE6);
			context.fill(barX + 2, fy + 4, barX + 2 + Math.round((barW - 4) * progress), fy + 9, SweetUi.DEEP_PINK);
			context.drawText(textRenderer, footer, barX + barW + 6, fy + 3, SweetUi.SOFT, false);
		}

		if (hovered != null) {
			Catalog.Entry entry = Catalog.entries().get(hovered);
			List<Text> lines = new ArrayList<>(getTooltipFromItem(MinecraftClient.getInstance(), entry.stack()));
			if (isLocked(hovered)) {
				lines.add(Text.translatable("screen.bakery.catalog.locked", entry.level().number(), entry.level().displayName()).withColor(0xFF9C8C94));
			} else {
				lines.add(Text.translatable("screen.bakery.catalog.price", Catalog.price(hovered, day())).withColor(0xFFD0567E));
				if (left(hovered) >= 0) {
					lines.add(Text.translatable(soldOut(hovered) ? "screen.bakery.catalog.sold_out_hint" : "screen.bakery.catalog.left_hint", left(hovered))
							.withColor(0xFFB07A3C));
				}
				if (entry.storeBought()) lines.add(Text.translatable("screen.bakery.catalog.store_hint").formatted(Formatting.GRAY));
				if (!soldOut(hovered)) {
					lines.add(Text.translatable(coins() >= Catalog.price(hovered, day()) || creative() ? "screen.bakery.catalog.click_buy" : "screen.bakery.catalog.poor")
							.formatted(Formatting.GRAY));
				}
			}
			context.drawTooltip(textRenderer, lines, mouseX, mouseY);
		}
	}

	/** One goodie: picture, name, and price (or the level it unlocks at). Returns whether the mouse is over it. */
	private boolean drawCard(DrawContext context, int index, boolean special, int cx, int cy, int cw, int ch, int mouseX, int mouseY) {
		Catalog.Entry entry = Catalog.entries().get(index);
		boolean locked = isLocked(index);
		int price = Catalog.price(index, day());
		boolean affordable = coins() >= price || creative();
		boolean hover = SweetUi.inside(mouseX, mouseY, cx, cy, cw, ch);
		boolean flashing = flash == index && client != null && client.world != null && client.world.getTime() - flashAt < 6;
		int fill = locked ? 0xFFF4EEF0 : flashing ? 0xFFFFF3C4 : hover ? 0xFFFFEAF1 : 0xFFFFFFFF;
		SweetUi.card(context, cx, cy, cw, ch, fill, special ? 0xFFF2C94C : hover && !locked ? SweetUi.DEEP_PINK : SweetUi.LINE);
		// Picture on a soft round plate
		SweetUi.card(context, cx + 3, cy + 3, ch - 6, ch - 6, locked ? 0xFFEDE4E8 : 0xFFFFF0F5, locked ? 0xFFEDE4E8 : 0xFFFFE0EC);
		int ix = cx + 3 + (ch - 6) / 2 - 8, iy = cy + ch / 2 - 8;
		context.drawItem(entry.stack(), ix, iy);
		context.drawItemInSlot(textRenderer, entry.stack(), ix, iy);
		if (locked) context.fill(cx + 3, cy + 3, cx + ch - 3, cy + ch - 3, 0x90F4EEF0);
		// Name
		int tx = cx + ch, textW = cw - ch - 3;
		String name = textRenderer.trimToWidth(entry.stack().getName().getString(), textW);
		if (name.length() < entry.stack().getName().getString().length()) name = textRenderer.trimToWidth(name, textW - 6) + "…";
		context.drawText(textRenderer, name, tx, cy + 5, locked ? 0xFFA898A0 : SweetUi.INK, false);
		// Price or lock
		if (locked) {
			context.drawText(textRenderer, Text.translatable("screen.bakery.catalog.lv", entry.level().number()), tx, cy + 17, 0xFFA898A0, false);
		} else {
			SweetUi.item(context, new ItemStack(BakeryItems.SWEET_COIN), tx - 1, cy + 15, 0.6f);
			context.drawText(textRenderer, String.valueOf(price), tx + 10, cy + 17, affordable ? 0xFFD0567E : 0xFFB0A0A6, false);
			if (left(index) >= 0) {
				// Daily stock: a little "3 left" tag, or "sold out" until tomorrow.
				Text tag = soldOut(index) ? Text.translatable("screen.bakery.catalog.sold_out") : Text.translatable("screen.bakery.catalog.left", left(index));
				int tw = textRenderer.getWidth(tag);
				context.drawText(textRenderer, tag, cx + cw - tw - 4, cy + 17, soldOut(index) ? 0xFFB0A0A6 : 0xFFB07A3C, false);
				if (soldOut(index)) context.fill(cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, 0x60FFFFFF);
			}
			if (special) {
				int was = Catalog.entries().get(index).price();
				int px = tx + 12 + textRenderer.getWidth(String.valueOf(price));
				context.drawText(textRenderer, String.valueOf(was), px, cy + 17, 0xFFB0A0A6, false);
				context.fill(px - 1, cy + 20, px + textRenderer.getWidth(String.valueOf(was)) + 1, cy + 21, 0xFFB0A0A6);
			}
		}
		return hover;
	}

	/** The Daily Special: a big picture, its name, and the price a quarter off. */
	private void drawSpecial(DrawContext context, int index, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
		Catalog.Entry entry = Catalog.entries().get(index);
		SweetUi.card(context, sx, sy, sw, sh - 4, 0xFFFFF8DA, 0xFFF2C94C);
		SweetUi.card(context, sx + 4, sy + 4, sw - 8, 14, 0xFFF2C94C, 0xFFF2C94C);
		SweetUi.centered(context, textRenderer, Text.translatable("screen.bakery.catalog.special_title"), sx + sw / 2, sy + 7, 0xFFFFFFFF);
		int top = sy + 24;
		SweetUi.card(context, sx + sw / 2 - 22, top, 44, 44, 0xFFFFFFFF, 0xFFFFE9A8);
		SweetUi.item(context, entry.stack(), sx + sw / 2 - 16, top + 6, 2f);
		int ty = top + 50;
		for (var line : textRenderer.wrapLines(entry.stack().getName(), sw - 8)) {
			context.drawText(textRenderer, line, sx + sw / 2 - textRenderer.getWidth(line) / 2, ty, SweetUi.INK, false);
			ty += 10;
		}
		ty += 2;
		String now = String.valueOf(Catalog.price(index, day())), was = String.valueOf(entry.price());
		int total = 12 + textRenderer.getWidth(now) + 4 + textRenderer.getWidth(was);
		int px = sx + sw / 2 - total / 2;
		SweetUi.item(context, new ItemStack(BakeryItems.SWEET_COIN), px, ty - 2, 0.6f);
		context.drawText(textRenderer, now, px + 12, ty, SweetUi.DEEP_PINK, false);
		int wx = px + 12 + textRenderer.getWidth(now) + 4;
		context.drawText(textRenderer, was, wx, ty, 0xFFB0A0A6, false);
		context.fill(wx - 1, ty + 3, wx + textRenderer.getWidth(was) + 1, ty + 4, 0xFFB0A0A6);
		ty += 14;
		boolean locked = isLocked(index);
		if (ty + 14 < sy + sh - 4) {
			boolean hover = SweetUi.button(context, textRenderer, Text.translatable("screen.bakery.catalog.buy"), sx + 10, ty, sw - 20, 14, mouseX, mouseY, !locked);
			if (hover) context.drawTooltip(textRenderer, Text.translatable("screen.bakery.catalog.special_hint"), mouseX, mouseY);
		}
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = left(), y = top();
		int tw = tabW();
		for (int t = 0; t < tabCount(); t++) {
			if (SweetUi.inside(mouseX, mouseY, x + 10 + t * tw, y + 30, tw - 3, 18)) {
				if (t == TAB_ICONS.length) {
					sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.9f);
					MinecraftClient.getInstance().setScreen(new GuideScreen(data));
					return true;
				}
				tab = t;
				shelf = 0;
				scroll = 0;
				sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.2f);
				return true;
			}
		}
		List<String> shelves = shelves();
		int sx = gridX();
		for (int i = 0; i < shelves.size(); i++) {
			int cw = textRenderer.getWidth(Text.translatable("catalog.bakery.shelf." + shelves.get(i))) + 12;
			if (SweetUi.inside(mouseX, mouseY, sx, shelfY(), cw, 15)) {
				shelf = i;
				scroll = 0;
				sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.4f);
				return true;
			}
			sx += cw + 3;
		}
		if (wide()) {
			int featured = Catalog.featured(day());
			int spx = x + 10, sw = 96;
			// The special's Buy button sits under its name; accept any click in the lower part of the card.
			if (SweetUi.inside(mouseX, mouseY, spx, shelfY() + 70, sw, panelH() - 52 - 24 - 74)) {
				buy(featured);
				return true;
			}
		}
		List<Integer> shown = shown();
		int cols = cols(), cw = cardW();
		for (int k = 0; k < visibleRows() * cols; k++) {
			int n = scroll * cols + k;
			if (n >= shown.size()) break;
			int cx = gridX() + (k % cols) * cw, cy = gridY() + (k / cols) * CARD_H;
			if (SweetUi.inside(mouseX, mouseY, cx, cy, cw - 4, CARD_H - 4)) {
				buy(shown.get(n));
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void buy(int index) {
		if (isLocked(index)) {
			sound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f);
			return;
		}
		if (soldOut(index)) {
			sound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f);
			return;
		}
		ClientPlayNetworking.send(new SweetPayloads.Buy(pos, index));
		if (left(index) > 0 && coins() >= Catalog.price(index, day())) stock.set(index, left(index) - 1);
		sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f);
		flash = index;
		flashAt = client != null && client.world != null ? client.world.getTime() : 0;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		scroll = MathHelper.clamp(scroll - (int) Math.signum(vertical), 0, maxScroll());
		return true;
	}

	private void sound(net.minecraft.sound.SoundEvent event, float pitch) {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(event, pitch, 0.5f));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
