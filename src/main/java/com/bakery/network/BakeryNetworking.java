package com.bakery.network;

import com.bakery.baking.BakingRecipes;
import com.bakery.block.PastryBoardBlockEntity;
import com.bakery.food.Freshness;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class BakeryNetworking {
	private BakeryNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(OpenRollingPayload.ID, OpenRollingPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ReviewPayload.ID, ReviewPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(OrdersPayload.ID, OrdersPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ShapeDoughPayload.ID, ShapeDoughPayload.CODEC);

		PayloadTypeRegistry.playS2C().register(BakeResultPayload.ID, BakeResultPayload.CODEC);

		PayloadTypeRegistry.playS2C().register(StickerPayload.ID, StickerPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SweetPayloads.GuideData.ID, SweetPayloads.GuideData.CODEC);
		PayloadTypeRegistry.playS2C().register(SweetPayloads.OpenCatalog.ID, SweetPayloads.OpenCatalog.CODEC);
		PayloadTypeRegistry.playS2C().register(SweetPayloads.OpenMenu.ID, SweetPayloads.OpenMenu.CODEC);
		PayloadTypeRegistry.playC2S().register(SweetPayloads.Buy.ID, SweetPayloads.Buy.CODEC);
		PayloadTypeRegistry.playC2S().register(SweetPayloads.ToggleDish.ID, SweetPayloads.ToggleDish.CODEC);
		PayloadTypeRegistry.playS2C().register(SweetPayloads.OpenCounter.ID, SweetPayloads.OpenCounter.CODEC);
		PayloadTypeRegistry.playS2C().register(SweetPayloads.OpenNotebook.ID, SweetPayloads.OpenNotebook.CODEC);
		PayloadTypeRegistry.playC2S().register(SweetPayloads.CounterAction.ID, SweetPayloads.CounterAction.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SweetPayloads.CounterAction.ID, (payload, context) -> context.server().execute(() -> {
			ServerPlayerEntity player = context.player();
			ServerWorld world = player.getServerWorld();
			if (player.squaredDistanceTo(Vec3d.ofCenter(payload.counter())) < 64 && world.getBlockState(payload.counter()).getBlock() instanceof com.bakery.shop.CounterBlock) {
				com.bakery.shop.CounterBlock.act(world, payload.counter(), player, payload.action());
			}
		}));
		ServerPlayNetworking.registerGlobalReceiver(SweetPayloads.Buy.ID, (payload, context) ->
				context.server().execute(() -> com.bakery.sweet.Catalog.buy(context.player(), payload.pos(), payload.index())));
		ServerPlayNetworking.registerGlobalReceiver(SweetPayloads.ToggleDish.ID, (payload, context) -> context.server().execute(() -> {
			ServerPlayerEntity player = context.player();
			if (player.squaredDistanceTo(Vec3d.ofCenter(payload.counter())) < 256
					&& player.getServerWorld().getBlockEntity(payload.counter()) instanceof com.bakery.shop.CounterBlockEntity counter) {
				counter.toggleDish(payload.dish());
			}
		}));

		ServerPlayNetworking.registerGlobalReceiver(ShapeDoughPayload.ID, (payload, context) ->
				context.server().execute(() -> shapeDough(context.player(), payload)));
	}

	/** Finishes the rolling minigame: turns the dough on the board into a shaped raw bake. Public for the game tests. */
	public static void shapeDough(ServerPlayerEntity player, ShapeDoughPayload payload) {
		ServerWorld world = player.getServerWorld();
		if (player.squaredDistanceTo(Vec3d.ofCenter(payload.pos())) > 64
				|| !(world.getBlockEntity(payload.pos()) instanceof PastryBoardBlockEntity board)) {
			return;
		}
		ItemStack dough = board.getDough();
		BakingRecipes.DoughType type = BakingRecipes.DoughType.of(dough);
		if (type == null || payload.shape() < 0 || payload.shape() >= BakingRecipes.Shape.values().length) {
			return;
		}
		BakingRecipes.Shape shape = BakingRecipes.Shape.values()[payload.shape()];
		if (!type.shapes().contains(shape)) {
			return;
		}
		Hand pinHand = player.getMainHandStack().isOf(BakeryItems.ROLLING_PIN) ? Hand.MAIN_HAND
				: player.getOffHandStack().isOf(BakeryItems.ROLLING_PIN) ? Hand.OFF_HAND : null;
		if (pinHand == null) {
			return;
		}

		boolean proofed = Boolean.TRUE.equals(dough.get(BakeryComponents.PROOFED));
		int stars = Math.min(5, BakingRecipes.prepStars(Math.clamp(payload.score(), 0, 10)) + (proofed ? 1 : 0));
		ItemStack raw = new ItemStack(shape.raw());
		if (dough.contains(BakeryComponents.FLAVOR)) {
			raw.set(BakeryComponents.FLAVOR, dough.get(BakeryComponents.FLAVOR));
		}
		raw.set(BakeryComponents.QUALITY, stars);
		Freshness.stampFresh(raw, world.getTime());
		board.setDough(ItemStack.EMPTY);
		player.getInventory().offerOrDrop(raw);
		player.getStackInHand(pinHand).damage(1, player, pinHand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		world.playSound(null, payload.pos(), SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 1f, 0.8f);
		player.sendMessage(Text.translatable("message.bakery.board.shaped", raw.getName()), true);
	}
}
