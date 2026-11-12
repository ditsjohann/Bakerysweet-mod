package com.bakery.client;

import com.bakery.sweet.Sticker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** "Sticker get!" - a little pink toast with the sticker's icon, name and coin reward. */
public class StickerToast implements Toast {
	private static final long DURATION = 5000;
	private final Sticker sticker;

	public StickerToast(Sticker sticker) {
		this.sticker = sticker;
	}

	@Override
	public Visibility draw(DrawContext context, ToastManager manager, long startTime) {
		SweetUi.card(context, 0, 0, getWidth(), getHeight(), 0xFFFFF0F5, SweetUi.PINK);
		// The sticker: a white blob with the icon on it, slightly tilted like it was stuck on in a hurry.
		context.getMatrices().push();
		context.getMatrices().translate(16, 16, 0);
		context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-8));
		SweetUi.card(context, -11, -11, 22, 22, 0xFFFFFFFF, 0xFFF0D2DC);
		context.drawItem(new ItemStack(sticker.icon()), -8, -8);
		context.getMatrices().pop();
		context.drawText(manager.getClient().textRenderer, Text.translatable("toast.bakery.sticker"), 32, 7, SweetUi.DEEP_PINK, false);
		context.drawText(manager.getClient().textRenderer, sticker.title(), 32, 18, SweetUi.INK, false);
		return startTime >= DURATION * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
	}
}
