package com.bakery.kitchen;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/** Base for kitchen block entities: syncs to the client and keeps the model's LEVEL up to date. */
public abstract class StationEntity extends BlockEntity implements KitchenBlock.Station {
	protected StationEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** 0-3, shown by the model. */
	protected abstract int level();

	protected void sync() {
		markDirty();
		if (world == null) return;
		BlockState state = getCachedState();
		int level = Math.clamp(level(), 0, 3);
		if (state.contains(KitchenBlock.LEVEL) && state.get(KitchenBlock.LEVEL) != level) {
			world.setBlockState(pos, state.with(KitchenBlock.LEVEL, level), Block.NOTIFY_ALL);
		} else {
			world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
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
