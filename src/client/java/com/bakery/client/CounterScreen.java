package com.bakery.client;

import com.bakery.network.SweetPayloads;
import com.bakery.registry.BakeryItems;
import com.bakery.shop.CounterBlock;
import com.bakery.shop.CounterBlockEntity;
import com.bakery.shop.Review;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * The bakery counter: flip the OPEN sign, empty the till, and read the review board - every customer's stars for
 * speed, taste and charm, the bakery's overall rating, and what they wrote.
 */
public class CounterScreen extends Screen {
	private static final int W = 330, H = 222, CARD_H = 34, LEFT_W = 116;

	private final BlockPos pos;
	private int scroll;

	public CounterScreen(BlockPos pos) {
		super(Text.translatable("screen.bakery.counter"));
		this.pos = pos;
	}

	private CounterBlockEntity counter() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.world != null && client.world.getBlockEntity(pos) instanceof CounterBlockEntity c ? c : null;
	}

	private int left() {
		return (width - W) / 2;
	}

	private int top() {
		return (height - H) / 2;
	}

	private int listX() {
		return left() + LEFT_W + 14;
	}

	private int listY() {
		return top() + 34;
	}

	private int listH() {
		return H - 34 - 10;
	}

	private int visible() {
		return listH() / CARD_H;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		CounterBlockEntity c = counter();
		if (c == null) {
			close();
			return;
		}
		int x = left(), y = top();
		context.fill(x + 3, y + 4, x + W + 3, y + H + 4, 0x40000000);
		SweetUi.card(context, x, y, W, H, SweetUi.CREAM, SweetUi.PINK);
		// Header with the shop sign.
		SweetUi.card(context, x + 4, y + 4, W - 8, 22, 0xFFF7A8C6, 0xFFF7A8C6);
		SweetUi.item(context, new ItemStack(BakeryItems.BAKERY_COUNTER), x + 8, y + 7, 1f);
		context.drawText(textRenderer, title, x + 27, y + 11, 0xFFFFFFFF, true);
		boolean open = c.getCachedState().get(CounterBlock.OPEN);
		Text sign = Text.translatable(open ? "screen.bakery.counter.open" : "screen.bakery.counter.closed");
		int sw = textRenderer.getWidth(sign) + 16;
		boolean signHover = SweetUi.inside(mouseX, mouseY, x + W - 10 - sw, y + 7, sw, 16);
		SweetUi.card(context, x + W - 10 - sw, y + 7, sw, 16, open ? (signHover ? 0xFFD8F5E4 : 0xFFE6FAEE) : (signHover ? 0xFFFFE0E0 : 0xFFFFFFFF),
				open ? 0xFF5DAA55 : 0xFFB0A0A6);
		context.drawText(textRenderer, sign, x + W - 10 - sw + 8, y + 11, open ? 0xFF3C8A3C : 0xFF9C8C94, false);

		// Left column: overall rating, the three parts, the till.
		int lx = x + 8, ly = y + 32;
		SweetUi.card(context, lx, ly, LEFT_W, 66, 0xFFFFFFFF, SweetUi.LINE);
		float avg = c.average(0);
		List<Review> reviews = c.getReviews();
		if (reviews.isEmpty()) {
			SweetUi.centered(context, textRenderer, Text.translatable("screen.bakery.counter.no_reviews"), lx + LEFT_W / 2, ly + 14, SweetUi.SOFT);
			SweetUi.centered(context, textRenderer, Text.translatable("screen.bakery.counter.no_reviews2"), lx + LEFT_W / 2, ly + 26, SweetUi.SOFT);
		} else {
			String score = String.format("%.1f", avg);
			context.getMatrices().push();
			context.getMatrices().translate(lx + 8, ly + 7, 0);
			context.getMatrices().scale(2f, 2f, 1);
			context.drawText(textRenderer, score, 0, 0, SweetUi.DEEP_PINK, false);
			context.getMatrices().pop();
			stars(context, lx + 12 + textRenderer.getWidth(score) * 2, ly + 7, avg, 1f);
			context.drawText(textRenderer, Text.translatable("screen.bakery.counter.count", reviews.size()), lx + 12 + textRenderer.getWidth(score) * 2,
					ly + 18, SweetUi.SOFT, false);
		}
		String[] parts = {"speed", "taste", "charm"};
		for (int i = 0; i < 3; i++) {
			int ry = ly + 32 + i * 11;
			context.drawText(textRenderer, Text.translatable("screen.bakery.counter.part." + parts[i]), lx + 6, ry, SweetUi.INK, false);
			float v = c.average(i + 1);
			int bx = lx + 48, bw = LEFT_W - 56;
			context.fill(bx, ry + 2, bx + bw, ry + 7, 0xFFF3DDE6);
			context.fill(bx, ry + 2, bx + Math.round(bw * v / 5f), ry + 7, i == 0 ? 0xFF7CC4E8 : i == 1 ? 0xFFF08AAE : 0xFFF2C94C);
		}
		ly += 72;
		SweetUi.card(context, lx, ly, LEFT_W, 50, 0xFFFFFDF2, 0xFFF2C94C);
		SweetUi.item(context, new ItemStack(BakeryItems.SWEET_COIN), lx + 5, ly + 5, 0.9f);
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.till", c.getTill()), lx + 22, ly + 8, 0xFF9A6A10, false);
		boolean collectHover = SweetUi.button(context, textRenderer, Text.translatable("screen.bakery.counter.collect"), lx + 6, ly + 28, LEFT_W - 12, 15,
				mouseX, mouseY, c.getTill() > 0);
		ly += 56;
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.served", c.getServed()), lx + 2, ly, SweetUi.INK, false);
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.walkouts", c.getWalkouts()), lx + 2, ly + 11, SweetUi.SOFT, false);
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.charm", c.getCharm()), lx + 2, ly + 22, SweetUi.SOFT, false);

		// Right: the review board.
		int rx = listX(), rw = x + W - 10 - rx;
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.board"), rx, y + 32 - 1, 0xFFB07A3C, false);
		scroll = MathHelper.clamp(scroll, 0, Math.max(0, reviews.size() - visible()));
		for (int k = 0; k < visible(); k++) {
			int i = scroll + k;
			if (i >= reviews.size()) break;
			Review r = reviews.get(i);
			int cy = listY() + 10 + k * CARD_H;
			SweetUi.card(context, rx, cy, rw, CARD_H - 3, k % 2 == 0 ? 0xFFFFFFFF : 0xFFFFF6F9, SweetUi.LINE);
			ItemStack dish = new ItemStack(Registries.ITEM.get(Identifier.of(r.dish())));
			SweetUi.item(context, dish, rx + 3, cy + 3, 0.75f);
			context.drawText(textRenderer, Text.literal(r.name()), rx + 18, cy + 4, SweetUi.DEEP_PINK, false);
			stars(context, rx + rw - 52, cy + 4, r.stars(), 1f);
			List<OrderedText> lines = textRenderer.wrapLines(Text.translatable(r.line()), rw - 22);
			for (int l = 0; l < Math.min(2, lines.size()); l++) {
				context.drawText(textRenderer, lines.get(l), rx + 18, cy + 14 + l * 9, SweetUi.INK, false);
			}
			// Speed / taste / charm, small, beside the name.
			Text bits = Text.literal("⚡" + (r.speed() > 0 ? r.speed() : "-") + " ♥" + (r.taste() > 0 ? r.taste() : "-") + " ✿" + r.charm());
			context.getMatrices().push();
			context.getMatrices().translate(rx + 22 + textRenderer.getWidth(r.name()), cy + 5, 0);
			context.getMatrices().scale(0.75f, 0.75f, 1);
			context.drawText(textRenderer, bits, 0, 0, SweetUi.SOFT, false);
			context.getMatrices().pop();
		}
		if (reviews.size() > visible()) {
			int sx = x + W - 8, sh = listH() - 4;
			context.fill(sx, listY(), sx + 3, listY() + sh, 0xFFF3DDE6);
			int max = reviews.size() - visible();
			int thumb = Math.max(12, sh * visible() / reviews.size());
			int ty = listY() + (sh - thumb) * scroll / Math.max(1, max);
			context.fill(sx, ty, sx + 3, ty + thumb, SweetUi.DEEP_PINK);
		}
		if (signHover) context.drawTooltip(textRenderer, Text.translatable(open ? "screen.bakery.counter.close_hint" : "screen.bakery.counter.open_hint"), mouseX, mouseY);
		if (collectHover) context.drawTooltip(textRenderer, Text.translatable("screen.bakery.counter.collect_hint"), mouseX, mouseY);
	}

	/** Five little stars, filled up to {@code value} (half stars round). */
	private void stars(DrawContext context, int x, int y, float value, float scale) {
		int full = Math.round(value);
		for (int i = 0; i < 5; i++) {
			context.drawText(textRenderer, "★", x + i * 10, y, i < full ? 0xFFF2B33D : 0xFFE6D8DD, false);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		CounterBlockEntity c = counter();
		if (c != null) {
			int x = left(), y = top();
			boolean open = c.getCachedState().get(CounterBlock.OPEN);
			int sw = textRenderer.getWidth(Text.translatable(open ? "screen.bakery.counter.open" : "screen.bakery.counter.closed")) + 16;
			if (SweetUi.inside(mouseX, mouseY, x + W - 10 - sw, y + 7, sw, 16)) {
				ClientPlayNetworking.send(new SweetPayloads.CounterAction(pos, 0));
				click(1.3f);
				return true;
			}
			if (c.getTill() > 0 && SweetUi.inside(mouseX, mouseY, x + 14, y + 32 + 72 + 28, LEFT_W - 12, 15)) {
				ClientPlayNetworking.send(new SweetPayloads.CounterAction(pos, 1));
				click(1.6f);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		scroll -= (int) Math.signum(vertical);
		return true;
	}

	private void click(float pitch) {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.5f));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
