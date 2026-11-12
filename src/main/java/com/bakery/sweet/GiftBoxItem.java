package com.bakery.sweet;

import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
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

/** An empty gift box. Use it with something in your other hand to wrap that up with a pretty ribbon. */
public class GiftBoxItem extends Item {
	public static final int[] RIBBONS = {0xF7A8C6, 0xCDB4F0, 0xA8E6CF, 0xA8D8F7, 0xFFEE99, 0xFF8FA3};

	public GiftBoxItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack box = user.getStackInHand(hand);
		Hand other = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
		ItemStack present = user.getStackInHand(other);
		if (present.isEmpty() || present.isOf(BakeryItems.WRAPPED_GIFT) || present.isOf(BakeryItems.GIFT_BOX)) {
			if (!world.isClient) user.sendMessage(Text.translatable("message.bakery.gift.hint"), true);
			return TypedActionResult.fail(box);
		}
		if (!world.isClient) {
			ItemStack wrapped = wrap(present.copy(), user.getName().getString(), RIBBONS[world.random.nextInt(RIBBONS.length)]);
			user.setStackInHand(other, ItemStack.EMPTY);
			box.decrementUnlessCreative(1, user);
			user.getInventory().offerOrDrop(wrapped);
			world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1f, 1.2f);
			world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6f, 1.5f);
			user.sendMessage(Text.translatable("message.bakery.gift.wrapped"), true);
		}
		return TypedActionResult.success(box, world.isClient);
	}

	public static ItemStack wrap(ItemStack contents, String from, int ribbon) {
		ItemStack wrapped = new ItemStack(BakeryItems.WRAPPED_GIFT);
		wrapped.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(List.of(contents)));
		wrapped.set(BakeryComponents.GIFT_COLOR, ribbon);
		wrapped.set(BakeryComponents.GIFT_FROM, from);
		return wrapped;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("tooltip.bakery.gift_box").formatted(Formatting.GRAY));
	}
}
