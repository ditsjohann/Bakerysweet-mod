package com.bakery.oven;

import com.bakery.Bakery;
import com.bakery.registry.BakeryBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bakery machines you build out of blocks instead of crafting. Place the blocks of the blueprint around
 * a core block and tap the core with a Sweetheart Spatula. Breaking any piece turns it back into loose blocks.
 *
 * <pre>
 * Grand Brick Oven      bottom: 3x3 bricks   middle: bricks ring, Oven Heart in front, empty chamber   top: plus of bricks
 * Grand Millstone       bottom: 3x3 stone bricks   middle: Millstone Core with a cobblestone wall on each side
 * Chocolate Fountain    bottom: 3x3 white terracotta   middle: Fountain Core   top: a gold block on the core
 * </pre>
 */
public enum Multiblock {
	OVEN("grand_oven"),
	MILLSTONE("grand_millstone"),
	FOUNTAIN("chocolate_fountain");

	public static final TagKey<Block> OVEN_BRICKS = TagKey.of(RegistryKeys.BLOCK, Bakery.id("oven_bricks"));
	private static final DustParticleEffect MISSING = new DustParticleEffect(new Vector3f(1f, 0.45f, 0.65f), 1.4f);
	private static boolean deforming;

	/** What a position of a blueprint must hold, and what a formed part turns back into. */
	public enum Role {
		BRICK, STONE, WALL, TERRACOTTA, GOLD, CORE, CHAMBER;

		boolean matches(BlockState state, Multiblock multiblock) {
			return switch (this) {
				case BRICK -> state.isIn(OVEN_BRICKS);
				case STONE -> state.isOf(Blocks.STONE_BRICKS) || state.isOf(Blocks.MOSSY_STONE_BRICKS) || state.isOf(Blocks.CRACKED_STONE_BRICKS);
				case WALL -> state.isIn(BlockTags.WALLS);
				case TERRACOTTA -> state.isOf(Blocks.WHITE_TERRACOTTA);
				case GOLD -> state.isOf(Blocks.GOLD_BLOCK);
				case CORE -> state.isOf(multiblock.core());
				case CHAMBER -> state.isAir();
			};
		}
	}

	public record Slot(BlockPos pos, Role role) {
	}

	private final String name;

	Multiblock(String name) {
		this.name = name;
	}

	public String id() {
		return name;
	}

	public Block core() {
		return switch (this) {
			case OVEN -> BakeryBlocks.OVEN_HEART;
			case MILLSTONE -> BakeryBlocks.MILLSTONE_CORE;
			case FOUNTAIN -> BakeryBlocks.FOUNTAIN_CORE;
		};
	}

	public Block controller() {
		return switch (this) {
			case OVEN -> BakeryBlocks.GRAND_OVEN;
			case MILLSTONE -> BakeryBlocks.GRAND_MILLSTONE;
			case FOUNTAIN -> BakeryBlocks.CHOCOLATE_FOUNTAIN;
		};
	}

	/** The controller sits in the oven's chamber, or on the core itself for the other machines. */
	public BlockPos controllerPos(BlockPos core, Direction facing) {
		return this == OVEN ? core.offset(facing.getOpposite()) : core;
	}

	/** Every position of the blueprint, around the controller position. */
	public List<Slot> layout(BlockPos center, Direction facing) {
		List<Slot> slots = new ArrayList<>();
		Direction back = facing.getOpposite();
		Direction side = facing.rotateYClockwise();
		for (int s = -1; s <= 1; s++) {
			for (int b = -1; b <= 1; b++) {
				BlockPos column = center.offset(side, s).offset(back, b);
				boolean middle = s == 0 && b == 0;
				boolean cross = s == 0 || b == 0;
				switch (this) {
					case OVEN -> {
						slots.add(new Slot(column.down(), Role.BRICK));
						slots.add(new Slot(column, middle ? Role.CHAMBER : (s == 0 && b == -1) ? Role.CORE : Role.BRICK));
						if (cross) slots.add(new Slot(column.up(), Role.BRICK));
					}
					case MILLSTONE -> {
						slots.add(new Slot(column.down(), Role.STONE));
						if (middle) slots.add(new Slot(column, Role.CORE));
						else if (cross) slots.add(new Slot(column, Role.WALL));
					}
					case FOUNTAIN -> {
						slots.add(new Slot(column.down(), Role.TERRACOTTA));
						if (middle) {
							slots.add(new Slot(column, Role.CORE));
							slots.add(new Slot(column.up(), Role.GOLD));
						}
					}
				}
			}
		}
		return slots;
	}

	public static boolean isDeforming() {
		return deforming;
	}

