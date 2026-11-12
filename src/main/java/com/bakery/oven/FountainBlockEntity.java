package com.bakery.oven;

import com.bakery.food.Flavor;
import com.bakery.food.Freshness;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

/**
 * The Chocolate Fountain. Melt chocolate bars into it, then dip fruit (or candyfloss, or peanuts...)
 * to get chocolate-dipped treats in that flavor.
 */
public class FountainBlockEntity extends BlockEntity implements MachineBlock.Machine {
	public static final int MAX_CHOCOLATE = 16;
	private static final DustParticleEffect DRIP = new DustParticleEffect(new Vector3f(0.36f, 0.2f, 0.12f), 0.9f);

	private int chocolate;

	public FountainBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.FOUNTAIN, pos, state);
	}

	public int getChocolate() {
		return chocolate;
	}

	@Override
	public void interact(PlayerEntity player, ItemStack stack) {
		if (world == null) return;
		if (stack.isOf(BakeryItems.CHOCOLATE_BAR) || stack.isOf(BakeryItems.COCOA_POWDER)) {
			if (chocolate >= MAX_CHOCOLATE) {
				player.sendMessage(Text.translatable("message.bakery.fountain.full"), true);
				return;
			}
			chocolate = Math.min(MAX_CHOCOLATE, chocolate + (stack.isOf(BakeryItems.CHOCOLATE_BAR) ? 4 : 2));
			stack.decrementUnlessCreative(1, player);
			world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.BLOCKS, 1f, 0.8f);
			sync();
			return;
		}
		Flavor flavor = Flavor.fromIngredient(stack);
		if (flavor == null || flavor == Flavor.CHOCOLATE) {
			player.sendMessage(Text.translatable(chocolate > 0 ? "message.bakery.fountain.hint" : "message.bakery.fountain.empty"), true);
			return;
		}
		if (chocolate <= 0) {
			player.sendMessage(Text.translatable("message.bakery.fountain.empty"), true);
			return;
		}
		chocolate--;
		stack.decrementUnlessCreative(1, player);
		ItemStack treat = new ItemStack(BakeryItems.CHOCOLATE_TREAT, 2);
		treat.set(BakeryComponents.FLAVOR, flavor.asString());
		treat.set(BakeryComponents.QUALITY, 4);
		Freshness.stampFresh(treat, world.getTime());
		player.getInventory().offerOrDrop(treat);
		world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 1f, 1.3f);
		sync();
	}

	@Override
	public void tick() {
		if (world == null) return;
		boolean active = chocolate > 0;
		BlockState state = getCachedState();
		if (!world.isClient && state.get(MachineBlock.ACTIVE) != active) {
			world.setBlockState(pos, state.with(MachineBlock.ACTIVE, active), Block.NOTIFY_ALL);
		}
		if (!world.isClient && active && world.getTime() % 8 == 0 && world instanceof ServerWorld server) {
			double a = world.random.nextDouble() * Math.PI * 2;
			server.spawnParticles(DRIP, pos.getX() + 0.5 + Math.cos(a) * 0.45, pos.getY() + 1.3, pos.getZ() + 0.5 + Math.sin(a) * 0.45, 1, 0, 0, 0, 0);
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
		nbt.putInt("chocolate", chocolate);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		chocolate = nbt.getInt("chocolate");
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
