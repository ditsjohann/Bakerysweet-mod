package com.bakery;

import com.bakery.food.BakeryFoods;
import com.bakery.oven.CoreBlock;
import com.bakery.oven.Multiblock;
import com.bakery.review.BakeryReputation;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * /bakery stats - your average villager rating and baker rank.
 * /bakery build &lt;pos&gt; - (operators) form the machine whose core is at pos, like tapping it with the spatula.
 */
public final class BakeryCommands {
	private BakeryCommands() {
	}

	/** Adds /bakery (stats, demo, build); each loader calls this when commands are registered. */
	public static void register(com.mojang.brigadier.CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
		{
				dispatcher.register(CommandManager.literal("bakery")
						.then(CommandManager.literal("stats").executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
							BakeryReputation.Stats stats = BakeryReputation.get(context.getSource().getServer()).of(player.getUuid());
							int rounded = Math.round(stats.average());
							context.getSource().sendFeedback(() -> Text.translatable("command.bakery.stats.title", stats.rank())
									.formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
							context.getSource().sendFeedback(() -> Text.translatable("command.bakery.stats.rating",
									BakeryFoods.stars(Math.max(rounded, 0)), String.format("%.1f", stats.average()), stats.reviews), false);
							context.getSource().sendFeedback(() -> Text.translatable("command.bakery.stats.five", stats.fiveStars)
									.formatted(Formatting.GOLD), false);
							context.getSource().sendFeedback(() -> Text.translatable("command.bakery.stats.recipes", stats.recipes.size(), BakeryReputation.TOTAL_RECIPES)
									.formatted(Formatting.LIGHT_PURPLE), false);
							return stats.reviews;
						}))
						.then(CommandManager.literal("demo").requires(source -> source.hasPermissionLevel(2))
								.then(CommandManager.argument("at", BlockPosArgumentType.blockPos()).executes(context -> {
									try {
										BakeryDemo.build(context.getSource().getWorld(), BlockPosArgumentType.getLoadedBlockPos(context, "at"));
									} catch (RuntimeException e) {
										Bakery.LOGGER.error("The bakery showroom could not be built", e);
										throw e;
									}
									return 1;
								})))
						.then(CommandManager.literal("build").requires(source -> source.hasPermissionLevel(2))
								.then(CommandManager.argument("core", BlockPosArgumentType.blockPos()).executes(context -> {
									BlockPos core = BlockPosArgumentType.getLoadedBlockPos(context, "core");
									BlockState state = context.getSource().getWorld().getBlockState(core);
									for (Multiblock multiblock : Multiblock.values()) {
										if (state.isOf(multiblock.core())) {
											boolean formed = multiblock.tryForm(context.getSource().getWorld(), core, state.get(CoreBlock.FACING),
													context.getSource().getPlayer(), message -> context.getSource().sendFeedback(() -> message, false));
											return formed ? 1 : 0;
										}
									}
									context.getSource().sendError(Text.translatable("command.bakery.build.no_core"));
									return 0;
								}))));
		}
	}
}
