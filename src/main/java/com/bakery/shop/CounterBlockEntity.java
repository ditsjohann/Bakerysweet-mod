package com.bakery.shop;

import com.bakery.cafe.CafeLevel;
import com.bakery.cafe.CustomerEntity;
import com.bakery.cafe.Regular;
import com.bakery.food.BakeryFoods;
import com.bakery.network.OrdersPayload;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.review.BakeryReputation;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Runs a little bakery. While open it invites the town's regulars: they walk in along the path laid with a Path
 * Ribbon, queue on its last stretch in front of the counter
 * with a wish bubble over their heads and wait - not forever. Serve them by hand, or let them help themselves
 * from a display case. How many come and what they ask for grows with the owner's {@link CafeLevel}.
 */
public class CounterBlockEntity extends BlockEntity {
	public static final int MAX_CUSTOMERS = 3;

	private final List<Order> orders = new ArrayList<>();
	/** Dishes the baker took off the menu (item ids). */
	private final Set<String> offMenu = new HashSet<>();
	private UUID owner;
	private int ownerServed;
	private int till;
	private int served;
	private int walkouts;
	/** 0-5 hearts of Charm from cozy decorations nearby, and how many of those there are. */
	private int charm;
	private int cozyCount;
	private int lastRegular = -1;
	/** The walking path customers follow, laid with a Path Ribbon: entrance first, the spot in front of the counter last. */
	private final List<BlockPos> path = new ArrayList<>();
	/** The review board: what customers wrote about the bakery, newest first. */
	private final List<Review> reviews = new ArrayList<>();

	public CounterBlockEntity(BlockPos pos, BlockState state) {
		super(BakeryBlockEntities.COUNTER, pos, state);
	}

	public List<Review> getReviews() {
		return reviews;
	}

	/** Most customers leave a few words on the review board; unhappy ones are the most likely to. */
	public void maybeReview(ServerWorld server, Review review) {
		float chance = review.stars() <= 2 ? 0.85f : 0.65f;
		if (server.random.nextFloat() >= chance) return;
		reviews.addFirst(review);
		while (reviews.size() > Review.KEEP) reviews.removeLast();
		sync();
	}

	/** Average of one part of the reviews: 0 overall, 1 speed, 2 taste, 3 charm. 0 if there are none. */
	public float average(int part) {
		float sum = 0;
		int n = 0;
		for (Review r : reviews) {
			int v = switch (part) {
				case 1 -> r.speed();
				case 2 -> r.taste();
				case 3 -> r.charm();
				default -> r.stars();
			};
			if (v <= 0) continue;
			sum += v;
			n++;
		}
		return n == 0 ? 0 : sum / n;
	}

	public List<Order> getOrders() {
		return orders;
	}

	public int getTill() {
		return till;
	}

	public int getServed() {
		return served;
	}

	public int getWalkouts() {
		return walkouts;
	}

	public int getCharm() {
		return charm;
	}

	public int getCozyCount() {
		return cozyCount;
	}

	public UUID getOwner() {
		return owner;
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
		sync();
	}

	public List<BlockPos> getPath() {
		return path;
	}

	/** Customers only come once a walking path leads in to the counter. */
	public boolean hasPath() {
		return path.size() >= 2;
	}

	public void setPath(List<BlockPos> points) {
		path.clear();
		path.addAll(points);
		sync();
	}

	public Set<String> getOffMenu() {
		return offMenu;
	}

	/** The bakery's level, from how many customers its owner has served (synced to the client for the menu board). */
	public CafeLevel getLevel() {
		return CafeLevel.of(ownerServed);
	}

	public int getOwnerServed() {
		return ownerServed;
	}

	public void toggleDish(String id) {
		if (!offMenu.remove(id)) offMenu.add(id);
		sync();
	}

	/** Counts the cozy decorations around the counter again. */
	public void updateCharm() {
		if (world == null) return;
		int count = com.bakery.sweet.Charm.count(world, pos);
		int level = com.bakery.sweet.Charm.level(count);
		if (count != cozyCount || level != charm) {
			cozyCount = count;
			charm = level;
			sync();
		}
		if (charm >= 5 && world instanceof ServerWorld server) {
			for (ServerPlayerEntity player : PlayerLookup.around(server, Vec3d.ofCenter(pos), 12)) {
				com.bakery.sweet.Stickers.award(player, com.bakery.sweet.Sticker.COZY_CAFE);
			}
		}
	}

	public boolean isOpen() {
		return getCachedState().get(CounterBlock.OPEN);
	}

