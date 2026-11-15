package com.bakery.pets;

import com.bakery.registry.BakeryEntities;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * A wicker basket with a kitten in it. A new basket from the catalog holds a kitten of a random breed; use it on the
 * floor to let the kitten out (it moves in with the nearest cat bed). Use an empty basket on a café cat to pick it up
 * and carry it somewhere else.
 */
public class KittenBasketItem extends Item {
	public KittenBasketItem(Settings settings) {
		super(settings);
	}

	private static NbtCompound data(ItemStack stack) {
		NbtComponent c = stack.get(DataComponentTypes.CUSTOM_DATA);
		return c == null ? new NbtCompound() : c.copyNbt();
	}

	public static boolean isEmpty(ItemStack stack) {
		return data(stack).getBoolean("empty");
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		ItemStack stack = context.getStack();
		if (isEmpty(stack)) return ActionResult.PASS;
		if (!(context.getWorld() instanceof ServerWorld world)) return ActionResult.SUCCESS;
		BlockPos pos = context.getBlockPos().offset(context.getSide());
		CafeCatEntity cat = BakeryEntities.CAFE_CAT.create(world);
		if (cat == null) return ActionResult.FAIL;
		cat.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getPlayerYaw() + 180, 0);
		cat.initialize(world, world.getLocalDifficulty(pos), SpawnReason.SPAWN_EGG, null);
		NbtCompound data = data(stack);
		if (data.contains("variant")) {
			Identifier id = Identifier.tryParse(data.getString("variant"));
			if (id != null) world.getRegistryManager().get(RegistryKeys.CAT_VARIANT).getEntry(id).ifPresent(cat::setVariant);
		} else {
			cat.randomBreed();
		}
		PlayerEntity player = context.getPlayer();
		if (player != null) {
			cat.setOwner(player);
			cat.setTamed(true, true);
		}
		cat.setCollar(data.contains("collar") ? DyeColor.byId(data.getInt("collar")) : DyeColor.PINK);
		if (data.contains("name")) cat.setCustomName(Text.literal(data.getString("name")));
		else if (stack.contains(DataComponentTypes.CUSTOM_NAME)) cat.setCustomName(stack.getName());
		cat.findBed();
		world.spawnEntity(cat);
		world.playSound(null, pos, SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.NEUTRAL, 1f, 1.2f);
		NbtCompound empty = new NbtCompound();
		empty.putBoolean("empty", true);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(empty));
		stack.remove(DataComponentTypes.CUSTOM_NAME);
		return ActionResult.SUCCESS;
	}

	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		if (!(entity instanceof CafeCatEntity cat) || !isEmpty(stack)) return ActionResult.PASS;
		if (user.getWorld().isClient) return ActionResult.SUCCESS;
		NbtCompound data = new NbtCompound();
		cat.getVariant().getKey().ifPresent(key -> data.putString("variant", key.getValue().toString()));
		data.putInt("collar", cat.getCollarColor().getId());
		if (cat.hasCustomName()) data.putString("name", cat.getCustomName().getString());
		ItemStack full = stack.copyWithCount(1);
		full.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
		stack.decrement(1);
		if (stack.isEmpty()) user.setStackInHand(hand, full);
		else user.getInventory().offerOrDrop(full);
		cat.getWorld().playSound(null, cat.getBlockPos(), SoundEvents.ENTITY_CAT_PURREOW, SoundCategory.NEUTRAL, 1f, 1f);
		cat.discard();
		return ActionResult.SUCCESS;
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		NbtCompound data = data(stack);
		if (data.getBoolean("empty")) {
			tooltip.add(Text.translatable("tooltip.bakery.kitten_basket.empty").formatted(Formatting.GRAY));
		} else if (data.contains("variant")) {
			String breed = Identifier.of(data.getString("variant")).getPath();
			Text name = data.contains("name") ? Text.literal(data.getString("name")) : Text.translatable("cat_breed.bakery." + breed);
			tooltip.add(Text.translatable("tooltip.bakery.kitten_basket.cat", name).formatted(Formatting.GRAY));
		} else {
			tooltip.add(Text.translatable("tooltip.bakery.kitten_basket.new").formatted(Formatting.GRAY));
		}
	}
}
