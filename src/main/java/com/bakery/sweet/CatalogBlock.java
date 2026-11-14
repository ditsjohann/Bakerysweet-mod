package com.bakery.sweet;

import com.bakery.deco.DecoBlock;
import com.bakery.item.GuideItem;
import com.bakery.network.SweetPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * A lectern with the Bakery Catalog on it - the first thing every baker gets. Right-click to browse and buy
 * with Sweet Coins, or open the Read tab for the bakery guide.
 */
public class CatalogBlock extends DecoBlock {
	public CatalogBlock(Settings settings, VoxelShape northShape) {
		super(settings, northShape, false);
	}

	@Override
	protected boolean dyeable() {
		return true;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (player instanceof ServerPlayerEntity serverPlayer) {
			world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1.1f);
			ServerPlayNetworking.send(serverPlayer, new SweetPayloads.OpenCatalog(pos, GuideItem.data(serverPlayer)));
		}
		return ActionResult.success(world.isClient);
	}
}
