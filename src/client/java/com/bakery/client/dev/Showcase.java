package com.bakery.client.dev;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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
 * gui catalog|guide|menu   open a screen; key left|right|up|down [n]   press keys on it; close
 * quit
 * </pre>
 */
public final class Showcase {
	private static final Deque<String> STEPS = new ArrayDeque<>();
	private static int wait = 100;
	private static boolean loaded;

	private Showcase() {
	}

	public static void init() {
		String script = System.getProperty("bakery.showcase");
		if (script == null) return;
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client, script));
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
				case "quit" -> client.scheduleStop();
				default -> {
				}
			}
		}
	}

	public static Path dir(MinecraftClient client) {
		return client.runDirectory.toPath().resolve("screenshots");
	}
}
