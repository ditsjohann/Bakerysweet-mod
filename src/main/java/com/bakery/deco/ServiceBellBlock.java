package com.bakery.deco;

import com.bakery.shop.CounterBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** Ding! Calls the next customer to a nearby open counter. */
public class ServiceBellBlock extends DecoBlock {
	public ServiceBellBlock(Settings settings, VoxelShape shape) {
		super(settings, shape, false);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		world.playSound(player, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1f, 2f);
		if (!world.isClient) {
			boolean called = false;
			boolean anyCounter = false;
			for (BlockPos p : BlockPos.iterate(pos.add(-5, -2, -5), pos.add(5, 2, 5))) {
				if (world.getBlockEntity(p) instanceof CounterBlockEntity counter) {
					anyCounter = true;
					if (counter.callCustomer()) {
						called = true;
						break;
					}
				}
			}
			player.sendMessage(Text.translatable(called ? "message.bakery.bell.called" : anyCounter ? "message.bakery.bell.nobody" : "message.bakery.bell.no_counter"), true);
		}
		return ActionResult.success(world.isClient);
	}
}
