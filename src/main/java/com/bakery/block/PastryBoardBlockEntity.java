package com.bakery.block;

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

/** The dough waiting on the pastry board to be rolled out. */
public class PastryBoardBlockEntity extends BlockEntity {
	private ItemStack dough = ItemStack.EMPTY;

	public PastryBoardBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.PASTRY_BOARD, pos, state);
	}

	public ItemStack getDough() {
		return dough;
	}

	public void setDough(ItemStack dough) {
		this.dough = dough;
		markDirty();
		if (world != null) {
			BlockState state = getCachedState();
			boolean has = !dough.isEmpty();
			if (state.get(PastryBoardBlock.HAS_DOUGH) != has) {
				world.setBlockState(pos, state.with(PastryBoardBlock.HAS_DOUGH, has), Block.NOTIFY_ALL);
			} else {
				world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
			}
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		if (!dough.isEmpty()) {
			nbt.put("dough", dough.encode(registries));
		}
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		dough = nbt.contains("dough") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("dough")) : ItemStack.EMPTY;
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
