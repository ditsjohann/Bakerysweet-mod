package com.bakery.registry;

import com.bakery.Bakery;
import com.bakery.cafe.CustomerEntity;
import com.bakery.deco.SeatEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class BakeryEntities {
	public static final EntityType<SeatEntity> SEAT = Registry.register(Registries.ENTITY_TYPE, Bakery.id("seat"),
			EntityType.Builder.<SeatEntity>create(SeatEntity::new, SpawnGroup.MISC).dimensions(0.01f, 0.01f)
					.disableSaving().disableSummon().maxTrackingRange(8).build("seat"));
	/** The café's customers. */
	public static final EntityType<CustomerEntity> CUSTOMER = Registry.register(Registries.ENTITY_TYPE, Bakery.id("customer"),
			EntityType.Builder.<CustomerEntity>create(CustomerEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.8f).eyeHeight(1.62f)
					.maxTrackingRange(10).build("customer"));

	private BakeryEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(CUSTOMER, CustomerEntity.createAttributes());
	}
}
