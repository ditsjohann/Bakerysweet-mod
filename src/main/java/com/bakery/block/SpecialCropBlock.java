package com.bakery.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.function.Supplier;

/**
 * Crops that need a little more care than wheat:
 * REGROW plants are picked by hand and grow back, PULL roots are yanked out of the ground,
 * BOG plants only grow with water right next to their farmland, DREAM plants only grow at night or in the rain.
 */
public class SpecialCropBlock extends BakeryCropBlock {
	public enum Style { REGROW, PULL, BOG, DREAM }

	private final Style style;
	private final Supplier<Item> produce;

	public SpecialCropBlock(Settings settings, Supplier<ItemConvertible> seeds, Supplier<Item> produce, Style style) {
		super(settings, seeds);
		this.produce = produce;
		this.style = style;
	}

	public Style style() {
		return style;
	}

	/** Whether the plant is happy enough to grow right now. Also shown as a hint when right-clicked. */
	public boolean conditionsMet(WorldView world, BlockPos pos) {
		return switch (style) {
			case BOG -> nearWater(world, pos.down());
			case DREAM -> world instanceof World w && (w.isNight() || w.isRaining());
			default -> true;
		};
	}

	private static boolean nearWater(WorldView world, BlockPos farmland) {
		for (Direction d : Direction.Type.HORIZONTAL) {
			if (world.getFluidState(farmland.offset(d)).isIn(FluidTags.WATER)) return true;
		}
		return false;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (conditionsMet(world, pos)) {
			super.randomTick(state, world, pos, random);
		}
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return super.isFertilizable(world, pos, state) && conditionsMet(world, pos);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!isMature(state)) {
			if (!conditionsMet(world, pos)) {
				if (!world.isClient) {
					player.sendMessage(Text.translatable("message.bakery.crop." + style.name().toLowerCase()), true);
				}
				return ActionResult.CONSUME;
			}
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (style == Style.PULL) {
			// Yank the whole root out: normal drops, a puff of dirt.
			Block.dropStacks(state, world, pos);
			((ServerWorld) world).spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, world.getBlockState(pos.down())),
					pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 12, 0.25, 0.1, 0.25, 0.1);
			world.playSound(null, pos, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.BLOCKS, 1f, 1.1f);
			world.removeBlock(pos, false);
		} else {
			Block.dropStack(world, pos, new ItemStack(produce.get(), 2 + world.random.nextInt(2)));
			world.playSound(null, pos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.BLOCKS, 1f, 0.9f + world.random.nextFloat() * 0.3f);
			world.setBlockState(pos, withAge(1), Block.NOTIFY_LISTENERS);
		}
		if (style == Style.DREAM && player instanceof net.minecraft.server.network.ServerPlayerEntity picker) {
			com.bakery.sweet.Stickers.award(picker, com.bakery.sweet.Sticker.DREAMY);
		}
		return ActionResult.SUCCESS;
	}
}
