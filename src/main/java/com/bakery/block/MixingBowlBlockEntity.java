package com.bakery.block;

import com.bakery.baking.BakingRecipes;
import com.bakery.food.Flavor;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Holds up to six ingredients and counts how many times they have been whisked. */
public class MixingBowlBlockEntity extends BlockEntity {
	private final List<ItemStack> contents = new ArrayList<>();
	private int stirs;
	private long stirredAt;

	public MixingBowlBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.MIXING_BOWL, pos, state);
	}

	public List<ItemStack> getContents() {
		return contents;
	}

	public int getStirs() {
		return stirs;
	}

	public long stirredAt() {
		return stirredAt;
	}

	public boolean add(ItemStack stack) {
		if (contents.size() >= BakingRecipes.MAX_BOWL_ITEMS) {
			return false;
		}
		contents.add(stack.copyWithCount(1));
		stirs = 0;
		sync();
		return true;
	}

	public List<ItemStack> takeAll() {
		List<ItemStack> taken = new ArrayList<>(contents);
		contents.clear();
		stirs = 0;
		sync();
		return taken;
	}

	public BakingRecipes.DoughType predict() {
		return contents.isEmpty() ? null : BakingRecipes.match(contents);
	}

	/** One whisk. Returns the finished dough once stirred enough, EMPTY while still mixing, or null if the recipe is wrong. */
	public ItemStack stir() {
		stirs++;
		if (world != null) stirredAt = world.getTime();
		if (stirs < BakingRecipes.STIRS_NEEDED) {
			sync();
			return ItemStack.EMPTY;
		}
		BakingRecipes.DoughType type = predict();
		if (type == null) {
			stirs = 0;
			sync();
			return null;
		}
		Flavor flavor = BakingRecipes.flavorOf(contents);
		ItemStack dough = new ItemStack(type.dough());
		dough.set(BakeryComponents.FLAVOR, flavor.asString());
		if (contents.stream().noneMatch(c -> Boolean.TRUE.equals(c.get(BakeryComponents.STORE_BOUGHT)))) {
			dough.set(BakeryComponents.HOMEMADE, true);
		}
		contents.clear();
		stirs = 0;
		sync();
		return dough;
	}

	private void sync() {
		markDirty();
		if (world != null) {
			BlockState state = getCachedState();
			boolean filled = !contents.isEmpty();
			if (state.get(MixingBowlBlock.FILLED) != filled) {
				world.setBlockState(pos, state.with(MixingBowlBlock.FILLED, filled), Block.NOTIFY_ALL);
			} else {
				world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
			}
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		NbtList list = new NbtList();
		for (ItemStack stack : contents) {
			list.add(stack.encode(registries));
		}
		nbt.put("contents", list);
		nbt.putInt("stirs", stirs);
		nbt.putLong("stirredAt", stirredAt);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		contents.clear();
		NbtList list = nbt.getList("contents", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			ItemStack.fromNbt(registries, list.getCompound(i)).ifPresent(contents::add);
		}
		stirs = nbt.getInt("stirs");
		stirredAt = nbt.getLong("stirredAt");
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
		return createNbt(registries);
	}
}
