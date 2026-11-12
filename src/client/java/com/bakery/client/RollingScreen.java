package com.bakery.client;

import com.bakery.Bakery;
import com.bakery.baking.BakingRecipes;
import com.bakery.food.BakeryFoods;
import com.bakery.network.ShapeDoughPayload;
import com.bakery.registry.BakeryItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.lwjgl.glfw.GLFW;

/**
 * The rolling pin minigame, drawn right on a wooden board with no frames.
 * Pick what to make, then press SPACE (or click) while the pin passes over the golden mark. Five rolls.
 */
public class RollingScreen extends Screen {
	private static final Identifier BOARD = Bakery.id("textures/block/pastry_board.png");
	private static final Identifier DOUGH = Bakery.id("textures/block/dough.png");
	private static final int ROUNDS = 5;
	private static final float PERFECT_WIDTH = 0.04f;

	private enum Phase { CHOOSE, ROLL, DONE }

	private final BlockPos pos;
	private final BakingRecipes.DoughType dough;
	private final Random random = Random.create();

	private Phase phase = Phase.CHOOSE;
	private BakingRecipes.Shape shape;
	private int round;
	private int score;
	private int ticks;
	private float target;
	private Text feedback = Text.empty();
	private int feedbackColor;
	private int feedbackTicks;
	private int doneTicks;

	public RollingScreen(BlockPos pos, BakingRecipes.DoughType dough) {
		super(Text.translatable("screen.bakery.rolling"));
		this.pos = pos;
		this.dough = dough;
	}

	@Override
	protected void init() {
		clearChildren();
		if (phase != Phase.CHOOSE) {
			return;
		}
		int count = dough.shapes().size();
		int buttonW = 64;
		int gap = 8;
		int start = width / 2 - (count * buttonW + (count - 1) * gap) / 2;
		for (int i = 0; i < count; i++) {
			BakingRecipes.Shape option = dough.shapes().get(i);
			addDrawableChild(ButtonWidget.builder(option.displayName(), b -> start(option))
					.dimensions(start + i * (buttonW + gap), height / 2 + 34, buttonW, 16).build());
		}
	}

	private void start(BakingRecipes.Shape chosen) {
		shape = chosen;
		phase = Phase.ROLL;
		ticks = 0;
		target = 0.25f + random.nextFloat() * 0.5f;
		clearChildren();
	}

	private float speed() {
		return 0.11f + round * 0.035f;
	}

	private float goodWidth() {
		return 0.15f - round * 0.018f;
	}

	private float marker(float delta) {
		return (MathHelper.sin((ticks + delta) * speed()) + 1) / 2;
	}

