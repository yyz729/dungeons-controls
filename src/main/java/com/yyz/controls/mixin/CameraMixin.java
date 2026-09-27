package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
	private void dungeons$keepDistance(float f, CallbackInfoReturnable<Float> cir) {
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player != null && ClickController.fallbackVanillaMove(client.player)) {
			return;
		}
		cir.setReturnValue(f);
	}

	@Inject(method = "setup", at = @At("RETURN"))
	private void dungeons$apply(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		if (!DungeonsMode.isEnabled() || focusedEntity == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client.player != null && ClickController.fallbackVanillaMove(client.player)) {
			return;
		}
		Vec3 raw = focusedEntity.getPosition(tickDelta).add(0.0, focusedEntity.getEyeHeight() * 0.55, 0.0);
		Vec3 focus = DungeonsCamera.smoothFocus(raw);
		Vec3 pos = focus.add(DungeonsCamera.offset());
		this.setRotation(DungeonsCamera.yaw(), DungeonsCamera.pitch());
		this.setPosition(pos.x, pos.y, pos.z);
	}
}
