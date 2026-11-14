package com.bakery.deco;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.DyeColor;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Furniture you can paint: use any dye on it to colour its cloth, cushion, panel or bowl (a soft pastel of the dye),
 * and a sponge to wash it back to how it came.
 */
public final class Dyeable {
	public static final BooleanProperty DYED = BooleanProperty.of("dyed");
	public static final EnumProperty<DyeColor> COLOR = EnumProperty.of("color", DyeColor.class);

	private Dyeable() {
	}

	/** Handles dye and sponge clicks; null when the item is neither (so the block does its usual thing). */
	public static ItemActionResult use(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player) {
		if (!state.contains(DYED)) return null;
		if (stack.getItem() instanceof DyeItem dye) {
			if (state.get(DYED) && state.get(COLOR) == dye.getColor()) return ItemActionResult.CONSUME;
			if (!world.isClient) {
				world.setBlockState(pos, state.with(DYED, true).with(COLOR, dye.getColor()), Block.NOTIFY_ALL);
				stack.decrementUnlessCreative(1, player);
				world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1f, 1f);
				if (world instanceof ServerWorld server) {
					server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0);
				}
			}
			return ItemActionResult.success(world.isClient);
		}
		if ((stack.isOf(Items.SPONGE) || stack.isOf(Items.WET_SPONGE)) && state.get(DYED)) {
			if (!world.isClient) {
				world.setBlockState(pos, state.with(DYED, false), Block.NOTIFY_ALL);
				world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.5f, 1.4f);
			}
			return ItemActionResult.success(world.isClient);
		}
		return null;
	}

	/** The tint for a dyed block: the dye's colour, softened toward white so it stays in the bakery's pastel look. */
	public static int tint(BlockState state) {
		if (!state.contains(DYED) || !state.get(DYED)) return -1;
		int c = state.get(COLOR).getEntityColor();
		float soft = state.get(COLOR) == DyeColor.BLACK || state.get(COLOR) == DyeColor.GRAY ? 0.15f : 0.3f;
		int r = (int) ((c >> 16 & 255) * (1 - soft) + 255 * soft);
		int g = (int) ((c >> 8 & 255) * (1 - soft) + 255 * soft);
		int b = (int) ((c & 255) * (1 - soft) + 255 * soft);
		return r << 16 | g << 8 | b;
	}
}
