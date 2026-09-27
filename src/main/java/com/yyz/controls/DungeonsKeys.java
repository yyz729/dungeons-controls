package com.yyz.controls;

import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class DungeonsKeys {
	public static KeyMapping TOGGLE;
	public static KeyMapping SNAP_ISO;
	public static KeyMapping SWIM_MODE;

	private DungeonsKeys() {
	}

	public static void register(RegisterKeyMappingsEvent event) {
		TOGGLE = bind("toggle", GLFW.GLFW_KEY_F4);
		SNAP_ISO = bind("snap_iso", GLFW.GLFW_KEY_HOME);
		SWIM_MODE = bind("swim_mode", GLFW.GLFW_KEY_H);
		event.register(TOGGLE);
		event.register(SNAP_ISO);
		event.register(SWIM_MODE);
	}

	public static void tick(Minecraft client) {
		if (TOGGLE == null) {
			return;
		}
		while (TOGGLE.consumeClick()) {
			DungeonsMode.toggle();
		}
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		while (SNAP_ISO.consumeClick()) {
			DungeonsCamera.snapIsometric();
		}
		while (SWIM_MODE.consumeClick()) {
			ClickController.toggleHorizontalSwim();
			if (client.player != null) {
				client.player.displayClientMessage(
						Component.translatable(ClickController.isHorizontalSwimEnabled()
								? "dungeons_controls.swim_mode.on"
								: "dungeons_controls.swim_mode.off"),
						true
				);
			}
		}
	}

	private static KeyMapping bind(String name, int key) {
		return new KeyMapping(
				"key.dungeons_controls." + name,
				InputConstants.Type.KEYSYM,
				key,
				"key.categories.dungeons_controls"
		);
	}
}