	public Order find(UUID customer) {
		for (Order order : orders) {
			if (order.customer.equals(customer)) {
				return order;
			}
		}
		return null;
	}

	public int collectTill() {
		int amount = till;
		till = 0;
		sync();
		return amount;
	}

	/** A customer got what they wanted from the player. */
	public void complete(Order order) {
		orders.remove(order);
		served++;
		refreshOwner();
		sync();
		broadcast();
	}

	/** Called by the service bell: invite a customer right now. */
	public boolean callCustomer() {
		return world instanceof ServerWorld server && isOpen() && hasPath() && orders.size() < maxCustomers() && recruit(server);
	}

	public int maxCustomers() {
		return Math.min(MAX_CUSTOMERS, getLevel().maxCustomers() + (charm >= 4 ? 1 : 0));
	}

	/** Everyone in line goes home (the counter was broken or closed). */
	public void clearCustomers() {
		if (world instanceof ServerWorld server) {
			for (Order order : orders) {
				CustomerEntity.byUuid(server, order.customer).ifPresent(CustomerEntity::leave);
			}
		}
		orders.clear();
	}

	private void refreshOwner() {
		if (owner != null && world instanceof ServerWorld server) {
			int now = BakeryReputation.get(server.getServer()).of(owner).served;
			if (now != ownerServed) {
				ownerServed = now;
				sync();
			}
		}
	}

	public static void tick(World world, BlockPos pos, BlockState state, CounterBlockEntity counter) {
		if (!(world instanceof ServerWorld server) || world.getTime() % 10 != 0) {
			return;
		}
		boolean changed = false;
		if (world.getTime() % 200 == 0) {
			counter.updateCharm();
			counter.refreshOwner();
		}

		// Everyone moves up the line as the people in front are served.
		int place = 0;
		for (Order order : counter.orders) {
			if (server.getEntity(order.customer) instanceof CustomerEntity customer && counter.hasPath()) {
				customer.queueAt(place++, counter.path);
			}
		}

		Iterator<Order> it = counter.orders.iterator();
		while (it.hasNext()) {
			Order order = it.next();
			Entity entity = server.getEntity(order.customer);
			if (!(entity instanceof CustomerEntity customer) || !customer.isAlive()) {
				// Their chunk may just be unloading; only forget them once they have been gone a while.
				order.patience -= 40;
				if (order.patience <= 0 || entity != null) {
					it.remove();
					changed = true;
				}
				continue;
			}
			boolean close = customer.atCounter();
			order.waiting = close;
			// Customers are happy to wait a little longer in a charming bakery.
			order.patience -= close ? 10 - counter.charm : 3;

			if (close && world.getTime() % 40 == 0 && counter.selfServe(server, customer, order)) {
				it.remove();
				changed = true;
				continue;
			}
			if (order.patience <= 0) {
				server.spawnParticles(ParticleTypes.ANGRY_VILLAGER, customer.getX(), customer.getY() + 2, customer.getZ(), 3, 0.3, 0.2, 0.3, 0);
				server.playSound(null, customer.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.NEUTRAL, 0.8f, 1.4f);
				customer.leave();
				it.remove();
				counter.walkouts++;
				counter.maybeReview(server, Review.walkout(customer.regular().name(), net.minecraft.registry.Registries.ITEM.getId(order.wanted.getItem()).toString(),
						counter.charm, server.getTimeOfDay() / 24000L, server.random));
				changed = true;
			}
		}
		if (!counter.isOpen() && !counter.orders.isEmpty()) {
			counter.clearCustomers();
			changed = true;
		}

		// Customers come by during the day; a charming, well-known bakery gets them more often.
		if (counter.isOpen() && counter.hasPath() && server.isDay() && counter.orders.size() < counter.maxCustomers()
				&& server.random.nextInt(Math.max(4, 36 - counter.charm * 4 - counter.getLevel().ordinal() * 2)) == 0) {
			changed |= counter.recruit(server);
		}
		if (changed) {
			counter.sync();
		}
		if (changed || world.getTime() % 20 == 0) {
			counter.broadcast();
		}
	}

