package com.bakery.shop;

import com.bakery.deco.DecoBlock;
import com.bakery.network.SweetPayloads;
import com.bakery.registry.BakeryBlockEntities;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
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
 * The Order Notebook: hang it on a wall and it keeps track of every open order at every bakery counter in the world,
 * who wants what, whether they wait in line or at a table, and how patient they still are. The first few are written
 * on the page; right-click to read the whole list.
 */
public class OrderNotebookBlock extends DecoBlock implements BlockEntityProvider {
	public OrderNotebookBlock(Settings settings, VoxelShape northShape) {
		super(settings, northShape, true);
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new OrderNotebookBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient || type != BakeryBlockEntities.ORDER_NOTEBOOK ? null
				: (BlockEntityTicker<T>) (BlockEntityTicker<OrderNotebookBlockEntity>) OrderNotebookBlockEntity::tick;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.isClient) return ActionResult.SUCCESS;
		if (world.getBlockEntity(pos) instanceof OrderNotebookBlockEntity notebook) notebook.refresh();
		if (player instanceof ServerPlayerEntity serverPlayer) com.bakery.network.BakeryNetworking.send(serverPlayer, new SweetPayloads.OpenNotebook(pos));
		world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 0.8f, 1.2f);
		return ActionResult.CONSUME;
	}
}
