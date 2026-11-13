package com.bakery.client;

import com.bakery.cafe.CafeLevel;
import com.bakery.cafe.Regular;
import com.bakery.network.SweetPayloads;
import com.bakery.registry.BakeryItems;
import com.bakery.shop.CounterBlock;
import com.bakery.shop.CounterBlockEntity;
import com.bakery.shop.Order;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * The counter screen: open or close up, empty the till, edit the menu, see who is waiting for what (in line or at a
 * table, and how patient they still are), today's numbers, and every perk the bakery's level has unlocked so far
 * (the locked ones say which level brings them).
 */
public class CounterScreen extends Screen {
	private final BlockPos pos;

	public CounterScreen(BlockPos pos) {
		super(Text.translatable("screen.bakery.counter"));
		this.pos = pos;
	}

	private CounterBlockEntity counter() {
		return client != null && client.world != null && client.world.getBlockEntity(pos) instanceof CounterBlockEntity c ? c : null;
	}

	private int panelW() {
		return Math.min(width - 12, 420);
	}

	private int panelH() {
		return Math.min(height - 12, 250);
	}

	private int left() {
		return (width - panelW()) / 2;
	}

	private int top() {
		return (height - panelH()) / 2;
	}

	private static final int BTN_W = 120, BTN_H = 16;

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		CounterBlockEntity counter = counter();
		if (counter == null) {
			close();
			return;
		}
		int x = left(), y = top(), w = panelW(), h = panelH();
		context.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x40000000);
		SweetUi.card(context, x, y, w, h, SweetUi.CREAM, SweetUi.PINK);
		// Header: name, level, the till.
		SweetUi.card(context, x + 4, y + 4, w - 8, 22, 0xFFA8D8F7, 0xFFA8D8F7);
		SweetUi.item(context, new ItemStack(BakeryItems.BAKERY_COUNTER), x + 8, y + 7, 1f);
		context.getMatrices().push();
		context.getMatrices().translate(x + 27, y + 9, 0);
		context.getMatrices().scale(1.3f, 1.3f, 1);
		context.drawText(textRenderer, title, 0, 0, 0xFFFFFFFF, true);
		context.getMatrices().pop();
		CafeLevel level = counter.getLevel();
		Text badge = Text.translatable("screen.bakery.catalog.level", level.number(), level.displayName());
		int bw = textRenderer.getWidth(badge) + 10;
		SweetUi.card(context, x + w - 8 - bw, y + 7, bw, 16, 0xFFFFFFFF, 0xFFFFFFFF);
		context.drawText(textRenderer, badge, x + w - 3 - bw, y + 11, SweetUi.DEEP_PINK, false);

		// Left column: buttons and today's numbers.
		int lx = x + 10, ly = y + 32;
		boolean open = counter.isOpen();
		SweetUi.button(context, textRenderer, Text.translatable(open ? "screen.bakery.counter.close_up" : "screen.bakery.counter.open_up"), lx, ly, BTN_W, BTN_H,
				mouseX, mouseY, true);
		SweetUi.button(context, textRenderer, Text.translatable("screen.bakery.counter.collect", counter.getTill()), lx, ly + 20, BTN_W, BTN_H, mouseX, mouseY,
				counter.getTill() > 0);
		SweetUi.button(context, textRenderer, Text.translatable("screen.bakery.counter.menu"), lx, ly + 40, BTN_W, BTN_H, mouseX, mouseY, true);
		int sy = ly + 64;
		context.drawText(textRenderer, Text.translatable(open ? "screen.bakery.counter.is_open" : "screen.bakery.counter.is_closed"), lx, sy,
				open ? 0xFF4FA060 : SweetUi.SOFT, false);
		if (!counter.hasPath()) {
			for (OrderedText line : textRenderer.wrapLines(Text.translatable("screen.bakery.counter.no_path"), BTN_W)) {
				sy += 10;
				context.drawText(textRenderer, line, lx, sy, 0xFFD08A30, false);
			}
		}
		sy += 14;
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.served", counter.getServed()), lx, sy, SweetUi.INK, false);
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.walkouts", counter.getWalkouts()), lx, sy + 10, SweetUi.INK, false);
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.charm").append(" ").append(SweetUi.hearts(counter.getCharm(), 5)), lx, sy + 20,
				SweetUi.INK, false);
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.capacity", counter.maxCustomers()), lx, sy + 30, SweetUi.INK, false);
		// Progress to the next level
		CafeLevel next = level.next();
		int by = y + h - 30;
		if (next != null) {
			float progress = MathHelper.clamp((counter.getOwnerServed() - level.served()) / (float) Math.max(1, next.served() - level.served()), 0, 1);
			SweetUi.card(context, lx, by, BTN_W, 9, 0xFFFFFFFF, 0xFFF3DDE6);
			context.fill(lx + 2, by + 2, lx + 2 + Math.round((BTN_W - 4) * progress), by + 7, SweetUi.DEEP_PINK);
			context.drawText(textRenderer, Text.translatable("screen.bakery.counter.next", next.served() - counter.getOwnerServed(), next.displayName()),
					lx, by + 12, SweetUi.SOFT, false);
		}

		// Middle: who is waiting.
		int mx = x + BTN_W + 18, mw = (w - BTN_W - 28) / 2 + 10;
		SweetUi.card(context, mx, y + 32, mw, h - 42, 0xFFFFFFFF, SweetUi.LINE);
		List<Order> orders = counter.getOrders();
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.orders", orders.size(), counter.maxCustomers()), mx + 5, y + 36,
				SweetUi.DEEP_PINK, false);
		int oy = y + 48;
		if (orders.isEmpty()) {
			for (OrderedText line : textRenderer.wrapLines(Text.translatable(open ? "screen.bakery.counter.no_orders" : "screen.bakery.counter.closed_hint"), mw - 10)) {
				context.drawText(textRenderer, line, mx + 5, oy, SweetUi.SOFT, false);
				oy += 10;
			}
		}
		for (Order order : orders) {
			if (oy > y + h - 30) break;
			drawOrder(context, order, mx + 3, oy, mw - 6);
			oy += 24;
		}

		// Right: the perks of every level.
		int px = mx + mw + 6, pw = x + w - 10 - px;
		SweetUi.card(context, px, y + 32, pw, h - 42, 0xFFFFF8FB, SweetUi.LINE);
		context.drawText(textRenderer, Text.translatable("screen.bakery.counter.perks"), px + 5, y + 36, SweetUi.DEEP_PINK, false);
		int py = y + 48;
		for (CafeLevel l : CafeLevel.values()) {
			boolean have = l.ordinal() <= level.ordinal();
			for (String key : perksOf(l)) {
				Text line = Text.literal(have ? "✔ " : "Lv " + l.number() + " ").append(Text.translatable(key));
				for (OrderedText wrapped : textRenderer.wrapLines(line, pw - 10)) {
					if (py > y + h - 18) break;
					context.drawText(textRenderer, wrapped, px + 5, py, have ? SweetUi.INK : 0xFFB0A0A6, false);
					py += 9;
				}
				py += 2;
			}
		}
	}

	/** A level's perks, plus the extra customer it brings. */
	private static List<String> perksOf(CafeLevel level) {
		List<String> keys = new java.util.ArrayList<>();
		if (level.ordinal() > 0) keys.add("counter.bakery.perk.customers." + level.maxCustomers());
		keys.addAll(level.perks());
		return keys;
	}

	private void drawOrder(DrawContext context, Order order, int ox, int oy, int ow) {
		SweetUi.card(context, ox, oy, ow, 22, order.seated ? 0xFFF4FBF6 : 0xFFFFF7FA, order.seated ? 0xFFCDEBD6 : 0xFFF6D3E0);
		context.drawItem(order.wanted, ox + 3, oy + 3);
		Regular regular = Regular.get(order.regular);
		context.drawText(textRenderer, textRenderer.trimToWidth(regular.displayName().getString(), ow - 26), ox + 22, oy + 3, SweetUi.INK, false);
		Text where = Text.translatable(order.seated ? "screen.bakery.counter.at_table" : order.waiting ? "screen.bakery.counter.in_line"
				: "screen.bakery.counter.walking");
		context.drawText(textRenderer, where, ox + 22, oy + 12, order.seated ? 0xFF4FA060 : SweetUi.SOFT, false);
		int hearts = MathHelper.ceil(order.patienceFraction() * 5);
		Text heartText = SweetUi.hearts(hearts, 5);
		context.drawText(textRenderer, heartText, ox + ow - 3 - textRenderer.getWidth(heartText), oy + 12, 0xFFFFFFFF, false);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int lx = left() + 10, ly = top() + 32;
		if (SweetUi.inside(mouseX, mouseY, lx, ly, BTN_W, BTN_H)) {
			send(CounterBlock.OPEN_SIGN);
			return true;
		}
		if (SweetUi.inside(mouseX, mouseY, lx, ly + 20, BTN_W, BTN_H)) {
			send(CounterBlock.COLLECT);
			return true;
		}
		if (SweetUi.inside(mouseX, mouseY, lx, ly + 40, BTN_W, BTN_H) && client != null) {
			client.setScreen(new MenuScreen(pos));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void send(int action) {
		ClientPlayNetworking.send(new SweetPayloads.CounterAction(pos, action));
		if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.3f, 0.5f));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
