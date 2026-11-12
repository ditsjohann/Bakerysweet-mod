package com.bakery.sweet;

import com.bakery.registry.BakeryComponents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * A wrapped present. Use it to tear it open (with confetti), give it to a friend, or hand it to a villager:
 * villagers adore gifts and become better friends.
 */
public class WrappedGiftItem extends Item {
	public WrappedGiftItem(Settings settings) {
		super(settings);
	}

	public static ItemStack contents(ItemStack gift) {
		ContainerComponent container = gift.get(DataComponentTypes.CONTAINER);
		if (container == null) return ItemStack.EMPTY;
		return container.stream().findFirst().orElse(ItemStack.EMPTY);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack gift = user.getStackInHand(hand);
		if (!world.isClient) {
			ItemStack inside = contents(gift).copy();
			gift.decrement(1);
			if (!inside.isEmpty()) {
				user.getInventory().offerOrDrop(inside);
				user.sendMessage(Text.translatable("message.bakery.gift.opened", inside.getName()), true);
			}
			world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 0.7f, 1.4f);
			world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_BUNDLE_DROP_CONTENTS, SoundCategory.PLAYERS, 1f, 1.2f);
			if (world instanceof ServerWorld server) confetti(server, user.getX(), user.getEyeY(), user.getZ());
		}
		return TypedActionResult.success(gift, world.isClient);
	}

	public static void confetti(ServerWorld world, double x, double y, double z) {
		world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 10, 0.5, 0.4, 0.5, 0);
		world.spawnParticles(ParticleTypes.NOTE, x, y + 0.3, z, 6, 0.6, 0.3, 0.6, 1);
		world.spawnParticles(ParticleTypes.HEART, x, y + 0.2, z, 3, 0.4, 0.2, 0.4, 0);
		world.spawnParticles(ParticleTypes.FIREWORK, x, y, z, 12, 0.2, 0.2, 0.2, 0.08);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		String from = stack.get(BakeryComponents.GIFT_FROM);
		if (from != null) {
			tooltip.add(Text.translatable("tooltip.bakery.gift_from", from).formatted(Formatting.LIGHT_PURPLE));
		}
		tooltip.add(Text.translatable("tooltip.bakery.wrapped_gift").formatted(Formatting.GRAY));
	}
}
