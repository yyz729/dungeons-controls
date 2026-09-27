package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsConfig;
import com.yyz.controls.DungeonsMode;
import com.yyz.controls.input.WorldPicker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
	MinecraftClient client;

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void dungeons$fov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
		if (DungeonsMode.isEnabled() && changingFov) {
			cir.setReturnValue((double) DungeonsConfig.get().cameraFov);
		}
	}

	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
	private void dungeons$noBob(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
		if (DungeonsMode.isEnabled()) {
			ci.cancel();
		}
	}

	@Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
	private void dungeons$cursorPick(float tickDelta, CallbackInfo ci) {
		if (!DungeonsMode.isGameplayActive() || this.client.player == null) {
			return;
		}
		this.client.crosshairTarget = WorldPicker.pick(this.client, tickDelta);
	}
}
