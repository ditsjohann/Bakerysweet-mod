package com.bakery.oven;

import com.bakery.registry.BakeryBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * An invisible, solid piece of a formed machine. The controller draws the whole machine; these give it a body,
 * pass clicks on to it, and turn back into the block they were made from when broken.
 */
public class MultiblockPartBlock extends Block {
	public enum Kind implements StringIdentifiable {
		BRICK, OVEN_HEART, STONE, WALL, TERRACOTTA, GOLD;

		@Override
		public String asString() {
			return name().toLowerCase();
		}

		public static Kind of(Multiblock multiblock, Multiblock.Role role) {
			return switch (role) {
				case BRICK -> BRICK;
				case STONE -> STONE;
				case WALL -> WALL;
				case TERRACOTTA -> TERRACOTTA;
				case GOLD -> GOLD;
				case CORE -> multiblock == Multiblock.OVEN ? OVEN_HEART : null;
				case CHAMBER -> null;
			};
		}

		public BlockState restore(Direction facing) {
			return switch (this) {
				case BRICK -> Blocks.BRICKS.getDefaultState();
				case OVEN_HEART -> BakeryBlocks.OVEN_HEART.getDefaultState().with(CoreBlock.FACING, facing);
				case STONE -> Blocks.STONE_BRICKS.getDefaultState();
				case WALL -> Blocks.COBBLESTONE_WALL.getDefaultState();
				case TERRACOTTA -> Blocks.WHITE_TERRACOTTA.getDefaultState();
				case GOLD -> Blocks.GOLD_BLOCK.getDefaultState();
			};
		}
	}

	public static final EnumProperty<Kind> KIND = EnumProperty.of("kind", Kind.class);

	public MultiblockPartBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(KIND, Kind.BRICK));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(KIND);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
		return 1f;
	}

	@Override
	protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
		return true;
	}

	private static BlockPos controller(World world, BlockPos pos) {
		return Multiblock.findController(world, pos);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		BlockPos controller = controller(world, pos);
		if (controller == null) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		BlockState controllerState = world.getBlockState(controller);
		return controllerState.onUseWithItem(stack, world, player, hand, hit.withBlockPos(controller));
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		BlockPos controller = controller(world, pos);
		if (controller == null) {
			return ActionResult.PASS;
		}
		return world.getBlockState(controller).onUse(world, player, hit.withBlockPos(controller));
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!newState.isOf(this) && !world.isClient && !Multiblock.isDeforming()) {
			BlockPos controller = controller(world, pos);
			if (controller != null) {
				BlockState controllerState = world.getBlockState(controller);
				Multiblock multiblock = Multiblock.ofController(controllerState);
				if (multiblock != null) {
					multiblock.deform(world, controller, controllerState.get(GrandOvenBlock.FACING), pos);
				}
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
