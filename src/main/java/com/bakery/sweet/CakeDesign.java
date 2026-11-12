package com.bakery.sweet;

import com.bakery.food.Topping;
import com.bakery.registry.BakeryItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;

import java.util.ArrayList;
import java.util.List;

/**
 * A cake sculpted and decorated by hand on the Cake Turntable: its shape (round, square or heart), one to three
 * tiers (each with its own frosting colour, or -1 to keep the flavour's colour), every dab of piped icing and
 * every topper, where they were put. Positions are in half-pixels around the cake's bottom centre
 * (see {@link CakeGeometry}). How cute it turned out adds stars.
 */
public record CakeDesign(int shape, List<Integer> tiers, List<Dab> dabs, List<Topper> toppers) {
	public static final int MAX_DABS = 480;
	public static final int MAX_TOPPERS = 24;

	/** Piped icing. Kinds: dot, rosette, or a drip {@code DRIP + length - 1} hanging down from (x, y, z). */
	public record Dab(int kind, int color, int x, int y, int z) {
		public static final int DOT = 0, ROSETTE = 1, DRIP = 2;
		public static final Codec<Dab> CODEC = Codec.INT.listOf().xmap(
				l -> new Dab(l.get(0), l.get(1), l.get(2), l.get(3), l.get(4)), d -> List.of(d.kind, d.color, d.x, d.y, d.z));
		public static final PacketCodec<RegistryByteBuf, Dab> PACKET_CODEC = PacketCodec.of((d, buf) -> {
			buf.writeByte(d.kind);
			buf.writeInt(d.color);
			buf.writeShort(d.x);
			buf.writeShort(d.y);
			buf.writeShort(d.z);
		}, buf -> new Dab(buf.readByte(), buf.readInt(), buf.readShort(), buf.readShort(), buf.readShort()));

		public boolean isDrip() {
			return kind >= DRIP;
		}

		public int dripLength() {
			return kind - DRIP + 1;
		}
	}

