package com.bakery.client;

import com.bakery.registry.BakeryItems;
import com.bakery.sweet.CakeDesign;
import com.bakery.sweet.IcingBagItem;
import com.bakery.sweet.TurntableBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * While you look at a cake on the turntable, a small card above the hotbar says what the tool in your hand will do:
 * which piping tip and colour you have, and what a click, a sneak-click and a sneak-use in the air do.
 */
public final class DecoratingHud {
	private DecoratingHud() {
	}

	public static void render(DrawContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.world == null || client.currentScreen != null || client.options.hudHidden) return;
		if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
		if (!(client.world.getBlockEntity(hit.getBlockPos()) instanceof TurntableBlockEntity table) || table.getCake().isEmpty()) return;
		ItemStack held = client.player.getMainHandStack();
		Text title;
		Text sub = null;
		String hint;
		int accent = SweetUi.DEEP_PINK;
		if (held.getItem() instanceof IcingBagItem bag) {
			title = IcingBagItem.tip(held).displayName().copy().append(" · ").append(bag.icing().displayName());
			sub = Text.translatable("hud.bakery.tip." + IcingBagItem.tip(held).name().toLowerCase());
			hint = "hud.bakery.cake.bag";
			accent = 0xFF000000 | bag.icing().color();
		} else if (held.isOf(BakeryItems.CAKE_SCRAPER)) {
			title = held.getName();
			hint = "hud.bakery.cake.scraper";
		} else if (held.isOf(BakeryItems.SPATULA)) {
			title = held.getName();
			hint = "hud.bakery.cake.spatula";
		} else if (CakeDesign.isTopper(held)) {
			title = held.getName();
			hint = "hud.bakery.cake.topper";
		} else if (held.isEmpty()) {
			title = Text.translatable("hud.bakery.cake.title", table.design().cuteness());
			hint = "hud.bakery.cake.hand";
		} else {
			return;
		}
		Text line = Text.translatable(hint);
		int w = Math.max(Math.max(client.textRenderer.getWidth(title), client.textRenderer.getWidth(line)),
				sub == null ? 0 : client.textRenderer.getWidth(sub)) + 16;
		int h = sub == null ? 26 : 36;
		int x = context.getScaledWindowWidth() / 2 - w / 2, y = context.getScaledWindowHeight() - 60 - h;
		SweetUi.card(context, x, y, w, h, 0xE8FFF7F0, SweetUi.PINK);
		context.fill(x + 4, y + 4, x + 7, y + 12, accent);
		context.drawText(client.textRenderer, title, x + 10, y + 4, SweetUi.INK, false);
		int ly = y + 15;
		if (sub != null) {
			context.drawText(client.textRenderer, sub, x + 8, ly, 0xFFB07A3C, false);
			ly += 10;
		}
		context.drawText(client.textRenderer, line, x + 8, ly, 0xFF8A6A76, false);
	}
}
