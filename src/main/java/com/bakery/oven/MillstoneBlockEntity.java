package com.bakery.oven;

import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Map;

/**
 * The Grand Millstone. Load it with something grindable, then right-click to push the crank around.
 * Keep a steady rhythm (not too fast, not too slow) and the stone grinds twice as fast.
 */
public class MillstoneBlockEntity extends BlockEntity implements MachineBlock.Machine, Multiblock.MachineContents {
	private record Grind(Item output, int count) {
	}

	private static Map<Item, Grind> recipes;

	private ItemStack input = ItemStack.EMPTY;
	private int cranks;
	private int combo;
	private long lastCrank;
	private float angle;
	private float prevAngle;
	private long crankedAt;

	public MillstoneBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.MILLSTONE, pos, state);
	}

	private static Map<Item, Grind> recipes() {
		if (recipes == null) {
			recipes = Map.of(
					Items.WHEAT, new Grind(BakeryItems.FLOUR, 1),
					Items.SUGAR_CANE, new Grind(Items.SUGAR, 2),
					Items.COCOA_BEANS, new Grind(BakeryItems.COCOA_POWDER, 1),
					BakeryItems.COCONUT, new Grind(BakeryItems.COCONUT_FLAKES, 4),
					BakeryItems.PEANUT, new Grind(BakeryItems.PEANUT_BUTTER, 1),
					BakeryItems.CORN, new Grind(BakeryItems.CORNMEAL, 2),
					BakeryItems.SUGAR_BEET, new Grind(Items.SUGAR, 3),
					BakeryItems.TEA_LEAVES, new Grind(BakeryItems.MATCHA_POWDER, 1),
					BakeryItems.ROSE_PETALS, new Grind(Items.PINK_DYE, 2),
					BakeryItems.HIBISCUS, new Grind(Items.RED_DYE, 2));
		}
		return recipes;
	}

	public static boolean grindable(ItemStack stack) {
		return recipes().containsKey(stack.getItem());
	}

	public ItemStack getInput() {
		return input;
	}

	public float getAngle() {
		return angle;
	}

	/** The runner stone's turn for rendering: eases round after each push of the crank. */
	public float renderAngle(float tickDelta) {
		if (world == null) return angle;
		float t = net.minecraft.util.math.MathHelper.clamp((world.getTime() - crankedAt + tickDelta) / 6f, 0, 1);
		float eased = 1 - (1 - t) * (1 - t);
		return prevAngle + net.minecraft.util.math.MathHelper.wrapDegrees(angle - prevAngle) * eased;
	}

	public int getCombo() {
		return combo;
	}

	@Override
	public void interact(PlayerEntity player, ItemStack stack) {
		if (world == null) return;
		if (grindable(stack)) {
			if (!input.isEmpty() && !ItemStack.areItemsEqual(input, stack)) {
				player.sendMessage(Text.translatable("message.bakery.mill.busy"), true);
				return;
			}
			int room = 16 - input.getCount();
			int moved = Math.min(room, stack.getCount());
			if (moved <= 0) {
				player.sendMessage(Text.translatable("message.bakery.mill.full"), true);
				return;
			}
			if (input.isEmpty()) {
				input = stack.copyWithCount(moved);
			} else {
				input.increment(moved);
			}
			stack.decrementUnlessCreative(moved, player);
			world.playSound(null, pos, SoundEvents.BLOCK_COMPOSTER_FILL, SoundCategory.BLOCKS, 1f, 0.9f);
			sync();
			return;
		}
		if (!stack.isEmpty()) {
			player.sendMessage(Text.translatable("message.bakery.mill.hint"), true);
			return;
		}
		if (player.isSneaking()) {
			if (!input.isEmpty()) {
				player.getInventory().offerOrDrop(input);
				input = ItemStack.EMPTY;
				sync();
			}
			return;
		}
		if (input.isEmpty()) {
			player.sendMessage(Text.translatable("message.bakery.mill.empty"), true);
			return;
		}
		crank(player);
	}

	private void crank(PlayerEntity player) {
		long now = world.getTime();
		long gap = now - lastCrank;
		lastCrank = now;
		combo = gap >= 4 && gap <= 14 ? Math.min(combo + 1, 12) : 0;
		prevAngle = angle;
		crankedAt = now;
		angle = (angle + 45) % 360;
		cranks++;
		world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.5f, 0.8f + combo * 0.04f);
		int needed = combo >= 3 ? 2 : 4;
		if (cranks >= needed) {
			cranks = 0;
			Grind grind = recipes().get(input.getItem());
			if (world instanceof ServerWorld server) {
				server.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, input.copyWithCount(1)),
						pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 8, 0.4, 0.1, 0.4, 0.05);
			}
			input.decrement(1);
			Direction facing = getCachedState().get(MachineBlock.FACING);
			BlockPos spout = pos.offset(facing);
			ItemScatterer.spawn(world, spout.getX() + 0.5, spout.getY() + 0.3, spout.getZ() + 0.5, new ItemStack(grind.output(), grind.count()));
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.4f, 1.6f);
		}
		if (combo >= 3) {
			player.sendMessage(Text.translatable("message.bakery.mill.rhythm", combo).formatted(Formatting.LIGHT_PURPLE), true);
		}
		sync();
	}

	@Override
	public void dropContents() {
		if (world != null && !input.isEmpty()) {
			ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, input);
			input = ItemStack.EMPTY;
		}
	}

	private void sync() {
		markDirty();
		if (world != null) {
			world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
		}
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		if (!input.isEmpty()) nbt.put("input", input.encode(registries));
		nbt.putInt("cranks", cranks);
		nbt.putInt("combo", combo);
		nbt.putFloat("angle", angle);
		nbt.putFloat("prevAngle", prevAngle);
		nbt.putLong("crankedAt", crankedAt);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		input = nbt.contains("input") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("input")) : ItemStack.EMPTY;
		cranks = nbt.getInt("cranks");
		combo = nbt.getInt("combo");
		angle = nbt.getFloat("angle");
		prevAngle = nbt.getFloat("prevAngle");
		crankedAt = nbt.getLong("crankedAt");
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
