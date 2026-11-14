package com.bakery.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The catalog's Recipes tab: every recipe listed on the left by group, and the chosen one on the right as numbered
 * step cards (station, what goes in, what comes out, and what to do). Hover an ingredient to see where to get it,
 * click it to jump to its own recipe.
 */
final class RecipePane {
	private static final int ROW = 18, HEADER = 13, ICON = 18;

	/** Remembered between openings, so the book stays on the page you were reading. */
	private static int selected = 0;
	private static int listScroll = 0;
	private int detailScroll = 0;

	private record Row(RecipeBook.Group header, int entry) {
	}

	private record Hit(int x, int y, ItemStack stack) {
	}

	private final List<Hit> hits = new ArrayList<>();
	private int x, y, w, h, listW, detailH;

	static void showFirstBake() {
		selected = 0;
		listScroll = 0;
	}

	private static List<Row> rows() {
		List<Row> rows = new ArrayList<>();
		RecipeBook.Group group = null;
		List<RecipeBook.Entry> all = RecipeBook.entries();
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).group() != group) {
				group = all.get(i).group();
				if (group != RecipeBook.Group.START) rows.add(new Row(group, -1));
			}
			rows.add(new Row(null, i));
		}
		return rows;
	}

	private static int rowH(Row row) {
		return row.header() != null ? HEADER : ROW;
	}

	private static int listHeight() {
		int total = 0;
		for (Row row : rows()) total += rowH(row);
		return total;
	}

	// ------------------------------------------------------------------ drawing

	/** Draws the pane; returns tooltip lines for whatever the mouse is over (or null). */
	List<Text> render(DrawContext context, TextRenderer text, int x, int y, int w, int h, int mouseX, int mouseY) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.listW = Math.max(96, Math.min(132, w / 3));
		hits.clear();
		List<RecipeBook.Entry> all = RecipeBook.entries();
		selected = MathHelper.clamp(selected, 0, all.size() - 1);

		// ---- the list
		SweetUi.card(context, x, y, listW, h, 0xFFFFFFFF, SweetUi.LINE);
		listScroll = MathHelper.clamp(listScroll, 0, Math.max(0, listHeight() - (h - 4)));
		context.enableScissor(x + 1, y + 2, x + listW - 1, y + h - 2);
		int ry = y + 2 - listScroll;
		for (Row row : rows()) {
			int rh = rowH(row);
			if (ry + rh > y && ry < y + h) {
				if (row.header() != null) {
					context.drawText(text, row.header().displayName(), x + 5, ry + 3, SweetUi.DEEP_PINK, false);
				} else {
					RecipeBook.Entry e = all.get(row.entry());
					boolean active = row.entry() == selected;
					boolean hover = SweetUi.inside(mouseX, mouseY, x + 2, ry, listW - 4, rh - 1) && mouseY >= y && mouseY < y + h;
					boolean start = e.group() == RecipeBook.Group.START;
					if (active || hover || start) {
						SweetUi.card(context, x + 2, ry, listW - 4, rh - 1, active ? 0xFFFFE0EC : start ? 0xFFFFF3C4 : 0xFFFFF7FA,
								active ? SweetUi.DEEP_PINK : start ? 0xFFF2C94C : 0xFFFFFFFF);
					}
					SweetUi.item(context, e.result(), x + 4, ry + 1, 0.9f);
					String name = text.trimToWidth(e.name().getString(), listW - 26);
					context.drawText(text, name, x + 21, ry + 5, active ? SweetUi.INK : 0xFF8A6A76, false);
				}
			}
			ry += rh;
		}
		context.disableScissor();
		if (listHeight() > h - 4) scrollbar(context, x + listW - 4, y + 2, h - 4, listScroll, listHeight() - (h - 4));

		// ---- the chosen recipe
		RecipeBook.Entry entry = all.get(selected);
		int dx = x + listW + 6, dw = w - listW - 6;
		SweetUi.card(context, dx, y, dw, h, SweetUi.CREAM, SweetUi.LINE);
		// Title strip
		SweetUi.card(context, dx + 3, y + 3, dw - 6, 26, 0xFFFFEAF1, 0xFFFFEAF1);
		SweetUi.item(context, entry.result(), dx + 6, y + 5, 1.35f);
		context.getMatrices().push();
		context.getMatrices().translate(dx + 32, y + 7, 0);
		context.getMatrices().scale(1.15f, 1.15f, 1);
		context.drawText(text, entry.name(), 0, 0, SweetUi.INK, false);
		context.getMatrices().pop();
		context.drawText(text, entry.group().displayName(), dx + 32, y + 19, SweetUi.SOFT, false);
		if (entry.result().getCount() > 1 || entry.steps().stream().anyMatch(s -> s.output().getCount() > 1 && s.output().isOf(entry.result().getItem()))) {
			ItemStack made = entry.steps().stream().map(RecipeBook.Step::output).filter(o -> o.isOf(entry.result().getItem())).findFirst().orElse(entry.result());
			Text makes = Text.translatable("recipe.bakery.makes", made.getCount());
			context.drawText(text, makes, dx + dw - 8 - text.getWidth(makes), y + 19, SweetUi.DEEP_PINK, false);
		}

		int top = y + 32;
		detailH = h - 34;
		int content = 0;
		List<int[]> cards = new ArrayList<>();
		for (RecipeBook.Step step : entry.steps()) {
			int lines = text.wrapLines(step.text(), dw - 40).size();
			int ch = 6 + ICON + 3 + lines * 9 + 4;
			cards.add(new int[]{content, ch});
			content += ch + 3;
		}
		List<OrderedText> tip = entry.tip().getString().isEmpty() ? List.of() : text.wrapLines(entry.tip(), dw - 28);
		if (!tip.isEmpty()) content += tip.size() * 9 + 10;
		detailScroll = MathHelper.clamp(detailScroll, 0, Math.max(0, content - detailH));
		context.enableScissor(dx + 1, top, dx + dw - 1, top + detailH);
		for (int i = 0; i < entry.steps().size(); i++) {
			RecipeBook.Step step = entry.steps().get(i);
			int cy = top + cards.get(i)[0] - detailScroll, ch = cards.get(i)[1];
			if (cy + ch < top || cy > top + detailH) continue;
			drawStep(context, text, step, i + 1, dx + 4, cy, dw - 12, ch, mouseX, mouseY, top);
		}
		if (!tip.isEmpty()) {
			int ty = top + content - tip.size() * 9 - 8 - detailScroll;
			SweetUi.card(context, dx + 4, ty, dw - 12, tip.size() * 9 + 6, 0xFFFFF8DA, 0xFFFFE9A8);
			for (int i = 0; i < tip.size(); i++) context.drawText(text, tip.get(i), dx + 9, ty + 4 + i * 9, 0xFF9A6A10, false);
		}
		context.disableScissor();
		if (content > detailH) scrollbar(context, dx + dw - 5, top, detailH, detailScroll, content - detailH);

		// Tooltip for an icon under the mouse
		for (Hit hit : hits) {
			if (SweetUi.inside(mouseX, mouseY, hit.x(), hit.y(), 16, 16) && mouseY >= top && mouseY < top + detailH) {
				List<Text> lines = new ArrayList<>();
				lines.add(hit.stack().getName().copy().formatted(Formatting.WHITE));
				for (Text source : RecipeBook.sources(hit.stack().getItem())) lines.add(source.copy().withColor(0xFFF7B6CF));
				return lines;
			}
		}
		return null;
	}

	private void drawStep(DrawContext context, TextRenderer text, RecipeBook.Step step, int number, int cx, int cy, int cw, int ch,
						  int mouseX, int mouseY, int clipTop) {
		SweetUi.card(context, cx, cy, cw, ch, step.optional() ? 0xFFFBF7FF : 0xFFFFFFFF, step.optional() ? 0xFFE3D6F5 : 0xFFF6D3E0);
		// Number bubble
		SweetUi.card(context, cx + 3, cy + 4, 14, 14, step.optional() ? 0xFFCDB4F0 : SweetUi.DEEP_PINK, step.optional() ? 0xFFCDB4F0 : SweetUi.DEEP_PINK);
		SweetUi.centered(context, text, Text.literal(String.valueOf(number)), cx + 10, cy + 7, 0xFFFFFFFF);
		// Station, then what goes in, then the result
		int ix = cx + 22, iy = cy + 3;
		icon(context, text, step.station(), ix, iy, 0xFFFFF0F5);
		ix += ICON + 2;
		if (!step.inputs().isEmpty()) {
			context.drawText(text, ":", ix, iy + 5, SweetUi.SOFT, false);
			ix += 5;
			for (ItemStack in : step.inputs()) {
				if (ix + ICON > cx + cw - (step.output().isEmpty() ? 4 : 30)) break;
				icon(context, text, in, ix, iy, 0x00000000);
				ix += ICON;
			}
		}
		if (!step.output().isEmpty()) {
			context.drawText(text, "→", ix + 3, iy + 5, SweetUi.DEEP_PINK, false);
			icon(context, text, step.output(), ix + 12, iy, 0xFFFFF3C4);
		}
		if (step.optional()) {
			Text opt = Text.translatable("recipe.bakery.optional");
			context.drawText(text, opt, cx + cw - 4 - text.getWidth(opt), cy + 4, 0xFFA48BC8, false);
		}
		int ty = cy + 6 + ICON;
		for (OrderedText line : text.wrapLines(step.text(), cw - 26)) {
			context.drawText(text, line, cx + 22, ty, SweetUi.INK, false);
			ty += 9;
		}
	}

	private void icon(DrawContext context, TextRenderer text, ItemStack stack, int ix, int iy, int plate) {
		if ((plate >>> 24) != 0) SweetUi.card(context, ix - 1, iy - 1, ICON, ICON, plate, plate);
		context.drawItem(stack, ix, iy);
		context.drawItemInSlot(text, stack, ix, iy);
		hits.add(new Hit(ix, iy, stack));
	}

	private static void scrollbar(DrawContext context, int sx, int sy, int sh, int scroll, int max) {
		context.fill(sx, sy, sx + 3, sy + sh, 0xFFF3DDE6);
		int thumb = Math.max(12, sh * sh / (sh + max));
		int ty = sy + (sh - thumb) * scroll / Math.max(1, max);
		context.fill(sx, ty, sx + 3, ty + thumb, SweetUi.DEEP_PINK);
	}

	// ------------------------------------------------------------------ input

	boolean click(double mouseX, double mouseY) {
		if (SweetUi.inside(mouseX, mouseY, x, y, listW, h)) {
			int ry = y + 2 - listScroll;
			for (Row row : rows()) {
				int rh = rowH(row);
				if (row.header() == null && mouseY >= ry && mouseY < ry + rh) {
					select(row.entry());
					return true;
				}
				ry += rh;
			}
			return true;
		}
		for (Hit hit : hits) {
			if (SweetUi.inside(mouseX, mouseY, hit.x(), hit.y(), 16, 16)) {
				int index = RecipeBook.indexOf(hit.stack().getItem());
				if (index >= 0 && index != selected) {
					select(index);
					scrollListTo(index);
				}
				return true;
			}
		}
		return SweetUi.inside(mouseX, mouseY, x, y, w, h);
	}

	private void select(int index) {
		selected = index;
		detailScroll = 0;
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.3f, 0.5f));
	}

	private void scrollListTo(int index) {
		int ry = 0;
		for (Row row : rows()) {
			if (row.header() == null && row.entry() == index) break;
			ry += rowH(row);
		}
		if (ry < listScroll || ry > listScroll + h - 24) listScroll = Math.max(0, ry - 20);
	}

	boolean scroll(double mouseX, double mouseY, double amount) {
		int step = (int) Math.signum(amount) * 16;
		if (mouseX < x + listW) listScroll = Math.max(0, listScroll - step);
		else detailScroll = Math.max(0, detailScroll - step);
		return true;
	}
}
