package com.bakery.oven;

import com.bakery.registry.BakeryBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
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
 * The Chocolate Fountain: a round, three-tier tabletop fountain you simply place, like any block.
 * Melt chocolate in, then dip fruit for chocolate treats.
 */
public class FountainBlock extends BlockWithEntity {
	public static final MapCodec<FountainBlock> CODEC = createCodec(FountainBlock::new);
	private static final VoxelShape SHAPE = VoxelShapes.union(
			Block.createCuboidShape(0, 0, 0, 16, 5, 16),
			Block.createCuboidShape(2.5, 5, 2.5, 13.5, 9.5, 13.5),
			Block.createCuboidShape(4, 9.5, 4, 12, 15, 12),
			Block.createCuboidShape(5.5, 15, 5.5, 10.5, 22, 10.5));
	private static final VoxelShape COLLISION = Block.createCuboidShape(0.5, 0, 0.5, 15.5, 16, 15.5);

	public FountainBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(MachineBlock.FACING, Direction.NORTH).with(MachineBlock.ACTIVE, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(MachineBlock.FACING, MachineBlock.ACTIVE);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(MachineBlock.FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return COLLISION;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new FountainBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return validateTicker(type, BakeryBlockEntities.FOUNTAIN, (w, p, s, fountain) -> fountain.tick());
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!world.isClient && world.getBlockEntity(pos) instanceof FountainBlockEntity fountain) {
			fountain.interact(player, stack);
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient && world.getBlockEntity(pos) instanceof FountainBlockEntity fountain) {
			fountain.interact(player, ItemStack.EMPTY);
		}
		return ActionResult.success(world.isClient);
	}
}
