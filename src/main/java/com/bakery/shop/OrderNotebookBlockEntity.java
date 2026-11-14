package com.bakery.shop;

import com.bakery.registry.BakeryBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Copies the open orders of every counter in its world once a second, for the page and the notebook screen. */
public class OrderNotebookBlockEntity extends BlockEntity {
	/** Orders shown at most (the screen scrolls through them). */
	public static final int MAX_ENTRIES = 32;

	/** One open order as the notebook writes it down. */
	public record Entry(int regular, ItemStack wanted, float patience, boolean seated, boolean waiting, BlockPos counter) {
	}

	private final List<Entry> entries = new ArrayList<>();

	public OrderNotebookBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.ORDER_NOTEBOOK, pos, state);
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public static void tick(World world, BlockPos pos, BlockState state, OrderNotebookBlockEntity notebook) {
		if (world.getTime() % 20 == (pos.asLong() & 15)) notebook.refresh();
	}

	/** Reads every counter again; seated customers first (the food goes to them), then by how impatient they are. */
	public void refresh() {
		if (!(world instanceof ServerWorld server)) return;
		List<Entry> fresh = new ArrayList<>();
		for (BlockPos at : CounterBlockEntity.active(server)) {
			if (!(server.getBlockEntity(at) instanceof CounterBlockEntity counter)) continue;
			for (Order order : counter.getOrders()) {
				fresh.add(new Entry(order.regular, order.wanted.copy(), order.patienceFraction(), order.seated, order.waiting, at));
			}
		}
		fresh.sort((a, b) -> a.seated() != b.seated() ? (a.seated() ? -1 : 1) : Float.compare(a.patience(), b.patience()));
		if (fresh.size() > MAX_ENTRIES) fresh = new ArrayList<>(fresh.subList(0, MAX_ENTRIES));
		if (!same(fresh)) {
			entries.clear();
			entries.addAll(fresh);
			markDirty();
			server.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
		}
	}

	/** Patience is compared in fifths (the hearts), so the page only updates when something visible changes. */
	private boolean same(List<Entry> other) {
		if (other.size() != entries.size()) return false;
		for (int i = 0; i < other.size(); i++) {
			Entry a = other.get(i), b = entries.get(i);
			if (a.regular() != b.regular() || a.seated() != b.seated() || a.waiting() != b.waiting() || !ItemStack.areItemsAndComponentsEqual(a.wanted(), b.wanted())
					|| Math.ceil(a.patience() * 5) != Math.ceil(b.patience() * 5)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		NbtList list = new NbtList();
		for (Entry e : entries) {
			NbtCompound c = new NbtCompound();
			c.putInt("regular", e.regular());
			c.put("wanted", e.wanted().encode(registries));
			c.putFloat("patience", e.patience());
			c.putBoolean("seated", e.seated());
			c.putBoolean("waiting", e.waiting());
			c.put("counter", NbtHelper.fromBlockPos(e.counter()));
			list.add(c);
		}
		nbt.put("entries", list);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		entries.clear();
		NbtList list = nbt.getList("entries", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound c = list.getCompound(i);
			entries.add(new Entry(c.getInt("regular"), ItemStack.fromNbtOrEmpty(registries, c.getCompound("wanted")), c.getFloat("patience"),
					c.getBoolean("seated"), c.getBoolean("waiting"), NbtHelper.toBlockPos(c, "counter").orElse(BlockPos.ORIGIN)));
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
