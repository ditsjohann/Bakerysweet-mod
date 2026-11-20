package com.bakery.registry;

import com.bakery.Bakery;
import com.bakery.cafe.CustomerEntity;
import com.bakery.deco.SeatEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class BakeryEntities {
	public static final EntityType<SeatEntity> SEAT = com.bakery.platform.Platform.get().register(Registries.ENTITY_TYPE, Bakery.id("seat"),
			EntityType.Builder.<SeatEntity>create(SeatEntity::new, SpawnGroup.MISC).dimensions(0.01f, 0.01f)
					.disableSaving().disableSummon().maxTrackingRange(8).build("seat"));
	/** The café's customers. */
	public static final EntityType<CustomerEntity> CUSTOMER = com.bakery.platform.Platform.get().register(Registries.ENTITY_TYPE, Bakery.id("customer"),
			EntityType.Builder.<CustomerEntity>create(CustomerEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.8f).eyeHeight(1.62f)
					// Sit down into a chair like a player does (feet 0.6 below the seat point), not on top of it.
					.vehicleAttachment(new net.minecraft.util.math.Vec3d(0, 0.6, 0))
					.maxTrackingRange(10).build("customer"));

	/** The café cats that live around a cat bed. */
	public static final EntityType<com.bakery.pets.CafeCatEntity> CAFE_CAT = com.bakery.platform.Platform.get().register(Registries.ENTITY_TYPE, Bakery.id("cafe_cat"),
			EntityType.Builder.<com.bakery.pets.CafeCatEntity>create(com.bakery.pets.CafeCatEntity::new, SpawnGroup.CREATURE).dimensions(0.6f, 0.7f)
					.maxTrackingRange(8).build("cafe_cat"));

	private BakeryEntities() {
	}

	public static void init() {
		com.bakery.platform.Hooks.attributes(CUSTOMER, CustomerEntity::createAttributes);
		com.bakery.platform.Hooks.attributes(CAFE_CAT, net.minecraft.entity.passive.CatEntity::createCatAttributes);
	}
}
