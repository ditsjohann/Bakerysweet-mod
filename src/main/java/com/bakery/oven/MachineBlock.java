package com.bakery.oven;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The controller of a formed Millstone or Chocolate Fountain: draws the big model and hands clicks to its block entity. */
public class MachineBlock extends BlockWithEntity {
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

	/** What a machine's block entity does with clicks. */
	public interface Machine {
		void interact(PlayerEntity player, ItemStack stack);

		default void tick() {
		}
	}

	private final Supplier<Multiblock> multiblock;
	private final BiFunction<BlockPos, BlockState, BlockEntity> factory;
	private final Supplier<BlockEntityType<?>> type;

	public MachineBlock(Settings settings, Supplier<Multiblock> multiblock, BiFunction<BlockPos, BlockState, BlockEntity> factory, Supplier<BlockEntityType<?>> type) {
		super(settings);
		this.multiblock = multiblock;
		this.factory = factory;
		this.type = type;
		setDefaultState(getDefaultState().with(ACTIVE, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return createCodec(settings -> this);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, ACTIVE);
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
		return blockEntityType == type.get() ? (w, p, s, be) -> ((Machine) be).tick() : null;
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!world.isClient && world.getBlockEntity(pos) instanceof Machine machine) {
			machine.interact(player, stack);
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!world.isClient && world.getBlockEntity(pos) instanceof Machine machine) {
			machine.interact(player, ItemStack.EMPTY);
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && !world.isClient && !Multiblock.isDeforming()) {
			if (world.getBlockEntity(pos) instanceof Multiblock.MachineContents contents) {
				contents.dropContents();
			}
			multiblock.get().deform(world, pos, state.get(FACING), pos);
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