	private boolean recruit(ServerWorld server) {
		Set<Integer> present = new HashSet<>();
		for (CustomerEntity c : CustomerEntity.near(server, pos, 48)) present.add(c.regular().index());
		List<Integer> free = new ArrayList<>();
		for (int i = 0; i < Regular.count(); i++) {
			if (!present.contains(i) && i != lastRegular) free.add(i);
		}
		if (free.isEmpty()) return false;
		Regular regular = Regular.get(free.get(server.random.nextInt(free.size())));
		if (!hasPath()) return false;
		CustomerEntity customer = CustomerEntity.create(server, pos, path, orders.size(), regular);
		Order order = Menu.randomOrder(customer.getUuid(), regular, getLevel(), offMenu, server.random);
		orders.add(order);
		server.spawnEntity(customer);
		lastRegular = regular.index();
		server.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.5f, 1.8f);
		sync();
		return true;
	}

	/** If a nearby display case has what the customer wants, they take it and leave the money in the till. */
	private boolean selfServe(ServerWorld server, CustomerEntity customer, Order order) {
		for (BlockPos p : BlockPos.iterate(pos.add(-6, -2, -6), pos.add(6, 2, 6))) {
			if (!(server.getBlockEntity(p) instanceof DisplayCaseBlockEntity display)) {
				continue;
			}
			ItemStack taken = display.takeMatching(order);
			if (taken.isEmpty()) {
				continue;
			}
			int stars = ShopManager.rate(customer.regular(), taken, server.getTime());
			boolean favorite = BakeryFoods.flavor(taken) == customer.regular().favorite();
			till += Math.max(1, Menu.price(order, stars, favorite) - 1) + charm / 2;
			maybeReview(server, Review.served(customer.regular().name(), net.minecraft.registry.Registries.ITEM.getId(taken.getItem()).toString(), stars,
					order.patienceFraction(), charm, server.getTimeOfDay() / 24000L, server.random));
			served++;
			server.spawnParticles(stars >= 4 ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER, customer.getX(), customer.getY() + 2.1, customer.getZ(), 3, 0.3, 0.15, 0.3, 0);
			server.playSound(null, p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 1f, 1.6f);
			server.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 0.5f, 1.2f);
			customer.served(server, order, taken, 0);
			return true;
		}
		return false;
	}

	private void broadcast() {
		if (!(world instanceof ServerWorld server)) {
			return;
		}
		ArrayList<OrdersPayload.OrderView> views = new ArrayList<>();
		for (Order order : orders) {
			Entity entity = server.getEntity(order.customer);
			if (entity != null) {
				views.add(new OrdersPayload.OrderView(entity.getId(), order.wanted, order.patienceFraction(), order.waiting, order.dineIn));
			}
		}
		OrdersPayload payload = new OrdersPayload(pos, views);
		for (ServerPlayerEntity player : PlayerLookup.around(server, Vec3d.ofCenter(pos), 48)) {
			ServerPlayNetworking.send(player, payload);
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
		NbtList list = new NbtList();
		for (Order order : orders) {
			list.add(order.write(registries));
		}
		nbt.put("orders", list);
		NbtList off = new NbtList();
		offMenu.forEach(id -> off.add(NbtString.of(id)));
		nbt.put("offMenu", off);
		if (owner != null) nbt.putUuid("owner", owner);
		nbt.putInt("ownerServed", ownerServed);
		nbt.putInt("till", till);
		nbt.putInt("served", served);
		nbt.putInt("walkouts", walkouts);
		nbt.putInt("charm", charm);
		nbt.putInt("cozy", cozyCount);
		nbt.putLongArray("path", path.stream().mapToLong(BlockPos::asLong).toArray());
		NbtList board = new NbtList();
		reviews.forEach(r -> board.add(r.write()));
		nbt.put("reviews", board);
	}

	@Override
	protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		super.readNbt(nbt, registries);
		orders.clear();
		NbtList list = nbt.getList("orders", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < list.size(); i++) {
			NbtCompound entry = list.getCompound(i);
			if (entry.containsUuid("customer")) orders.add(Order.read(entry, registries));
		}
		offMenu.clear();
		NbtList off = nbt.getList("offMenu", NbtElement.STRING_TYPE);
		for (int i = 0; i < off.size(); i++) offMenu.add(off.getString(i));
		owner = nbt.containsUuid("owner") ? nbt.getUuid("owner") : null;
		ownerServed = nbt.getInt("ownerServed");
		till = nbt.getInt("till");
		served = nbt.getInt("served");
		walkouts = nbt.getInt("walkouts");
		charm = nbt.getInt("charm");
		cozyCount = nbt.getInt("cozy");
		path.clear();
		for (long l : nbt.getLongArray("path")) path.add(BlockPos.fromLong(l));
		reviews.clear();
		NbtList board = nbt.getList("reviews", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < board.size(); i++) reviews.add(Review.read(board.getCompound(i)));
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
