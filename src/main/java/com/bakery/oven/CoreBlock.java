package com.bakery.oven;

import com.bakery.registry.BakeryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

/** The heart of a buildable machine (Oven Heart, Millstone Core, Fountain Core). Tap it with a Sweetheart Spatula. */
public class CoreBlock extends HorizontalFacingBlock {
	private final Supplier<Multiblock> multiblock;

	public CoreBlock(Settings settings, Supplier<Multiblock> multiblock) {
		super(settings);
		this.multiblock = multiblock;
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return createCodec(settings -> this);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!stack.isOf(BakeryItems.SPATULA)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (world instanceof ServerWorld server) {
			multiblock.get().tryForm(server, pos, state.get(FACING), player, message -> player.sendMessage(message, true));
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient) {
			player.sendMessage(Text.translatable("message.bakery.build.hint." + multiblock.get().id()), true);
		}
		return ActionResult.CONSUME;
	}
}