	/** Spatula on a core: show what is missing with pink sparkles, or build the machine. */
	public boolean tryForm(ServerWorld world, BlockPos core, Direction facing, PlayerEntity player, Consumer<Text> feedback) {
		BlockPos center = controllerPos(core, facing);
		List<Slot> layout = layout(center, facing);
		List<Slot> missing = new ArrayList<>();
		for (Slot slot : layout) {
			if (!slot.role().matches(world.getBlockState(slot.pos()), this)) {
				missing.add(slot);
			}
		}
		if (!missing.isEmpty()) {
			for (Slot slot : missing) {
				for (int i = 0; i < 6; i++) {
					double x = slot.pos().getX() + world.random.nextDouble(), y = slot.pos().getY() + world.random.nextDouble(), z = slot.pos().getZ() + world.random.nextDouble();
					if (player instanceof ServerPlayerEntity viewer) {
						world.spawnParticles(viewer, MISSING, true, x, y, z, 1, 0, 0, 0, 0);
					} else {
						world.spawnParticles(MISSING, x, y, z, 1, 0, 0, 0, 0);
					}
				}
			}
			boolean chamberBlocked = missing.stream().anyMatch(s -> s.role() == Role.CHAMBER);
			feedback.accept(Text.translatable(chamberBlocked ? "message.bakery.build.chamber" : "message.bakery.build.missing",
					missing.size(), Text.translatable("block.bakery." + name)).formatted(Formatting.LIGHT_PURPLE));
			world.playSound(null, core, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundCategory.BLOCKS, 0.7f, 0.8f);
			return false;
		}

		for (Slot slot : layout) {
			MultiblockPartBlock.Kind kind = MultiblockPartBlock.Kind.of(this, slot.role());
			if (kind != null && !slot.pos().equals(center)) {
				world.setBlockState(slot.pos(), BakeryBlocks.MULTIBLOCK_PART.getDefaultState().with(MultiblockPartBlock.KIND, kind));
			}
		}
		world.setBlockState(center, controller().getDefaultState().with(GrandOvenBlock.FACING, facing));
		world.spawnParticles(ParticleTypes.HEART, core.getX() + 0.5, core.getY() + 1.2, core.getZ() + 0.5, 12, 0.8, 0.5, 0.8, 0.05);
		world.spawnParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5, 20, 1, 1, 1, 0.02);
		world.playSound(null, core, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.BLOCKS, 1f, 1.2f);
		world.playSound(null, core, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.6f, 1.4f);
		feedback.accept(Text.translatable("message.bakery.build.formed", Text.translatable("block.bakery." + name)).formatted(Formatting.GOLD));
		return true;
	}

	/** Turns a formed machine back into loose blocks. {@code except} is the block being broken right now. */
	public void deform(World world, BlockPos center, Direction facing, BlockPos except) {
		if (deforming) {
			return;
		}
		deforming = true;
		try {
			for (Slot slot : layout(center, facing)) {
				if (slot.pos().equals(except)) {
					continue;
				}
				BlockState state = world.getBlockState(slot.pos());
				if (slot.pos().equals(center)) {
					if (state.isOf(controller())) {
						if (world.getBlockEntity(center) instanceof MachineContents contents) {
							contents.dropContents();
						}
						world.setBlockState(center, this == OVEN ? Blocks.AIR.getDefaultState() : core().getDefaultState().with(CoreBlock.FACING, facing));
					}
				} else if (state.isOf(BakeryBlocks.MULTIBLOCK_PART)) {
					world.setBlockState(slot.pos(), state.get(MultiblockPartBlock.KIND).restore(facing));
				}
			}
		} finally {
			deforming = false;
		}
	}

	/** Finds the controller a part belongs to, or null. */
	public static BlockPos findController(World world, BlockPos part) {
		for (BlockPos pos : BlockPos.iterate(part.add(-1, -1, -1), part.add(1, 1, 1))) {
			BlockState state = world.getBlockState(pos);
			for (Multiblock multiblock : values()) {
				if (state.isOf(multiblock.controller())) {
					for (Slot slot : multiblock.layout(pos, state.get(GrandOvenBlock.FACING))) {
						if (slot.pos().equals(part)) {
							return pos.toImmutable();
						}
					}
				}
			}
		}
		return null;
	}

	public static Multiblock ofController(BlockState state) {
		for (Multiblock multiblock : values()) {
			if (state.isOf(multiblock.controller())) return multiblock;
		}
		return null;
	}

	/** Controllers whose block entity holds items that should spill when the machine is taken apart. */
	public interface MachineContents {
		void dropContents();
	}
}
