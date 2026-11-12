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
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The Bakery Catalog: six tabs of goodies to buy with Sweet Coins, plus a Read tab that opens the bakery guide.
 * Fancier goodies are locked until the bakery reaches their level. One Daily Special a day.
 */
public class CatalogScreen extends Screen {
	private static final int W = 300, H = 206;
	private static final int COLS = 7, ROWS = 4, CELL_W = 39, CELL_H = 30;
	private static final Item[] TAB_ICONS = {BakeryItems.MIXING_BOWL, BakeryItems.FLOUR, BakeryItems.STRAWBERRY_SEEDS, BakeryItems.PINK_HEART_BALLOON,
			BakeryItems.TULIP_POT, BakeryItems.BUNNY_EARS};
	private static final int READ = -1;

	private final BlockPos pos;
	private final SweetPayloads.GuideData data;
	private int tab;
	private int page;

	public CatalogScreen(BlockPos pos, SweetPayloads.GuideData data) {
		super(Text.translatable("screen.bakery.catalog"));
		this.pos = pos;
		this.data = data;
	}

	private int left() {
		return (width - W) / 2;
	}

	private int top() {
		return (height - H) / 2;
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
			(isLocked(i) ? locked : open).add(i);
		}
		open.addAll(locked);
		return open;
	}

	private boolean isLocked(int index) {
		return !creative() && Catalog.entries().get(index).level().ordinal() > level().ordinal();
	}

	private int pages() {
		return Math.max(1, (shown().size() + COLS * ROWS - 1) / (COLS * ROWS));
	}

	private int coins() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.player == null ? 0 : Coins.count(client.player);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x = left(), y = top();
		SweetUi.card(context, x, y, W, H, SweetUi.CREAM, SweetUi.PINK);
		context.drawText(textRenderer, title, x + 10, y + 8, SweetUi.DEEP_PINK, false);
		Text balance = Text.literal(String.valueOf(coins()));
		int bx = x + W - 14 - textRenderer.getWidth(balance);
		SweetUi.item(context, new ItemStack(BakeryItems.SWEET_COIN), bx - 12, y + 5, 0.7f);
		context.drawText(textRenderer, balance, bx, y + 8, SweetUi.INK, false);

		// Bakery level badge
		CafeLevel level = level();
		Text badge = Text.translatable("screen.bakery.catalog.level", level.number(), level.displayName());
		int badgeX = x + 12 + textRenderer.getWidth(title);
		context.drawText(textRenderer, badge, badgeX, y + 8, 0xFFB07A90, false);

		// Tabs: six shop tabs and the Read tab
		for (int t = 0; t <= TAB_ICONS.length; t++) {
			int tx = x + 10 + t * 40, ty = y + 22;
			boolean read = t == TAB_ICONS.length;
			boolean active = read ? tab == READ : tab == t;
			boolean hover = SweetUi.inside(mouseX, mouseY, tx, ty, 37, 18);
			SweetUi.card(context, tx, ty, 37, 18, active ? 0xFFFFD6E5 : hover ? 0xFFFFF0F5 : 0xFFFFFFFF, active ? SweetUi.DEEP_PINK : SweetUi.LINE);
			SweetUi.item(context, new ItemStack(read ? BakeryItems.BAKERY_GUIDE : TAB_ICONS[t]), tx + 11, ty + 1, 1f);
		}
		Text tabName = tab == READ ? Text.translatable("screen.bakery.catalog.read") : Catalog.Category.values()[tab].displayName();
		context.drawText(textRenderer, tabName, x + 12, y + 44, SweetUi.DEEP_PINK, false);

		// Daily special banner
		int featured = Catalog.featured(day());
		Catalog.Entry special = Catalog.entries().get(featured);
		Text banner = Text.translatable("screen.bakery.catalog.special", special.stack().getName(), Catalog.price(featured, day()));
		int bannerW = textRenderer.getWidth(banner) + 12;
		SweetUi.card(context, x + W - 10 - bannerW, y + 41, bannerW, 13, 0xFFFFF3C4, 0xFFF2C94C);
		context.drawText(textRenderer, banner, x + W - 4 - bannerW, y + 44, 0xFF9A6A10, false);

		// Goodies
		List<Integer> shown = shown();
		page = Math.min(page, pages() - 1);
		int gx = x + 12, gy = y + 58;
		Integer hovered = null;
		for (int k = 0; k < COLS * ROWS; k++) {
			int n = page * COLS * ROWS + k;
			if (n >= shown.size()) break;
			int index = shown.get(n);
			Catalog.Entry entry = Catalog.entries().get(index);
			boolean locked = isLocked(index);
			int cx = gx + (k % COLS) * CELL_W, cy = gy + (k / COLS) * CELL_H;
			int price = Catalog.price(index, day());
			boolean affordable = coins() >= price || creative();
			boolean hover = SweetUi.inside(mouseX, mouseY, cx, cy, CELL_W - 4, CELL_H - 3);
			int fill = locked ? 0xFFF1E9EC : hover ? 0xFFFFE7F0 : 0xFFFFFFFF;
			SweetUi.card(context, cx, cy, CELL_W - 4, CELL_H - 3, fill, index == featured ? 0xFFF2C94C : hover && !locked ? SweetUi.DEEP_PINK : SweetUi.LINE);
			context.drawItem(entry.stack(), cx + (CELL_W - 4) / 2 - 8, cy + 2);
			context.drawItemInSlot(textRenderer, entry.stack(), cx + (CELL_W - 4) / 2 - 8, cy + 2);
			if (locked) {
				context.fill(cx + 1, cy + 1, cx + CELL_W - 5, cy + CELL_H - 4, 0xA0F6EEF1);
			}
			Text priceText = locked ? Text.translatable("screen.bakery.catalog.lv", entry.level().number()) : Text.literal(price + "c");
			context.getMatrices().push();
			context.getMatrices().translate(cx + (CELL_W - 4) / 2f - textRenderer.getWidth(priceText) * 0.35f, cy + 20, 200);
			context.getMatrices().scale(0.7f, 0.7f, 1);
			context.drawText(textRenderer, priceText, 0, 0, locked ? 0xFF9C8C94 : affordable ? 0xFFD0567E : 0xFFB0A0A6, false);
			context.getMatrices().pop();
			if (hover) hovered = index;
		}

		CafeLevel next = level.next();
		Text footer = next == null ? Text.translatable("screen.bakery.catalog.max_level")
				: Text.translatable("screen.bakery.catalog.next_level", next.served() - data.served(), next.displayName());
		if (pages() > 1) {
			SweetUi.button(context, textRenderer, Text.literal("<"), x + W - 70, y + H - 17, 16, 12, mouseX, mouseY, page > 0);
			SweetUi.centered(context, textRenderer, Text.literal((page + 1) + "/" + pages()), x + W - 43, y + H - 15, SweetUi.SOFT);
			SweetUi.button(context, textRenderer, Text.literal(">"), x + W - 30, y + H - 17, 16, 12, mouseX, mouseY, page < pages() - 1);
		}
		context.drawText(textRenderer, footer, x + 12, y + H - 15, SweetUi.SOFT, false);

		if (hovered != null) {
			Catalog.Entry entry = Catalog.entries().get(hovered);
			List<Text> lines = new ArrayList<>(getTooltipFromItem(MinecraftClient.getInstance(), entry.stack()));
			if (isLocked(hovered)) {
				lines.add(Text.translatable("screen.bakery.catalog.locked", entry.level().number(), entry.level().displayName()).withColor(0xFF9C8C94));
			} else {
				lines.add(Text.translatable("screen.bakery.catalog.price", Catalog.price(hovered, day())).withColor(0xFFD0567E));
			}
			context.drawTooltip(textRenderer, lines, mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x = left(), y = top();
		for (int t = 0; t <= TAB_ICONS.length; t++) {
			if (SweetUi.inside(mouseX, mouseY, x + 10 + t * 40, y + 22, 37, 18)) {
				if (t == TAB_ICONS.length) {
					sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 0.9f);
					MinecraftClient.getInstance().setScreen(new GuideScreen(data));
					return true;
				}
				tab = t;
				page = 0;
				sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.2f);
				return true;
			}
		}
		if (pages() > 1) {
			if (SweetUi.inside(mouseX, mouseY, x + W - 70, y + H - 17, 16, 12) && page > 0) {
				page--;
				sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f);
				return true;
			}
			if (SweetUi.inside(mouseX, mouseY, x + W - 30, y + H - 17, 16, 12) && page < pages() - 1) {
				page++;
				sound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f);
				return true;
			}
		}
		List<Integer> shown = shown();
		int gx = x + 12, gy = y + 58;
		for (int k = 0; k < COLS * ROWS; k++) {
			int n = page * COLS * ROWS + k;
			if (n >= shown.size()) break;
			int cx = gx + (k % COLS) * CELL_W, cy = gy + (k / COLS) * CELL_H;
			if (SweetUi.inside(mouseX, mouseY, cx, cy, CELL_W - 4, CELL_H - 3)) {
				if (isLocked(shown.get(n))) {
					sound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.8f);
					return true;
				}
				ClientPlayNetworking.send(new SweetPayloads.Buy(pos, shown.get(n)));
				sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void sound(net.minecraft.sound.SoundEvent event, float pitch) {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(event, pitch, 0.5f));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
