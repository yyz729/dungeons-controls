package com.yyz.controls;

import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class DungeonsKeys {
	public static KeyBinding TOGGLE;
	public static KeyBinding SNAP_ISO;
	public static KeyBinding SWIM_MODE;

	private DungeonsKeys() {
	}

	public static void register() {
		TOGGLE = bind("toggle", GLFW.GLFW_KEY_F4);
		SNAP_ISO = bind("snap_iso", GLFW.GLFW_KEY_HOME);
		SWIM_MODE = bind("swim_mode", GLFW.GLFW_KEY_H);
	}

	public static void tick(MinecraftClient client) {
		while (TOGGLE.wasPressed()) {
			DungeonsMode.toggle();
		}
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		while (SNAP_ISO.wasPressed()) {
			DungeonsCamera.snapIsometric();
		}
		while (SWIM_MODE.wasPressed()) {
			ClickController.toggleHorizontalSwim();
			if (client.player != null) {
				client.player.sendMessage(
						Text.translatable(ClickController.isHorizontalSwimEnabled()
								? "dungeons_controls.swim_mode.on"
								: "dungeons_controls.swim_mode.off"),
						true
				);
			}
		}
	}

	private static KeyBinding bind(String name, int key) {
		return KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.dungeons_controls." + name,
				InputUtil.Type.KEYSYM,
				key,
				"key.categories.dungeons_controls"
		));
	}
}