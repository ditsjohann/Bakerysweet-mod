package com.bakery.pets;

import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.CatVariant;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * A café cat: one of the vanilla breeds, living around its {@link CatBedBlock}. It wanders inside the bed's area
 * (the baker sets the size at the bed), lounges about, naps on its bed at night and purrs when petted.
 * It never wanders off, follows nobody, and does not breed.
 */
public class CafeCatEntity extends CatEntity {
	public static final int DEFAULT_RADIUS = 8;

	private BlockPos home;
	private int radius = DEFAULT_RADIUS;
	/** Ticks left of the current lounge or day nap. */
	int restTicks;

	public CafeCatEntity(EntityType<? extends CatEntity> type, World world) {
		super(type, world);
		setPersistent();
	}

	// ---------------------------------------------------------------- home and area

	public BlockPos home() {
		return home != null ? home : getBlockPos();
	}

	public int radius() {
		return radius;
	}

	public void setHome(BlockPos home, int radius) {
		this.home = home.toImmutable();
		this.radius = radius;
	}

	/** The cat bed at home, if there is one. */
	public BlockPos bed() {
		return home != null && getWorld().getBlockState(home).isOf(BakeryBlocks.CAT_BED) ? home : null;
	}

	boolean inArea(BlockPos pos) {
		BlockPos h = home();
		return Math.abs(pos.getX() - h.getX()) <= radius && Math.abs(pos.getZ() - h.getZ()) <= radius && Math.abs(pos.getY() - h.getY()) <= 4;
	}

	/** Moves in with the nearest cat bed (up to 16 blocks away). */
	public void findBed() {
		BlockPos center = getBlockPos();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (BlockPos p : BlockPos.iterate(center.add(-16, -4, -16), center.add(16, 4, 16))) {
			if (getWorld().getBlockState(p).isOf(BakeryBlocks.CAT_BED)) {
				double d = p.getSquaredDistance(center);
				if (d < bestDist) {
					bestDist = d;
					best = p.toImmutable();
				}
			}
		}
		if (best != null) {
			setHome(best, CatBedBlock.radius(getWorld().getBlockState(best)));
		} else if (home == null) {
			setHome(center, DEFAULT_RADIUS);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && age % 100 == 0) {
			BlockPos bed = bed();
			if (bed != null) {
				radius = CatBedBlock.radius(getWorld().getBlockState(bed));
			} else if (age % 400 == 0) {
				findBed();
			}
		}
	}

