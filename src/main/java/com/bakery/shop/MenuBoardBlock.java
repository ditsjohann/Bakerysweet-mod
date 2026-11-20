package com.bakery.shop;

import com.bakery.deco.DecoBlock;
import com.bakery.network.SweetPayloads;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** The chalkboard menu: right-click to choose which dishes the nearest bakery counter offers. */
public class MenuBoardBlock extends DecoBlock {
	public MenuBoardBlock(Settings settings, VoxelShape northShape) {
		super(settings, northShape, true);
	}

	public static BlockPos nearestCounter(World world, BlockPos pos) {
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.iterate(pos.add(-8, -3, -8), pos.add(8, 3, 8))) {
			if (world.getBlockEntity(p) instanceof CounterBlockEntity) {
				double d = p.getSquaredDistance(pos);
				if (d < bestDistance) {
					bestDistance = d;
					best = p.toImmutable();
				}
			}
		}
		return best;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			BlockPos counter = nearestCounter(world, pos);
			if (counter == null) {
				player.sendMessage(Text.translatable("message.bakery.menu.no_counter"), true);
			} else {
				com.bakery.network.BakeryNetworking.send(serverPlayer, new SweetPayloads.OpenMenu(counter));
			}
		}
		return ActionResult.success(world.isClient);
	}
}
