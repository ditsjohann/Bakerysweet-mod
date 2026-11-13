package com.bakery.cafe;

import com.bakery.deco.ChairBlock;
import com.bakery.deco.SeatEntity;
import com.bakery.item.DrinkItem;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryEntities;
import com.bakery.registry.BakeryItems;
import com.bakery.shop.CounterBlock;
import com.bakery.shop.CounterBlockEntity;
import com.bakery.shop.Order;
import com.bakery.shop.ShopManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/**
 * A café customer. Walks in through the bakery's entrance along the path the baker laid (see
 * {@link com.bakery.shop.CustomerPath}), joins the line on it showing what they'd like, moves up as the people in
 * front are served, and waits
 * (not forever). Once served they either take it to go, or find a free café chair, sit down and eat or drink,
 * leave a little tip on the table and head home.
 */
public class CustomerEntity extends PathAwareEntity {
	public static final int ARRIVING = 0, WAITING = 1, TO_SEAT = 2, EATING = 3, LEAVING = 4;
	private static final TrackedData<Integer> REGULAR = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> STATE = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** Who this customer is chatting with (entity id, -1 for nobody) and what about (picks the little speech-bubble faces). */
	private static final TrackedData<Integer> CHAT_WITH = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Integer> CHAT_TOPIC = DataTracker.registerData(CustomerEntity.class, TrackedDataHandlerRegistry.INTEGER);
	/** How long a diner eats, and how long they stay at the table in all (random for each visit). */
	private static final int EAT_MIN = 400, EAT_RANDOM = 500, STAY_MIN = 1800, STAY_RANDOM = 2400;
	private static final double SPEED = 0.62;

	private BlockPos counter;
	private BlockPos spot;
	private BlockPos exit;
	private BlockPos seat;
	private int timer;
	private int stuck;
	private int tip;
	private Vec3d lastPos = Vec3d.ZERO;
	/** Path points still to walk through on the way in, and on the way out. */
	private final List<BlockPos> route = new java.util.ArrayList<>();
	private final List<BlockPos> exitRoute = new java.util.ArrayList<>();
	private int routeIndex;
	private int exitIndex;
	private int queueIndex = -1;
	private int eatTicks;
	private int stayTicks;
	private int chatTicks;

