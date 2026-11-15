package com.bakery.kitchen;

import com.bakery.food.Flavor;
import com.bakery.item.DrinkItem;
import com.bakery.item.SyrupItem;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Holds the four syrup bottles (with their pumps left) of a {@link SyrupRackBlock}. */
public class SyrupRackBlockEntity extends BlockEntity {
	private final ItemStack[] bottles = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

	public SyrupRackBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.SYRUP_RACK, pos, state);
	}

	public ItemStack bottle(int slot) {
		return bottles[slot];
	}

	/** Coffee and lattes take a pump of syrup (once). */
	public static boolean isCoffee(ItemStack stack) {
		return stack.isOf(BakeryItems.COFFEE) || stack.isOf(BakeryItems.LATTE);
	}

	boolean wouldUse(int slot, ItemStack stack) {
		boolean has = getCachedState().get(SyrupRackBlock.SLOTS[slot]) > 0;
		return stack.getItem() instanceof SyrupItem ? !has : isCoffee(stack) && has;
	}

	boolean use(PlayerEntity player, int slot, ItemStack stack) {
		if (world == null) return false;
		ItemStack bottle = bottles[slot];
		if (stack.getItem() instanceof SyrupItem) {
			if (!bottle.isEmpty()) {
				player.sendMessage(Text.translatable("message.bakery.syrup.spot_taken"), true);
				return true;
			}
			bottles[slot] = stack.copyWithCount(1);
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_DECORATED_POT_INSERT, SoundCategory.BLOCKS, 0.6f, 1.4f);
			update();
			return true;
		}
		if (isCoffee(stack)) {
			if (bottle.isEmpty()) {
				player.sendMessage(Text.translatable("message.bakery.syrup.empty_spot"), true);
				return true;
			}
			if (stack.contains(BakeryComponents.FLAVOR)) {
				player.sendMessage(Text.translatable("message.bakery.syrup.already"), true);
				return true;
			}
			Flavor flavor = ((SyrupItem) bottle.getItem()).flavor();
			boolean mainHand = player.getMainHandStack() == stack;
			ItemStack drink = stack.split(1);
			drink.set(BakeryComponents.FLAVOR, flavor.asString());
			if (stack.isEmpty() && mainHand) player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, drink);
			else player.getInventory().offerOrDrop(drink);
			bottle.setDamage(bottle.getDamage() + 1);
			world.playSound(null, pos, SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, SoundCategory.BLOCKS, 0.8f, 1.5f);
			if (SyrupItem.pumpsLeft(bottle) <= 0) {
				bottles[slot] = ItemStack.EMPTY;
				player.getInventory().offerOrDrop(new ItemStack(Items.GLASS_BOTTLE));
				player.sendMessage(Text.translatable("message.bakery.syrup.used_up", flavor.displayName()), true);
			} else {
				player.sendMessage(Text.translatable("message.bakery.syrup.pumped", flavor.displayName(), SyrupItem.pumpsLeft(bottle)), true);
			}
			update();
			return true;
		}
		if (stack.isEmpty() && !bottle.isEmpty()) {
			player.getInventory().offerOrDrop(bottle);
			bottles[slot] = ItemStack.EMPTY;
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.2f);
			update();
			return true;
		}
		if (stack.getItem() instanceof DrinkItem && !bottle.isEmpty()) {
			player.sendMessage(Text.translatable("message.bakery.syrup.coffee_only"), true);
			return true;
		}
		return false;
	}

	void dropAll() {
		if (world == null) return;
		for (int i = 0; i < 4; i++) {
			KitchenBlock.spill(world, pos, bottles[i]);
			bottles[i] = ItemStack.EMPTY;
		}
	}

	/** Shows the bottles in the block state and syncs the pumps left. */
	private void update() {
		markDirty();
		if (world == null) return;
		BlockState state = getCachedState();
		for (int i = 0; i < 4; i++) {
			int index = 0;
			if (bottles[i].getItem() instanceof SyrupItem syrup) {
				for (int k = 0; k < Flavor.SYRUPS.length; k++) if (Flavor.SYRUPS[k] == syrup.flavor()) index = k + 1;
			}
			state = state.with(SyrupRackBlock.SLOTS[i], index);
		}
		world.setBlockState(pos, state, Block.NOTIFY_ALL);
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		NbtList list = new NbtList();
		for (ItemStack bottle : bottles) {
			list.add(bottle.isEmpty() ? new NbtCompound() : bottle.encode(registries));
		}
		nbt.put("bottles", list);
		nbt.putBoolean("synced", true);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		NbtList list = nbt.getList("bottles", NbtCompound.COMPOUND_TYPE);
		for (int i = 0; i < 4; i++) {
			bottles[i] = i < list.size() && !list.getCompound(i).isEmpty()
					? ItemStack.fromNbtOrEmpty(registries, list.getCompound(i)) : ItemStack.EMPTY;
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
