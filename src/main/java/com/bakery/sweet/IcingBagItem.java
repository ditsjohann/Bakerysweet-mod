package com.bakery.sweet;

import com.bakery.food.BakeryFoods;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
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
 * A piping bag of pastel icing with a little metal tip on the end. Pipe straight onto a cake on the
 * Cake Turntable - dots, rosettes, lines, a border, drips, or frost a whole tier. Sneak-use it in the air to
 * swap the tip. Hold a baked sugar cookie in your other hand and use the bag to ice the cookie.
 */
public class IcingBagItem extends Item {
	public enum Tip {
		DOTS, ROSETTES, LINE, BORDER, DRIPS, FROST;

		public Text displayName() {
			return Text.translatable("tip.bakery." + name().toLowerCase());
		}
	}

	private final Icing icing;

	public IcingBagItem(Settings settings, Icing icing) {
		super(settings);
		this.icing = icing;
	}

	public Icing icing() {
		return icing;
	}

	public static Tip tip(ItemStack bag) {
		Tip[] tips = Tip.values();
		return tips[Math.floorMod(bag.getOrDefault(BakeryComponents.PIPING_TIP, 0), tips.length)];
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack bag = user.getStackInHand(hand);
		ItemStack cookie = user.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
		if (user.isSneaking() || !cookie.isOf(BakeryItems.SUGAR_COOKIE)) {
			// Swap the piping tip.
			if (!world.isClient) {
				Tip next = Tip.values()[(tip(bag).ordinal() + (user.isSneaking() ? 1 : 0)) % Tip.values().length];
				bag.set(BakeryComponents.PIPING_TIP, next.ordinal());
				world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.4f, 1.9f);
				user.sendMessage(Text.translatable(user.isSneaking() ? "message.bakery.icing.tip" : "message.bakery.icing.hint", next.displayName())
						.formatted(Formatting.LIGHT_PURPLE), true);
			}
			return TypedActionResult.success(bag, world.isClient);
		}
		if (cookie.contains(BakeryComponents.ICING)) {
			if (!world.isClient) user.sendMessage(Text.translatable("message.bakery.icing.already"), true);
			return TypedActionResult.fail(bag);
		}
		if (!world.isClient) {
			ItemStack iced = cookie.split(1);
			iced.set(BakeryComponents.ICING, icing.color());
			iced.set(BakeryComponents.QUALITY, Math.min(5, BakeryFoods.quality(iced) + 1));
			user.getInventory().offerOrDrop(iced);
			bag.damage(1, user, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
			world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.PLAYERS, 0.8f, 1.6f);
			world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6f, 1.8f);
			if (world instanceof ServerWorld server) {
				server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, user.getX(), user.getEyeY() - 0.4, user.getZ(), 6, 0.3, 0.2, 0.3, 0);
			}
			user.sendMessage(Text.translatable("message.bakery.icing.done", iced.getName()), true);
			if (user instanceof ServerPlayerEntity player) Stickers.award(player, Sticker.COOKIE_ARTIST);
		}
		return TypedActionResult.success(bag, world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("tooltip.bakery.icing_tip", tip(stack).displayName()).formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("tooltip.bakery.icing_bag").formatted(Formatting.GRAY));
	}
}
