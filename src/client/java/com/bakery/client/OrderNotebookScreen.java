package com.bakery.client;

import com.bakery.cafe.Regular;
import com.bakery.registry.BakeryItems;
import com.bakery.shop.OrderNotebookBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/** Every open order in the world, read from an Order Notebook: who wants what, where they wait, and how patient they are. */
public class OrderNotebookScreen extends Screen {
	private static final int ROW = 24;
	private final BlockPos pos;
	private int scroll;

	public OrderNotebookScreen(BlockPos pos) {
		super(Text.translatable("block.bakery.order_notebook"));
		this.pos = pos;
	}

	private List<OrderNotebookBlockEntity.Entry> entries() {
		return client != null && client.world != null && client.world.getBlockEntity(pos) instanceof OrderNotebookBlockEntity n ? n.getEntries() : List.of();
	}

	private int panelW() {
		return Math.min(width - 12, 300);
	}

	private int panelH() {
		return Math.min(height - 12, 240);
	}

	private int visible() {
		return Math.max(1, (panelH() - 44) / ROW);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int w = panelW(), h = panelH(), x = (width - w) / 2, y = (height - h) / 2;
		context.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x40000000);
		SweetUi.card(context, x, y, w, h, 0xFFFFFDF6, SweetUi.PINK);
		SweetUi.item(context, new ItemStack(BakeryItems.ORDER_NOTEBOOK), x + 8, y + 7, 1f);
		context.drawText(textRenderer, title, x + 28, y + 11, SweetUi.DEEP_PINK, false);
		List<OrderNotebookBlockEntity.Entry> entries = entries();
		Text count = Text.translatable("block.bakery.order_notebook.page", entries.size());
		context.drawText(textRenderer, count, x + w - 10 - textRenderer.getWidth(count), y + 11, SweetUi.SOFT, false);
		context.fill(x + 8, y + 26, x + w - 8, y + 27, SweetUi.LINE);
		scroll = MathHelper.clamp(scroll, 0, Math.max(0, entries.size() - visible()));
		if (entries.isEmpty()) {
			SweetUi.centered(context, textRenderer, Text.translatable("block.bakery.order_notebook.none_long"), x + w / 2, y + 50, SweetUi.SOFT);
		}
		for (int i = 0; i < visible() && scroll + i < entries.size(); i++) {
			OrderNotebookBlockEntity.Entry e = entries.get(scroll + i);
			int ry = y + 32 + i * ROW;
			SweetUi.card(context, x + 8, ry, w - 16, ROW - 3, e.seated() ? 0xFFF4FBF6 : 0xFFFFF7FA, e.seated() ? 0xFFCDEBD6 : 0xFFF6D3E0);
			context.drawItem(e.wanted(), x + 11, ry + 2);
			context.drawText(textRenderer, Regular.get(e.regular()).displayName(), x + 31, ry + 3, SweetUi.INK, false);
			context.drawText(textRenderer, e.wanted().getName(), x + 31, ry + 12, SweetUi.SOFT, false);
			Text where = Text.translatable(e.seated() ? "screen.bakery.counter.at_table" : e.waiting() ? "screen.bakery.counter.in_line"
					: "screen.bakery.counter.walking");
			int wx = x + w - 12 - textRenderer.getWidth(where);
			context.drawText(textRenderer, where, wx, ry + 3, e.seated() ? 0xFF4FA060 : SweetUi.SOFT, false);
			Text hearts = SweetUi.hearts(MathHelper.ceil(e.patience() * 5), 5);
			context.drawText(textRenderer, hearts, x + w - 12 - textRenderer.getWidth(hearts), ry + 12, 0xFFFFFFFF, false);
		}
		if (entries.size() > visible()) {
			Text page = Text.literal((scroll + 1) + "-" + Math.min(entries.size(), scroll + visible()) + " / " + entries.size());
			SweetUi.centered(context, textRenderer, page, x + w / 2, y + h - 12, SweetUi.SOFT);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		scroll -= (int) Math.signum(vertical);
		return true;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
