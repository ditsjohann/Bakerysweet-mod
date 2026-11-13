package com.bakery.oven;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The oven's wire rack, pulled out over the open door. Only exists while it is out; the oven draws it.
 * Clicks on it go to the oven, which works out which square of the rack was clicked.
 */
public class OvenRackBlock extends HorizontalFacingBlock {
	public static final MapCodec<OvenRackBlock> CODEC = createCodec(OvenRackBlock::new);
	private static final VoxelShape[] SHAPES = new VoxelShape[4];

	static {
		// The rack (north-facing model space: x 2..14, z 1..15 of this block, a little above the hearth), plus its rails.
		for (Direction d : Direction.Type.HORIZONTAL) {
			SHAPES[d.getHorizontal()] = rotate(d, 1.5, 0, 0, 14.5, 5, 15.5);
		}
	}

	private static VoxelShape rotate(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
		// Model space faces north; turn it like the oven's blockstate does.
		return switch (facing) {
			case SOUTH -> Block.createCuboidShape(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
			case EAST -> Block.createCuboidShape(16 - z2, y1, x1, 16 - z1, y2, x2);
			case WEST -> Block.createCuboidShape(z1, y1, 16 - x2, z2, y2, 16 - x1);
			default -> Block.createCuboidShape(x1, y1, z1, x2, y2, z2);
		};
	}

	public OvenRackBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[state.get(FACING).getHorizontal()];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

	private static GrandOvenBlockEntity oven(World world, BlockPos pos, BlockState state) {
		BlockPos controller = pos.offset(state.get(FACING).getOpposite(), 2);
		return world.getBlockEntity(controller) instanceof GrandOvenBlockEntity oven ? oven : null;
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		GrandOvenBlockEntity oven = oven(world, pos, state);
		if (oven == null) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		oven.interact(player, hand, stack, hit.getPos(), true);
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		GrandOvenBlockEntity oven = oven(world, pos, state);
		if (oven == null) return ActionResult.PASS;
		oven.interact(player, Hand.MAIN_HAND, ItemStack.EMPTY, hit.getPos(), true);
		return ActionResult.success(world.isClient);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!newState.isOf(this) && !world.isClient) {
			GrandOvenBlockEntity oven = oven(world, pos, state);
			if (oven != null) oven.rackRemoved();
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
