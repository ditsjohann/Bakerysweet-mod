package com.bakery;

import com.bakery.registry.BakeryBlockEntities;
import com.bakery.registry.BakeryBlocks;
import com.bakery.registry.BakeryComponents;
import com.bakery.registry.BakeryEntities;
import com.bakery.registry.BakeryItems;
import com.bakery.registry.BakeryVillagers;
import com.bakery.registry.BakeryWorldgen;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sweetheart Bakery. Each mod loader's entry point installs its {@link com.bakery.platform.Platform}, then calls {@link #init()}. */
public final class Bakery {
	public static final String MOD_ID = "bakery";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	private static boolean initialized;

	private Bakery() {
	}

	/** Registers everything. Safe to call more than once (only the first call does anything). */
	public static void init() {
		if (initialized) return;
		initialized = true;
		BakeryComponents.init();
		com.bakery.registry.BakerySounds.init();
		BakeryBlocks.init();
		BakeryItems.init();
		BakeryBlockEntities.init();
		BakeryEntities.init();
		BakeryVillagers.init();
		BakeryWorldgen.init();

		LOGGER.info("The oven is warm. Sweetheart Bakery loaded.");
	}
}
