package com.bakery.sweet;

import com.bakery.registry.BakeryItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The Cake Turntable: set a whole cake on it and decorate it right there - see {@link TurntableBlockEntity}.
 * The outline grows with the cake so every tier can be clicked.
 */
public class TurntableBlock extends BlockWithEntity {
	public static final MapCodec<TurntableBlock> CODEC = createCodec(TurntableBlock::new);
	private static final VoxelShape PLATE = Block.createCuboidShape(2, 0, 2, 14, 3, 14);
	private static final VoxelShape[] WITH_CAKE = new VoxelShape[CakeGeometry.MAX_TIERS];

	static {
		for (int tiers = 1; tiers <= CakeGeometry.MAX_TIERS; tiers++) {
			VoxelShape shape = PLATE;
			for (int i = 0; i < tiers; i++) {
				float r = CakeGeometry.radius(i, tiers) + 0.5f;
				shape = VoxelShapes.union(shape, Block.createCuboidShape(8 - r, 3 + CakeGeometry.bottom(i, tiers), 8 - r, 8 + r,
						Math.min(16, 3 + CakeGeometry.top(i, tiers) + 0.6), 8 + r));
			}
			WITH_CAKE[tiers - 1] = shape;
		}
	}

	public TurntableBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		if (world.getBlockEntity(pos) instanceof TurntableBlockEntity table && !table.getCake().isEmpty()) {
			return WITH_CAKE[Math.clamp(table.design().tierCount(), 1, CakeGeometry.MAX_TIERS) - 1];
		}
		return PLATE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return PLATE;
	}

	@Override
	protected BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new TurntableBlockEntity(pos, state);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof TurntableBlockEntity table)) {
			return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (table.getCake().isEmpty()) {
			if (!stack.isOf(BakeryItems.CAKE)) {
				if (!world.isClient && hand == Hand.MAIN_HAND) player.sendMessage(Text.translatable("message.bakery.turntable.empty"), true);
				return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			}
			if (!world.isClient) {
				table.setCake(stack.copyWithCount(1));
				stack.decrementUnlessCreative(1, player);
				world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 1f, 1.2f);
				player.sendMessage(Text.translatable("message.bakery.turntable.placed"), true);
			}
			return ItemActionResult.success(world.isClient);
		}
		if (world.isClient) {
			return CakeDesign.isTopper(stack) || stack.getItem() instanceof IcingBagItem || stack.isOf(BakeryItems.CAKE) || stack.isOf(BakeryItems.SPATULA)
					|| stack.isOf(BakeryItems.CAKE_SCRAPER)
					|| TurntableBlockEntity.mouldShape(stack) >= 0
					? ItemActionResult.SUCCESS : ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		return table.use((ServerPlayerEntity) player, hand, stack, hit.getPos(), hit.getSide()) ? ItemActionResult.SUCCESS : ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof TurntableBlockEntity table)) {
			return ActionResult.PASS;
		}
		if (table.getCake().isEmpty()) {
			if (!world.isClient) player.sendMessage(Text.translatable("message.bakery.turntable.empty"), true);
			return ActionResult.CONSUME;
		}
		if (!world.isClient) {
			if (player.isSneaking()) {
				player.getInventory().offerOrDrop(table.takeCake(player));
				world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.2f);
			} else {
				table.spin(45);
				world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.3f, 1.9f);
			}
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof TurntableBlockEntity table && !table.getCake().isEmpty()) {
			ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, table.takeCake(null));
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
}
