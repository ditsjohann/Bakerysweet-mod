package com.bakery.review;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Every review a villager has given each player, saved with the world. */
public class BakeryReputation extends PersistentState {
	private static final Type<BakeryReputation> TYPE = new Type<>(BakeryReputation::new, BakeryReputation::fromNbt, null);

	public static final class Stats {
		public int reviews;
		public int totalStars;
		public int fiveStars;
		/** Every "shape:flavor" this baker has pulled out of an oven. */
		public final java.util.Set<String> recipes = new java.util.TreeSet<>();
		/** Sticker book: ids of every sticker earned. */
		public final java.util.Set<String> stickers = new java.util.TreeSet<>();
		/** Friendship hearts (0-10) with each villager. */
		public final Map<UUID, Integer> friends = new HashMap<>();
		public int coinsEarned;
		public int served;
		/** Catalog stock bought today (entry index -> times), and which day that was. */
		public long stockDay = -1;
		public final Map<Integer, Integer> bought = new HashMap<>();

		public int bestFriends() {
			return (int) friends.values().stream().filter(h -> h >= 5).count();
		}

		public float average() {
			return reviews == 0 ? 0 : totalStars / (float) reviews;
		}

		/** Apprentice, Home Baker, Village Baker, Master Baker, Star Patissier. */
		public String rankKey() {
			float avg = average();
			if (reviews >= 50 && avg >= 4.5f) return "star_patissier";
			if (reviews >= 25 && avg >= 4f) return "master_baker";
			if (reviews >= 10 && avg >= 3.5f) return "village_baker";
			if (reviews >= 3) return "home_baker";
			return "apprentice";
		}

		public Text rank() {
			return Text.translatable("rank.bakery." + rankKey());
		}
	}

	private final Map<UUID, Stats> stats = new HashMap<>();

	public static BakeryReputation get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "bakery_reputation");
	}

	public Stats of(UUID player) {
		return stats.computeIfAbsent(player, id -> new Stats());
	}

	public Stats record(UUID player, int stars) {
		Stats s = of(player);
		s.reviews++;
		s.totalStars += stars;
		if (stars == 5) {
			s.fiveStars++;
		}
		markDirty();
		return s;
	}

	public static final int TOTAL_RECIPES = com.bakery.baking.BakingRecipes.Shape.values().length * com.bakery.food.Flavor.values().length;

	/** Remembers a bake for the recipe collection; the first time, it is announced as a discovery. */
	public static void discover(ServerPlayerEntity player, String shape, com.bakery.food.Flavor flavor, Text name) {
		BakeryReputation reputation = get(player.getServer());
		Stats s = reputation.of(player.getUuid());
		if (s.recipes.add(shape + ":" + flavor.asString())) {
			reputation.markDirty();
			player.sendMessage(Text.translatable("message.bakery.recipe.new", name, s.recipes.size(), TOTAL_RECIPES).formatted(Formatting.GOLD), false);
			player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.8f, 1.6f);
		}
		com.bakery.sweet.Stickers.award(player, com.bakery.sweet.Sticker.FIRST_BAKE);
		if (s.recipes.size() >= 25) com.bakery.sweet.Stickers.award(player, com.bakery.sweet.Sticker.COLLECTOR);
	}

	/** Records a review and, when it lifts the player to a new baker rank, throws them a little party. */
	public static void recordAndCelebrate(ServerPlayerEntity player, int stars) {
		BakeryReputation reputation = get(player.getServer());
		String before = reputation.of(player.getUuid()).rankKey();
		Stats after = reputation.record(player.getUuid(), stars);
		if (after.rankKey().equals("star_patissier")) com.bakery.sweet.Stickers.award(player, com.bakery.sweet.Sticker.STAR_PATISSIER);
		if (!after.rankKey().equals(before)) {
			com.bakery.platform.Platform.get().sendVanillaPacket(player, new TitleFadeS2CPacket(10, 60, 20));
			com.bakery.platform.Platform.get().sendVanillaPacket(player, new TitleS2CPacket(after.rank().copy().formatted(Formatting.LIGHT_PURPLE)));
			com.bakery.platform.Platform.get().sendVanillaPacket(player, new SubtitleS2CPacket(Text.translatable("rank.bakery.up")));
			player.getServerWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1.2f);
			player.getServerWorld().spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 2, player.getZ(), 12, 0.6, 0.4, 0.6, 0.02);
		}
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		NbtCompound players = new NbtCompound();
		stats.forEach((uuid, s) -> {
			NbtCompound entry = new NbtCompound();
			entry.putInt("reviews", s.reviews);
			entry.putInt("stars", s.totalStars);
			entry.putInt("five", s.fiveStars);
			net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
			s.recipes.forEach(r -> list.add(net.minecraft.nbt.NbtString.of(r)));
			entry.put("recipes", list);
			net.minecraft.nbt.NbtList stickers = new net.minecraft.nbt.NbtList();
			s.stickers.forEach(r -> stickers.add(net.minecraft.nbt.NbtString.of(r)));
			entry.put("stickers", stickers);
			NbtCompound friends = new NbtCompound();
			s.friends.forEach((villager, hearts) -> friends.putInt(villager.toString(), hearts));
			entry.put("friends", friends);
			entry.putInt("coins", s.coinsEarned);
			entry.putInt("served", s.served);
			entry.putLong("stockDay", s.stockDay);
			NbtCompound bought = new NbtCompound();
			s.bought.forEach((i, n) -> bought.putInt(String.valueOf(i), n));
			entry.put("bought", bought);
			players.put(uuid.toString(), entry);
		});
		nbt.put("players", players);
		return nbt;
	}

	private static BakeryReputation fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		BakeryReputation reputation = new BakeryReputation();
		NbtCompound players = nbt.getCompound("players");
		for (String key : players.getKeys()) {
			NbtCompound entry = players.getCompound(key);
			Stats s = new Stats();
			s.reviews = entry.getInt("reviews");
			s.totalStars = entry.getInt("stars");
			s.fiveStars = entry.getInt("five");
			net.minecraft.nbt.NbtList list = entry.getList("recipes", net.minecraft.nbt.NbtElement.STRING_TYPE);
			for (int i = 0; i < list.size(); i++) s.recipes.add(list.getString(i));
			net.minecraft.nbt.NbtList stickers = entry.getList("stickers", net.minecraft.nbt.NbtElement.STRING_TYPE);
			for (int i = 0; i < stickers.size(); i++) s.stickers.add(stickers.getString(i));
			NbtCompound friends = entry.getCompound("friends");
			for (String villager : friends.getKeys()) s.friends.put(UUID.fromString(villager), friends.getInt(villager));
			s.coinsEarned = entry.getInt("coins");
			s.served = entry.getInt("served");
			s.stockDay = entry.contains("stockDay") ? entry.getLong("stockDay") : -1;
			NbtCompound bought = entry.getCompound("bought");
			for (String i : bought.getKeys()) s.bought.put(Integer.parseInt(i), bought.getInt(i));
			reputation.stats.put(UUID.fromString(key), s);
		}
		return reputation;
	}
}
