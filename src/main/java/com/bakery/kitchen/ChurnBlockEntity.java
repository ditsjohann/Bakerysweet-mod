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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * The Ice Cream Churn. Add whipped cream, sugar and something icy (plus any flavor), then turn the handle
 * twelve times with a steady rhythm. Steadier churning makes creamier (higher star) ice cream.
 * Ice cream melts fast - wrap it in frost wrap!
 */
public class ChurnBlockEntity extends StationEntity {
	private static final int CHURNS = 12;

	private boolean cream;
	private boolean sugar;
	private boolean ice;
	private String flavor = "vanilla";
	private int churns;
	private int good;
	private long lastChurn;


	public ChurnBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.CHURN, pos, state);
	}

	public boolean ready() {
		return cream && sugar && ice;
	}

	public int churns() {
		return churns;
	}

	public Flavor flavor() {
		return Flavor.byName(flavor);
	}

	public long churnedAt() {
		return lastChurn;
	}

	public boolean hasCream() {
		return cream;
	}

	public boolean hasSugar() {
		return sugar;
	}

	public boolean hasIce() {
		return ice;
	}

	public int ingredients() {
		return (cream ? 1 : 0) + (sugar ? 1 : 0) + (ice ? 1 : 0);
	}

	@Override
	protected int level() {
		return ingredients();
	}

	@Override
	public boolean interact(PlayerEntity player, ItemStack stack) {
		if (world == null) return false;
		if (stack.isOf(BakeryItems.WHIPPED_CREAM) && !cream) {
			cream = true;
		} else if (stack.isOf(Items.SUGAR) && !sugar) {
			sugar = true;
		} else if ((stack.isOf(Items.ICE) || stack.isOf(Items.PACKED_ICE) || stack.isOf(Items.SNOWBALL) || stack.isOf(Items.BLUE_ICE)) && !ice) {
			ice = true;
		} else if (!stack.isEmpty() && Flavor.fromIngredient(stack) != null && churns == 0) {
			flavor = Flavor.fromIngredient(stack).asString();
		} else if (stack.isEmpty()) {
			if (!ready()) {
				player.sendMessage(Text.translatable("message.bakery.churn.needs"), true);
				return true;
			}
			churn(player);
			return true;
		} else {
			return false;
		}
		stack.decrementUnlessCreative(1, player);
		world.playSound(null, pos, SoundEvents.BLOCK_POWDER_SNOW_PLACE, SoundCategory.BLOCKS, 0.8f, 1.2f);
		sync();
		return true;
	}

	private void churn(PlayerEntity player) {
		long now = world.getTime();
		long gap = now - lastChurn;
		lastChurn = now;
		if (churns > 0 && gap >= 6 && gap <= 16) good++;
		churns++;
		world.playSound(null, pos, SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.5f, 1.4f + churns * 0.03f);
		if (world instanceof ServerWorld server) {
			server.spawnParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
		}
		if (churns < CHURNS) {
			player.sendMessage(Text.translatable("message.bakery.churn.progress", churns, CHURNS), true);
			sync();
			return;
		}
		int stars = Math.clamp(1 + Math.round(good / (float) (CHURNS - 1) * 4), 1, 5);
		ItemStack scoops = new ItemStack(BakeryItems.ICE_CREAM, 3);
		scoops.set(BakeryComponents.FLAVOR, flavor);
		scoops.set(BakeryComponents.QUALITY, stars);
		Freshness.stampFresh(scoops, world.getTime());
		player.getInventory().offerOrDrop(scoops);
		player.sendMessage(Text.translatable("message.bakery.churn.done").append(" ").append(com.bakery.food.BakeryFoods.stars(stars)).formatted(Formatting.AQUA), true);
		world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.5f, 1.8f);
		cream = sugar = ice = false;
		flavor = "vanilla";
		churns = good = 0;
		sync();
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		nbt.putBoolean("cream", cream);
		nbt.putBoolean("sugar", sugar);
		nbt.putBoolean("ice", ice);
		nbt.putString("flavor", flavor);
		nbt.putInt("churns", churns);
		nbt.putInt("good", good);
		nbt.putLong("lastChurn", lastChurn);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		cream = nbt.getBoolean("cream");
		sugar = nbt.getBoolean("sugar");
		ice = nbt.getBoolean("ice");
		flavor = nbt.contains("flavor") ? nbt.getString("flavor") : "vanilla";
		churns = nbt.getInt("churns");
		good = nbt.getInt("good");
		lastChurn = nbt.getLong("lastChurn");
	}
}
