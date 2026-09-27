package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private boolean mouseGrabbed;

	@Shadow
	private double xpos;

	@Shadow
	private double ypos;

	@Shadow
	private boolean ignoreFirstMove;

	@Shadow
	private double accumulatedDX;

	@Shadow
	private double accumulatedDY;

	@Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
	private void dungeons$lock(CallbackInfo ci) {
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		ci.cancel();
		if (!this.minecraft.isWindowActive()) {
			return;
		}
		if (!Minecraft.ON_OSX) {
			KeyMapping.setAll();
		}
		this.mouseGrabbed = true;
		InputConstants.grabOrReleaseMouse(
				this.minecraft.getWindow().getWindow(),
				GLFW.GLFW_CURSOR_CAPTURED,
				this.xpos, this.ypos
		);
		this.ignoreFirstMove = true;
	}

	@Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
	private void dungeons$unlock(CallbackInfo ci) {
		if (!DungeonsMode.isEnabled() || !this.mouseGrabbed) {
			return;
		}
		ci.cancel();
		this.mouseGrabbed = false;
		InputConstants.grabOrReleaseMouse(
				this.minecraft.getWindow().getWindow(),
				GLFW.GLFW_CURSOR_NORMAL,
				this.xpos, this.ypos
		);
	}

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void dungeons$noLook(double timeDelta, CallbackInfo ci) {
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		if (ClickController.fallbackVanillaMove(player)) {
			return;
		}
		if (DungeonsCamera.isOrbiting() && this.minecraft.screen == null) {
			DungeonsCamera.addOrbit((float) this.accumulatedDX, (float) this.accumulatedDY);
		}
		this.accumulatedDX = 0.0;
		this.accumulatedDY = 0.0;
		ci.cancel();
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void dungeons$zoom(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (!DungeonsMode.isGameplayActive()) {
			return;
		}
		LocalPlayer player = this.minecraft.player;
		if (player != null && ClickController.fallbackVanillaMove(player)) {
			return;
		}
		DungeonsCamera.addZoom((float) -vertical);
		ci.cancel();
	}
}
