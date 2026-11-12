package com.bakery.kitchen;

import com.bakery.baking.BakingRecipes;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * The Proofing Basket. Let dough rest here and it rises: take it out once it has fully risen
 * (but before it collapses) and it is "proofed" - worth an extra star when you roll it out.
 */
public class ProofingBasketBlockEntity extends StationEntity {
	public static final int RISEN = 600;
	public static final int COLLAPSE = 4800;

	private ItemStack dough = ItemStack.EMPTY;
	private long placedAt;

	public ProofingBasketBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.PROOFING_BASKET, pos, state);
	}

	public ItemStack dough() {
		return dough;
	}

	public long elapsed() {
		return world == null || dough.isEmpty() ? 0 : world.getTime() - placedAt;
	}

	@Override
	protected int level() {
		if (dough.isEmpty()) return 0;
		long t = elapsed();
		if (t > COLLAPSE) return 1;
		if (t >= RISEN) return 3;
		return t >= RISEN / 2 ? 2 : 1;
	}

	@Override
	public boolean interact(PlayerEntity player, ItemStack stack) {
		if (world == null) return false;
		if (BakingRecipes.DoughType.of(stack) != null) {
			if (!dough.isEmpty()) {
				player.sendMessage(Text.translatable("message.bakery.proof.full"), true);
				return true;
			}
			if (Boolean.TRUE.equals(stack.get(BakeryComponents.PROOFED))) {
				player.sendMessage(Text.translatable("message.bakery.proof.already"), true);
				return true;
			}
			dough = stack.copyWithCount(1);
			placedAt = world.getTime();
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 0.8f, 1.2f);
			player.sendMessage(Text.translatable("message.bakery.proof.placed"), true);
			sync();
			return true;
		}
		if (!stack.isEmpty() || dough.isEmpty()) {
			return false;
		}
		long t = elapsed();
		ItemStack out = dough;
		if (t >= RISEN && t <= COLLAPSE) {
			out.set(BakeryComponents.PROOFED, true);
			player.sendMessage(Text.translatable("message.bakery.proof.perfect").formatted(Formatting.GOLD), true);
			world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1f, 1.5f);
		} else {
			player.sendMessage(Text.translatable(t < RISEN ? "message.bakery.proof.early" : "message.bakery.proof.collapsed"), true);
		}
		player.getInventory().offerOrDrop(out);
		dough = ItemStack.EMPTY;
		sync();
		return true;
	}

	@Override
	public void tick() {
		if (world != null && !dough.isEmpty() && world.getTime() % 20 == 0) {
			long t = elapsed();
			if (t >= RISEN && t < RISEN + 20) {
				world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.7f, 1.8f);
			}
			sync();
		}
	}

	@Override
	public void dropContents() {
		if (world != null) KitchenBlock.spill(world, pos, dough);
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		if (!dough.isEmpty()) nbt.put("dough", dough.encode(registries));
		nbt.putLong("placedAt", placedAt);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		dough = nbt.contains("dough") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("dough")) : ItemStack.EMPTY;
		placedAt = nbt.getLong("placedAt");
	}
}
