package com.bakery.pets;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * A round, cushioned cat bed: the home of the café cats nearby. Click it with an empty hand to see how far they may
 * roam (pink dust shows the edge); sneak-click to change it (4 to 16 blocks).
 */
public class CatBedBlock extends HorizontalFacingBlock {
	public static final int[] RADII = {4, 6, 8, 12, 16};
	public static final IntProperty AREA = IntProperty.of("area", 0, RADII.length - 1);
	private static final VoxelShape SHAPE = createCuboidShape(1, 0, 1, 15, 5, 15);

	public CatBedBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH).with(AREA, 2));
	}

	public static int radius(BlockState state) {
		return state.contains(AREA) ? RADII[state.get(AREA)] : CafeCatEntity.DEFAULT_RADIUS;
	}

	@Override
	protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
		return createCodec(CatBedBlock::new);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, AREA);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext context) {
		return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!player.getMainHandStack().isEmpty()) return ActionResult.PASS;
		if (world.isClient) return ActionResult.SUCCESS;
		// A click shows the area; only a sneak-click changes it (so petting around the bed never shrinks it).
		BlockState next = player.isSneaking() ? state.cycle(AREA) : state;
		if (next != state) world.setBlockState(pos, next, Block.NOTIFY_ALL);
		int r = radius(next);
		world.playSound(null, pos, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.BLOCKS, 0.8f, next != state ? 1.2f : 1.6f);
		player.sendMessage(Text.translatable(next != state ? "message.bakery.cat_bed.area" : "message.bakery.cat_bed.shown", r * 2 + 1, r * 2 + 1), true);
		showArea((ServerWorld) world, pos, r);
		return ActionResult.SUCCESS;
	}

	/** Pink dust along the edge of the square the cats roam in. */
	public static void showArea(ServerWorld world, BlockPos pos, int r) {
		DustParticleEffect dust = new DustParticleEffect(new Vector3f(1f, 0.6f, 0.78f), 1.2f);
		for (int i = -r; i <= r; i++) {
			for (int[] edge : new int[][]{{i, -r}, {i, r}, {-r, i}, {r, i}}) {
				world.spawnParticles(dust, pos.getX() + 0.5 + edge[0], pos.getY() + 0.3, pos.getZ() + 0.5 + edge[1], 1, 0, 0.1, 0, 0);
			}
		}
	}
}
