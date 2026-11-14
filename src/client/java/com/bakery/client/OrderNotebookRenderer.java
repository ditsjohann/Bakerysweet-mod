package com.bakery.client;

import com.bakery.cafe.Regular;
import com.bakery.deco.DecoBlock;
import com.bakery.shop.OrderNotebookBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.List;

/** Writes the first few open orders on the notebook's page: what they want, who, where, and patience hearts. */
public class OrderNotebookRenderer implements BlockEntityRenderer<OrderNotebookBlockEntity> {
	/** Page area in model pixels (north-facing model): x 3..13, y 2..13, its surface at z = 14.5. */
	private static final float PAGE_LEFT = 13, PAGE_TOP = 12.6f, PAGE_Z = 14.45f, PAGE_W = 10;
	/** Text pixels per model pixel. */
	private static final int TEXT_PX = 9;
	private static final int ROWS = 4;

	public OrderNotebookRenderer(BlockEntityRendererFactory.Context context) {
	}

	@Override
	public void render(OrderNotebookBlockEntity notebook, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.player.squaredDistanceTo(notebook.getPos().toCenterPos()) > 24 * 24) return;
		Direction facing = notebook.getCachedState().get(DecoBlock.FACING);
		TextRenderer text = client.textRenderer;
		List<OrderNotebookBlockEntity.Entry> entries = notebook.getEntries();

		matrices.push();
		PixelBox.orient(matrices, facing);
		// Onto the page, turned to face out of the wall, text pixels from the page's top-left corner.
		matrices.translate(PAGE_LEFT / 16f, PAGE_TOP / 16f, PAGE_Z / 16f);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
		float s = 1f / 16f / TEXT_PX;
		int width = (int) (PAGE_W * TEXT_PX);
		// Item icons go in block space (a mirrored text frame hides them), at text-pixel positions.
		for (int i = 0; i < Math.min(ROWS, entries.size()); i++) {
			matrices.push();
			matrices.translate(5 * s, -(12 + i * 22 + 7) * s, 0.012f);
			matrices.scale(0.06f, 0.06f, 0.012f);
			client.getItemRenderer().renderItem(entries.get(i).wanted(), ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, matrices, consumers,
					notebook.getWorld(), i);
			matrices.pop();
		}
		matrices.scale(s, -s, s);
		Matrix4f m = matrices.peek().getPositionMatrix();

		Text title = Text.translatable("block.bakery.order_notebook.page", entries.size());
		text.draw(title, (width - text.getWidth(title)) / 2f, 0, 0xFFD0567E, false, m, consumers, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
		if (entries.isEmpty()) {
			Text none = Text.translatable("block.bakery.order_notebook.none");
			text.draw(none, (width - text.getWidth(none)) / 2f, 30, 0xFFB98A9A, false, m, consumers, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
		}
		for (int i = 0; i < Math.min(ROWS, entries.size()); i++) {
			OrderNotebookBlockEntity.Entry e = entries.get(i);
			float y = 12 + i * 22;
			String name = text.trimToWidth(Regular.get(e.regular()).displayName().getString(), width - 14 - text.getWidth("♥♥♥♥♥") - 2);
			text.draw(name, 12, y, 0xFF5A3A46, false, m, consumers, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
			Text where = Text.translatable(e.seated() ? "block.bakery.order_notebook.table" : "block.bakery.order_notebook.line");
			text.draw(where, 12, y + 9, e.seated() ? 0xFF4FA060 : 0xFFB98A9A, false, m, consumers, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
			int hearts = MathHelper.ceil(e.patience() * 5);
			String full = "♥".repeat(hearts), empty = "♥".repeat(Math.max(0, 5 - hearts));
			float hx = width - text.getWidth(full + empty);
			text.draw(full, hx, y, e.patience() < 0.3f ? 0xFFE0A030 : 0xFFFF6F91, false, m, consumers, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
			text.draw(empty, hx + text.getWidth(full), y, 0xFFE3D3DA, false, m, consumers, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
		}
		if (entries.size() > ROWS) {
			Text more = Text.translatable("block.bakery.order_notebook.more", entries.size() - ROWS);
			text.draw(more, (width - text.getWidth(more)) / 2f, 12 + ROWS * 22, 0xFFB98A9A, false, m, consumers, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
		}
		matrices.pop();
	}
}
