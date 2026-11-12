package com.bakery.item;

import com.bakery.food.Freshness;
import com.bakery.food.Preservation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Predicate;

/** Wax paper, frost wrap and glass jars. Use one with the food you want to keep in your other hand. */
public class PreservingItem extends Item {
	private final Preservation method;
	private final Predicate<ItemStack> accepts;

	public PreservingItem(Settings settings, Preservation method, Predicate<ItemStack> accepts) {
		super(settings);
		this.method = method;
		this.accepts = accepts;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack wrapper = user.getStackInHand(hand);
		ItemStack food = user.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
		if (!Freshness.isPerishable(food) || !accepts.test(food)) {
			if (!world.isClient) {
				user.sendMessage(Text.translatable("message.bakery.preserve_how", wrapper.getName()), true);
			}
			return TypedActionResult.fail(wrapper);
		}
		if (world.isClient) {
			return TypedActionResult.success(wrapper);
		}
		Freshness.stamp(food, world);
		if (!Freshness.preserve(food, method, world.getTime())) {
			user.sendMessage(Text.translatable("message.bakery.preserve_failed"), true);
			return TypedActionResult.fail(wrapper);
		}
		world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.2f);
		user.sendMessage(Text.translatable("message.bakery.preserved", food.getName(), method.displayName()), true);
		wrapper.decrementUnlessCreative(1, user);
		return TypedActionResult.consume(wrapper);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("tooltip.bakery.preserver", method.multiplier()).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.bakery.preserver_how").formatted(Formatting.DARK_GRAY));
	}
}
