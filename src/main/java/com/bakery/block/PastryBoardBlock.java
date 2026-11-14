package com.bakery.block;

import com.bakery.baking.BakingRecipes;
import com.bakery.network.OpenRollingPayload;
import com.bakery.registry.BakeryItems;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Put dough on the board, then use a rolling pin to open the shaping minigame. */
public class PastryBoardBlock extends BlockWithEntity {
	public static final MapCodec<PastryBoardBlock> CODEC = createCodec(PastryBoardBlock::new);
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty HAS_DOUGH = BooleanProperty.of("has_dough");
	private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 2, 15, 2, 14);

	public PastryBoardBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(HAS_DOUGH, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HAS_DOUGH);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new PastryBoardBlockEntity(pos, state);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof PastryBoardBlockEntity board)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		BakingRecipes.DoughType type = BakingRecipes.DoughType.of(stack);
		if (type != null && board.getDough().isEmpty()) {
			if (!world.isClient) {
				board.setDough(stack.copyWithCount(1));
				stack.decrementUnlessCreative(1, player);
				world.playSound(null, pos, SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundCategory.BLOCKS, 0.7f, 1.3f);
				player.sendMessage(Text.translatable("message.bakery.board.placed"), true);
			}
			return ItemActionResult.success(world.isClient);
		}
		com.bakery.sweet.CookieShape cut = com.bakery.sweet.CookieShape.ofCutter(stack);
		if (cut != null) {
			ItemStack dough = board.getDough();
			if (!dough.isOf(BakeryItems.COOKIE_DOUGH)) {
				if (!world.isClient) player.sendMessage(Text.translatable("message.bakery.board.cutter_needs_cookie_dough"), true);
				return ItemActionResult.CONSUME;
			}
			if (!world.isClient) {
				// Cutters are quick and tidy: a solid three stars, four with rested (proofed) dough.
				boolean proofed = Boolean.TRUE.equals(dough.get(com.bakery.registry.BakeryComponents.PROOFED));
				ItemStack tray = new ItemStack(BakeryItems.RAW_SUGAR_COOKIE);
				tray.set(com.bakery.registry.BakeryComponents.COOKIE_SHAPE, cut.asString());
				if (dough.contains(com.bakery.registry.BakeryComponents.FLAVOR)) {
					tray.set(com.bakery.registry.BakeryComponents.FLAVOR, dough.get(com.bakery.registry.BakeryComponents.FLAVOR));
				}
				tray.set(com.bakery.registry.BakeryComponents.QUALITY, proofed ? 4 : 3);
				tray.set(com.bakery.registry.BakeryComponents.HOMEMADE, dough.get(com.bakery.registry.BakeryComponents.HOMEMADE));
				com.bakery.food.Freshness.stampFresh(tray, world.getTime());
				board.setDough(ItemStack.EMPTY);
				player.getInventory().offerOrDrop(tray);
				world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 1f, 1.4f);
				player.sendMessage(Text.translatable("message.bakery.board.shaped", tray.getName()), true);
			}
			return ItemActionResult.success(world.isClient);
		}
		if (stack.isOf(BakeryItems.ROLLING_PIN)) {
			BakingRecipes.DoughType onBoard = BakingRecipes.DoughType.of(board.getDough());
			if (onBoard == null) {
				if (!world.isClient) {
					player.sendMessage(Text.translatable("message.bakery.board.no_dough"), true);
				}
				return ItemActionResult.CONSUME;
			}
			if (player instanceof ServerPlayerEntity serverPlayer) {
				ServerPlayNetworking.send(serverPlayer, new OpenRollingPayload(pos, onBoard.ordinal()));
			}
			return ItemActionResult.success(world.isClient);
		}
		return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof PastryBoardBlockEntity board) || board.getDough().isEmpty()) {
			return ActionResult.PASS;
		}
		if (!player.isSneaking()) {
			if (!world.isClient) {
				player.sendMessage(Text.translatable("message.bakery.board.hint"), true);
			}
			return ActionResult.CONSUME;
		}
		if (!world.isClient) {
			player.getInventory().offerOrDrop(board.getDough());
			board.setDough(ItemStack.EMPTY);
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof PastryBoardBlockEntity board && !board.getDough().isEmpty()) {
			ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, board.getDough());
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
