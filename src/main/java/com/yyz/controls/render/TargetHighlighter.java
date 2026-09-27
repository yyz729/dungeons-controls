package com.yyz.controls.render;

import com.yyz.controls.DungeonsConfig;
import com.yyz.controls.input.ClickController;
import com.yyz.controls.input.WorldPicker;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class TargetHighlighter {
	private TargetHighlighter() {
	}

	public static void render(WorldRenderContext context) {
		HitResult hit = ClickController.lastHit();
		if (hit == null || context.consumers() == null || context.matrixStack() == null) {
			return;
		}
		MatrixStack matrices = context.matrixStack();
		VertexConsumerProvider consumers = context.consumers();
		Vec3d cam = context.camera().getPos();
		VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

		matrices.push();
		matrices.translate(-cam.x, -cam.y, -cam.z);

		if (hit instanceof EntityHitResult entityHit) {
			Entity entity = entityHit.getEntity();
			Box box = entity.getBoundingBox().expand(0.05);
			float r = WorldPicker.isItem(hit) ? 0.35F : 1.0F;
			float g = WorldPicker.isItem(hit) ? 1.0F : 0.35F;
			float b = WorldPicker.isItem(hit) ? 0.45F : 0.25F;
			WorldRenderer.drawBox(matrices, lines, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, 0.95F);
		} else if (DungeonsConfig.get().showGroundMarker) {
			Vec3d p = hit.getPos();
			double s = 0.28;
			float g = hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult ? 0.85F : 0.95F;
			WorldRenderer.drawBox(matrices, lines, p.x - s, p.y + 0.02, p.z - s, p.x + s, p.y + 0.06, p.z + s, 0.95F, g, 0.25F, 0.9F);
		}

		matrices.pop();
	}
}