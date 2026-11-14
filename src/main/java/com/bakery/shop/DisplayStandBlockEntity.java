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

/** The bakes on a display stand, shelf or cloche; as many slots as its block has spots. */
public class DisplayStandBlockEntity extends BlockEntity implements SelfServe {
	public static final int MAX_SLOTS = 6;
	private final ItemStack[] slots = new ItemStack[MAX_SLOTS];

	public DisplayStandBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.DISPLAY_STAND, pos, state);
		java.util.Arrays.fill(slots, ItemStack.EMPTY);
	}

	public int size() {
		return getCachedState().getBlock() instanceof DisplayStandBlock stand ? stand.spots().length : MAX_SLOTS;
	}

	public ItemStack getSlot(int i) {
		return slots[i];
	}

	public boolean add(ItemStack stack) {
		for (int i = 0; i < size(); i++) {
			if (slots[i].isEmpty()) {
				slots[i] = stack.copyWithCount(1);
				sync();
				return true;
			}
		}
		return false;
	}

	public ItemStack takeLast() {
		for (int i = size() - 1; i >= 0; i--) {
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
		for (int i = 0; i < size(); i++) {
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
		if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (!slots[i].isEmpty()) nbt.put("slot" + i, slots[i].encode(registries));
		}
		nbt.putBoolean("synced", true);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		for (int i = 0; i < MAX_SLOTS; i++) {
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
