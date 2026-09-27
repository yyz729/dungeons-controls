package com.yyz.controls.render;

import com.yyz.controls.DungeonsConfig;
import com.yyz.controls.input.ClickController;
import com.yyz.controls.input.WorldPicker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class TargetHighlighter {
	private TargetHighlighter() {
	}

	public static void render(RenderLevelStageEvent event) {
		HitResult hit = ClickController.lastHit();
		if (hit == null) {
			return;
		}
		PoseStack matrices = event.getPoseStack();
		if (matrices == null) {
			return;
		}
		MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
		Vec3 cam = event.getCamera().getPosition();
		VertexConsumer lines = consumers.getBuffer(RenderType.lines());

		matrices.pushPose();
		matrices.translate(-cam.x, -cam.y, -cam.z);

		if (hit instanceof EntityHitResult entityHit) {
			Entity entity = entityHit.getEntity();
			AABB box = entity.getBoundingBox().inflate(0.05);
			float r = WorldPicker.isItem(hit) ? 0.35F : 1.0F;
			float g = WorldPicker.isItem(hit) ? 1.0F : 0.35F;
			float b = WorldPicker.isItem(hit) ? 0.45F : 0.25F;
			LevelRenderer.renderLineBox(matrices, lines, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, 0.95F);
		} else if (DungeonsConfig.get().showGroundMarker) {
			Vec3 p = hit.getLocation();
			double s = 0.28;
			float g = hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult ? 0.85F : 0.95F;
			LevelRenderer.renderLineBox(matrices, lines, p.x - s, p.y + 0.02, p.z - s, p.x + s, p.y + 0.06, p.z + s, 0.95F, g, 0.25F, 0.9F);
		}

		matrices.popPose();
		consumers.endBatch(RenderType.lines());
	}
}
