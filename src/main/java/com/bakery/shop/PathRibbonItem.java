package com.bakery.shop;

import com.bakery.registry.BakeryComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays the path customers walk. Click the floor at your bakery's door (the entrance), then at every turn on the
 * way in, then click the counter to tie the ribbon to it. Customers come in along it and queue on its last
 * stretch. Sneak-click the counter to pick its path back up and change it; sneak-use in the air to start over.
 * Holding it shows the paths around you.
 */
public class PathRibbonItem extends Item {
	public PathRibbonItem(Settings settings) {
		super(settings);
	}

	public static List<BlockPos> points(ItemStack stack) {
		return stack.getOrDefault(BakeryComponents.PATH_POINTS, List.of());
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();
		ItemStack stack = context.getStack();
		BlockPos clicked = context.getBlockPos();
		if (world.isClient) return ActionResult.SUCCESS;
		if (world.getBlockEntity(clicked) instanceof CounterBlockEntity counter) {
			return link(world, player, stack, counter);
		}
		if (player != null && player.isSneaking()) {
			clear(world, player, stack);
			return ActionResult.SUCCESS;
		}
		List<BlockPos> points = new ArrayList<>(points(stack));
		BlockPos spot = context.getSide() == Direction.UP || context.getSide() == Direction.DOWN ? clicked.up() : clicked.offset(context.getSide());
		if (!points.isEmpty() && points.getLast().equals(spot)) return ActionResult.SUCCESS;
		if (points.size() >= CustomerPath.MAX_POINTS) {
			tell(player, Text.translatable("message.bakery.path.full").formatted(Formatting.GOLD));
			return ActionResult.FAIL;
		}
		if (!points.isEmpty() && Math.sqrt(points.getLast().getSquaredDistance(spot)) > CustomerPath.MAX_STEP) {
			tell(player, Text.translatable("message.bakery.path.far").formatted(Formatting.GOLD));
			return ActionResult.FAIL;
		}
		points.add(spot.toImmutable());
		stack.set(BakeryComponents.PATH_POINTS, points);
		world.playSound(null, spot, SoundEvents.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 0.8f, 1.2f + points.size() * 0.02f);
		tell(player, Text.translatable(points.size() == 1 ? "message.bakery.path.entrance" : "message.bakery.path.point", points.size())
				.formatted(Formatting.LIGHT_PURPLE));
		return ActionResult.SUCCESS;
	}

	private ActionResult link(World world, PlayerEntity player, ItemStack stack, CounterBlockEntity counter) {
		List<BlockPos> points = new ArrayList<>(points(stack));
		if (points.isEmpty()) {
			if (counter.hasPath() && player != null && player.isSneaking()) {
				// Sneak-click: pick the counter's path back up to change it.
				stack.set(BakeryComponents.PATH_POINTS, counter.getPath().subList(0, Math.max(1, counter.getPath().size() - 1)));
				tell(player, Text.translatable("message.bakery.path.picked_up").formatted(Formatting.LIGHT_PURPLE));
				return ActionResult.SUCCESS;
			}
			tell(player, Text.translatable("message.bakery.path.empty").formatted(Formatting.GOLD));
			return ActionResult.FAIL;
		}
		BlockPos front = CustomerPath.counterFront(world, counter.getPos());
		if (Math.sqrt(points.getLast().getSquaredDistance(front)) > CustomerPath.MAX_STEP) {
			tell(player, Text.translatable("message.bakery.path.far").formatted(Formatting.GOLD));
			return ActionResult.FAIL;
		}
		if (!points.getLast().equals(front)) points.add(front);
		counter.setPath(points);
		stack.remove(BakeryComponents.PATH_POINTS);
		world.playSound(null, counter.getPos(), SoundEvents.ENTITY_LEASH_KNOT_PLACE, SoundCategory.PLAYERS, 1f, 1.2f);
		world.playSound(null, counter.getPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.6f, 1.6f);
		tell(player, Text.translatable("message.bakery.path.linked", CustomerPath.blocks(points).size()).formatted(Formatting.LIGHT_PURPLE));
		return ActionResult.SUCCESS;
	}

	private static void clear(World world, PlayerEntity player, ItemStack stack) {
		stack.remove(BakeryComponents.PATH_POINTS);
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 0.8f, 1.2f);
		tell(player, Text.translatable("message.bakery.path.cleared").formatted(Formatting.GRAY));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (user.isSneaking() && !points(stack).isEmpty()) {
			if (!world.isClient) clear(world, user, stack);
			return TypedActionResult.success(stack, world.isClient);
		}
		return TypedActionResult.pass(stack);
	}

	private static void tell(PlayerEntity player, Text text) {
		if (player != null) player.sendMessage(text, true);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		int n = points(stack).size();
		if (n > 0) tooltip.add(Text.translatable("tooltip.bakery.path_ribbon.points", n).formatted(Formatting.LIGHT_PURPLE));
		tooltip.add(Text.translatable("tooltip.bakery.path_ribbon.1").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.bakery.path_ribbon.2").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.bakery.path_ribbon.3").formatted(Formatting.GRAY));
	}
}
