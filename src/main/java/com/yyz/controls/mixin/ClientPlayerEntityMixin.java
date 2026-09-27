package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.yyz.controls.input.ClickController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void dungeons$faceCursor(CallbackInfo ci) {
		if (!DungeonsMode.isGameplayActive()) {
			return;
		}
		LocalPlayer player = (LocalPlayer) (Object) this;
		if (ClickController.fallbackVanillaMove(player)) {
			return;
		}

		if (player.isFallFlying()) {
			Vec3 target = ClickController.walkTarget();
			if (target == null) {
				HitResult hit = ClickController.lastHit();
				if (hit != null && hit.getType() != HitResult.Type.MISS) {
					target = hit.getLocation();
				}
			}
			if (target != null) {
				float yaw = yawTo(player, target);
				player.setYRot(yaw);
				player.setYHeadRot(yaw);
				player.setYBodyRot(yaw);
				Vec3 aim = target.subtract(player.getEyePosition());
				double len = aim.length();
				if (len > 1.0E-4) {
					player.setXRot((float) Math.toDegrees(-Math.asin(Mth.clamp(aim.y / len, -1.0, 1.0))));
				}
			}
			return;
		}

		Minecraft client = Minecraft.getInstance();
		boolean usingKey = client.options.keyUse.isDown();
		boolean aiming = usingKey || player.isUsingItem();

		Float yaw = null;
		Vec3 aimTarget = null;
		if (aiming) {
			HitResult hit = ClickController.lastHit();
			if (hit != null && hit.getType() != HitResult.Type.MISS) {
				aimTarget = hit.getLocation();
				yaw = yawTo(player, aimTarget);
			}
		}
		if (yaw == null) {
			if (!ClickController.hasDesiredYaw()) {
				return;
			}
			yaw = ClickController.desiredYaw();
		}

		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setYBodyRot(Mth.rotLerp(0.55F, player.yBodyRot, yaw));

		if (aiming && aimTarget != null) {
			// Bow, trident, snowball, potion: all resolved inside aimPitch.
			player.setXRot(ClickController.aimPitch(player));
			return;
		}

		if (player.isInWater()) {
			Vec3 target = ClickController.walkTarget();
			if (target == null) {
				HitResult hit = ClickController.lastHit();
				if (hit != null) {
					target = hit.getLocation();
				}
			}
			if (target != null) {
				Vec3 aim = target.subtract(player.getEyePosition());
				double len = aim.length();
				if (len > 1.0E-4) {
					player.setXRot((float) Math.toDegrees(-Math.asin(Mth.clamp(aim.y / len, -1.0, 1.0))));
				}
			}
			return;
		}

		player.setXRot(0.0F);
	}

	private static float yawTo(LocalPlayer player, Vec3 world) {
		double dx = world.x - player.getX();
		double dz = world.z - player.getZ();
		if (dx * dx + dz * dz < 1.0E-6) {
			return player.getYRot();
		}
		return (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
	}
}
