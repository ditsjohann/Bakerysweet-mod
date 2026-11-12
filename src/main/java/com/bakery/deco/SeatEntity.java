package com.bakery.deco;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** An invisible seat that lets you sit on a cafe chair. Disappears as soon as nobody is sitting on it. */
public class SeatEntity extends Entity {
	public SeatEntity(EntityType<?> type, World world) {
		super(type, world);
		noClip = true;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && (!hasPassengers() || !(getWorld().getBlockState(getBlockPos()).getBlock() instanceof ChairBlock))) {
			removeAllPassengers();
			discard();
		}
	}

	@Override
	public Vec3d updatePassengerForDismount(LivingEntity passenger) {
		return Vec3d.ofBottomCenter(getBlockPos().up());
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
	}
}
