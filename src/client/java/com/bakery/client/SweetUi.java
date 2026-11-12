package com.bakery.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Little drawing helpers shared by the pastel screens: rounded cards, soft buttons, scaled icons. */
public final class SweetUi {
	public static final int CREAM = 0xFFFFF7F0;
	public static final int PINK = 0xFFF7B6CF;
	public static final int DEEP_PINK = 0xFFD0567E;
	public static final int INK = 0xFF5A3A46;
	public static final int SOFT = 0xFFB98A9A;
	public static final int LINE = 0xFFF0D2DC;

	private SweetUi() {
	}

	/** A card with rounded (2px) corners and a 1px border. */
	public static void card(DrawContext context, int x, int y, int w, int h, int fill, int border) {
		context.fill(x + 2, y, x + w - 2, y + h, border);
		context.fill(x + 1, y + 1, x + w - 1, y + h - 1, border);
		context.fill(x, y + 2, x + w, y + h - 2, border);
		context.fill(x + 2, y + 1, x + w - 2, y + h - 1, fill);
		context.fill(x + 1, y + 2, x + w - 1, y + h - 2, fill);
	}

	public static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	/** A soft pill button; returns whether the mouse is over it. */
	public static boolean button(DrawContext context, TextRenderer text, Text label, int x, int y, int w, int h, int mouseX, int mouseY, boolean active) {
		boolean hover = active && inside(mouseX, mouseY, x, y, w, h);
		card(context, x, y, w, h, !active ? 0xFFEDE3E6 : hover ? 0xFFFFD6E5 : 0xFFFFEAF1, hover ? DEEP_PINK : PINK);
		context.drawText(text, label, x + w / 2 - text.getWidth(label) / 2, y + (h - 8) / 2, active ? INK : SOFT, false);
		return hover;
	}

	public static void item(DrawContext context, ItemStack stack, float x, float y, float scale) {
		context.getMatrices().push();
		context.getMatrices().translate(x, y, 0);
		context.getMatrices().scale(scale, scale, 1);
		context.drawItem(stack, 0, 0);
		context.getMatrices().pop();
	}

	public static void centered(DrawContext context, TextRenderer text, Text label, int cx, int y, int color) {
		context.drawText(text, label, cx - text.getWidth(label) / 2, y, color, false);
	}

	/** ♥♥♥♡♡ in pink and grey. */
	public static Text hearts(int filled, int total) {
		return Text.literal("♥".repeat(filled)).withColor(0xFF6F91).append(Text.literal("♥".repeat(Math.max(0, total - filled))).withColor(0xD9C6CE));
	}
}
