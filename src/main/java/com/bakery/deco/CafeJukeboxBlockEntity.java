package com.bakery.deco;

import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakerySounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;

/** Which café tune (if any) a {@link CafeJukeboxBlock} plays. The client loops it (see the client's JukeboxMusic). */
public class CafeJukeboxBlockEntity extends BlockEntity {
	/** Set by the client: starts or keeps the music going for this jukebox. */
	public static Consumer<CafeJukeboxBlockEntity> clientTick = be -> {
	};

	/** 0 = off, otherwise 1 + index into {@link BakerySounds#TRACKS}. */
	private int track;

	public CafeJukeboxBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.CAFE_JUKEBOX, pos, state);
	}

	public int track() {
		return track;
	}

	void next(PlayerEntity player) {
		if (world == null) return;
		track = (track + 1) % (BakerySounds.TRACKS.length + 1);
		world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.4f, track == 0 ? 0.7f : 1.4f);
		if (track == 0) {
			player.sendMessage(Text.translatable("message.bakery.jukebox.off"), true);
		} else {
			Text song = Text.translatable("music.bakery." + BakerySounds.TRACKS[track - 1]).formatted(Formatting.LIGHT_PURPLE);
			player.sendMessage(Text.translatable("message.bakery.jukebox.playing", song), true);
			if (world instanceof ServerWorld server) {
				server.spawnParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.6);
			}
		}
		markDirty();
		BlockState state = getCachedState().with(CafeJukeboxBlock.PLAYING, track > 0);
		world.setBlockState(pos, state, Block.NOTIFY_ALL);
		world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
	}

	@Override
	protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.writeNbt(nbt, registries);
		nbt.putInt("track", track);
		nbt.putBoolean("synced", true);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		track = nbt.getInt("track");
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
