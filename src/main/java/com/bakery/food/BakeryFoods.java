package com.bakery.food;

import com.bakery.registry.BakeryComponents;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

/** Shared behaviour for everything edible in the bakery: stars, names, tooltips and what eating does to you. */
public final class BakeryFoods {
	public static final int DEFAULT_QUALITY = 3;

	private BakeryFoods() {
	}

	public static int quality(ItemStack stack) {
		return Math.clamp(stack.getOrDefault(BakeryComponents.QUALITY, DEFAULT_QUALITY), 1, 5);
	}

	public static Flavor flavor(ItemStack stack) {
		return Flavor.byName(stack.getOrDefault(BakeryComponents.FLAVOR, "plain"));
	}

	public static Topping topping(ItemStack stack) {
		return Topping.byName(stack.getOrDefault(BakeryComponents.TOPPING, "none"));
	}

	public static MutableText stars(int stars) {
		return Text.literal("★".repeat(stars)).formatted(Formatting.GOLD)
				.append(Text.literal("★".repeat(5 - stars)).formatted(Formatting.DARK_GRAY));
	}

	/** "Matcha Cupcake", or "Moldy Matcha Cupcake" once it has gone off. */
	public static Text name(ItemStack stack, Text base) {
		Text name = base;
		Flavor flavor = flavor(stack);
		if (stack.contains(BakeryComponents.FLAVOR) && flavor != Flavor.PLAIN) {
			name = Text.translatable("item.bakery.flavored", flavor.displayName(), base);
		}
		if (Freshness.clientTime > 0 && Freshness.isPerishable(stack)
				&& Freshness.stage(stack, Freshness.clientTime) == Freshness.Stage.SPOILED) {
			return Text.translatable("item.bakery.moldy", name).formatted(Formatting.DARK_GREEN);
		}
		// Perfect bakes get a golden name.
		if (stack.getItem() instanceof Perishable perishable && perishable.isBake() && quality(stack) == 5) {
			return Text.empty().append(name).formatted(Formatting.GOLD);
		}
		return name;
	}

	public static void appendTooltip(ItemStack stack, List<Text> tooltip) {
		if (stack.getItem() instanceof Perishable perishable && perishable.isBake()) {
			tooltip.add(stars(quality(stack)));
		} else if (stack.contains(BakeryComponents.QUALITY)) {
			tooltip.add(Text.translatable("tooltip.bakery.prep", stars(quality(stack))).formatted(Formatting.GRAY));
		}
		if (Boolean.TRUE.equals(stack.get(BakeryComponents.PROOFED))) {
			tooltip.add(Text.translatable("tooltip.bakery.proofed").formatted(Formatting.GOLD));
		}
		Topping topping = topping(stack);
		if (topping != Topping.NONE) {
			tooltip.add(Text.translatable("tooltip.bakery.topped", topping.displayName()).formatted(Formatting.LIGHT_PURPLE));
		}
		if (Freshness.isPerishable(stack) && Freshness.clientTime > 0) {
			long now = Freshness.clientTime;
			Freshness.Stage stage = Freshness.stage(stack, now);
			MutableText line = stage.displayName().copy();
			if (stage != Freshness.Stage.SPOILED) {
				line.append(Text.literal(" · ").formatted(Formatting.DARK_GRAY))
						.append(Freshness.timeLeft(Freshness.remaining(stack, now)).copy().formatted(Formatting.GRAY));
			}
			tooltip.add(line);
		}
		Preservation preservation = Freshness.preservation(stack);
		if (preservation != Preservation.NONE) {
			tooltip.add(preservation.displayName());
		}
	}

	public static void inventoryTick(ItemStack stack, World world) {
		if (!world.isClient) {
			Freshness.stamp(stack, world);
		}
	}

	/** Bonuses on top of the normal food values: good bakes fill you up more, flavors give little effects, bad food bites back. */
	public static void afterEating(ItemStack eaten, World world, LivingEntity eater) {
		if (world.isClient) {
			return;
		}
		Freshness.Stage stage = Freshness.stage(eaten, world.getTime());
		if (stage == Freshness.Stage.SPOILED) {
			eater.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100));
			eater.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200));
			return;
		}
		if (stage == Freshness.Stage.STALE) {
			eater.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 200));
		}

		if (eaten.getItem() instanceof Perishable perishable && perishable.isBake() && eater instanceof PlayerEntity player) {
			int stars = quality(eaten);
			if (stars >= 4) {
				player.getHungerManager().add(stars - 3, (stars - 3) * 1.5f);
			}
			if (stars == 5) {
				eater.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120));
			}
			if (topping(eaten) == Topping.CREAM) {
				player.getHungerManager().add(0, 1.5f);
			}
		}

		switch (flavor(eaten)) {
			case CHOCOLATE -> eater.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 400));
			case MATCHA -> eater.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 800));
			case LAVENDER -> eater.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80));
			case ROSE -> eater.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600));
			case BLUEBERRY -> eater.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400));
			case STRAWBERRY -> eater.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 400));
			case STARBLOOM -> {
				eater.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 1200));
				eater.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200));
				eater.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100));
			}
			default -> {
			}
		}
	}

	/** Eats one serving straight off a placed block. Returns false if the player is not hungry. */
	public static boolean eatFromBlock(PlayerEntity player, World world, BlockPos pos, ItemStack stack, FoodComponent food) {
		if (!player.canConsume(food.canAlwaysEat())) {
			return false;
		}
		if (!world.isClient) {
			player.getHungerManager().eat(food);
			for (FoodComponent.StatusEffectEntry entry : food.effects()) {
				if (world.random.nextFloat() < entry.probability()) {
					player.addStatusEffect(new StatusEffectInstance(entry.effect()));
				}
			}
			afterEating(stack, world, player);
		}
		world.playSound(player, pos, SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 0.8f, 0.9f + world.random.nextFloat() * 0.2f);
		world.emitGameEvent(player, GameEvent.EAT, pos);
		return true;
	}
}
