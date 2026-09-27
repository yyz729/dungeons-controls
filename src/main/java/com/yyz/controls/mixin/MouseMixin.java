package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	private boolean cursorLocked;

	@Shadow
	private double x;

	@Shadow
	private double y;

	@Shadow
	private boolean hasResolutionChanged;

	@Shadow
	private double cursorDeltaX;

	@Shadow
	private double cursorDeltaY;

	@Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
	private void dungeons$lock(CallbackInfo ci) {
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		ci.cancel();
		if (!this.client.isWindowFocused()) {
			return;
		}
		if (!MinecraftClient.IS_SYSTEM_MAC) {
			KeyBinding.updatePressedStates();
		}
		this.cursorLocked = true;

		InputUtil.setCursorParameters(
				this.client.getWindow().getHandle(),
				GLFW.GLFW_CURSOR_CAPTURED,
				this.x, this.y
		);
		this.hasResolutionChanged = true;
	}

	@Inject(method = "unlockCursor", at = @At("HEAD"), cancellable = true)
	private void dungeons$unlock(CallbackInfo ci) {
		if (!DungeonsMode.isEnabled() || !this.cursorLocked) {
			return;
		}
		ci.cancel();
		this.cursorLocked = false;

		InputUtil.setCursorParameters(
				this.client.getWindow().getHandle(),
				GLFW.GLFW_CURSOR_NORMAL,
				this.x, this.y
		);
	}

	@Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
	private void dungeons$noLook(double timeDelta, CallbackInfo ci) {
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		ClientPlayerEntity player = this.client.player;
		if (player == null) {
			return;
		}
		if (ClickController.fallbackVanillaMove(player)) {
			return;
		}
		if (DungeonsCamera.isOrbiting() && this.client.currentScreen == null) {
			DungeonsCamera.addOrbit((float) this.cursorDeltaX, (float) this.cursorDeltaY);
		}
		this.cursorDeltaX = 0.0;
		this.cursorDeltaY = 0.0;
		ci.cancel();
	}

	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
	private void dungeons$zoom(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (!DungeonsMode.isGameplayActive()) {
			return;
		}
		ClientPlayerEntity player = this.client.player;
		if (player != null && ClickController.fallbackVanillaMove(player)) {
			return;
		}
		DungeonsCamera.addZoom((float) -vertical);
		ci.cancel();
	}
}