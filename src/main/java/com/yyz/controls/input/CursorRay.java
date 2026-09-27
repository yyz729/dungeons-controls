package com.yyz.controls.input;

import com.yyz.controls.mixin.GameRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public final class CursorRay {
	public final Vec3d origin;
	public final Vec3d direction;
	public final Vec3d end;

	private CursorRay(Vec3d origin, Vec3d direction, double length) {
		this.origin = origin;
		this.direction = direction;
		this.end = origin.add(direction.multiply(length));
	}

	public static CursorRay fromMouse(MinecraftClient client, float tickDelta, double length) {
		Camera camera = client.gameRenderer.getCamera();
		Window window = client.getWindow();

		double ndcX = (client.mouse.getX() / window.getWidth()) * 2.0 - 1.0;
		double ndcY = 1.0 - (client.mouse.getY() / window.getHeight()) * 2.0;

		double fov = ((GameRendererAccessor) client.gameRenderer).dungeons$getFov(camera, tickDelta, true);
		double tan = Math.tan(Math.toRadians(fov) / 2.0);
		double aspect = (double) window.getFramebufferWidth() / (double) window.getFramebufferHeight();

		Vector3f fwd = camera.getHorizontalPlane();
		Vector3f up = camera.getVerticalPlane();
		Vector3f right = camera.getDiagonalPlane();

		Vec3d direction = new Vec3d(
			fwd.x() - right.x() * ndcX * tan * aspect + up.x() * ndcY * tan,
			fwd.y() - right.y() * ndcX * tan * aspect + up.y() * ndcY * tan,
			fwd.z() - right.z() * ndcX * tan * aspect + up.z() * ndcY * tan
		).normalize();

		return new CursorRay(camera.getPos(), direction, length);
	}
}
