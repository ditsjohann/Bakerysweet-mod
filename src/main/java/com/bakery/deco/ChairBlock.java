package com.bakery.deco;

import com.bakery.registry.BakeryEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** A cafe chair you can actually sit on. */
public class ChairBlock extends DecoBlock {
	public ChairBlock(Settings settings, VoxelShape northShape) {
		super(settings, northShape, false);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (player.isSneaking() || player.hasVehicle()) {
			return ActionResult.PASS;
		}
		if (!world.isClient) {
			if (!world.getEntitiesByClass(SeatEntity.class, new Box(pos), seat -> true).isEmpty()) {
				return ActionResult.CONSUME;
			}
			SeatEntity seat = new SeatEntity(BakeryEntities.SEAT, world);
			seat.setPosition(pos.getX() + 0.5, pos.getY() + 0.45, pos.getZ() + 0.5);
			seat.setYaw(state.get(FACING).asRotation());
			world.spawnEntity(seat);
			player.startRiding(seat);
		}
		return ActionResult.success(world.isClient);
	}
}
