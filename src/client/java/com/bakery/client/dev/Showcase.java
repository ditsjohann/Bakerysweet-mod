package com.bakery.client.dev;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.integrated.IntegratedServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Development helper, only active with -Dbakery.showcase=&lt;script&gt; (the "Showcase" run config).
 * Reads a little script from the run directory, one step per line:
 * <pre>
 * cmd &lt;command&gt;      run a server command with full permissions (no leading slash)
 * wait &lt;ticks&gt;       wait
 * cam x y z yaw pitch  move the player (spectator) there
 * shot &lt;name&gt;        save run/screenshots/&lt;name&gt;.png
 * gui catalog|guide|menu [served]   open a screen; key left|right|up|down [n]   press keys on it; click x y; close
 * Playtesting with real input (goes through the game's own key handling):
 * look x y z           turn to face a point          slot n          select hotbar slot n (0-8)
 * use [n]              right-click n times           attack          left-click once
 * hold use|attack|forward|back|left|right|sneak|jump ticks   hold a key down
 * inventory            open the inventory screen     state [label]   write the player's state to run/playtest.log
 * note text            write a line to run/playtest.log (game and action-bar messages are logged too)
 * quit
 * </pre>
 */
public final class Showcase {
	private static final Deque<String> STEPS = new ArrayDeque<>();
	private static int wait = 100;
	private static boolean loaded;

	private Showcase() {
	}

	private static final java.util.Map<net.minecraft.client.option.KeyBinding, Integer> HELD = new java.util.HashMap<>();
	private static java.io.PrintWriter log;

	public static void init() {
		String script = System.getProperty("bakery.showcase");
		if (script == null) return;
		com.bakery.client.ClientContent.TICK.add(client -> tick(client, script));
		com.bakery.client.ClientContent.GAME_MESSAGES.add((message, overlay) ->
				log((overlay ? "[actionbar] " : "[chat] ") + message.getString()));
	}

	private static void log(String line) {
		try {
			if (log == null) {
				log = new java.io.PrintWriter(Files.newBufferedWriter(MinecraftClient.getInstance().runDirectory.toPath().resolve("playtest.log")), true);
			}
			MinecraftClient client = MinecraftClient.getInstance();
			long t = client.world != null ? client.world.getTime() : 0;
			log.println("[t=" + t + "] " + line);
		} catch (IOException ignored) {
		}
	}

	private static net.minecraft.client.option.KeyBinding key(MinecraftClient client, String name) {
		return switch (name) {
			case "use" -> client.options.useKey;
			case "attack" -> client.options.attackKey;
			case "back" -> client.options.backKey;
			case "left" -> client.options.leftKey;
			case "right" -> client.options.rightKey;
			case "sneak" -> client.options.sneakKey;
			case "jump" -> client.options.jumpKey;
			default -> client.options.forwardKey;
		};
	}

	private static void state(MinecraftClient client, String label) {
		var p = client.player;
		StringBuilder b = new StringBuilder("STATE " + label + ": ");
		b.append(client.interactionManager.getCurrentGameMode()).append(" hp=").append(p.getHealth()).append(" food=").append(p.getHungerManager().getFoodLevel())
				.append(String.format(" pos=%.1f,%.1f,%.1f", p.getX(), p.getY(), p.getZ())).append(" slot=").append(p.getInventory().selectedSlot)
				.append(" held=").append(p.getMainHandStack()).append(" screen=").append(client.currentScreen == null ? "-" : client.currentScreen.getClass().getSimpleName())
				.append(" target=").append(client.crosshairTarget == null ? "-" : client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult bh
						? client.world.getBlockState(bh.getBlockPos()) + "@" + bh.getBlockPos().toShortString()
						: client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult eh ? eh.getEntity().getName().getString() : "miss")
				.append("\n    inv=");
		java.util.List<String> items = new java.util.ArrayList<>();
		for (int i = 0; i < p.getInventory().size(); i++) {
			var st = p.getInventory().getStack(i);
			if (!st.isEmpty()) items.add(i + ":" + st.getCount() + "x" + st.getName().getString());
		}
		b.append(items);
		log(b.toString());
	}

	private static void tick(MinecraftClient client, String script) {
		if (client.world == null || client.player == null || client.getServer() == null) return;
		if (!loaded) {
			loaded = true;
			try {
				for (String line : Files.readAllLines(client.runDirectory.toPath().resolve(script))) {
					if (!line.isBlank() && !line.startsWith("#")) STEPS.add(line.trim());
				}
			} catch (IOException e) {
				STEPS.add("quit");
			}
			client.options.hudHidden = true;
		}
		HELD.entrySet().removeIf(e -> {
			if (e.getValue() <= 1) {
				e.getKey().setPressed(false);
				return true;
			}
			e.getKey().setPressed(true);
			e.setValue(e.getValue() - 1);
			return false;
		});
		if (wait-- > 0) return;
		IntegratedServer server = client.getServer();
		while (!STEPS.isEmpty() && wait <= 0) {
			String step = STEPS.poll();
			String[] parts = step.split(" ", 2);
			String arg = parts.length > 1 ? parts[1] : "";
			switch (parts[0]) {
				case "cmd" -> server.execute(() -> server.getCommandManager().executeWithPrefix(server.getCommandSource(), arg));
				case "wait" -> wait = Integer.parseInt(arg);
				case "cam" -> {
					String[] p = arg.split(" ");
					String name = client.player.getGameProfile().getName();
					server.execute(() -> server.getCommandManager().executeWithPrefix(server.getCommandSource(),
							"tp " + name + " " + p[0] + " " + p[1] + " " + p[2] + " " + p[3] + " " + p[4]));
					wait = 30;
				}
				case "shot" -> {
					ScreenshotRecorder.saveScreenshot(client.runDirectory, arg + ".png", client.getFramebuffer(), text -> {
					});
					wait = 5;
				}
				case "gui" -> {
					String[] p = arg.split(" ");
					com.bakery.network.SweetPayloads.GuideData data = new com.bakery.network.SweetPayloads.GuideData(
							java.util.List.of("first_bake", "first_customer", "shopper"), "home_baker", 4, 6, 280, 37, Integer.parseInt(p.length > 1 ? p[1] : "7"), 1,
							com.bakery.sweet.Catalog.entries().stream().map(e -> e.stock() > 0 ? e.stock() : -1).toList());
					net.minecraft.util.math.BlockPos pos = p.length > 4 ? new net.minecraft.util.math.BlockPos(Integer.parseInt(p[2]), Integer.parseInt(p[3]), Integer.parseInt(p[4]))
							: net.minecraft.util.math.BlockPos.ORIGIN;
					client.setScreen(switch (p[0]) {
						case "catalog" -> new com.bakery.client.CatalogScreen(pos, data);
						case "menu" -> new com.bakery.client.MenuScreen(pos);
						case "counter" -> new com.bakery.client.CounterScreen(pos);
						default -> new com.bakery.client.GuideScreen(data);
					});
					client.options.hudHidden = false;
					wait = 20;
				}
				case "key" -> {
					// Presses a key on the open screen n times: "key right 3", "key down 1".
					String[] k = arg.split(" ");
					int code = switch (k[0]) {
						case "left" -> org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
						case "up" -> org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
						case "down" -> org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
						case "space" -> org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
						case "escape" -> org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
						default -> org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
					};
					for (int i = 0; i < (k.length > 1 ? Integer.parseInt(k[1]) : 1); i++) {
						if (client.currentScreen != null) client.currentScreen.keyPressed(code, 0, 0);
					}
					wait = 5;
				}
				case "hud" -> client.options.hudHidden = !arg.trim().equals("on");
				case "click" -> {
					// Clicks the open screen at a point given in scaled GUI pixels: "click 120 64".
					String[] c = arg.split(" ");
					if (client.currentScreen != null) client.currentScreen.mouseClicked(Double.parseDouble(c[0]), Double.parseDouble(c[1]), 0);
					wait = 5;
				}
				case "close" -> {
					client.setScreen(null);
					client.options.hudHidden = true;
				}
				case "look" -> {
					String[] c = arg.split(" ");
					var p = client.player;
					double dx = Double.parseDouble(c[0]) - p.getX(), dy = Double.parseDouble(c[1]) - p.getEyeY(), dz = Double.parseDouble(c[2]) - p.getZ();
					float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
					float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
					p.setYaw(yaw);
					p.setPitch(pitch);
					p.setHeadYaw(yaw);
					wait = 2;
				}
				case "slot" -> client.player.getInventory().selectedSlot = Integer.parseInt(arg.trim());
				case "use" -> {
					var k = client.options.useKey;
					int n = arg.isBlank() ? 1 : Integer.parseInt(arg.trim());
					for (int i = 0; i < n; i++) net.minecraft.client.option.KeyBinding.onKeyPressed(net.minecraft.client.util.InputUtil.fromTranslationKey(k.getBoundKeyTranslationKey()));
					wait = 4;
				}
				case "attack" -> {
					net.minecraft.client.option.KeyBinding.onKeyPressed(net.minecraft.client.util.InputUtil.fromTranslationKey(client.options.attackKey.getBoundKeyTranslationKey()));
					wait = 4;
				}
				case "hold" -> {
					String[] c = arg.split(" ");
					int ticks = Integer.parseInt(c[1]);
					HELD.put(key(client, c[0]), ticks);
					wait = ticks + 2;
				}
				case "press" -> {
					// "press sneak on" ... "press sneak off": keeps a key down across other steps.
					String[] c = arg.split(" ");
					var k = key(client, c[0]);
					boolean on = c.length < 2 || c[1].equals("on");
					if (on) HELD.put(k, 1_000_000);
					else {
						HELD.remove(k);
						k.setPressed(false);
					}
					wait = 3;
				}
				case "inventory" -> {
					client.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player));
					client.options.hudHidden = false;
					wait = 10;
				}
				case "grab" -> {
					// Like dragging an item into the hand in the inventory screen: swaps it into the selected hotbar slot.
					java.util.UUID id = client.player.getUuid();
					int sel = client.player.getInventory().selectedSlot;
					String want = arg.trim();
					server.execute(() -> {
						var sp = server.getPlayerManager().getPlayer(id);
						if (sp == null) return;
						var inv = sp.getInventory();
						inv.selectedSlot = sel;
						for (int i = 0; i < inv.main.size(); i++) {
							if (net.minecraft.registry.Registries.ITEM.getId(inv.main.get(i).getItem()).toString().equals(want)) {
								var a = inv.main.get(i);
								inv.main.set(i, inv.main.get(sel));
								inv.main.set(sel, a);
								return;
							}
						}
						log("NOTE grab: no " + want + " in inventory");
					});
					wait = 4;
				}
				case "state" -> state(client, arg);
				case "note" -> log("NOTE " + arg);
				case "quit" -> {
					if (log != null) log.flush();
					client.scheduleStop();
				}
				default -> {
				}
			}
		}
	}

	public static Path dir(MinecraftClient client) {
		return client.runDirectory.toPath().resolve("screenshots");
	}
}
