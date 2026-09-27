package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.yyz.controls.input.ClickController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow
	@Nullable
	public ClientPlayerInteractionManager interactionManager;

	@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
	private void dungeons$noVanillaAttack(CallbackInfoReturnable<Boolean> cir) {
		if (DungeonsMode.isEnabled()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "doItemUse", at = @At("HEAD"))
	private void dungeons$aimBeforeUse(CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!DungeonsMode.isGameplayActive()) {
			return;
		}
		ClientPlayerEntity player = client.player;
		if (player == null || ClickController.fallbackVanillaMove(player)) {
			return;
		}

		HitResult hit = client.crosshairTarget;
		if (hit == null || hit.getType() == HitResult.Type.MISS) {
			return;
		}
		Vec3d target = hit.getPos();

		double dx = target.x - player.getX();
		double dz = target.z - player.getZ();
		if (dx * dx + dz * dz > 1.0E-6) {
			float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
			player.setYaw(yaw);
			player.setHeadYaw(yaw);
			player.setBodyYaw(yaw);
		}

		player.setPitch(ClickController.aimPitch(player));
	}

	@Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
	private void dungeons$breaking(boolean breaking, CallbackInfo ci) {
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		if (ClickController.isMining() && breaking) {
			return;
		}
		if (this.interactionManager != null) {
			this.interactionManager.cancelBlockBreaking();
		}
		ci.cancel();
	}
}