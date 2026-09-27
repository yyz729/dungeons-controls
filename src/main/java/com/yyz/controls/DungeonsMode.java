package com.yyz.controls;

import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import com.yyz.controls.occlusion.OcclusionTracker;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class DungeonsMode {
	private static boolean enabled;
	private static boolean worldReady;
	private static CameraType savedPerspective = CameraType.FIRST_PERSON;
	private static int screenGraceTicks;

	private DungeonsMode() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static boolean isGameplayActive() {
		Minecraft client = Minecraft.getInstance();
		return enabled
				&& client.player != null
				&& client.level != null
				&& client.screen == null
				&& !client.player.isDeadOrDying();
	}

	public static int screenGraceTicks() {
		return screenGraceTicks;
	}

	public static void onWorldTick(Minecraft client) {
		if (!worldReady) {
			worldReady = true;
			if (DungeonsConfig.get().enableOnWorldJoin && !enabled) {
				setEnabled(true);
			}
		}
		if (enabled) {
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
		if (client.screen != null) {
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
		Minecraft client = Minecraft.getInstance();
		if (enabled == value) {
			refreshCursor(client);
			return;
		}

		if (value) {
			savedPerspective = client.options.getCameraType();
			enabled = true;
			DungeonsCamera.onEnabled();
			client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		} else {
			enabled = false;
			client.options.setCameraType(savedPerspective);
			OcclusionTracker.forceRebuildAll(client);
			ClickController.reset();
		}

		refreshCursor(client);
	}

	private static void refreshCursor(Minecraft client) {
		if (client.mouseHandler == null || client.getWindow() == null) {
			return;
		}
		if (client.screen == null && client.level != null) {
			client.mouseHandler.grabMouse();
		}
	}
}
