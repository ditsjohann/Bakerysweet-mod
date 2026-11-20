package com.bakery.network;

import com.bakery.baking.BakingRecipes;
import com.bakery.block.PastryBoardBlockEntity;
import com.bakery.food.Freshness;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import com.bakery.platform.Platform;
import net.minecraft.entity.Entity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class BakeryNetworking {
	private BakeryNetworking() {
	}

	/** A packet the server sends to players. */
	public record ToClient<T extends CustomPayload>(CustomPayload.Id<T> id, PacketCodec<RegistryByteBuf, T> codec) {
	}

	/** A packet players send to the server, with what the server does with it (on the server thread). */
	public record ToServer<T extends CustomPayload>(CustomPayload.Id<T> id, PacketCodec<RegistryByteBuf, T> codec,
													 BiConsumer<T, ServerPlayerEntity> handler) {
	}

	public static final List<ToClient<?>> TO_CLIENT = List.of(
			new ToClient<>(OpenRollingPayload.ID, OpenRollingPayload.CODEC),
			new ToClient<>(ReviewPayload.ID, ReviewPayload.CODEC),
			new ToClient<>(OrdersPayload.ID, OrdersPayload.CODEC),
			new ToClient<>(BakeResultPayload.ID, BakeResultPayload.CODEC),
			new ToClient<>(AppraisalPayload.ID, AppraisalPayload.CODEC),
			new ToClient<>(StickerPayload.ID, StickerPayload.CODEC),
			new ToClient<>(SweetPayloads.GuideData.ID, SweetPayloads.GuideData.CODEC),
			new ToClient<>(SweetPayloads.OpenCatalog.ID, SweetPayloads.OpenCatalog.CODEC),
			new ToClient<>(SweetPayloads.OpenMenu.ID, SweetPayloads.OpenMenu.CODEC),
			new ToClient<>(SweetPayloads.OpenCounter.ID, SweetPayloads.OpenCounter.CODEC),
			new ToClient<>(SweetPayloads.OpenNotebook.ID, SweetPayloads.OpenNotebook.CODEC));

	public static final List<ToServer<?>> TO_SERVER = List.of(
			new ToServer<>(ShapeDoughPayload.ID, ShapeDoughPayload.CODEC, (payload, player) -> shapeDough(player, payload)),
			new ToServer<>(SweetPayloads.Buy.ID, SweetPayloads.Buy.CODEC,
					(payload, player) -> com.bakery.sweet.Catalog.buy(player, payload.pos(), payload.index())),
			new ToServer<>(SweetPayloads.ToggleDish.ID, SweetPayloads.ToggleDish.CODEC, (payload, player) -> {
				if (player.squaredDistanceTo(Vec3d.ofCenter(payload.counter())) < 256
						&& player.getServerWorld().getBlockEntity(payload.counter()) instanceof com.bakery.shop.CounterBlockEntity counter) {
					counter.toggleDish(payload.dish());
				}
			}),
			new ToServer<>(SweetPayloads.CounterAction.ID, SweetPayloads.CounterAction.CODEC, (payload, player) -> {
				ServerWorld world = player.getServerWorld();
				if (player.squaredDistanceTo(Vec3d.ofCenter(payload.counter())) < 64 && world.getBlockState(payload.counter()).getBlock() instanceof com.bakery.shop.CounterBlock) {
					com.bakery.shop.CounterBlock.act(world, payload.counter(), player, payload.action());
				}
			}));

	/** Sends one of the mod's packets to a player. */
	public static void send(ServerPlayerEntity player, CustomPayload payload) {
		Platform.get().sendToPlayer(player, payload);
	}

	/** Players in {@code world} within {@code radius} blocks of {@code center}. */
	public static List<ServerPlayerEntity> around(ServerWorld world, Vec3d center, double radius) {
		List<ServerPlayerEntity> out = new ArrayList<>();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.squaredDistanceTo(center) <= radius * radius) out.add(player);
		}
		return out;
	}

	/** Players close enough to see {@code entity} (in its world, within normal entity view range). */
	public static List<ServerPlayerEntity> watching(Entity entity) {
		return entity.getWorld() instanceof ServerWorld world ? around(world, entity.getPos(), 80) : List.of();
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
		raw.set(BakeryComponents.HOMEMADE, dough.get(BakeryComponents.HOMEMADE));
		Freshness.stampFresh(raw, world.getTime());
		board.setDough(ItemStack.EMPTY);
		Text shapedName = raw.getName();
		player.getInventory().offerOrDrop(raw);
		player.getStackInHand(pinHand).damage(1, player, pinHand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		world.playSound(null, payload.pos(), SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 1f, 0.8f);
		player.sendMessage(Text.translatable("message.bakery.board.shaped", shapedName), true);
	}
}