	/** A topper standing on the cake, turned {@code yaw} degrees. */
	public record Topper(Item item, int x, int y, int z, int yaw) {
		public static final Codec<Topper> CODEC = RecordCodecBuilder.create(i -> i.group(
				Registries.ITEM.getCodec().fieldOf("item").forGetter(Topper::item),
				Codec.INT.fieldOf("x").forGetter(Topper::x),
				Codec.INT.fieldOf("y").forGetter(Topper::y),
				Codec.INT.fieldOf("z").forGetter(Topper::z),
				Codec.INT.optionalFieldOf("yaw", 0).forGetter(Topper::yaw)).apply(i, Topper::new));
		public static final PacketCodec<RegistryByteBuf, Topper> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.registryValue(RegistryKeys.ITEM), Topper::item,
				PacketCodecs.VAR_INT, Topper::x,
				PacketCodecs.VAR_INT, Topper::y,
				PacketCodecs.VAR_INT, Topper::z,
				PacketCodecs.VAR_INT, Topper::yaw,
				Topper::new);
	}

	public static final Codec<CakeDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("shape", 0).forGetter(CakeDesign::shape),
			Codec.INT.listOf().optionalFieldOf("tiers", List.of(-1)).forGetter(CakeDesign::tiers),
			Dab.CODEC.listOf().optionalFieldOf("dabs", List.of()).forGetter(CakeDesign::dabs),
			Topper.CODEC.listOf().optionalFieldOf("toppers", List.of()).forGetter(CakeDesign::toppers)).apply(i, CakeDesign::new));
	public static final PacketCodec<RegistryByteBuf, CakeDesign> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, CakeDesign::shape,
			PacketCodecs.INTEGER.collect(PacketCodecs.toList()), CakeDesign::tiers,
			Dab.PACKET_CODEC.collect(PacketCodecs.toList()), CakeDesign::dabs,
			Topper.PACKET_CODEC.collect(PacketCodecs.toList()), CakeDesign::toppers,
			CakeDesign::new);

	public static final CakeDesign PLAIN = new CakeDesign(CakeGeometry.ROUND, List.of(-1), List.of(), List.of());

	public CakeDesign {
		shape = Math.floorMod(shape, CakeGeometry.SHAPES);
		tiers = tiers.isEmpty() ? List.of(-1) : List.copyOf(tiers.subList(0, Math.min(tiers.size(), CakeGeometry.MAX_TIERS)));
		dabs = List.copyOf(dabs.subList(0, Math.min(dabs.size(), MAX_DABS)));
		toppers = List.copyOf(toppers.subList(0, Math.min(toppers.size(), MAX_TOPPERS)));
	}

	public int tierCount() {
		return tiers.size();
	}

	public boolean frosted() {
		return tiers.stream().anyMatch(c -> c != -1);
	}

	// ---- edits (designs are immutable; each edit returns a new one) ----

	public CakeDesign withShape(int shape) {
		return new CakeDesign(shape, tiers, dabs, toppers);
	}

	public CakeDesign withTier(int color) {
		List<Integer> list = new ArrayList<>(tiers);
		list.add(color);
		// Piping and toppers on the old top would end up inside the new tier: they get lifted off.
		return new CakeDesign(shape, list, List.of(), List.of());
	}

	public CakeDesign frost(int tier, int color) {
		List<Integer> list = new ArrayList<>(tiers);
		list.set(tier, color);
		return new CakeDesign(shape, list, dabs, toppers);
	}

	public CakeDesign withDabs(List<Dab> more) {
		List<Dab> list = new ArrayList<>(dabs);
		list.addAll(more);
		return new CakeDesign(shape, tiers, list, toppers);
	}

	public CakeDesign withTopper(Topper topper) {
		List<Topper> list = new ArrayList<>(toppers);
		list.add(topper);
		return new CakeDesign(shape, tiers, dabs, list);
	}

	public CakeDesign withoutDabsNear(float x, float y, float z, float range) {
		List<Dab> list = new ArrayList<>();
		for (Dab d : dabs) {
			float dx = d.x / 2f - x, dy = d.y / 2f - y, dz = d.z / 2f - z;
			if (dx * dx + dy * dy + dz * dz > range * range) list.add(d);
		}
		return new CakeDesign(shape, tiers, list, toppers);
	}

	/** Takes off the topper closest to (x, y, z) within {@code range} pixels, or returns this. */
	public CakeDesign withoutTopperNear(float x, float y, float z, float range, Item[] removed) {
		Topper best = null;
		float bestDistance = range * range;
		for (Topper t : toppers) {
			float dx = t.x / 2f - x, dy = t.y / 2f - y, dz = t.z / 2f - z;
			float d = dx * dx + dy * dy + dz * dz;
			if (d <= bestDistance) {
				bestDistance = d;
				best = t;
			}
		}
		if (best == null) return this;
		removed[0] = best.item();
		List<Topper> list = new ArrayList<>(toppers);
		list.remove(best);
		return new CakeDesign(shape, tiers, dabs, list);
	}

	// ---- judging ----

	/** Toppings, fruit, fancy toppers and candles can all go on a cake. */
	public static boolean isTopper(ItemStack stack) {
		if (stack.isIn(ItemTags.CANDLES) || Topping.fromItem(stack) != Topping.NONE) return true;
		for (Item item : BakeryItems.TOPPERS) {
			if (stack.isOf(item)) return true;
		}
		return false;
	}

	public boolean isValid() {
		if (tiers.isEmpty() || tiers.size() > CakeGeometry.MAX_TIERS || dabs.size() > MAX_DABS || toppers.size() > MAX_TOPPERS) return false;
		for (Topper t : toppers) {
			if (!isTopper(new ItemStack(t.item()))) return false;
		}
		for (int color : tiers) {
			if (color != -1 && Icing.byColor(color) == null) return false;
		}
		return true;
	}

	/** Mirror image left to right: toppers have a twin of the same kind on the other side (or sit in the middle). */
	public boolean symmetric() {
		if (toppers.isEmpty()) return false;
		for (Topper t : toppers) {
			boolean twin = Math.abs(t.x) <= 1 || toppers.stream().anyMatch(o -> o.item() == t.item() && Math.abs(o.x + t.x) <= 2 && Math.abs(o.z - t.z) <= 2);
			if (!twin) return false;
		}
		return true;
	}

	/** 1-5 hearts: frosting, tiers, a fancy shape, plenty of piping in more than one style, and well-chosen toppers. */
	public int cuteness() {
		int score = 1;
		if (frosted()) score++;
		if (tiers.size() >= 2) score++;
		if (shape != CakeGeometry.ROUND) score++;
		if (dabs.size() >= 12) score++;
		if (dabs.stream().map(d -> Math.min(d.kind, Dab.DRIP)).distinct().count() >= 2 || dabs.stream().map(Dab::color).distinct().count() >= 2) score++;
		if (toppers.size() >= 3) score++;
		if (toppers.stream().map(Topper::item).distinct().count() >= 3) score++;
		if (toppers.size() >= 2 && symmetric()) score++;
		return Math.min(5, score);
	}

	/** Extra stars the decoration gives the cake. */
	public int starBonus() {
		int cute = cuteness();
		return cute >= 5 ? 2 : cute >= 3 ? 1 : 0;
	}
}
