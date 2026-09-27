package com.yyz.controls;

import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import com.yyz.controls.occlusion.OcclusionTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

public final class DungeonsMode {
	private static boolean enabled;
	private static boolean worldReady;
	private static Perspective savedPerspective = Perspective.FIRST_PERSON;
	private static int screenGraceTicks;

	private DungeonsMode() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static boolean isGameplayActive() {
		MinecraftClient client = MinecraftClient.getInstance();
		return enabled
				&& client.player != null
				&& client.world != null
				&& client.currentScreen == null
				&& !client.player.isDead();
	}

	public static int screenGraceTicks() {
		return screenGraceTicks;
	}

	public static void onWorldTick(MinecraftClient client) {
		if (!worldReady) {
			worldReady = true;
			if (DungeonsConfig.get().enableOnWorldJoin && !enabled) {
				setEnabled(true);
			}
		}
		if (enabled) {
			client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
		}
		if (client.currentScreen != null) {
			screenGraceTicks = 6;
		} else if (screenGraceTicks > 0) {
			screenGraceTicks--;
		}
	}

	public static void onLeftWorld() {
		worldReady = false;
		ClickController.reset();
		OcclusionTracker.reset();
	}

	public static void toggle() {
		setEnabled(!enabled);
	}

	public static void setEnabled(boolean value) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (enabled == value) {
			refreshCursor(client);
			return;
		}

		if (value) {
			savedPerspective = client.options.getPerspective();
			enabled = true;
			DungeonsCamera.onEnabled();
			client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
		} else {
			enabled = false;
			client.options.setPerspective(savedPerspective);
			OcclusionTracker.forceRebuildAll(client);
			ClickController.reset();
		}

		refreshCursor(client);
	}

	private static void refreshCursor(MinecraftClient client) {
		if (client.mouse == null || client.getWindow() == null) {
			return;
		}
		if (client.currentScreen == null && client.world != null) {
			client.mouse.lockCursor();
		}
	}
}