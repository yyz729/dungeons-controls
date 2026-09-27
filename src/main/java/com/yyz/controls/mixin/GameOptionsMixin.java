package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
	@Inject(method = "getPerspective", at = @At("HEAD"), cancellable = true)
	private void dungeons$thirdPerson(CallbackInfoReturnable<Perspective> cir) {
		if (DungeonsMode.isEnabled()) {
			cir.setReturnValue(Perspective.THIRD_PERSON_BACK);
		}
	}
}
