package com.bakery.deco;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

import java.util.EnumMap;
import java.util.Map;

/**
 * A decorative bakery block that faces the player. The shape is given for north and rotated for the other sides.
 * Wall-mounted pieces (bunting, shelves, menu boards) face away from the wall they are placed on.
 */
public class DecoBlock extends HorizontalFacingBlock {
	public static final MapCodec<DecoBlock> CODEC = createCodec(settings -> new DecoBlock(settings, VoxelShapes.fullCube(), false));

	private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
	private final boolean wallMounted;

	public DecoBlock(Settings settings, VoxelShape northShape, boolean wallMounted) {
		super(settings);
		this.wallMounted = wallMounted;
		if (dyeable()) setDefaultState(getDefaultState().with(Dyeable.DYED, false));
		for (Direction direction : Direction.Type.HORIZONTAL) {
			shapes.put(direction, rotate(northShape, direction));
		}
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		if (dyeable()) builder.add(Dyeable.DYED, Dyeable.COLOR);
	}

	/** Whether dye paints this block. Called while the block is being built, so overrides return a constant. */
	protected boolean dyeable() {
		return false;
	}

	@Override
	protected net.minecraft.util.ItemActionResult onUseWithItem(net.minecraft.item.ItemStack stack, BlockState state, net.minecraft.world.World world, BlockPos pos,
			net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hit) {
		net.minecraft.util.ItemActionResult dyed = Dyeable.use(stack, state, world, pos, player);
		return dyed != null ? dyed : super.onUseWithItem(stack, state, world, pos, player, hand, hit);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		if (wallMounted && context.getSide().getAxis().isHorizontal()) {
			return getDefaultState().with(FACING, context.getSide());
		}
		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shapes.get(state.get(FACING));
	}

	/** Rotates a shape drawn facing north around the block center. */
	public static VoxelShape rotate(VoxelShape shape, Direction to) {
		VoxelShape[] result = {VoxelShapes.empty()};
		int turns = (to.getHorizontal() - Direction.NORTH.getHorizontal() + 4) % 4;
		shape.forEachBox((x1, y1, z1, x2, y2, z2) -> {
			double ax1 = x1, az1 = z1, ax2 = x2, az2 = z2;
			for (int i = 0; i < turns; i++) {
				double nx1 = 1 - az2, nz1 = ax1, nx2 = 1 - az1, nz2 = ax2;
				ax1 = nx1;
				az1 = nz1;
				ax2 = nx2;
				az2 = nz2;
			}
			result[0] = VoxelShapes.combine(result[0], VoxelShapes.cuboid(ax1, y1, az1, ax2, y2, az2), BooleanBiFunction.OR);
		});
		return result[0];
	}
}