	// ---------------------------------------------------------------- behaviour

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(2, new NapGoal(this));
		goalSelector.add(3, new LoungeGoal(this));
		goalSelector.add(4, new RoamGoal(this));
		goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6f));
		goalSelector.add(6, new LookAroundGoal(this));
	}

	public boolean resting() {
		return isInSleepingPose() || isInSittingPose() || isSitting();
	}

	@Override
	public ActionResult interactMob(PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		World world = getWorld();
		if (stack.isOf(BakeryItems.KITTEN_BASKET)) {
			return ActionResult.PASS; // the basket picks the cat up
		}
		if (stack.getItem() instanceof DyeItem dye) {
			if (!world.isClient) {
				setCollar(dye.getColor());
				stack.decrementUnlessCreative(1, player);
			}
			return ActionResult.success(world.isClient);
		}
		if (stack.isIn(ItemTags.CAT_FOOD)) {
			if (!world.isClient) {
				stack.decrementUnlessCreative(1, player);
				heal(4f);
				playSound(SoundEvents.ENTITY_CAT_EAT, 1f, 1f);
				hearts(5);
			}
			return ActionResult.success(world.isClient);
		}
		if (stack.isEmpty()) {
			if (!world.isClient) {
				if (player.isSneaking()) {
					boolean stay = !isSitting();
					setSitting(stay);
					setInSittingPose(stay);
					setInSleepingPose(false);
					player.sendMessage(Text.translatable(stay ? "message.bakery.cat.stay" : "message.bakery.cat.roam", getDisplayName()), true);
				} else {
					playSound(random.nextBoolean() ? SoundEvents.ENTITY_CAT_PURR : SoundEvents.ENTITY_CAT_PURREOW, 1f, 1f);
					hearts(3);
				}
			}
			return ActionResult.success(world.isClient);
		}
		return ActionResult.PASS;
	}

	void hearts(int n) {
		if (getWorld() instanceof ServerWorld server) {
			server.spawnParticles(ParticleTypes.HEART, getX(), getY() + 0.7, getZ(), n, 0.3, 0.2, 0.3, 0);
		}
	}

	public void setCollar(DyeColor color) {
		// The collar colour has no public setter: go through the cat's own save data.
		NbtCompound nbt = new NbtCompound();
		writeCustomDataToNbt(nbt);
		nbt.putByte("CollarColor", (byte) color.getId());
		readCustomDataFromNbt(nbt);
	}

	public void randomBreed() {
		getWorld().getRegistryManager().get(RegistryKeys.CAT_VARIANT).getRandom(random).ifPresent(this::setVariant);
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return false;
	}

	@Override
	public boolean canBreedWith(AnimalEntity other) {
		return false;
	}

	@Override
	public CatEntity createChild(ServerWorld world, PassiveEntity entity) {
		return null;
	}

	@Override
	public boolean canImmediatelyDespawn(double distance) {
		return false;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		if (home != null) nbt.put("CafeHome", NbtHelper.fromBlockPos(home));
		nbt.putInt("CafeRadius", radius);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		home = NbtHelper.toBlockPos(nbt, "CafeHome").orElse(null);
		if (nbt.contains("CafeRadius")) radius = nbt.getInt("CafeRadius");
	}

	// ---------------------------------------------------------------- goals

	/** Walks to a random spot inside the area now and then (and straight home when it finds itself outside). */
	static class RoamGoal extends Goal {
		private final CafeCatEntity cat;

		RoamGoal(CafeCatEntity cat) {
			this.cat = cat;
			setControls(EnumSet.of(Control.MOVE));
		}

		@Override
		public boolean canStart() {
			if (cat.resting() || cat.hasVehicle()) return false;
			if (!cat.inArea(cat.getBlockPos())) return true;
			return cat.getRandom().nextInt(90) == 0;
		}

		@Override
		public boolean shouldContinue() {
			return !cat.getNavigation().isIdle() && !cat.resting();
		}

		@Override
		public void start() {
			BlockPos target = spot();
			if (target != null) cat.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.8);
		}

		private BlockPos spot() {
			BlockPos h = cat.home();
			int r = cat.radius();
			if (!cat.inArea(cat.getBlockPos())) return h;
			for (int tries = 0; tries < 10; tries++) {
				BlockPos p = h.add(cat.getRandom().nextBetween(-r, r), 0, cat.getRandom().nextBetween(-r, r));
				for (int dy = 3; dy >= -3; dy--) {
					BlockPos q = p.up(dy);
					if (standable(cat.getWorld(), q)) return q;
				}
			}
			return null;
		}
	}

	static boolean standable(World world, BlockPos pos) {
		BlockState below = world.getBlockState(pos.down());
		return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
				&& world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
				&& below.isSideSolidFullSquare(world, pos.down(), Direction.UP);
	}

	/** Sits down for half a minute to a minute, tail curled. */
	static class LoungeGoal extends Goal {
		private final CafeCatEntity cat;

		LoungeGoal(CafeCatEntity cat) {
			this.cat = cat;
			setControls(EnumSet.of(Control.MOVE, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return !cat.isSitting() && !cat.isInSleepingPose() && cat.isOnGround() && cat.getRandom().nextInt(500) == 0;
		}

		@Override
		public boolean shouldContinue() {
			return cat.restTicks > 0 && !cat.isSitting();
		}

		@Override
		public void start() {
			cat.restTicks = 600 + cat.getRandom().nextInt(600);
			cat.getNavigation().stop();
			cat.setInSittingPose(true);
		}

		@Override
		public void tick() {
			cat.restTicks--;
		}

		@Override
		public void stop() {
			cat.restTicks = 0;
			if (!cat.isSitting()) cat.setInSittingPose(false);
		}
	}

	/** Curls up on the cat bed: all night long, and for a little nap now and then in the day. */
	static class NapGoal extends Goal {
		private final CafeCatEntity cat;
		private boolean dayNap;

		NapGoal(CafeCatEntity cat) {
			this.cat = cat;
			setControls(EnumSet.of(Control.MOVE, Control.JUMP, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			if (cat.isSitting() || cat.bed() == null) return false;
			if (cat.getWorld().isNight()) return true;
			return cat.getRandom().nextInt(1600) == 0;
		}

		@Override
		public boolean shouldContinue() {
			if (cat.isSitting() || cat.bed() == null) return false;
			return dayNap ? cat.restTicks > 0 : cat.getWorld().isNight();
		}

		@Override
		public void start() {
			dayNap = !cat.getWorld().isNight();
			cat.restTicks = 1200 + cat.getRandom().nextInt(1200);
			BlockPos bed = cat.bed();
			cat.getNavigation().startMovingTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.9);
		}

		@Override
		public void tick() {
			BlockPos bed = cat.bed();
			if (bed == null) return;
			if (cat.isInSleepingPose()) {
				cat.restTicks--;
				return;
			}
			if (cat.squaredDistanceTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5) < 1.6) {
				cat.getNavigation().stop();
				cat.refreshPositionAndAngles(bed.getX() + 0.5, bed.getY() + 0.2, bed.getZ() + 0.5, cat.getYaw(), 0);
				cat.setInSittingPose(false);
				cat.setInSleepingPose(true);
			} else if (cat.getNavigation().isIdle()) {
				cat.getNavigation().startMovingTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.9);
			}
		}

		@Override
		public void stop() {
			cat.restTicks = 0;
			cat.setInSleepingPose(false);
		}
	}
}
