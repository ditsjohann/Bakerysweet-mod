package com.bakery.shop;

import com.bakery.deco.DecoBlock;
import com.bakery.food.Perishable;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * Pretty display furniture that holds real bakes: click with a bake to set it out, with an empty hand to take one
 * back. Customers waiting at a counter nearby help themselves from it and leave the money in the till.
 */
public class DisplayStandBlock extends DecoBlock implements BlockEntityProvider {
	/** Where the bakes sit, for the north-facing model: {x, y, z, size} in pixels (size in blocks). */
	private final float[][] spots;

	public DisplayStandBlock(Settings settings, VoxelShape northShape, float[][] spots) {
		super(settings, northShape, false);
		this.spots = spots;
	}

	public float[][] spots() {
		return spots;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new DisplayStandBlockEntity(pos, state);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!(stack.getItem() instanceof Perishable) || !(world.getBlockEntity(pos) instanceof DisplayStandBlockEntity display)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!world.isClient && display.add(stack)) {
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_DECORATED_POT_INSERT, SoundCategory.BLOCKS, 0.6f, 1.4f);
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof DisplayStandBlockEntity display)) return ActionResult.PASS;
		if (!world.isClient) {
			ItemStack taken = display.takeLast();
			if (!taken.isEmpty()) {
				player.getInventory().offerOrDrop(taken);
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.4f);
			}
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof DisplayStandBlockEntity display) {
			for (int i = 0; i < DisplayStandBlockEntity.MAX_SLOTS; i++) {
				ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, display.getSlot(i));
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
