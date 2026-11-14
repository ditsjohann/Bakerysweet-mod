package com.bakery.shop;

import com.bakery.registry.BakeryBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/** Four bakes on show. Customers can buy straight from here; the money goes to the counter's till. */
public class DisplayCaseBlockEntity extends BlockEntity implements SelfServe {
	public static final int SLOTS = 4;
	private final ItemStack[] slots = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

	public DisplayCaseBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.DISPLAY_CASE, pos, state);
	}

	public ItemStack getSlot(int i) {
		return slots[i];
	}

	public boolean add(ItemStack stack) {
		for (int i = 0; i < SLOTS; i++) {
			if (slots[i].isEmpty()) {
				slots[i] = stack.copyWithCount(1);
				sync();
				return true;
			}
		}
		return false;
	}

	public ItemStack takeLast() {
		for (int i = SLOTS - 1; i >= 0; i--) {
			if (!slots[i].isEmpty()) {
				ItemStack taken = slots[i];
				slots[i] = ItemStack.EMPTY;
				sync();
				return taken;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack takeMatching(Order order) {
		for (int i = 0; i < SLOTS; i++) {
			if (!slots[i].isEmpty() && Menu.matches(order, slots[i])) {
				ItemStack taken = slots[i];
				slots[i] = ItemStack.EMPTY;
				sync();
				return taken;
			}
		}
		return ItemStack.EMPTY;
	}

	private void sync() {
		markDirty();
		if (world != null) {
			world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		for (int i = 0; i < SLOTS; i++) {
			if (!slots[i].isEmpty()) {
				nbt.put("slot" + i, slots[i].encode(registries));
			}
		}
		// Always write something: the client ignores an empty update, so an emptied block would keep showing its old item.
		nbt.putBoolean("synced", true);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		for (int i = 0; i < SLOTS; i++) {
			slots[i] = nbt.contains("slot" + i) ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("slot" + i)) : ItemStack.EMPTY;
		}
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
