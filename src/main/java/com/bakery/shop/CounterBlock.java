package com.bakery.shop;

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
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The bakery counter. Right-click to flip the sign between Open and Closed; sneak + right-click to empty the till.
 * Customers line up on the side it faces. It is also the Baker villager's job site.
 */
public class CounterBlock extends BlockWithEntity {
	public static final MapCodec<CounterBlock> CODEC = createCodec(CounterBlock::new);
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final BooleanProperty OPEN = Properties.OPEN;
	private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 15, 16);

	public CounterBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(com.bakery.deco.Dyeable.DYED, false));
		setDefaultState(getDefaultState().with(OPEN, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, com.bakery.deco.Dyeable.DYED, com.bakery.deco.Dyeable.COLOR);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing());
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
		return new CounterBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return world.isClient ? null : validateTicker(type, BakeryBlockEntities.COUNTER, CounterBlockEntity::tick);
	}

	@Override
	protected net.minecraft.util.ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player,
															  net.minecraft.util.Hand hand, BlockHitResult hit) {
		// A Path Ribbon ties its path to the counter (the item handles it) instead of flipping the sign.
		if (stack.isOf(com.bakery.registry.BakeryItems.PATH_RIBBON)) return net.minecraft.util.ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		net.minecraft.util.ItemActionResult dyed = com.bakery.deco.Dyeable.use(stack, state, world, pos, player);
		if (dyed != null) return dyed;
		return net.minecraft.util.ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof CounterBlockEntity counter)) {
			return ActionResult.PASS;
		}
		if (world.isClient) {
			return ActionResult.SUCCESS;
		}
		if (counter.getOwner() == null) {
			counter.setOwner(player.getUuid());
		}
		if (player.isSneaking()) {
			collect(world, pos, player);
			return ActionResult.CONSUME;
		}
		// Everything else (open sign, till, the review board) is on the counter's screen.
		if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(serverPlayer, new com.bakery.network.SweetPayloads.OpenCounter(pos));
		}
		return ActionResult.CONSUME;
	}

	/** Empties the till into the player's purse. */
	public static void collect(World world, BlockPos pos, PlayerEntity player) {
		if (!(world.getBlockEntity(pos) instanceof CounterBlockEntity counter)) return;
		int coins = counter.collectTill();
		if (coins > 0) {
			if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) com.bakery.sweet.Coins.pay(serverPlayer, coins);
			world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.8f, 1.1f);
			player.sendMessage(Text.translatable("message.bakery.shop.till", coins).formatted(Formatting.LIGHT_PURPLE), true);
		} else {
			player.sendMessage(Text.translatable("message.bakery.shop.till_empty"), true);
		}
	}

	/** Flips the OPEN sign (a shop can only open once customers have a path to walk in on). */
	public static void toggleOpen(BlockState state, World world, BlockPos pos, PlayerEntity player) {
		if (!(world.getBlockEntity(pos) instanceof CounterBlockEntity counter)) return;
		boolean open = !state.get(OPEN);
		if (open && !counter.hasPath()) {
			player.sendMessage(Text.translatable("message.bakery.shop.needs_path").formatted(Formatting.GOLD), false);
			world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 0.7f, 0.8f);
			return;
		}
		world.setBlockState(pos, state.with(OPEN, open));
		world.playSound(null, pos, open ? SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value() : SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 1f, open ? 1.4f : 1f);
		player.sendMessage(Text.translatable(open ? "message.bakery.shop.open" : "message.bakery.shop.closed")
				.formatted(open ? Formatting.LIGHT_PURPLE : Formatting.GRAY), true);
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, net.minecraft.entity.LivingEntity placer, ItemStack stack) {
		super.onPlaced(world, pos, state, placer, stack);
		if (!world.isClient && placer instanceof PlayerEntity player && world.getBlockEntity(pos) instanceof CounterBlockEntity counter) {
			counter.setOwner(player.getUuid());
		}
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof CounterBlockEntity counter) {
			counter.clearCustomers();
			if (counter.getTill() > 0) {
				Block.dropStack(world, pos, new ItemStack(com.bakery.registry.BakeryItems.SWEET_COIN, counter.getTill()));
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
