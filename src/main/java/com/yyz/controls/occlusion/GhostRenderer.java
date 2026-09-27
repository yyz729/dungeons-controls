package com.yyz.controls.occlusion;

import com.yyz.controls.DungeonsConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class GhostRenderer {
    private GhostRenderer() {
    }

    public static void render(WorldRenderContext context) {
        if (!DungeonsConfig.get().fadeOccluders) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || OcclusionTracker.hiddenView().isEmpty()) {
            return;
        }

        Camera camera = context.camera();
        Vec3d cam = camera.getPos();
        float maxAlpha = MathHelper.clamp(DungeonsConfig.get().ghostAlpha, 0.08F, 0.85F);
        int cap = Math.max(16, DungeonsConfig.get().ghostMaxBlocks);

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        int a = MathHelper.clamp((int) (maxAlpha * 255.0F), 20, 200);

        RenderSystem.disableBlend();
        RenderSystem.colorMask(false, false, false, false);

        BufferBuilder depthBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        LongIterator depthIt = OcclusionTracker.hiddenView().iterator();
        int depthDrawn = 0;
        BlockPos.Mutable depthPos = new BlockPos.Mutable();
        while (depthIt.hasNext() && depthDrawn < cap) {
            long key = depthIt.nextLong();
            depthPos.set(BlockPos.unpackLongX(key), BlockPos.unpackLongY(key), BlockPos.unpackLongZ(key));
            if (world.getBlockState(depthPos).isAir()) {
                continue;
            }
            cube(depthBuf, matrix, depthPos.getX(), depthPos.getY(), depthPos.getZ(), 255, 255, 255, 255);
            depthDrawn++;
        }
        BuiltBuffer depthBuilt = depthBuf.endNullable();
        if (depthBuilt != null) {
            BufferRenderer.drawWithGlobalProgram(depthBuilt);
        }

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder colorBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        LongIterator colorIt = OcclusionTracker.hiddenView().iterator();
        int colorDrawn = 0;
        BlockPos.Mutable colorPos = new BlockPos.Mutable();
        while (colorIt.hasNext() && colorDrawn < cap) {
            long key = colorIt.nextLong();
            colorPos.set(BlockPos.unpackLongX(key), BlockPos.unpackLongY(key), BlockPos.unpackLongZ(key));
            if (world.getBlockState(colorPos).isAir()) {
                continue;
            }
            cube(colorBuf, matrix, colorPos.getX(), colorPos.getY(), colorPos.getZ(), 0xDD, 0xDD, 0xDD, a);
            colorDrawn++;
        }
        BuiltBuffer colorBuilt = colorBuf.endNullable();
        if (colorBuilt != null) {
            BufferRenderer.drawWithGlobalProgram(colorBuilt);
        }

        matrices.pop();
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
        buffer.vertex(m, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(m, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(m, x3, y3, z3).color(r, g, b, a);
        buffer.vertex(m, x4, y4, z4).color(r, g, b, a);
    }
}