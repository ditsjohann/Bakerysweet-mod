package com.bakery.shop;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

import java.util.UUID;

/** One customer's order at a bakery counter, and how much patience they have left. */
public final class Order {
	/** The customer entity. */
	public final UUID customer;
	/** Which {@link com.bakery.cafe.Regular} it is. */
	public final int regular;
	public final ItemStack wanted;
	public final boolean flavorRequested;
	public final int maxPatience;
	/** Sits down at a table to eat, rather than taking it to go. */
	public final boolean dineIn;
	public int patience;
	public boolean waiting;

	public Order(UUID customer, int regular, ItemStack wanted, boolean flavorRequested, int patience, boolean dineIn) {
		this.customer = customer;
		this.regular = regular;
		this.wanted = wanted;
		this.flavorRequested = flavorRequested;
		this.maxPatience = patience;
		this.patience = patience;
		this.dineIn = dineIn;
	}

	public float patienceFraction() {
		return maxPatience == 0 ? 0 : patience / (float) maxPatience;
	}

	public NbtCompound write(RegistryWrapper.WrapperLookup registries) {
		NbtCompound nbt = new NbtCompound();
		nbt.putUuid("customer", customer);
		nbt.putInt("regular", regular);
		nbt.put("wanted", wanted.encode(registries));
		nbt.putBoolean("flavor", flavorRequested);
		nbt.putInt("max", maxPatience);
		nbt.putInt("patience", patience);
		nbt.putBoolean("waiting", waiting);
		nbt.putBoolean("dineIn", dineIn);
		return nbt;
	}

	public static Order read(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		Order order = new Order(nbt.getUuid("customer"), nbt.getInt("regular"), ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("wanted")),
				nbt.getBoolean("flavor"), nbt.getInt("max"), nbt.getBoolean("dineIn"));
		order.patience = nbt.getInt("patience");
		order.waiting = nbt.getBoolean("waiting");
		return order;
	}
}
