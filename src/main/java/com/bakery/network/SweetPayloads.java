package com.bakery.network;

import com.bakery.Bakery;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Packets for the Sweet Life update: catalog, cake studio and the guide's sticker book. */
public final class SweetPayloads {
	private SweetPayloads() {
	}

	/** Client -> server: buy catalog entry {@code index} at the catalog block at {@code pos}. */
	public record Buy(BlockPos pos, int index) implements CustomPayload {
		public static final Id<Buy> ID = new Id<>(Bakery.id("catalog_buy"));
		public static final PacketCodec<RegistryByteBuf, Buy> CODEC = PacketCodec.tuple(
				BlockPos.PACKET_CODEC, Buy::pos, PacketCodecs.VAR_INT, Buy::index, Buy::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	/** Server -> client: open the catalog at {@code pos}, with the player's progress (for locked goodies and the Read tab). */
	public record OpenCatalog(BlockPos pos, GuideData data) implements CustomPayload {
		public static final Id<OpenCatalog> ID = new Id<>(Bakery.id("open_catalog"));
		public static final PacketCodec<RegistryByteBuf, OpenCatalog> CODEC = PacketCodec.tuple(
				BlockPos.PACKET_CODEC, OpenCatalog::pos, GuideData.CODEC, OpenCatalog::data, OpenCatalog::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	/** Server -> client: open the menu board for the counter at {@code counter}. */
	public record OpenMenu(BlockPos counter) implements CustomPayload {
		public static final Id<OpenMenu> ID = new Id<>(Bakery.id("open_menu"));
		public static final PacketCodec<RegistryByteBuf, OpenMenu> CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, OpenMenu::counter, OpenMenu::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	/** Server -> client: open the counter's screen (shop sign, till and review board). */
	public record OpenCounter(BlockPos counter) implements CustomPayload {
		public static final Id<OpenCounter> ID = new Id<>(Bakery.id("open_counter"));
		public static final PacketCodec<RegistryByteBuf, OpenCounter> CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, OpenCounter::counter, OpenCounter::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	/** Server -> client: opens the Order Notebook screen (the orders come with the notebook's block entity sync). */
	public record OpenNotebook(BlockPos pos) implements CustomPayload {
		public static final Id<OpenNotebook> ID = new Id<>(Bakery.id("open_notebook"));
		public static final PacketCodec<RegistryByteBuf, OpenNotebook> CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, OpenNotebook::pos, OpenNotebook::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	/** Client -> server: a button on the counter screen ({@link com.bakery.shop.CounterBlock#OPEN_SIGN} etc.). */
	public record CounterAction(BlockPos counter, int action) implements CustomPayload {
		public static final Id<CounterAction> ID = new Id<>(Bakery.id("counter_action"));
		public static final PacketCodec<RegistryByteBuf, CounterAction> CODEC = PacketCodec.tuple(
				BlockPos.PACKET_CODEC, CounterAction::counter, PacketCodecs.VAR_INT, CounterAction::action, CounterAction::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	/** Client -> server: take a dish off (or put it back on) the menu of the counter at {@code counter}. */
	public record ToggleDish(BlockPos counter, String dish) implements CustomPayload {
		public static final Id<ToggleDish> ID = new Id<>(Bakery.id("toggle_dish"));
		public static final PacketCodec<RegistryByteBuf, ToggleDish> CODEC = PacketCodec.tuple(
				BlockPos.PACKET_CODEC, ToggleDish::counter, PacketCodecs.STRING, ToggleDish::dish, ToggleDish::new);

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}

	/** Server -> client: everything the guide's sticker book and progress pages show; opens the guide. */
	public record GuideData(List<String> stickers, String rank, int reviews, int recipes, int totalRecipes, int coinsEarned,
							int served, int friends, List<Integer> stockLeft) implements CustomPayload {
		public static final Id<GuideData> ID = new Id<>(Bakery.id("guide_data"));
		private static final PacketCodec<io.netty.buffer.ByteBuf, List<String>> STRINGS = PacketCodecs.STRING.collect(PacketCodecs.toList());
		private static final PacketCodec<io.netty.buffer.ByteBuf, List<Integer>> INTS = PacketCodecs.VAR_INT.collect(PacketCodecs.toList());
		public static final PacketCodec<RegistryByteBuf, GuideData> CODEC = PacketCodec.of((p, buf) -> {
			STRINGS.encode(buf, p.stickers);
			buf.writeString(p.rank);
			buf.writeVarInt(p.reviews);
			buf.writeVarInt(p.recipes);
			buf.writeVarInt(p.totalRecipes);
			buf.writeVarInt(p.coinsEarned);
			buf.writeVarInt(p.served);
			buf.writeVarInt(p.friends);
			INTS.encode(buf, p.stockLeft.stream().map(i -> i + 1).toList());
		}, buf -> new GuideData(STRINGS.decode(buf), buf.readString(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), INTS.decode(buf).stream().map(i -> i - 1).toList()));

		/** Stock left today for catalog entry {@code index}; -1 = unlimited. */
		public int left(int index) {
			return index < stockLeft.size() ? stockLeft.get(index) : -1;
		}

		@Override
		public Id<? extends CustomPayload> getId() {
			return ID;
		}
	}
}
