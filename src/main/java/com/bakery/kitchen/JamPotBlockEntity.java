package com.bakery.kitchen;

import com.bakery.food.Flavor;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * The Jam Pot. Put it over a campfire (or fire, magma, lava), add three of the same fruit and some sugar,
 * and keep stirring with a whisk while it bubbles. Forget to stir and it scorches!
 * Spoon the jam into glass jars when it is done.
 */
public class JamPotBlockEntity extends StationEntity {
	public static final int COOK_TIME = 500;
	public static final int SCORCH_TIME = 220;
	public static final int EMPTY = 0, FILLING = 1, COOKING = 2, DONE = 3, BURNT = 4;

	private String flavor = "";
	private ItemStack fruitType = ItemStack.EMPTY;
	private int fruit;
	private boolean sugar;
	private int state = EMPTY;
	private int cooked;
	private int sinceStir;
	private int servings;
	private long stirredAt;

	public JamPotBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.JAM_POT, pos, state);
	}

	public int stage() {
		return state;
	}

	public Flavor flavor() {
		return Flavor.byName(flavor);
	}

	public float cookProgress() {
		return cooked / (float) COOK_TIME;
	}

	public float scorch() {
		return sinceStir / (float) SCORCH_TIME;
	}

	public long stirredAt() {
		return stirredAt;
	}

	public ItemStack fruitType() {
		return fruitType;
	}

	public int servings() {
		return servings;
	}

	public int fruit() {
		return fruit;
	}

	public boolean sugar() {
		return sugar;
	}

	@Override
	protected int level() {
		return switch (state) {
			case EMPTY -> 0;
			case FILLING -> 1;
			case COOKING -> 2;
			default -> 3;
		};
	}

	public boolean heated() {
		if (world == null) return false;
		BlockState below = world.getBlockState(pos.down());
		return CampfireBlock.isLitCampfire(below) || below.isIn(BlockTags.FIRE) || below.isOf(Blocks.MAGMA_BLOCK) || below.isOf(Blocks.LAVA);
	}

	@Override
	public boolean interact(PlayerEntity player, ItemStack stack) {
		if (world == null) return false;
		if (state == DONE) {
			if (!stack.isOf(BakeryItems.GLASS_JAR)) {
				player.sendMessage(Text.translatable("message.bakery.jam.jar"), true);
				return true;
			}
			stack.decrementUnlessCreative(1, player);
			player.getInventory().offerOrDrop(jam());
			world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1f, 0.9f);
			if (--servings <= 0) reset();
			sync();
			return true;
		}
		if (state == BURNT) {
			if (stack.isEmpty()) {
				reset();
				world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.6f, 1.2f);
				player.sendMessage(Text.translatable("message.bakery.jam.cleaned"), true);
				sync();
			} else {
				player.sendMessage(Text.translatable("message.bakery.jam.burnt"), true);
			}
			return true;
		}
		if (state == COOKING) {
			if (stack.isOf(BakeryItems.WHISK) || stack.isEmpty()) {
				sinceStir = 0;
				stirredAt = world.getTime();
				world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 0.8f, 1.1f + world.random.nextFloat() * 0.3f);
				player.sendMessage(Text.translatable("message.bakery.jam.stirring", Math.round(cookProgress() * 100)).formatted(Formatting.LIGHT_PURPLE), true);
				sync();
				return true;
			}
			return false;
		}
		// EMPTY or FILLING
		if (Juicing.isFruit(stack)) {
			Flavor f = Flavor.fromIngredient(stack);
			if (fruit > 0 && !flavor.equals(f.asString())) {
				player.sendMessage(Text.translatable("message.bakery.jam.same_fruit"), true);
				return true;
			}
			if (fruit >= 3) {
				player.sendMessage(Text.translatable("message.bakery.jam.enough_fruit"), true);
				return true;
			}
			flavor = f.asString();
			fruitType = stack.copyWithCount(1);
			fruit++;
			stack.decrementUnlessCreative(1, player);
			state = FILLING;
		} else if (stack.isOf(Items.SUGAR) && !sugar) {
			sugar = true;
			stack.decrementUnlessCreative(1, player);
			state = FILLING;
		} else if (stack.isEmpty() || stack.isOf(BakeryItems.WHISK)) {
			player.sendMessage(Text.translatable("message.bakery.jam.needs", fruit, sugar ? 1 : 0), true);
			return true;
		} else {
			return false;
		}
		world.playSound(null, pos, SoundEvents.BLOCK_DECORATED_POT_INSERT, SoundCategory.BLOCKS, 0.8f, 1.2f);
		if (fruit >= 3 && sugar) {
			if (heated()) {
				state = COOKING;
				cooked = 0;
				sinceStir = 0;
				player.sendMessage(Text.translatable("message.bakery.jam.started").formatted(Formatting.GOLD), true);
			} else {
				player.sendMessage(Text.translatable("message.bakery.jam.no_heat"), true);
			}
		}
		sync();
		return true;
	}

	private ItemStack jam() {
		Flavor f = flavor();
		if (f == Flavor.STRAWBERRY) return new ItemStack(BakeryItems.STRAWBERRY_JAM);
		if (f == Flavor.BLUEBERRY) return new ItemStack(BakeryItems.BLUEBERRY_JAM);
		if (f == Flavor.RASPBERRY) return new ItemStack(BakeryItems.RASPBERRY_JAM);
		ItemStack jar = new ItemStack(BakeryItems.FRUIT_JAM);
		jar.set(BakeryComponents.FLAVOR, flavor);
		if (!Boolean.TRUE.equals(fruitType.get(BakeryComponents.STORE_BOUGHT))) jar.set(BakeryComponents.HOMEMADE, true);
		return jar;
	}

	private void reset() {
		flavor = "";
		fruitType = ItemStack.EMPTY;
		fruit = 0;
		sugar = false;
		state = EMPTY;
		cooked = sinceStir = servings = 0;
	}

	@Override
	public void tick() {
		if (world == null) return;
		if (state == FILLING && fruit >= 3 && sugar && heated()) {
			state = COOKING;
			sync();
		}
		if (state != COOKING || !heated()) return;
		cooked++;
		sinceStir++;
		if (sinceStir == SCORCH_TIME - 60) {
			world.playSound(null, pos, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 0.8f, 1.4f);
		}
		if (sinceStir >= SCORCH_TIME) {
			state = BURNT;
			world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1f, 0.7f);
			sync();
		} else if (cooked >= COOK_TIME) {
			state = DONE;
			servings = 3;
			world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1f, 1.6f);
			sync();
		} else if (world.getTime() % 20 == 0) {
			sync();
		}
	}

	@Override
	public void displayTick(Random random) {
		if (world == null) return;
		if (state == COOKING) {
			world.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6, pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
			if (sinceStir > SCORCH_TIME * 0.6) {
				world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, 0.03, 0);
			}
		} else if (state == BURNT) {
			world.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, 0.03, 0);
		}
	}

	@Override
	public void dropContents() {
		if (world != null && state == FILLING) {
			if (fruit > 0 && !fruitType.isEmpty()) KitchenBlock.spill(world, pos, fruitType.copyWithCount(fruit));
			if (sugar) KitchenBlock.spill(world, pos, new ItemStack(Items.SUGAR));
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		nbt.putString("flavor", flavor);
		if (!fruitType.isEmpty()) nbt.put("fruitType", fruitType.encode(registries));
		nbt.putInt("fruit", fruit);
		nbt.putBoolean("sugar", sugar);
		nbt.putInt("state", state);
		nbt.putInt("cooked", cooked);
		nbt.putInt("sinceStir", sinceStir);
		nbt.putInt("servings", servings);
		nbt.putLong("stirredAt", stirredAt);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		flavor = nbt.getString("flavor");
		fruitType = nbt.contains("fruitType") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("fruitType")) : ItemStack.EMPTY;
		fruit = nbt.getInt("fruit");
		sugar = nbt.getBoolean("sugar");
		state = nbt.getInt("state");
		cooked = nbt.getInt("cooked");
		sinceStir = nbt.getInt("sinceStir");
		servings = nbt.getInt("servings");
		stirredAt = nbt.getLong("stirredAt");
	}
}
