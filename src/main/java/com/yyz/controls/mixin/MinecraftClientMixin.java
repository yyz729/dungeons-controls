package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.yyz.controls.input.ClickController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
	@Shadow
	@Nullable
	public MultiPlayerGameMode gameMode;

	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void dungeons$noVanillaAttack(CallbackInfoReturnable<Boolean> cir) {
		if (DungeonsMode.isEnabled()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "startUseItem", at = @At("HEAD"))
	private void dungeons$aimBeforeUse(CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		if (!DungeonsMode.isGameplayActive()) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || ClickController.fallbackVanillaMove(player)) {
			return;
		}

		HitResult hit = client.hitResult;
		if (hit == null || hit.getType() == HitResult.Type.MISS) {
			return;
		}
		Vec3 target = hit.getLocation();

		double dx = target.x - player.getX();
		double dz = target.z - player.getZ();
		if (dx * dx + dz * dz > 1.0E-6) {
			float yaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
			player.setYRot(yaw);
			player.setYHeadRot(yaw);
			player.setYBodyRot(yaw);
		}

		player.setXRot(ClickController.aimPitch(player));
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void dungeons$breaking(boolean breaking, CallbackInfo ci) {
		if (!DungeonsMode.isEnabled()) {
			return;
		}
		if (ClickController.isMining() && breaking) {
			return;
		}
		if (this.gameMode != null) {
			this.gameMode.stopDestroyBlock();
		}
		ci.cancel();
	}
}