	private void roll() {
		if (phase != Phase.ROLL || client == null) {
			return;
		}
		float distance = Math.abs(marker(0) - target);
		if (distance <= PERFECT_WIDTH) {
			score += 2;
			feedback = Text.translatable("screen.bakery.rolling.perfect");
			feedbackColor = 0xFFFFD34D;
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_CHIME, 1.2f + round * 0.1f));
		} else if (distance <= goodWidth()) {
			score += 1;
			feedback = Text.translatable("screen.bakery.rolling.good");
			feedbackColor = 0xFFA8F0A0;
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_WOOL_PLACE, 1f));
		} else {
			feedback = Text.translatable("screen.bakery.rolling.miss");
			feedbackColor = 0xFFFF8A8A;
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_WOOL_PLACE, 0.6f));
		}
		feedbackTicks = 20;
		round++;
		target = 0.2f + random.nextFloat() * 0.6f;
		if (round >= ROUNDS) {
			phase = Phase.DONE;
			doneTicks = 0;
			ClientPlayNetworking.send(new ShapeDoughPayload(pos, shape.ordinal(), score));
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.5f));
		}
	}

	@Override
	public void tick() {
		ticks++;
		if (feedbackTicks > 0) feedbackTicks--;
		if (phase == Phase.DONE && ++doneTicks > 40) {
			close();
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_SPACE && phase == Phase.ROLL) {
			roll();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (phase == Phase.ROLL && button == 0) {
			roll();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		// A wooden board, drawn from the pastry board texture at double size.
		int bw = 192;
		int bh = 96;
		int bx = width / 2 - bw / 2;
		int by = height / 2 - 62;
		context.fill(bx + 3, by + 4, bx + bw + 3, by + bh + 4, 0x50000000);
		context.getMatrices().push();
		context.getMatrices().translate(bx, by, 0);
		context.getMatrices().scale(2, 2, 1);
		for (int tx = 0; tx < bw / 2; tx += 16) {
			for (int ty = 0; ty < bh / 2; ty += 16) {
				context.drawTexture(BOARD, tx, ty, 0, 0, Math.min(16, bw / 2 - tx), Math.min(16, bh / 2 - ty), 16, 16);
			}
		}
		context.getMatrices().pop();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int cx = width / 2;
		int boardMid = height / 2 - 14;
		context.drawCenteredTextWithShadow(textRenderer, phase == Phase.CHOOSE
				? Text.translatable("screen.bakery.rolling.choose", dough.dough().getName())
				: Text.translatable("screen.bakery.rolling.making", shape.displayName()), cx, height / 2 - 78, 0xFFFFF4E6);

		if (phase == Phase.CHOOSE) {
			drawDough(context, cx, boardMid, 0);
			for (int i = 0; i < dough.shapes().size(); i++) {
				BakingRecipes.Shape option = dough.shapes().get(i);
				int count = dough.shapes().size();
				int start = cx - (count * 64 + (count - 1) * 8) / 2;
				context.drawItem(new ItemStack(option.baked()), start + i * 72 + 24, height / 2 + 16);
			}
			return;
		}

		float m = phase == Phase.ROLL ? marker(delta) : 0.5f;
		int sheetW = drawDough(context, cx, boardMid, round);

		// The rolling pin rides along with the marker.
		int pinX = cx - sheetW / 2 + Math.round(m * sheetW);
		context.getMatrices().push();
		context.getMatrices().translate(pinX, boardMid, 100);
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
		context.getMatrices().scale(3.2f, 3.2f, 1);
		context.drawItem(new ItemStack(BakeryItems.ROLLING_PIN), -8, -8);
		context.getMatrices().pop();

		// Slim timing line under the board.
		int barX = cx - 80;
		int barY = height / 2 + 44;
		int barW = 160;
		context.fill(barX, barY, barX + barW, barY + 2, 0x70FFFFFF);
		if (phase == Phase.ROLL) {
			int goodL = barX + Math.round((target - goodWidth()) * barW);
			int goodR = barX + Math.round((target + goodWidth()) * barW);
			context.fill(Math.max(barX, goodL), barY - 1, Math.min(barX + barW, goodR), barY + 3, 0xC08FD18A);
			int perfL = barX + Math.round((target - PERFECT_WIDTH) * barW);
			int perfR = barX + Math.round((target + PERFECT_WIDTH) * barW);
			context.fill(perfL, barY - 2, perfR, barY + 4, 0xFFFFD34D);
			int markX = barX + Math.round(m * barW);
			context.fill(markX - 1, barY - 5, markX + 1, barY + 7, 0xFFFFFFFF);
		}

		// Round dots
		for (int i = 0; i < ROUNDS; i++) {
			int x = cx - 22 + i * 10;
			context.fill(x, barY + 12, x + 4, barY + 16, i < round ? 0xFFF7B6CF : 0x50FFFFFF);
		}
		Text hint = phase == Phase.ROLL ? Text.translatable("screen.bakery.rolling.press")
				: Text.translatable("screen.bakery.rolling.done").append(" ").append(BakeryFoods.stars(BakingRecipes.prepStars(score)));
		context.drawCenteredTextWithShadow(textRenderer, hint, cx, barY + 22, 0xFFFFF4E6);

		if (feedbackTicks > 0) {
			float pop = 1.5f + feedbackTicks / 30f;
			context.getMatrices().push();
			context.getMatrices().translate(cx, boardMid - 34, 200);
			context.getMatrices().scale(pop, pop, 1);
			context.drawCenteredTextWithShadow(textRenderer, feedback, 0, -4, feedbackColor);
			context.getMatrices().pop();
		}
	}

	/** Draws the dough sheet on the board; it gets wider and thinner with every roll. Returns its width. */
	private int drawDough(DrawContext context, int cx, int cy, int rolls) {
		int w = 56 + rolls * 22;
		int h = Math.max(14, 44 - rolls * 6);
		int x = cx - w / 2;
		int y = cy - h / 2;
		context.fill(x + 2, y + 3, x + w + 2, y + h + 3, 0x40000000);
		for (int tx = 0; tx < w; tx += 16) {
			for (int ty = 0; ty < h; ty += 16) {
				context.drawTexture(DOUGH, x + tx, y + ty, 0, 0, Math.min(16, w - tx), Math.min(16, h - ty), 16, 16);
			}
		}
		context.fill(x, y, x + w, y + 1, 0x60FFFFFF);
		return w;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
