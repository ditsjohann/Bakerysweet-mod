package com.bakery.block;

import com.bakery.food.BakeryFoods;
import com.bakery.food.Drink;
import com.bakery.food.Flavor;
import com.bakery.food.Topping;
import com.bakery.item.DrinkItem;
import com.bakery.item.PlaceableFoodItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/**
 * Food sitting on a table. Right-click to eat a serving, sneak + right-click with an empty hand to pick it back up.
 * The block entity keeps the original item so stars and freshness are not lost.
 */
public abstract class PlacedFoodBlock extends BlockWithEntity {
	public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
	public static final EnumProperty<Flavor> FLAVOR = EnumProperty.of("flavor", Flavor.class);
	public static final EnumProperty<Topping> TOPPING = EnumProperty.of("topping", Topping.class);
	public static final EnumProperty<Drink> DRINK = EnumProperty.of("drink", Drink.class);
	public static final IntProperty COUNT = IntProperty.of("count", 1, 4);
	public static final IntProperty BITES = IntProperty.of("bites", 0, 3);
	/** A cake decorated by hand at the turntable: the block shows just a plate and the renderer draws the cake. */
	public static final net.minecraft.state.property.BooleanProperty CUSTOM = net.minecraft.state.property.BooleanProperty.of("custom");

	private final VoxelShape shape;

	protected PlacedFoodBlock(Settings settings, VoxelShape shape) {
		super(settings);
		this.shape = shape;
	}

	/** Copies the flavor, topping or drink of an item onto the block state it is being placed as. */
	public static BlockState withStackProperties(BlockState state, ItemStack stack) {
		if (state.contains(FLAVOR)) {
			state = state.with(FLAVOR, BakeryFoods.flavor(stack));
		}
		if (state.contains(TOPPING)) {
			state = state.with(TOPPING, BakeryFoods.topping(stack));
		}
		if (state.contains(CUSTOM)) {
			state = state.with(CUSTOM, stack.contains(com.bakery.registry.BakeryComponents.CAKE_DESIGN));
		}
		if (state.contains(DRINK) && stack.getItem() instanceof DrinkItem drink) {
			state = state.with(DRINK, drink.drink());
		}
		return state;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
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
		return new PlacedFoodBlockEntity(pos, state);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return Block.sideCoversSmallSquare(world, pos.down(), Direction.UP);
	}

	@Override
	protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
		if (direction == Direction.DOWN && !state.canPlaceAt(world, pos)) {
			return Blocks.AIR.getDefaultState();
		}
		return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	/** The item this block holds. Falls back to a plain one for blocks placed with commands. */
	protected ItemStack stored(World world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof PlacedFoodBlockEntity food && !food.getStack().isEmpty()) {
			return food.getStack();
		}
		return new ItemStack(asItem());
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		// Holding another bake: let the item place itself next to this one instead of eating.
		if (stack.getItem() instanceof PlaceableFoodItem) {
			return ItemActionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		}
		return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (player.isSneaking()) {
			ItemStack pickup = pickUpStack(state, stored(world, pos));
			if (pickup == null) {
				if (!world.isClient) {
					player.sendMessage(Text.translatable("message.bakery.cant_pick_up"), true);
				}
				return ActionResult.CONSUME;
			}
			if (!world.isClient) {
				removeQuietly(world, pos);
				player.getInventory().offerOrDrop(pickup);
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.2f);
			}
			return ActionResult.success(world.isClient);
		}
		if (serve(state, world, pos, player)) {
			return ActionResult.success(world.isClient);
		}
		if (!world.isClient) {
			player.sendMessage(Text.translatable("message.bakery.not_hungry"), true);
		}
		return ActionResult.CONSUME;
	}

	/** Removes the block without dropping what it holds. */
	protected void removeQuietly(World world, BlockPos pos) {
		if (world.getBlockEntity(pos) instanceof PlacedFoodBlockEntity food) {
			food.setStack(ItemStack.EMPTY);
		}
		world.removeBlock(pos, false);
	}

	/** Eat or drink one serving. Return false if nothing happened (for example the player is full). */
	protected abstract boolean serve(BlockState state, World world, BlockPos pos, PlayerEntity player);

	/** What sneak-clicking gives back, or null if it cannot be picked up right now. */
	protected ItemStack pickUpStack(BlockState state, ItemStack stored) {
		return stored.copyWithCount(1);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof PlacedFoodBlockEntity food && !food.getStack().isEmpty()) {
			ItemStack drop = pickUpStack(state, food.getStack());
			if (drop != null) {
				ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, drop);
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
