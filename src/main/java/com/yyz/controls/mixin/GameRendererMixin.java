package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsConfig;
import com.yyz.controls.DungeonsMode;
import com.yyz.controls.input.WorldPicker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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
	Minecraft minecraft;

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void dungeons$fov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
		if (DungeonsMode.isEnabled() && changingFov) {
			cir.setReturnValue((double) DungeonsConfig.get().cameraFov);
		}
	}

	@Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
	private void dungeons$noBob(PoseStack matrices, float tickDelta, CallbackInfo ci) {
		if (DungeonsMode.isEnabled()) {
			ci.cancel();
		}
	}

	@Inject(method = "pick(F)V", at = @At("TAIL"))
	private void dungeons$cursorPick(float tickDelta, CallbackInfo ci) {
		if (!DungeonsMode.isGameplayActive() || this.minecraft.player == null) {
			return;
		}
		this.minecraft.hitResult = WorldPicker.pick(this.minecraft, tickDelta);
	}
}
