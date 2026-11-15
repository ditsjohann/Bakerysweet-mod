package com.bakery.client;

import com.bakery.deco.CafeJukeboxBlockEntity;
import com.bakery.registry.BakerySounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Loops each Café Jukebox's tune at the jukebox, and stops it when the tune changes or the jukebox goes away. */
public final class JukeboxMusic {
	private static final Map<BlockPos, Tune> PLAYING = new HashMap<>();

	private JukeboxMusic() {
	}

	public static void init() {
		CafeJukeboxBlockEntity.clientTick = JukeboxMusic::tick;
	}

	private static void tick(CafeJukeboxBlockEntity jukebox) {
		MinecraftClient client = MinecraftClient.getInstance();
		BlockPos pos = jukebox.getPos().toImmutable();
		Tune tune = PLAYING.get(pos);
		int track = jukebox.track();
		if (track <= 0) {
			if (tune != null) {
				tune.stop();
				PLAYING.remove(pos);
			}
			return;
		}
		boolean lost = tune != null && !tune.isDone() && jukebox.getWorld() != null && jukebox.getWorld().getTime() % 40 == 0
				&& !client.getSoundManager().isPlaying(tune);
		if (tune == null || tune.isDone() || tune.track != track || lost) {
			if (tune != null) tune.stop();
			tune = new Tune(pos, track);
			PLAYING.put(pos, tune);
			client.getMusicTracker().stop();
			client.getSoundManager().play(tune);
		}
	}

	/** One looping tune, playing from the jukebox block for as long as that jukebox plays it. */
	static final class Tune extends MovingSoundInstance {
		private final BlockPos pos;
		final int track;

		Tune(BlockPos pos, int track) {
			super(BakerySounds.MUSIC[track - 1], SoundCategory.RECORDS, SoundInstance.createRandom());
			this.pos = pos;
			this.track = track;
			this.repeat = true;
			this.repeatDelay = 0;
			this.volume = 1f;
			this.x = pos.getX() + 0.5;
			this.y = pos.getY() + 0.5;
			this.z = pos.getZ() + 0.5;
		}

		@Override
		public void tick() {
			MinecraftClient client = MinecraftClient.getInstance();
			if (client.world == null || !(client.world.getBlockEntity(pos) instanceof CafeJukeboxBlockEntity jukebox) || jukebox.track() != track) {
				stop();
			}
		}

		void stop() {
			setDone();
		}

		@Override
		public boolean shouldAlwaysPlay() {
			return true;
		}
	}
}
