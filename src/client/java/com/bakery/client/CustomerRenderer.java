package com.bakery.client;

import com.bakery.Bakery;
import com.bakery.cafe.CustomerEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import org.joml.Matrix4f;

/**
 * Café customers look like little people in pastel outfits (player-style skins, slim or classic arms).
 * Seated customers lift their food to their mouth now and then.
 */
public class CustomerRenderer extends BipedEntityRenderer<CustomerEntity, CustomerRenderer.Model> {
	private static final Identifier[] SKINS = new Identifier[com.bakery.cafe.Regular.SKINS];

	static {
		for (int i = 0; i < SKINS.length; i++) SKINS[i] = Bakery.id("textures/entity/customer/customer_" + i + ".png");
	}

	private final Model wide;
	private final Model slim;

	public CustomerRenderer(EntityRendererFactory.Context context) {
		super(context, new Model(context.getPart(EntityModelLayers.PLAYER_SLIM), true), 0.45f);
		this.slim = model;
		this.wide = new Model(context.getPart(EntityModelLayers.PLAYER), false);
	}

	@Override
	public void render(CustomerEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
		model = entity.regular().slim() ? slim : wide;
		super.render(entity, yaw, tickDelta, matrices, consumers, light);
		if (entity.isTalking()) chatBubble(entity, tickDelta, matrices, consumers);
	}

	/** What a chat looks like from outside: little faces and notes, a different set for each topic. */
	private static final String[][] TOPICS = {{"…", "☺", "♪"}, {"♥", "☺", "♥"}, {"?", "!", "☺"}, {"✿", "♥", "✿"}, {"…", "!!", "☺"}, {"♪", "♫", "☺"}};

	/** A small speech bubble just beside the head of whoever is talking right now (clear of the name and the order wish). */
	private void chatBubble(CustomerEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers) {
		String[] topic = TOPICS[Math.floorMod(entity.chatTopic(), TOPICS.length)];
		float t = entity.age + tickDelta;
		String glyph = topic[(int) (t / 11) % topic.length];
		float pop = MathHelper.clamp((t % 34) / 4f, 0, 1);
		TextRenderer text = getTextRenderer();
		matrices.push();
		// Head height (the model is drawn at 15/16 scale), beside the face on the viewer's right.
		matrices.translate(0, entity.getHeight() * 0.9375f - 0.2f + MathHelper.sin(t * 0.3f) * 0.02, 0);
		matrices.multiply(dispatcher.getRotation());
		matrices.translate(-0.5, 0, 0);
		float s = 0.03f * pop;
		matrices.scale(s, -s, s);
		Matrix4f m = matrices.peek().getPositionMatrix();
		float w = text.getWidth(glyph);
		text.draw(" " + glyph + " ", -w / 2f - 3, -4, 0xFF5A3A48, false, m, consumers, TextRenderer.TextLayerType.NORMAL, 0xE0FFFFFF,
				LightmapTextureManager.MAX_LIGHT_COORDINATE);
		// The little tail pointing at the talker.
		text.draw("◢", w / 2f - 1, 4, 0xE0FFFFFF, false, m, consumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
		matrices.pop();
	}

	@Override
	protected void scale(CustomerEntity entity, MatrixStack matrices, float amount) {
		matrices.scale(0.9375f, 0.9375f, 0.9375f);
	}

	@Override
	public Identifier getTexture(CustomerEntity entity) {
		return SKINS[Math.floorMod(entity.regular().skin(), SKINS.length)];
	}

	public static class Model extends PlayerEntityModel<CustomerEntity> {
		public Model(ModelPart root, boolean thinArms) {
			super(root, thinArms);
		}

		@Override
		public void setAngles(CustomerEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch) {
			super.setAngles(entity, limbAngle, limbDistance, age, headYaw, headPitch);
			if (entity.isBiting()) {
				// Bring the food up for a bite every couple of seconds.
				float cycle = (age % 48) / 48f;
				float lift = cycle < 0.35f ? MathHelper.sin(cycle / 0.35f * MathHelper.PI) : 0;
				rightArm.pitch = -0.6f - lift * 0.95f;
				rightArm.yaw = -0.35f * lift;
				leftArm.pitch = -0.25f;
				head.pitch += lift * 0.15f;
				rightSleeve.copyTransform(rightArm);
				leftSleeve.copyTransform(leftArm);
				hat.copyTransform(head);
			} else if (entity.hasVehicle()) {
				// Sitting with empty hands (waiting for their order, or done eating): hands resting in the lap.
				rightArm.pitch = -0.55f;
				leftArm.pitch = -0.55f;
				rightArm.yaw = 0.25f;
				leftArm.yaw = -0.25f;
				rightSleeve.copyTransform(rightArm);
				leftSleeve.copyTransform(leftArm);
			} else if (!entity.getMainHandStack().isEmpty()) {
				// Carrying a treat home: hold it in front.
				rightArm.pitch = -0.45f + MathHelper.cos(limbAngle * 0.6662f) * 0.15f * limbDistance;
				rightSleeve.copyTransform(rightArm);
			}
		}
	}
}
