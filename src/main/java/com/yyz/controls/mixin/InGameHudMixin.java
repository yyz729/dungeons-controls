package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsConfig;
import com.yyz.controls.DungeonsMode;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

	@Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
	private void dungeons$hideCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
		if (DungeonsMode.isEnabled() && DungeonsConfig.get().hideCrosshair) {
			ci.cancel();
		}
	}
}
