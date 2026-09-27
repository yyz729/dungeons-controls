package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
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
	protected abstract void setPos(double x, double y, double z);

	@Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
	private void dungeons$keepDistance(float f, CallbackInfoReturnable<Float> cir) {
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null && ClickController.fallbackVanillaMove(client.player)) {
			return;
		}
		cir.setReturnValue(f);
	}

	@Inject(method = "update", at = @At("RETURN"))
	private void dungeons$apply(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
		if (!DungeonsMode.isEnabled() || focusedEntity == null) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null && ClickController.fallbackVanillaMove(client.player)) {
			return;
		}
		Vec3d raw = focusedEntity.getLerpedPos(tickDelta).add(0.0, focusedEntity.getStandingEyeHeight() * 0.55, 0.0);
		Vec3d focus = DungeonsCamera.smoothFocus(raw);
		Vec3d pos = focus.add(DungeonsCamera.offset());
		this.setRotation(DungeonsCamera.yaw(), DungeonsCamera.pitch());
		this.setPos(pos.x, pos.y, pos.z);
	}
}