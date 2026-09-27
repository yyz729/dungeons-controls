package com.yyz.controls.camera;

import com.yyz.controls.DungeonsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class DungeonsCamera {
	private static float yaw = 45.0F;
	private static float pitch = 50.0F;
	private static float distance = 12.0F;
	private static Vec3d smoothedFocus = Vec3d.ZERO;
	private static boolean orbiting;

	private DungeonsCamera() {
	}

	public static void onEnabled() {
		DungeonsConfig cfg = DungeonsConfig.get();
		yaw = cfg.cameraYaw;
		pitch = cfg.cameraPitch;
		distance = cfg.cameraDistance;
		smoothedFocus = Vec3d.ZERO;
	}

	public static void tick(MinecraftClient client) {
		orbiting = client.options.pickItemKey.isPressed()
				|| client.mouse.wasMiddleButtonClicked();
	}

	public static boolean isOrbiting() {
		return orbiting;
	}

	public static float yaw() {
		return yaw;
	}

	public static float pitch() {
		return pitch;
	}

	public static float distance() {
		return distance;
	}

	public static void addOrbit(float dx, float dy) {
		DungeonsConfig cfg = DungeonsConfig.get();
		float sens = cfg.orbitSensitivity;
		if (cfg.orbitInvertX) {
			dx = -dx;
		}
		if (cfg.orbitInvertY) {
			dy = -dy;
		}
		yaw = MathHelper.wrapDegrees(yaw + dx * sens);
		pitch = MathHelper.clamp(pitch + dy * sens * 0.35F, 20.0F, 80.0F);
	}

	public static void addZoom(float scroll) {
		DungeonsConfig cfg = DungeonsConfig.get();
		distance = MathHelper.clamp(distance + scroll * 0.85F, cfg.minDistance, cfg.maxDistance);
	}

	public static void rotateBy(float degrees) {
		yaw = MathHelper.wrapDegrees(yaw + degrees);
	}

	public static void snapIsometric() {
		DungeonsConfig cfg = DungeonsConfig.get();
		float[] snaps = {45.0F, 135.0F, 225.0F, 315.0F};
		float wrapped = MathHelper.wrapDegrees(yaw);
		if (wrapped < 0.0F) {
			wrapped += 360.0F;
		}
		float best = snaps[0];
		float bestDist = Float.MAX_VALUE;
		for (float snap : snaps) {
			float d = Math.abs(MathHelper.wrapDegrees(wrapped - snap));
			if (d < bestDist) {
				bestDist = d;
				best = snap;
			}
		}
		yaw = best;
		pitch = cfg.cameraPitch;
	}

	public static Vec3d smoothFocus(Vec3d rawFocus) {
		if (smoothedFocus == Vec3d.ZERO) {
			smoothedFocus = rawFocus;
			return rawFocus;
		}
		float t = MathHelper.clamp(DungeonsConfig.get().cameraSmooth, 0.05F, 1.0F);
		smoothedFocus = smoothedFocus.lerp(rawFocus, t);
		return smoothedFocus;
	}

	public static Vec3d offset() {
		return Vec3d.fromPolar(pitch, yaw).multiply(-distance);
	}

	public static Vec3d lookVector() {
		return Vec3d.fromPolar(pitch, yaw);
	}

	public static Vec3d horizontalLook() {
		Vec3d look = Vec3d.fromPolar(0.0F, yaw);
		double len = Math.sqrt(look.x * look.x + look.z * look.z);
		if (len < 1.0E-6) {
			return new Vec3d(0.0, 0.0, 1.0);
		}
		return new Vec3d(look.x / len, 0.0, look.z / len);
	}
}