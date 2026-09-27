package com.yyz.controls.camera;

import com.yyz.controls.DungeonsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DungeonsCamera {
	private static float yaw = 45.0F;
	private static float pitch = 50.0F;
	private static float distance = 12.0F;
	private static Vec3 smoothedFocus = Vec3.ZERO;
	private static boolean orbiting;

	private DungeonsCamera() {
	}

	public static void onEnabled() {
		DungeonsConfig cfg = DungeonsConfig.get();
		yaw = cfg.cameraYaw;
		pitch = cfg.cameraPitch;
		distance = cfg.cameraDistance;
		smoothedFocus = Vec3.ZERO;
	}

	public static void tick(Minecraft client) {
		orbiting = client.options.keyPickItem.isDown()
				|| client.mouseHandler.isMiddlePressed();
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
		yaw = Mth.wrapDegrees(yaw + dx * sens);
		pitch = Mth.clamp(pitch + dy * sens * 0.35F, 20.0F, 80.0F);
	}

	public static void addZoom(float scroll) {
		DungeonsConfig cfg = DungeonsConfig.get();
		distance = Mth.clamp(distance + scroll * 0.85F, cfg.minDistance, cfg.maxDistance);
	}

	public static void rotateBy(float degrees) {
		yaw = Mth.wrapDegrees(yaw + degrees);
	}

	public static void snapIsometric() {
		DungeonsConfig cfg = DungeonsConfig.get();
		float[] snaps = {45.0F, 135.0F, 225.0F, 315.0F};
		float wrapped = Mth.wrapDegrees(yaw);
		if (wrapped < 0.0F) {
			wrapped += 360.0F;
		}
		float best = snaps[0];
		float bestDist = Float.MAX_VALUE;
		for (float snap : snaps) {
			float d = Math.abs(Mth.wrapDegrees(wrapped - snap));
			if (d < bestDist) {
				bestDist = d;
				best = snap;
			}
		}
		yaw = best;
		pitch = cfg.cameraPitch;
	}

	public static Vec3 smoothFocus(Vec3 rawFocus) {
		if (smoothedFocus == Vec3.ZERO) {
			smoothedFocus = rawFocus;
			return rawFocus;
		}
		float t = Mth.clamp(DungeonsConfig.get().cameraSmooth, 0.05F, 1.0F);
		smoothedFocus = smoothedFocus.lerp(rawFocus, t);
		return smoothedFocus;
	}

	public static Vec3 offset() {
		return Vec3.directionFromRotation(pitch, yaw).scale(-distance);
	}

	public static Vec3 lookVector() {
		return Vec3.directionFromRotation(pitch, yaw);
	}

	public static Vec3 horizontalLook() {
		Vec3 look = Vec3.directionFromRotation(0.0F, yaw);
		double len = Math.sqrt(look.x * look.x + look.z * look.z);
		if (len < 1.0E-6) {
			return new Vec3(0.0, 0.0, 1.0);
		}
		return new Vec3(look.x / len, 0.0, look.z / len);
	}
}
