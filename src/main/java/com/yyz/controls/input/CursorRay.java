package com.yyz.controls.input;

import com.yyz.controls.mixin.GameRendererAccessor;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class CursorRay {
	public final Vec3 origin;
	public final Vec3 direction;
	public final Vec3 end;

	private CursorRay(Vec3 origin, Vec3 direction, double length) {
		this.origin = origin;
		this.direction = direction;
		this.end = origin.add(direction.scale(length));
	}

	public static CursorRay fromMouse(Minecraft client, float tickDelta, double length) {
		Camera camera = client.gameRenderer.getMainCamera();
		Window window = client.getWindow();

		double ndcX = (client.mouseHandler.xpos() / window.getScreenWidth()) * 2.0 - 1.0;
		double ndcY = 1.0 - (client.mouseHandler.ypos() / window.getScreenHeight()) * 2.0;

		double fov = ((GameRendererAccessor) client.gameRenderer).dungeons$getFov(camera, tickDelta, true);
		double tan = Math.tan(Math.toRadians(fov) / 2.0);
		double aspect = (double) window.getWidth() / (double) window.getHeight();

		Vector3f fwd = camera.getLookVector();
		Vector3f up = camera.getUpVector();
		Vector3f right = camera.getLeftVector();

		Vec3 direction = new Vec3(
			fwd.x() - right.x() * ndcX * tan * aspect + up.x() * ndcY * tan,
			fwd.y() - right.y() * ndcX * tan * aspect + up.y() * ndcY * tan,
			fwd.z() - right.z() * ndcX * tan * aspect + up.z() * ndcY * tan
		).normalize();

		return new CursorRay(camera.getPosition(), direction, length);
	}
}
