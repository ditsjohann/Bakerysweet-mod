package com.bakery.kitchen;

import com.bakery.food.Freshness;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * The espresso machine. Pour roasted beans into the hopper (each handful makes four shots) and, for lattes, a bucket
 * of milk into the pitcher (eight lattes). Hold out an empty mug to pull a drink: a latte while there is milk, a
 * plain coffee otherwise. Click with an empty hand to switch the steam wand off (plain coffee) or on (lattes).
 * Flavour it afterwards at the syrup rack.
 */
public class CoffeeMachineBlockEntity extends StationEntity {
	public static final int SHOTS_PER_BEANS = 4;
	public static final int MAX_SHOTS = 16;
	public static final int MILK_PER_BUCKET = 8;

	private int shots;
	private int milk;
	/** Whether every bean in the hopper was home-grown and roasted (no catalog beans). */
	private boolean homemade = true;
	/** Whether the steam wand is on: lattes while there is milk. */
	private boolean steam = true;

	public CoffeeMachineBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.COFFEE_MACHINE, pos, state);
	}

	public int shots() {
		return shots;
	}

	public int milk() {
		return milk;
	}

	@Override
	protected int level() {
		return (shots > 0 ? 1 : 0) | (milk > 0 ? 2 : 0);
	}

	@Override
	public boolean interact(PlayerEntity player, ItemStack stack) {
		if (world == null) return false;
		if (stack.isOf(BakeryItems.ROASTED_COFFEE_BEANS)) {
			if (shots + SHOTS_PER_BEANS > MAX_SHOTS) {
				player.sendMessage(Text.translatable("message.bakery.coffee.hopper_full", shots), true);
				return true;
			}
			if (Boolean.TRUE.equals(stack.get(BakeryComponents.STORE_BOUGHT))) homemade = false;
			shots += SHOTS_PER_BEANS;
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_GRAVEL_PLACE, SoundCategory.BLOCKS, 0.5f, 1.6f);
			player.sendMessage(Text.translatable("message.bakery.coffee.beans", shots), true);
			sync();
			return true;
		}
		if (stack.isOf(Items.MILK_BUCKET)) {
			if (milk >= MILK_PER_BUCKET) {
				player.sendMessage(Text.translatable("message.bakery.coffee.milk_full"), true);
				return true;
			}
			milk = MILK_PER_BUCKET;
			if (!player.getAbilities().creativeMode) {
				stack.decrement(1);
				player.getInventory().offerOrDrop(new ItemStack(Items.BUCKET));
			}
			world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.7f, 1.3f);
			player.sendMessage(Text.translatable("message.bakery.coffee.milk", milk), true);
			sync();
			return true;
		}
		if (stack.isOf(BakeryItems.MUG)) {
			if (shots <= 0) {
				player.sendMessage(Text.translatable("message.bakery.coffee.no_beans"), true);
				return true;
			}
			boolean latte = milk > 0 && steam;
			shots--;
			if (latte) milk--;
			ItemStack drink = new ItemStack(latte ? BakeryItems.LATTE : BakeryItems.COFFEE);
			if (homemade) drink.set(BakeryComponents.HOMEMADE, true);
			Freshness.stampFresh(drink, world.getTime());
			stack.decrementUnlessCreative(1, player);
			player.getInventory().offerOrDrop(drink);
			if (shots == 0) homemade = true;
			world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.6f, 1.4f);
			if (latte) world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.25f, 1.8f);
			if (world instanceof ServerWorld server) {
				server.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, 4, 0.12, 0.05, 0.12, 0.01);
			}
			player.sendMessage(Text.translatable(latte ? "message.bakery.coffee.pulled_latte" : "message.bakery.coffee.pulled", shots), true);
			sync();
			return true;
		}
		if (!stack.isEmpty()) return false;
		if (shots <= 0 && milk <= 0) {
			player.sendMessage(Text.translatable("message.bakery.coffee.hint"), true);
			return true;
		}
		steam = !steam;
		world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.5f, steam ? 1.4f : 1.1f);
		player.sendMessage(Text.translatable(steam ? "message.bakery.coffee.mode_latte" : "message.bakery.coffee.mode_coffee", shots, milk), true);
		sync();
		return true;
	}

	@Override
	public void displayTick(Random random) {
		if (world != null && shots > 0 && random.nextInt(3) == 0) {
			world.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 0.95,
					pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		nbt.putInt("shots", shots);
		nbt.putInt("milk", milk);
		nbt.putBoolean("homemade", homemade);
		nbt.putBoolean("steam", steam);
		nbt.putBoolean("synced", true);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		shots = nbt.getInt("shots");
		milk = nbt.getInt("milk");
		homemade = !nbt.contains("homemade") || nbt.getBoolean("homemade");
		steam = !nbt.contains("steam") || nbt.getBoolean("steam");
	}
}
