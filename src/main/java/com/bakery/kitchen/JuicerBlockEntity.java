package com.bakery.kitchen;

import com.bakery.food.Flavor;
import com.bakery.food.Freshness;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

/**
 * The Juicer. Drop in a fruit, press the lever five times, and pour the juice into glass bottles.
 * Each fruit makes two glasses; the jug holds up to four.
 */
public class JuicerBlockEntity extends StationEntity {
	private static final int PRESSES = 5;

	private String flavor = "";
	private boolean loaded;
	private int presses;
	private int juice;
	/** The fruit sitting on the reamer, and when the lever was last pulled (for the animation). */
	private ItemStack fruit = ItemStack.EMPTY;
	private long pressedAt;

	public JuicerBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.JUICER, pos, state);
	}

	public Flavor flavor() {
		return Flavor.byName(flavor);
	}

	public int juice() {
		return juice;
	}

	public int presses() {
		return loaded ? presses : 0;
	}

	public ItemStack fruit() {
		return fruit;
	}

	public long pressedAt() {
		return pressedAt;
	}

	public boolean loaded() {
		return loaded;
	}

	@Override
	protected int level() {
		return Math.min(3, juice);
	}

	@Override
	public boolean interact(PlayerEntity player, ItemStack stack) {
		if (world == null) return false;
		if (stack.isOf(Items.GLASS_BOTTLE)) {
			if (juice <= 0) {
				player.sendMessage(Text.translatable("message.bakery.juicer.empty"), true);
				return true;
			}
			juice--;
			stack.decrementUnlessCreative(1, player);
			ItemStack glass = new ItemStack(BakeryItems.JUICE);
			glass.set(BakeryComponents.FLAVOR, flavor);
			Freshness.stampFresh(glass, world.getTime());
			player.getInventory().offerOrDrop(glass);
			world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1f, 1.2f);
			if (juice == 0 && !loaded) flavor = "";
			sync();
			return true;
		}
		if (Juicing.isFruit(stack)) {
			Flavor f = Flavor.fromIngredient(stack);
			if (loaded) {
				player.sendMessage(Text.translatable("message.bakery.juicer.press_first"), true);
				return true;
			}
			if (juice > 0 && !flavor.equals(f.asString())) {
				player.sendMessage(Text.translatable("message.bakery.juicer.other", Flavor.byName(flavor).displayName()), true);
				return true;
			}
			if (juice >= 4) {
				player.sendMessage(Text.translatable("message.bakery.juicer.full"), true);
				return true;
			}
			flavor = f.asString();
			loaded = true;
			presses = 0;
			fruit = stack.copyWithCount(1);
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_FILL, SoundCategory.BLOCKS, 1f, 1.3f);
			sync();
			return true;
		}
		if (!stack.isEmpty()) {
			return false;
		}
		if (!loaded) {
			player.sendMessage(Text.translatable(juice > 0 ? "message.bakery.juicer.bottle" : "message.bakery.juicer.hint"), true);
			return true;
		}
		presses++;
		pressedAt = world.getTime();
		world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.4f, 1.6f + presses * 0.08f);
		if (world instanceof ServerWorld server) {
			int c = flavor().color();
			server.spawnParticles(new DustParticleEffect(new Vector3f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f), 1f),
					pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0);
		}
		if (presses >= PRESSES) {
			loaded = false;
			fruit = ItemStack.EMPTY;
			juice = Math.min(4, juice + 2);
			world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 0.8f, 1.4f);
			player.sendMessage(Text.translatable("message.bakery.juicer.done", flavor().displayName()), true);
		}
		sync();
		return true;
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		nbt.putString("flavor", flavor);
		nbt.putBoolean("loaded", loaded);
		nbt.putInt("presses", presses);
		nbt.putInt("juice", juice);
		if (!fruit.isEmpty()) nbt.put("fruit", fruit.encode(registries));
		nbt.putLong("pressedAt", pressedAt);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		flavor = nbt.getString("flavor");
		loaded = nbt.getBoolean("loaded");
		presses = nbt.getInt("presses");
		juice = nbt.getInt("juice");
		fruit = nbt.contains("fruit") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("fruit")) : ItemStack.EMPTY;
		pressedAt = nbt.getLong("pressedAt");
	}
}
