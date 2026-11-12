package com.bakery.block;

import com.bakery.food.BakeryFoods;
import com.bakery.food.Topping;
import com.bakery.registry.BakeryComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** A frosted cake you can decorate with one topping while it is still whole. Each topping adds stars. */
public class DecoratedCakeBlock extends SliceFoodBlock {
	public static final MapCodec<DecoratedCakeBlock> CODEC = createCodec(settings -> new DecoratedCakeBlock(settings, createCuboidShape(1, 0, 1, 15, 8, 15), new FoodComponent.Builder().nutrition(3).saturationModifier(0.4f).build()));

	public DecoratedCakeBlock(Settings settings, VoxelShape shape, FoodComponent slice) {
		super(settings, shape, slice);
		setDefaultState(getDefaultState().with(TOPPING, Topping.NONE).with(CUSTOM, false));
	}

	@Override
	protected MapCodec<? extends BlockWithEntity> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(TOPPING, CUSTOM);
	}

	@Override
	protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		Topping topping = Topping.fromItem(stack);
		if (topping == Topping.NONE) {
			return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
		}
		if (state.get(TOPPING) != Topping.NONE || state.get(BITES) != 0 || state.get(CUSTOM)) {
			if (!world.isClient) {
				player.sendMessage(Text.translatable("message.bakery.already_decorated"), true);
			}
			return ItemActionResult.CONSUME;
		}
		if (!world.isClient) {
			world.setBlockState(pos, state.with(TOPPING, topping));
			if (world.getBlockEntity(pos) instanceof PlacedFoodBlockEntity food) {
				ItemStack cake = stored(world, pos).copy();
				cake.set(BakeryComponents.TOPPING, topping.asString());
				cake.set(BakeryComponents.QUALITY, Math.min(5, BakeryFoods.quality(cake) + topping.bonus()));
				food.setStack(cake);
			}
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1f, 1.4f);
			if (world instanceof ServerWorld server) {
				server.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0);
			}
			player.sendMessage(Text.translatable("message.bakery.decorated", topping.displayName()), true);
		}
		return ItemActionResult.success(world.isClient);
	}
}
