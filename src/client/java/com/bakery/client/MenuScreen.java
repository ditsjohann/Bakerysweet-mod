package com.bakery.client;

import com.bakery.cafe.CafeLevel;
import com.bakery.network.SweetPayloads;
import com.bakery.shop.CounterBlockEntity;
import com.bakery.shop.Menu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Today's Menu, written on the menu board: every dish the bakery can offer. Unlocked dishes can be ticked on
 * and off (customers only ask for what's on the menu); locked ones show which bakery level brings them.
 */
public class MenuScreen extends Screen {
	private static final int W = 260, H = 196, COLS = 2, ROW_H = 17;

	private final BlockPos counter;

	public MenuScreen(BlockPos counter) {
		super(Text.translatable("screen.bakery.menu"));
		this.counter = counter;
	}

	private CounterBlockEntity counter() {
		MinecraftClient client = MinecraftClient.getInstance();
		return client.world != null && client.world.getBlockEntity(counter) instanceof CounterBlockEntity c ? c : null;
	}

	private int left() {
		return (width - W) / 2;
	}

	private int top() {
		return (height - H) / 2;
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
		// A chalkboard in a pink wooden frame.
		SweetUi.card(context, x - 3, y - 3, W + 6, H + 6, 0xFFE9A0B8, 0xFFC97E98);
		SweetUi.card(context, x, y, W, H, 0xFF3F4A48, 0xFF2E3735);
		SweetUi.centered(context, textRenderer, title, x + W / 2, y + 7, 0xFFFFF4E6);
		CafeLevel level = c.getLevel();
		SweetUi.centered(context, textRenderer, Text.translatable("screen.bakery.menu.level", level.number(), level.displayName()), x + W / 2, y + 18, 0xFFF7B6CF);

		List<Menu.Dish> dishes = Menu.dishes();
		for (int i = 0; i < dishes.size(); i++) {
			Menu.Dish dish = dishes.get(i);
			int dx = x + 10 + (i % COLS) * (W / 2 - 6), dy = y + 32 + (i / COLS) * ROW_H;
			boolean unlocked = dish.level().ordinal() <= level.ordinal();
			boolean on = unlocked && !c.getOffMenu().contains(dish.id());
			boolean hover = unlocked && SweetUi.inside(mouseX, mouseY, dx, dy, W / 2 - 12, ROW_H - 1);
			if (hover) context.fill(dx - 2, dy - 1, dx + W / 2 - 12, dy + ROW_H - 2, 0x30FFFFFF);
			SweetUi.item(context, new ItemStack(dish.item()), dx + 12, dy, 0.85f);
			// Tick box
			context.fill(dx, dy + 3, dx + 9, dy + 12, unlocked ? 0xFFFFF4E6 : 0xFF66706E);
			context.fill(dx + 1, dy + 4, dx + 8, dy + 11, 0xFF3F4A48);
			if (on) context.drawText(textRenderer, "♥", dx + 1, dy + 3, 0xFFFF8DB0, false);
			Text name = unlocked ? dish.item().getName() : Text.translatable("screen.bakery.menu.locked", dish.level().number());
			context.drawText(textRenderer, name, dx + 28, dy + 4, unlocked ? (on ? 0xFFFFF4E6 : 0xFF9AA6A3) : 0xFF77827F, false);
		}
		SweetUi.centered(context, textRenderer, Text.translatable("screen.bakery.menu.hint"), x + W / 2, y + H - 12, 0xFFB8C4C0);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		CounterBlockEntity c = counter();
		if (c != null) {
			int x = left(), y = top();
			List<Menu.Dish> dishes = Menu.dishes();
			for (int i = 0; i < dishes.size(); i++) {
				Menu.Dish dish = dishes.get(i);
				int dx = x + 10 + (i % COLS) * (W / 2 - 6), dy = y + 32 + (i / COLS) * ROW_H;
				if (dish.level().ordinal() <= c.getLevel().ordinal() && SweetUi.inside(mouseX, mouseY, dx, dy, W / 2 - 12, ROW_H - 1)) {
					ClientContent.sendToServer(new SweetPayloads.ToggleDish(counter, dish.id()));
					MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.5f, 0.4f));
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
