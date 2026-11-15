package com.bakery.kitchen;

import com.bakery.food.Flavor;
import com.bakery.registry.BakeryBlockEntities;
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
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A little wooden rack holding up to four syrup bottles with pumps. Put a bottle in a spot, then hold a coffee or
 * latte against that bottle to pump its flavour in. An empty hand takes the bottle back.
 */
public class SyrupRackBlock extends BlockWithEntity {
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	/** Which syrup stands in each of the four spots: 0 = none, 1.. = {@link Flavor#SYRUPS} + 1. */
	public static final IntProperty[] SLOTS = {IntProperty.of("slot0", 0, 5), IntProperty.of("slot1", 0, 5),
			IntProperty.of("slot2", 0, 5), IntProperty.of("slot3", 0, 5)};
	private static final VoxelShape[] SHAPES = new VoxelShape[4];

	static {
		VoxelShape north = VoxelShapes.union(createCuboidShape(0, 0, 4, 16, 2, 12), createCuboidShape(0.5, 0, 4, 15.5, 11, 6.5));
		for (Direction d : Direction.Type.HORIZONTAL) {
			SHAPES[d.getHorizontal()] = rotate(north, d);
		}
	}

	public SyrupRackBlock(Settings settings) {
		super(settings);
		BlockState state = getDefaultState().with(FACING, Direction.NORTH);
		for (IntProperty slot : SLOTS) state = state.with(slot, 0);
		setDefaultState(state);
	}

	private static VoxelShape rotate(VoxelShape shape, Direction to) {
		VoxelShape[] out = {VoxelShapes.empty()};
		int turns = (to.getHorizontal() - Direction.NORTH.getHorizontal() + 4) % 4;
		shape.forEachBox((x1, y1, z1, x2, y2, z2) -> {
			double ax = x1, az = z1, bx = x2, bz = z2;
			for (int i = 0; i < turns; i++) {
				double nax = 1 - bz, nbx = 1 - az, naz = ax, nbz = bx;
				ax = nax; bx = nbx; az = naz; bz = nbz;
			}
			out[0] = VoxelShapes.union(out[0], VoxelShapes.cuboid(ax, y1, az, bx, y2, bz));
		});
		return out[0];
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return createCodec(SyrupRackBlock::new);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING).add(SLOTS);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES[state.get(FACING).getHorizontal()];
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new SyrupRackBlockEntity(pos, state);
	}

	/** The spot (0 left .. 3 right, as seen from the front) a click landed on. */
	public static int slotAt(BlockState state, BlockPos pos, Vec3d hit) {
		Direction right = state.get(FACING).getOpposite().rotateYClockwise();
		double along = (hit.x - pos.getX() - 0.5) * right.getOffsetX() + (hit.z - pos.getZ() - 0.5) * right.getOffsetZ();
		return Math.clamp((int) Math.floor((along + 0.5) * 4), 0, 3);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof SyrupRackBlockEntity rack)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		int slot = slotAt(state, pos, hit.getPos());
		if (world.isClient) return rack.wouldUse(slot, stack) ? ItemActionResult.SUCCESS : ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return rack.use(player, slot, stack) ? ItemActionResult.SUCCESS : ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!player.getMainHandStack().isEmpty() || !(world.getBlockEntity(pos) instanceof SyrupRackBlockEntity rack)) return ActionResult.PASS;
		int slot = slotAt(state, pos, hit.getPos());
		if (world.isClient) return state.get(SLOTS[slot]) > 0 ? ActionResult.SUCCESS : ActionResult.PASS;
		return rack.use(player, slot, ItemStack.EMPTY) ? ActionResult.SUCCESS : ActionResult.PASS;
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof SyrupRackBlockEntity rack) {
			rack.dropAll();
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
