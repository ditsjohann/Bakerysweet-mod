package com.bakery.block;

import com.bakery.baking.BakingRecipes;
import com.bakery.registry.BakeryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Drop in ingredients one by one, then whisk until the dough comes together. */
public class MixingBowlBlock extends BlockWithEntity {
	public static final MapCodec<MixingBowlBlock> CODEC = createCodec(MixingBowlBlock::new);
	public static final BooleanProperty FILLED = BooleanProperty.of("filled");
	private static final VoxelShape SHAPE = Block.createCuboidShape(2, 0, 2, 14, 7, 14);

	public MixingBowlBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(com.bakery.deco.Dyeable.DYED, false));
		setDefaultState(getDefaultState().with(FILLED, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FILLED, com.bakery.deco.Dyeable.DYED, com.bakery.deco.Dyeable.COLOR);
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
		return new MixingBowlBlockEntity(pos, state);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		ItemActionResult dyed = com.bakery.deco.Dyeable.use(stack, state, world, pos, player);
		if (dyed != null) return dyed;
		if (!(world.getBlockEntity(pos) instanceof MixingBowlBlockEntity bowl)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (stack.isOf(BakeryItems.WHISK)) {
			if (bowl.getContents().isEmpty()) {
				if (!world.isClient) {
					player.sendMessage(Text.translatable("message.bakery.bowl.empty"), true);
				}
				return ItemActionResult.CONSUME;
			}
			if (!world.isClient) {
				stir(bowl, stack, (ServerWorld) world, pos, player, hand);
			}
			return ItemActionResult.success(world.isClient);
		}
		if (BakingRecipes.isBowlIngredient(stack)) {
			if (!world.isClient) {
				if (bowl.add(stack)) {
					if (stack.isOf(Items.WATER_BUCKET)) {
						if (!player.isCreative()) {
							player.setStackInHand(hand, new ItemStack(Items.BUCKET));
						}
					} else {
						stack.decrementUnlessCreative(1, player);
					}
					world.playSound(null, pos, SoundEvents.BLOCK_DECORATED_POT_INSERT, SoundCategory.BLOCKS, 0.8f, 1.1f + world.random.nextFloat() * 0.3f);
				} else {
					player.sendMessage(Text.translatable("message.bakery.bowl.full"), true);
				}
			}
			return ItemActionResult.success(world.isClient);
		}
		return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	private static void stir(MixingBowlBlockEntity bowl, ItemStack whisk, ServerWorld world, BlockPos pos, PlayerEntity player, Hand hand) {
		ItemStack particleItem = bowl.getContents().get(world.random.nextInt(bowl.getContents().size()));
		world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, particleItem), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.15, 0.05, 0.15, 0.05);
		world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.8f, 1.2f + world.random.nextFloat() * 0.4f);

		ItemStack result = bowl.stir();
		if (result == null) {
			player.sendMessage(Text.translatable("message.bakery.bowl.wrong").formatted(Formatting.RED), true);
			world.playSound(null, pos, SoundEvents.BLOCK_MUD_BREAK, SoundCategory.BLOCKS, 1f, 0.8f);
		} else if (!result.isEmpty()) {
			player.getInventory().offerOrDrop(result);
			player.sendMessage(Text.translatable("message.bakery.bowl.done", result.getName()).formatted(Formatting.GREEN), true);
			world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.4f, 1.8f);
			whisk.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		}
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof MixingBowlBlockEntity bowl) || bowl.getContents().isEmpty()) {
			return ActionResult.PASS;
		}
		if (!player.isSneaking()) {
			if (!world.isClient) {
				player.sendMessage(Text.translatable("message.bakery.bowl.hint"), true);
			}
			return ActionResult.CONSUME;
		}
		if (!world.isClient) {
			for (ItemStack stack : bowl.takeAll()) {
				player.getInventory().offerOrDrop(stack);
			}
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof MixingBowlBlockEntity bowl) {
			for (ItemStack stack : bowl.getContents()) {
				ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
