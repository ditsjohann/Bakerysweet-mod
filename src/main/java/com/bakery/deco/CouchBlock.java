package com.bakery.deco;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * A couch you can sit on. Couches placed side by side facing the same way join into one long sofa: the arms move
 * to the two ends and the cushions run on in between.
 */
public class CouchBlock extends ChairBlock {
	public enum Part implements StringIdentifiable {
		SINGLE, LEFT, MIDDLE, RIGHT;

		@Override
		public String asString() {
			return name().toLowerCase();
		}
	}

	public static final EnumProperty<Part> PART = EnumProperty.of("part", Part.class);

	public CouchBlock(Settings settings, VoxelShape northShape) {
		super(settings, northShape);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(PART, Part.SINGLE));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		BlockState state = super.getPlacementState(context);
		return state == null ? null : connect(state, context.getWorld(), context.getBlockPos());
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighbor, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		return direction.getAxis().isHorizontal() ? connect(state, world, pos) : state;
	}

	/** Seen sitting on it (looking the way it faces): which neighbours are couches facing the same way. */
	private BlockState connect(BlockState state, WorldView world, BlockPos pos) {
		Direction facing = state.get(FACING);
		boolean left = joins(world.getBlockState(pos.offset(facing.rotateYCounterclockwise())), facing);
		boolean right = joins(world.getBlockState(pos.offset(facing.rotateYClockwise())), facing);
		Part part = left && right ? Part.MIDDLE : right ? Part.LEFT : left ? Part.RIGHT : Part.SINGLE;
		return state.with(PART, part);
	}

	private boolean joins(BlockState other, Direction facing) {
		return other.getBlock() instanceof CouchBlock && other.get(FACING) == facing;
	}
}