	public CustomerEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		if (getNavigation() instanceof MobNavigation navigation) {
			navigation.setCanPathThroughDoors(true);
			navigation.setCanEnterOpenDoors(true);
		}
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return PathAwareEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(REGULAR, 0);
		builder.add(STATE, ARRIVING);
		builder.add(CHAT_WITH, -1);
		builder.add(CHAT_TOPIC, 0);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new LongDoorInteractGoal(this, true));
		goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6f));
	}

	// ------------------------------------------------------------------ state

	public Regular regular() {
		return Regular.get(dataTracker.get(REGULAR));
	}

	public int state() {
		return dataTracker.get(STATE);
	}

	private void setState(int state) {
		dataTracker.set(STATE, state);
		timer = 0;
		stuck = 0;
	}

	/** Sitting at a table (eating, or lingering afterwards). */
	public boolean isEating() {
		return state() == EATING;
	}

	/** Seated and still has food in hand: takes bites. */
	public boolean isBiting() {
		return state() == EATING && !getMainHandStack().isEmpty();
	}

	public int chatPartnerId() {
		return dataTracker.get(CHAT_WITH);
	}

	public int chatTopic() {
		return dataTracker.get(CHAT_TOPIC);
	}

	/** Chats go back and forth: whose turn it is to talk (the bubble shows over the talker). */
	public boolean isTalking() {
		int partner = chatPartnerId();
		if (partner < 0) return false;
		int turn = (age / 34) % 2;
		return (getId() < partner) == (turn == 0);
	}

	public boolean atCounter() {
		return state() == WAITING;
	}

	public BlockPos counter() {
		return counter;
	}

	public BlockPos spot() {
		return spot;
	}

	/**
	 * Makes a new customer for a counter with a walking path: they appear just outside the entrance, walk in along
	 * the path and take place {@code queueIndex} in the line.
	 */
	public static CustomerEntity create(ServerWorld world, BlockPos counterPos, List<BlockPos> path, int queueIndex, Regular regular) {
		CustomerEntity customer = new CustomerEntity(BakeryEntities.CUSTOMER, world);
		customer.dataTracker.set(REGULAR, regular.index());
		customer.counter = counterPos.toImmutable();
		customer.setCustomName(regular.displayName());
		customer.queueAt(queueIndex, path);
		BlockPos start = customer.outside(world, path);
		customer.exit = start;
		customer.refreshPositionAndAngles(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, world.random.nextFloat() * 360, 0);
		return customer;
	}

	/** A spot a few steps outside the entrance (away from the path), so customers walk in through the door. */
	private BlockPos outside(ServerWorld world, List<BlockPos> path) {
		BlockPos entrance = path.getFirst();
		BlockPos next = path.size() > 1 ? path.get(1) : counter;
		int dx = Integer.signum(entrance.getX() - next.getX()), dz = Integer.signum(entrance.getZ() - next.getZ());
		if (Math.abs(entrance.getX() - next.getX()) > Math.abs(entrance.getZ() - next.getZ())) dz = 0;
		else dx = 0;
		BlockPos best = entrance;
		for (int step = 1; step <= 5; step++) {
			BlockPos ground = standable(world, entrance.add(dx * step, 0, dz * step));
			if (ground == null || Math.abs(ground.getY() - entrance.getY()) > 1) break;
			best = ground;
		}
		return best;
	}

	/** Takes place {@code index} in the line (0 = at the counter); called again whenever the line moves up. */
	public void queueAt(int index, List<BlockPos> path) {
		if (index == queueIndex || path.size() < 2) return;
		queueIndex = index;
		spot = com.bakery.shop.CustomerPath.queueSpot(path, index);
		if (state() == ARRIVING) {
			List<BlockPos> left = com.bakery.shop.CustomerPath.routeTo(path, index);
			int done = Math.min(routeIndex, left.size());
			route.clear();
			route.addAll(left);
			routeIndex = done;
		}
	}

	public int queueIndex() {
		return queueIndex;
	}

	/** Makes a new customer for a counter, standing somewhere they can walk in from (no path: used by the showroom). */
	public static CustomerEntity create(ServerWorld world, BlockPos counterPos, BlockPos spot, Regular regular) {
		CustomerEntity customer = new CustomerEntity(BakeryEntities.CUSTOMER, world);
		customer.dataTracker.set(REGULAR, regular.index());
		customer.counter = counterPos.toImmutable();
		customer.spot = spot.toImmutable();
		customer.setCustomName(regular.displayName());
		BlockPos start = customer.findEntrance(world);
		customer.exit = start;
		customer.refreshPositionAndAngles(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, world.random.nextFloat() * 360, 0);
		return customer;
	}

	/** A spot 10-18 blocks out that has a real path to the counter. Falls back to closer spots. */
	private BlockPos findEntrance(ServerWorld world) {
		for (int range : new int[]{16, 10, 6}) {
			for (int attempt = 0; attempt < 10; attempt++) {
				double angle = world.random.nextDouble() * Math.PI * 2;
				int r = range - 3 + world.random.nextInt(4);
				BlockPos column = counter.add(MathHelper.floor(Math.cos(angle) * r), 0, MathHelper.floor(Math.sin(angle) * r));
				BlockPos ground = standable(world, column);
				if (ground == null) continue;
				setPosition(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
				Path path = getNavigation().findPathTo(spot, 0);
				if (path != null && path.reachesTarget()) {
					return ground;
				}
			}
		}
		return spot;
	}

	/** The spot a person could stand at in this column, near the counter's height. */
	private BlockPos standable(ServerWorld world, BlockPos column) {
		BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column);
		for (BlockPos p : new BlockPos[]{top, column, column.up(), column.down()}) {
			if (Math.abs(p.getY() - counter.getY()) <= 5 && world.getBlockState(p.down()).isSolidBlock(world, p.down())
					&& world.getBlockState(p).getCollisionShape(world, p).isEmpty() && world.getBlockState(p.up()).getCollisionShape(world, p.up()).isEmpty()
					&& world.getFluidState(p).isEmpty()) {
				return p;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ behaviour

	@Override
	protected void mobTick() {
		super.mobTick();
		if (!(getWorld() instanceof ServerWorld world)) return;
		timer++;
		switch (state()) {
			case ARRIVING -> {
				if (!counterStillWants()) {
					leave();
				} else if (routeIndex < route.size()) {
					// Follow the path in, turn by turn.
					BlockPos next = route.get(routeIndex);
					if (horizontalDistance(next) < 1.6) routeIndex++;
					else walkTo(next, true);
				} else if (spot != null && squaredDistanceTo(Vec3d.ofBottomCenter(spot)) < 1.1) {
					getNavigation().stop();
					setState(WAITING);
				} else {
					walkTo(spot, true);
				}
			}
			case WAITING -> {
				if (!counterStillWants()) {
					leave();
					return;
				}
				// Shuffle up the line when someone ahead is served.
				if (squaredDistanceTo(Vec3d.ofBottomCenter(spot)) > 0.7) walkTo(spot, false);
				else getNavigation().stop();
				if (chatPartnerId() < 0) getLookControl().lookAt(counter.getX() + 0.5, counter.getY() + 1.2, counter.getZ() + 0.5);
				socialize(world);
			}
			case TO_SEAT -> {
				if (seat == null || !(world.getBlockState(seat).getBlock() instanceof ChairBlock) || timer > 900) {
					leave();
				} else if (squaredDistanceTo(Vec3d.ofBottomCenter(seat)) < 2.2) {
					sitDown(world);
				} else {
					walkTo(seat, true);
				}
			}
			case EATING -> {
				if (!hasVehicle()) {
					finishEating(world);
					return;
				}
				ItemStack food = getMainHandStack();
				if (timer % 24 == 0 && !food.isEmpty()) {
					boolean drink = food.getItem() instanceof DrinkItem || food.isOf(BakeryItems.JUICE);
					world.playSound(null, getBlockPos(), drink ? SoundEvents.ENTITY_GENERIC_DRINK : SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.NEUTRAL,
							0.5f, 0.9f + random.nextFloat() * 0.3f);
					if (!drink) {
						Vec3d mouth = getRotationVec(1f).multiply(0.3).add(getX(), getEyeY() - 0.15, getZ());
						world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, food), mouth.x, mouth.y, mouth.z, 5, 0.05, 0.05, 0.05, 0.05);
					}
				}
				if (!food.isEmpty() && timer % 120 == 60) {
					world.spawnParticles(ParticleTypes.HEART, getX(), getY() + 1.9, getZ(), 1, 0.1, 0.05, 0.1, 0);
				}
				// Finished the treat: crumbs on the plate, then they linger a while before going home.
				if (!food.isEmpty() && timer >= eatTicks) {
					world.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, food), getX(), getEyeY() - 0.3, getZ(), 6, 0.1, 0.05, 0.1, 0.03);
					setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
				}
				if (food.isEmpty() && chatPartnerId() < 0 && timer % 100 == 0 && random.nextFloat() < 0.5f) {
					// Look around the café.
					PlayerEntity near = world.getClosestPlayer(this, 8);
					if (near != null && random.nextBoolean()) getLookControl().lookAt(near, 30, 30);
					else getLookControl().lookAt(getX() + random.nextGaussian() * 3, getEyeY() + random.nextGaussian() * 0.5, getZ() + random.nextGaussian() * 3);
				}
				socialize(world);
				if (timer > stayTicks) finishEating(world);
			}
			case LEAVING -> {
				if (exitIndex < exitRoute.size()) {
					// Back out along the path and through the door.
					BlockPos next = exitRoute.get(exitIndex);
					if (horizontalDistance(next) < 1.6) exitIndex++;
					else walkTo(next, true);
					if (timer > 900) discard();
				} else if (exit == null || squaredDistanceTo(Vec3d.ofBottomCenter(exit)) < 2.5 || timer > 700) {
					discard();
				} else {
					walkTo(exit, false);
				}
			}
			default -> {
			}
		}
	}

	private boolean counterStillWants() {
		if (counter == null || !(getWorld().getBlockEntity(counter) instanceof CounterBlockEntity table)) return false;
		return table.find(getUuid()) != null;
	}

	private double horizontalDistance(BlockPos target) {
		double dx = getX() - (target.getX() + 0.5), dz = getZ() - (target.getZ() + 0.5);
		return Math.sqrt(dx * dx + dz * dz) + Math.abs(getY() - target.getY()) * 0.5;
	}

	/** Walks somewhere; if stuck for a long time on the way in, hops there so a customer never gets lost forever. */
	private void walkTo(BlockPos target, boolean mayHop) {
		if (target == null) return;
		if (getNavigation().isIdle() || timer % 40 == 0) {
			getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, SPEED);
		}
		if (timer % 20 == 0) {
			if (getPos().squaredDistanceTo(lastPos) < 0.3) stuck++;
			else stuck = 0;
			lastPos = getPos();
		}
		if (mayHop && stuck >= 8 && getWorld() instanceof ServerWorld world) {
			world.spawnParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 6, 0.2, 0.4, 0.2, 0.01);
			refreshPositionAndAngles(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, getYaw(), 0);
			world.spawnParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 6, 0.2, 0.4, 0.2, 0.01);
			stuck = 0;
		}
	}

	/** Called by the shop when this customer got what they ordered. */
	public void served(ServerWorld world, Order order, ItemStack food, int tipCoins) {
		setStackInHand(Hand.MAIN_HAND, food.copyWithCount(1));
		setEquipmentDropChance(net.minecraft.entity.EquipmentSlot.MAINHAND, 0);
		tip = tipCoins;
		BlockPos chair = order.dineIn ? freeChair(world) : null;
		if (chair != null) {
			seat = chair;
			setState(TO_SEAT);
		} else {
			leave();
		}
	}

	/** Walks off home, unhappy or not. */
	public void leave() {
		if (state() == LEAVING) return;
		stopChat();
		stopRiding();
		setState(LEAVING);
		exitRoute.clear();
		exitIndex = 0;
		if (counter != null && getWorld().getBlockEntity(counter) instanceof CounterBlockEntity table && table.hasPath()) {
			exitRoute.addAll(com.bakery.shop.CustomerPath.routeOut(table.getPath(), getBlockPos()));
		}
	}

	/**
	 * Picks a seat: free chairs and couches near the counter, preferring ones at a table, sometimes next to
	 * someone already eating, with a little randomness so diners spread around the café.
	 */
	private BlockPos freeChair(ServerWorld world) {
		if (counter == null) return null;
		BlockPos best = null;
		double bestScore = Double.MAX_VALUE;
		List<CustomerEntity> others = world.getEntitiesByClass(CustomerEntity.class, new Box(counter).expand(16), c -> c != this);
		boolean social = random.nextFloat() < 0.5f;
		for (BlockPos p : BlockPos.iterate(counter.add(-10, -2, -10), counter.add(10, 2, 10))) {
			if (!(world.getBlockState(p).getBlock() instanceof ChairBlock)) continue;
			if (!world.getEntitiesByClass(SeatEntity.class, new Box(p), s -> true).isEmpty()) continue;
			BlockPos chair = p.toImmutable();
			if (others.stream().anyMatch(c -> chair.equals(c.seat))) continue;
			double score = Math.sqrt(squaredDistanceTo(Vec3d.ofCenter(p))) + random.nextDouble() * 3;
			if (tableNextTo(world, chair) != null) score -= 6;
			if (social && others.stream().anyMatch(c -> c.isEating() && c.squaredDistanceTo(Vec3d.ofCenter(chair)) < 6.5)) score -= 3;
			if (score < bestScore) {
				bestScore = score;
				best = chair;
			}
		}
		return best;
	}

	private static BlockPos tableNextTo(ServerWorld world, BlockPos seat) {
		for (Direction d : Direction.Type.HORIZONTAL) {
			BlockState state = world.getBlockState(seat.offset(d));
			if (state.isOf(BakeryBlocks.CAFE_TABLE) || state.isOf(BakeryBlocks.PARASOL_TABLE)) return seat.offset(d);
		}
		return null;
	}

	// ------------------------------------------------------------------ chatting

	/** Starts a chat between these two for a while. */
	public void chatWith(CustomerEntity other, int ticks) {
		int topic = random.nextInt(6);
		dataTracker.set(CHAT_WITH, other.getId());
		dataTracker.set(CHAT_TOPIC, topic);
		chatTicks = ticks;
		other.dataTracker.set(CHAT_WITH, getId());
		other.dataTracker.set(CHAT_TOPIC, topic);
		other.chatTicks = ticks;
	}

	private CustomerEntity chatPartner() {
		int id = chatPartnerId();
		return id >= 0 && getWorld().getEntityById(id) instanceof CustomerEntity other ? other : null;
	}

	private void stopChat() {
		dataTracker.set(CHAT_WITH, -1);
		chatTicks = 0;
	}

	/**
	 * Customers chat with whoever is right there: diners at the same table, or the person just ahead of or behind
	 * them in the line (who turns round). The talk goes back and forth, and now and then ends in a laugh.
	 */
	private void socialize(ServerWorld world) {
		CustomerEntity partner = chatPartner();
		if (partner != null || chatPartnerId() >= 0) {
			if (partner == null || !partner.isAlive() || partner.chatPartnerId() != getId() || squaredDistanceTo(partner) > 20
					|| partner.state() != state()) {
				stopChat();
				return;
			}
			getLookControl().lookAt(partner, 40, 40);
			if (--chatTicks <= 0) {
				if (getId() < partner.getId() && random.nextFloat() < 0.45f) {
					boolean hearts = random.nextFloat() < 0.4f;
					for (CustomerEntity c : List.of(this, partner)) {
						world.spawnParticles(hearts ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER, c.getX(), c.getY() + 2.0, c.getZ(), hearts ? 1 : 4, 0.2, 0.1, 0.2, 0);
					}
					world.playSound(null, getBlockPos(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.NEUTRAL, 0.35f, 1.7f);
				}
				stopChat();
				partner.stopChat();
				return;
			}
			if (isTalking() && age % 34 == 6) {
				world.playSound(null, getBlockPos(), SoundEvents.ENTITY_VILLAGER_AMBIENT, SoundCategory.NEUTRAL, 0.25f, 1.5f + random.nextFloat() * 0.3f);
			}
			return;
		}
		if (timer % 40 != 0 || random.nextFloat() > 0.2f) return;
		for (CustomerEntity other : world.getEntitiesByClass(CustomerEntity.class, getBoundingBox().expand(3.2), c -> c != this && c.chatPartnerId() < 0)) {
			boolean pair = state() == EATING ? other.state() == EATING
					: state() == WAITING && other.state() == WAITING && counter != null && counter.equals(other.counter) && Math.abs(other.queueIndex - queueIndex) == 1;
			if (!pair) continue;
			chatWith(other, 90 + random.nextInt(180));
			return;
		}
	}

	private void sitDown(ServerWorld world) {
		BlockState chair = world.getBlockState(seat);
		if (!world.getEntitiesByClass(SeatEntity.class, new Box(seat), s -> true).isEmpty()) {
			BlockPos other = freeChair(world);
			if (other == null) leave();
			else seat = other;
			return;
		}
		getNavigation().stop();
		SeatEntity seatEntity = new SeatEntity(BakeryEntities.SEAT, world);
		seatEntity.setPosition(seat.getX() + 0.5, seat.getY() + 0.45, seat.getZ() + 0.5);
		// Chairs face the way you look when sitting on them (towards their table).
		float yaw = chair.get(ChairBlock.FACING).asRotation();
		seatEntity.setYaw(yaw);
		world.spawnEntity(seatEntity);
		startRiding(seatEntity, true);
		setYaw(yaw);
		setHeadYaw(yaw);
		setBodyYaw(yaw);
		setState(EATING);
		eatTicks = EAT_MIN + random.nextInt(EAT_RANDOM);
		stayTicks = Math.max(eatTicks + 200, STAY_MIN + random.nextInt(STAY_RANDOM));
	}

	private void finishEating(ServerWorld world) {
		setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
		if (tip > 0 && seat != null) {
			// A little thank-you left on the table (or the chair if there is no table).
			BlockPos table = seat;
			for (Direction d : Direction.Type.HORIZONTAL) {
				if (world.getBlockState(seat.offset(d)).isOf(BakeryBlocks.CAFE_TABLE) || world.getBlockState(seat.offset(d)).isOf(BakeryBlocks.PARASOL_TABLE)) {
					table = seat.offset(d);
					break;
				}
			}
			ItemEntity coins = new ItemEntity(world, table.getX() + 0.5, table.getY() + 1.05, table.getZ() + 0.5, new ItemStack(BakeryItems.SWEET_COIN, tip), 0, 0, 0);
			coins.setPickupDelay(10);
			world.spawnEntity(coins);
			world.playSound(null, table, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.4f, 1.8f);
		}
		tip = 0;
		stopRiding();
		seat = null;
		leave();
	}

	// ------------------------------------------------------------------ interaction

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
		if (getWorld().isClient) return ActionResult.SUCCESS;
		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
		if (stack.isOf(BakeryItems.WRAPPED_GIFT)) {
			com.bakery.sweet.Friendship.giveGift(serverPlayer, this, stack);
			return ActionResult.SUCCESS;
		}
		if (state() == WAITING || state() == ARRIVING) {
			if (!stack.isEmpty() && ShopManager.serve(serverPlayer, this, stack)) return ActionResult.SUCCESS;
			Order order = counter != null && getWorld().getBlockEntity(counter) instanceof CounterBlockEntity table ? table.find(getUuid()) : null;
			if (order != null) {
				player.sendMessage(Text.translatable("message.bakery.customer.wants", getName(), order.wanted.getName()).formatted(Formatting.LIGHT_PURPLE), true);
				getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_VILLAGER_AMBIENT, SoundCategory.NEUTRAL, 0.6f, 1.6f);
			}
			return ActionResult.SUCCESS;
		}
		int hearts = com.bakery.sweet.Friendship.hearts(serverPlayer, regular().id());
		player.sendMessage(Text.translatable("message.bakery.customer.hello", getName(), com.bakery.sweet.Friendship.heartText(hearts)), true);
		return ActionResult.SUCCESS;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.isOf(DamageTypes.OUT_OF_WORLD) && !source.isOf(DamageTypes.GENERIC_KILL);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	protected void pushAway(net.minecraft.entity.Entity entity) {
		if (state() != EATING) super.pushAway(entity);
	}

	@Override
	public boolean isPushable() {
		return state() != EATING;
	}

	@Override
	protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
	}

	// ------------------------------------------------------------------ saving

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Regular", dataTracker.get(REGULAR));
		nbt.putInt("CafeState", state());
		if (counter != null) nbt.put("Counter", NbtHelper.fromBlockPos(counter));
		if (spot != null) nbt.put("Spot", NbtHelper.fromBlockPos(spot));
		if (exit != null) nbt.put("Exit", NbtHelper.fromBlockPos(exit));
		if (seat != null) nbt.put("Seat", NbtHelper.fromBlockPos(seat));
		nbt.putInt("Tip", tip);
		nbt.putLongArray("Route", route.stream().mapToLong(BlockPos::asLong).toArray());
		nbt.putLongArray("ExitRoute", exitRoute.stream().mapToLong(BlockPos::asLong).toArray());
		nbt.putInt("RouteIndex", routeIndex);
		nbt.putInt("ExitIndex", exitIndex);
		nbt.putInt("QueueIndex", queueIndex);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		dataTracker.set(REGULAR, nbt.getInt("Regular"));
		int state = nbt.getInt("CafeState");
		counter = NbtHelper.toBlockPos(nbt, "Counter").orElse(null);
		spot = NbtHelper.toBlockPos(nbt, "Spot").orElse(null);
		exit = NbtHelper.toBlockPos(nbt, "Exit").orElse(null);
		seat = NbtHelper.toBlockPos(nbt, "Seat").orElse(null);
		tip = nbt.getInt("Tip");
		route.clear();
		for (long l : nbt.getLongArray("Route")) route.add(BlockPos.fromLong(l));
		exitRoute.clear();
		for (long l : nbt.getLongArray("ExitRoute")) exitRoute.add(BlockPos.fromLong(l));
		routeIndex = nbt.getInt("RouteIndex");
		exitIndex = nbt.getInt("ExitIndex");
		queueIndex = nbt.contains("QueueIndex") ? nbt.getInt("QueueIndex") : -1;
		// Seats are not saved, so someone who was eating gets up and goes home.
		dataTracker.set(STATE, state == EATING || state == TO_SEAT ? LEAVING : state);
	}

	/** Showroom helper: sit straight down on a chair with something to eat. */
	public void demoSit(ServerWorld world, BlockPos chair, ItemStack food) {
		setStackInHand(Hand.MAIN_HAND, food);
		seat = chair.toImmutable();
		sitDown(world);
	}

	/** For the spawn egg / summon command: without a counter they just stroll off. */
	public void setRegular(int index) {
		dataTracker.set(REGULAR, Math.floorMod(index, Regular.count()));
		setCustomName(regular().displayName());
	}

	public static Optional<CustomerEntity> byUuid(ServerWorld world, java.util.UUID id) {
		return world.getEntity(id) instanceof CustomerEntity customer ? Optional.of(customer) : Optional.empty();
	}

	public static List<CustomerEntity> near(ServerWorld world, BlockPos pos, double range) {
		return world.getEntitiesByClass(CustomerEntity.class, new Box(pos).expand(range), c -> true);
	}

	/** Where the n-th customer in line stands, in front of a counter. */
	public static BlockPos queueSpot(World world, BlockPos counterPos, int index) {
		BlockState state = world.getBlockState(counterPos);
		Direction facing = state.contains(CounterBlock.FACING) ? state.get(CounterBlock.FACING) : Direction.NORTH;
		BlockPos front = counterPos.offset(facing);
		Direction side = facing.rotateYClockwise();
		return switch (index) {
			case 1 -> front.offset(side);
			case 2 -> front.offset(side.getOpposite());
			default -> front;
		};
	}
}
