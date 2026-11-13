package com.bakery.client;

import com.bakery.registry.BakeryItems;
import com.bakery.shop.CounterBlockEntity;
import com.bakery.shop.CustomerPath;
import com.bakery.shop.PathRibbonItem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector3f;

import java.util.List;

/**
 * While a Path Ribbon is in hand, the customer paths around you show up in the world: a dotted pink ribbon along
 * the floor, a heart over the entrance and soft gold markers where the line forms in front of each counter.
 * The path you are laying right now shows in a brighter pink.
 */
public final class PathPreview {
	private static final DustParticleEffect LAYING = new DustParticleEffect(new Vector3f(1f, 0.35f, 0.62f), 1.0f);
	private static final DustParticleEffect LINKED = new DustParticleEffect(new Vector3f(0.98f, 0.62f, 0.78f), 0.8f);
	private static final DustParticleEffect QUEUE = new DustParticleEffect(new Vector3f(1f, 0.82f, 0.35f), 1.2f);

	private PathPreview() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(PathPreview::tick);
	}

	private static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;
		if (player == null || world == null || world.getTime() % 5 != 0) return;
		ItemStack held = player.getMainHandStack().isOf(BakeryItems.PATH_RIBBON) ? player.getMainHandStack()
				: player.getOffHandStack().isOf(BakeryItems.PATH_RIBBON) ? player.getOffHandStack() : ItemStack.EMPTY;
		if (held.isEmpty()) return;
		List<BlockPos> laying = PathRibbonItem.points(held);
		if (!laying.isEmpty()) draw(world, laying, LAYING, false);
		ChunkPos center = player.getChunkPos();
		for (int cx = -2; cx <= 2; cx++) {
			for (int cz = -2; cz <= 2; cz++) {
				WorldChunk chunk = world.getChunkManager().getWorldChunk(center.x + cx, center.z + cz);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof CounterBlockEntity counter && counter.hasPath()) draw(world, counter.getPath(), LINKED, true);
				}
			}
		}
	}

	private static void draw(ClientWorld world, List<BlockPos> points, DustParticleEffect dust, boolean linked) {
		List<BlockPos> blocks = CustomerPath.blocks(points);
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos b = blocks.get(i);
			world.addParticle(dust, b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, 0, 0, 0);
			if (i + 1 < blocks.size()) {
				BlockPos n = blocks.get(i + 1);
				world.addParticle(dust, (b.getX() + n.getX()) / 2.0 + 0.5, (b.getY() + n.getY()) / 2.0 + 0.1, (b.getZ() + n.getZ()) / 2.0 + 0.5, 0, 0, 0);
			}
		}
		BlockPos entrance = points.getFirst();
		if (world.getTime() % 20 == 0) {
			world.addParticle(ParticleTypes.HEART, entrance.getX() + 0.5, entrance.getY() + 1.4, entrance.getZ() + 0.5, 0, 0, 0);
		}
		if (linked) {
			for (int k = 0; k < 3 && k < blocks.size(); k++) {
				BlockPos q = blocks.get(blocks.size() - 1 - k);
				world.addParticle(QUEUE, q.getX() + 0.5, q.getY() + 0.25, q.getZ() + 0.5, 0, 0, 0);
			}
		} else {
			// Each turn of the path being laid gets a little pillar so you can see where you clicked.
			for (BlockPos p : points) {
				world.addParticle(LAYING, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 0, 0, 0);
			}
		}
	}
}
