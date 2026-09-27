package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.yyz.controls.input.ClickController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void dungeons$faceCursor(CallbackInfo ci) {
		if (!DungeonsMode.isGameplayActive()) {
			return;
		}
		ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
		if (ClickController.fallbackVanillaMove(player)) {
			return;
		}

		if (player.isFallFlying()) {
			Vec3d target = ClickController.walkTarget();
			if (target == null) {
				HitResult hit = ClickController.lastHit();
				if (hit != null && hit.getType() != HitResult.Type.MISS) {
					target = hit.getPos();
				}
			}
			if (target != null) {
				float yaw = yawTo(player, target);
				player.setYaw(yaw);
				player.setHeadYaw(yaw);
				player.setBodyYaw(yaw);
				Vec3d aim = target.subtract(player.getEyePos());
				double len = aim.length();
				if (len > 1.0E-4) {
					player.setPitch((float) Math.toDegrees(-Math.asin(MathHelper.clamp(aim.y / len, -1.0, 1.0))));
				}
			}
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		boolean usingKey = client.options.useKey.isPressed();
		boolean aiming = usingKey || player.isUsingItem();

		Float yaw = null;
		Vec3d aimTarget = null;
		if (aiming) {
			HitResult hit = ClickController.lastHit();
			if (hit != null && hit.getType() != HitResult.Type.MISS) {
				aimTarget = hit.getPos();
				yaw = yawTo(player, aimTarget);
			}
		}
		if (yaw == null) {
			if (!ClickController.hasDesiredYaw()) {
				return;
			}
			yaw = ClickController.desiredYaw();
		}

		player.setYaw(yaw);
		player.setHeadYaw(yaw);
		player.setBodyYaw(MathHelper.lerpAngleDegrees(0.55F, player.getBodyYaw(), yaw));

		if (aiming && aimTarget != null) {
			// Bow, trident, snowball, potion: all resolved inside aimPitch.
			player.setPitch(ClickController.aimPitch(player));
			return;
		}

		if (player.isTouchingWater()) {
			Vec3d target = ClickController.walkTarget();
			if (target == null) {
				HitResult hit = ClickController.lastHit();
				if (hit != null) {
					target = hit.getPos();
				}
			}
			if (target != null) {
				Vec3d aim = target.subtract(player.getEyePos());
				double len = aim.length();
				if (len > 1.0E-4) {
					player.setPitch((float) Math.toDegrees(-Math.asin(MathHelper.clamp(aim.y / len, -1.0, 1.0))));
				}
			}
			return;
		}

		player.setPitch(0.0F);
	}

	private static float yawTo(ClientPlayerEntity player, Vec3d world) {
		double dx = world.x - player.getX();
		double dz = world.z - player.getZ();
		if (dx * dx + dz * dz < 1.0E-6) {
			return player.getYaw();
		}
		return (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
	}
}