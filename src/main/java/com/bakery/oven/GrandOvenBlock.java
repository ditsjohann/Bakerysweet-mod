package com.bakery.oven;

import com.bakery.registry.BakeryBlockEntities;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * The heart of a formed Grand Brick Oven. Sits in the chamber and renders the whole 3x3x3 oven model.
 */
public class GrandOvenBlock extends BlockWithEntity {
	public static final MapCodec<GrandOvenBlock> CODEC = createCodec(GrandOvenBlock::new);
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = Properties.LIT;

	public GrandOvenBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(LIT, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new GrandOvenBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return validateTicker(type, BakeryBlockEntities.GRAND_OVEN, GrandOvenBlockEntity::tick);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof GrandOvenBlockEntity oven) {
			oven.interact(player, hand, stack, hit.getPos());
		}
		return ItemActionResult.success(world.isClient);
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof GrandOvenBlockEntity oven) {
			oven.interact(player, Hand.MAIN_HAND, ItemStack.EMPTY, hit.getPos());
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		if (!state.get(LIT)) {
			return;
		}
		Direction facing = state.get(FACING);
		BlockPos mouth = pos.offset(facing);
		double side = (random.nextDouble() - 0.5) * 0.6;
		double x = mouth.getX() + 0.5 - facing.getOffsetX() * 0.45 + facing.getOffsetZ() * side;
		double z = mouth.getZ() + 0.5 - facing.getOffsetZ() * 0.45 + facing.getOffsetX() * side;
		world.addParticle(ParticleTypes.FLAME, x, pos.getY() + 0.1 + random.nextDouble() * 0.15, z, 0, 0.01, 0);
		if (random.nextFloat() < 0.4f) {
			world.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 0.2, z, 0, 0.02, 0);
		}
		// Chimney smoke at the back of the dome.
		BlockPos chimney = pos.offset(facing.getOpposite()).up();
		world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, chimney.getX() + 0.5 + random.nextDouble() * 0.2, chimney.getY() + 1.0, chimney.getZ() + 0.5, 0, 0.04, 0);
		if (world.getBlockEntity(pos) instanceof GrandOvenBlockEntity oven) {
			for (int i = 0; i < GrandOvenBlockEntity.SLOTS; i++) {
				if (oven.isBaking(i) && oven.doneness(i, 0) > oven.getTemperature().perfectEnd()) {
					world.addParticle(ParticleTypes.LARGE_SMOKE, mouth.getX() + 0.5, mouth.getY() + 0.7, mouth.getZ() + 0.5, 0, 0.05, 0);
				}
			}
		}
		if (random.nextDouble() < 0.08) {
			world.playSound(x, pos.getY(), z, SoundEvents.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.BLOCKS, 0.8f, 1f, false);
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && !world.isClient && !Multiblock.isDeforming()) {
			if (world.getBlockEntity(pos) instanceof GrandOvenBlockEntity oven) {
				oven.dropContents();
			}
			Multiblock.OVEN.deform(world, pos, state.get(FACING), pos);
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
