package com.bakery.shop;

import com.bakery.food.Perishable;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** A glass bakery case. Right-click with a bake to put it on show, right-click empty-handed to take one back. */
public class DisplayCaseBlock extends BlockWithEntity {
	public static final MapCodec<DisplayCaseBlock> CODEC = createCodec(DisplayCaseBlock::new);
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 16);

	public DisplayCaseBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(com.bakery.deco.Dyeable.DYED, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, com.bakery.deco.Dyeable.DYED, com.bakery.deco.Dyeable.COLOR);
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
		return new DisplayCaseBlockEntity(pos, state);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		ItemActionResult dyed = com.bakery.deco.Dyeable.use(stack, state, world, pos, player);
		if (dyed != null) return dyed;
		if (!(stack.getItem() instanceof Perishable) || !(world.getBlockEntity(pos) instanceof DisplayCaseBlockEntity display)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!world.isClient && display.add(stack)) {
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_GLASS_PLACE, SoundCategory.BLOCKS, 0.5f, 1.6f);
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof DisplayCaseBlockEntity display)) {
			return ActionResult.PASS;
		}
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
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof DisplayCaseBlockEntity display) {
			for (int i = 0; i < DisplayCaseBlockEntity.SLOTS; i++) {
				ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, display.getSlot(i));
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
