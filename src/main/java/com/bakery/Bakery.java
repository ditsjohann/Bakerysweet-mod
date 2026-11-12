package com.bakery;

import com.bakery.network.BakeryNetworking;
import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryEntities;
import com.bakery.registry.BakeryItems;
import com.bakery.registry.BakeryVillagers;
import com.bakery.registry.BakeryWorldgen;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bakery implements ModInitializer {
	public static final String MOD_ID = "bakery";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		BakeryComponents.init();
		BakeryBlocks.init();
		BakeryItems.init();
		BakeryBlockEntities.init();
		BakeryEntities.init();
		BakeryVillagers.init();
		BakeryWorldgen.init();
		BakeryNetworking.init();
		BakeryEvents.register();
		BakeryCommands.register();

		LOGGER.info("The oven is warm. Sweetheart Bakery loaded.");
	}
}
