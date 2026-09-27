package com.yyz.controls.occlusion;

import com.yyz.controls.DungeonsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class GhostRenderer {
	private GhostRenderer() {
	}

	public static void render(RenderLevelStageEvent event) {
		if (!DungeonsConfig.get().fadeOccluders) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		ClientLevel world = client.level;
		if (world == null || OcclusionTracker.hiddenView().isEmpty()) {
			return;
		}

		Camera camera = event.getCamera();
		Vec3 cam = camera.getPosition();
		float maxAlpha = Mth.clamp(DungeonsConfig.get().ghostAlpha, 0.08F, 0.85F);
		int cap = Math.max(16, DungeonsConfig.get().ghostMaxBlocks);

		PoseStack matrices = event.getPoseStack();
		if (matrices == null) {
			return;
		}
		matrices.pushPose();
		matrices.translate(-cam.x, -cam.y, -cam.z);
		Matrix4f matrix = matrices.last().pose();

		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		int a = Mth.clamp((int) (maxAlpha * 255.0F), 20, 200);

		RenderSystem.disableBlend();
		RenderSystem.colorMask(false, false, false, false);

		BufferBuilder depthBuf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		LongIterator depthIt = OcclusionTracker.hiddenView().iterator();
		int depthDrawn = 0;
		BlockPos.MutableBlockPos depthPos = new BlockPos.MutableBlockPos();
		while (depthIt.hasNext() && depthDrawn < cap) {
			long key = depthIt.nextLong();
			depthPos.set(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
			if (world.getBlockState(depthPos).isAir()) {
				continue;
			}
			cube(depthBuf, matrix, depthPos.getX(), depthPos.getY(), depthPos.getZ(), 255, 255, 255, 255);
			depthDrawn++;
		}
		MeshData depthBuilt = depthBuf.build();
		if (depthBuilt != null) {
			BufferUploader.drawWithShader(depthBuilt);
		}

		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		BufferBuilder colorBuf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		LongIterator colorIt = OcclusionTracker.hiddenView().iterator();
		int colorDrawn = 0;
		BlockPos.MutableBlockPos colorPos = new BlockPos.MutableBlockPos();
		while (colorIt.hasNext() && colorDrawn < cap) {
			long key = colorIt.nextLong();
			colorPos.set(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
			if (world.getBlockState(colorPos).isAir()) {
				continue;
			}
			cube(colorBuf, matrix, colorPos.getX(), colorPos.getY(), colorPos.getZ(), 0xDD, 0xDD, 0xDD, a);
			colorDrawn++;
		}
		MeshData colorBuilt = colorBuf.build();
		if (colorBuilt != null) {
			BufferUploader.drawWithShader(colorBuilt);
		}

		matrices.popPose();
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void cube(BufferBuilder buffer, Matrix4f m, int x, int y, int z, int r, int g, int b, int a) {
		float x1 = x;
		float y1 = y;
		float z1 = z;
		float x2 = x + 1.0F;
		float y2 = y + 1.0F;
		float z2 = z + 1.0F;
		quad(buffer, m, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, a);
		quad(buffer, m, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, r, g, b, a);
		quad(buffer, m, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, r, g, b, a);
		quad(buffer, m, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, a);
		quad(buffer, m, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, a);
		quad(buffer, m, x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1, r, g, b, a);
	}

	private static void quad(BufferBuilder buffer, Matrix4f m,
							 float x1, float y1, float z1,
							 float x2, float y2, float z2,
							 float x3, float y3, float z3,
							 float x4, float y4, float z4,
							 int r, int g, int b, int a) {
		buffer.addVertex(m, x1, y1, z1).setColor(r, g, b, a);
		buffer.addVertex(m, x2, y2, z2).setColor(r, g, b, a);
		buffer.addVertex(m, x3, y3, z3).setColor(r, g, b, a);
		buffer.addVertex(m, x4, y4, z4).setColor(r, g, b, a);
	}
}
