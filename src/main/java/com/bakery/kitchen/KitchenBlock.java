package com.bakery.kitchen;

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
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Shared block for the Juicer, Ice Cream Churn, Jam Pot and Proofing Basket.
 * LEVEL (0-3) is what the model shows: how full, how risen, how cooked.
 */
public class KitchenBlock extends BlockWithEntity {
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final IntProperty LEVEL = IntProperty.of("level", 0, 3);

	/** What a kitchen block entity does. */
	public interface Station {
		/** Return true if the click did something. */
		boolean interact(PlayerEntity player, ItemStack stack);

		default void tick() {
		}

		default void displayTick(Random random) {
		}

		default void dropContents() {
		}
	}

	private final VoxelShape shape;
	private final BiFunction<BlockPos, BlockState, BlockEntity> factory;
	private final Supplier<BlockEntityType<?>> type;

	public KitchenBlock(Settings settings, VoxelShape shape, BiFunction<BlockPos, BlockState, BlockEntity> factory, Supplier<BlockEntityType<?>> type) {
		super(settings);
		this.shape = shape;
		this.factory = factory;
		this.type = type;
		setDefaultState(getDefaultState().with(LEVEL, 0));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return createCodec(settings -> this);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEVEL);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return shape;
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return factory.apply(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> blockEntityType) {
		return !world.isClient && blockEntityType == type.get() ? (w, p, s, be) -> ((Station) be).tick() : null;
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient) {
			return ItemActionResult.SUCCESS;
		}
		return world.getBlockEntity(pos) instanceof Station station && station.interact(player, stack)
				? ItemActionResult.SUCCESS : ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		// Only a truly empty hand presses, churns or stirs - not a sword the machine had no use for.
		if (!player.getMainHandStack().isEmpty()) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		return world.getBlockEntity(pos) instanceof Station station && station.interact(player, ItemStack.EMPTY)
				? ActionResult.SUCCESS : ActionResult.PASS;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (world.getBlockEntity(pos) instanceof Station station) {
			station.displayTick(random);
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof Station station) {
			station.dropContents();
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}

	/** Helper for stations: drop a stack at the block. */
	public static void spill(World world, BlockPos pos, ItemStack stack) {
		if (!stack.isEmpty()) {
			ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, stack);
		}
	}
}
