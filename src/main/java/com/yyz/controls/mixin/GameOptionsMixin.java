package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class GameOptionsMixin {
	@Inject(method = "getCameraType", at = @At("HEAD"), cancellable = true)
	private void dungeons$thirdPerson(CallbackInfoReturnable<CameraType> cir) {
		if (DungeonsMode.isEnabled()) {
			cir.setReturnValue(CameraType.THIRD_PERSON_BACK);
		}
	}
}
