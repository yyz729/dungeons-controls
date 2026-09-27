package com.yyz.controls.occlusion;

import com.yyz.controls.DungeonsConfig;
import com.yyz.controls.DungeonsMode;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

public final class OcclusionTracker {
    private static final LongSet hidden = new LongOpenHashSet();
    private static final Long2IntOpenHashMap hold = new Long2IntOpenHashMap();
    private static final LongSet dirtySections = new LongOpenHashSet();
    private static boolean lastEnabled;

    private static final double[] PLAYER_RAY_HEIGHTS = {0.15, 0.6, 1.0, 1.4, 1.75};
    private static final double PLAYER_BODY_RADIUS = 0.35;
    private static final int PLAYER_RAY_DIRECTIONS = 8;

    private static final int EXPAND_RADIUS = 3;
    private static final int MAX_HIDDEN = 3000;

    private OcclusionTracker() {
    }

    public static boolean shouldHide(BlockPos pos) {
        return DungeonsMode.isEnabled() && DungeonsConfig.get().fadeOccluders && hidden.contains(pos.asLong());
    }

    public static LongSet hiddenView() {
        return hidden;
    }

    public static void reset() {
        hidden.clear();
        hold.clear();
        dirtySections.clear();
        lastEnabled = false;
    }

    public static void tick(MinecraftClient client) {
        boolean enabled = DungeonsMode.isEnabled() && DungeonsConfig.get().fadeOccluders;
        if (!enabled) {
            if (lastEnabled) {
                forceRebuildAll(client);
            }
            lastEnabled = false;
            return;
        }
        lastEnabled = true;

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            return;
        }

        int minBlockY = MathHelper.floor(player.getBoundingBox().maxY) + 1;

        LongSet seeds = new LongOpenHashSet();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();

        collectCameraToPlayerRays(world, cam, player, minBlockY, seeds);

        LongSet next = new LongOpenHashSet(seeds);
        expandLocal(world, seeds, minBlockY, next);

        int holdTicks = Math.max(1, DungeonsConfig.get().occlusionHoldTicks);
        LongSet gone = new LongOpenHashSet(hidden);
        gone.removeAll(next);

        LongIterator goneIt = gone.iterator();
        while (goneIt.hasNext()) {
            long key = goneIt.nextLong();
            int left = hold.get(key) - 1;
            if (left <= 0) {
                hold.remove(key);
                hidden.remove(key);
                markSection(key);
            } else {
                hold.put(key, left);
            }
        }

        LongIterator addIt = next.iterator();
        while (addIt.hasNext()) {
            long key = addIt.nextLong();
            hold.put(key, holdTicks);
            if (hidden.add(key)) {
                markSection(key);
            }
        }

        flushDirty(client);
    }

    public static void forceRebuildAll(MinecraftClient client) {
        if (client.worldRenderer == null) {
            hidden.clear();
            hold.clear();
            dirtySections.clear();
            return;
        }
        LongIterator it = hidden.iterator();
        while (it.hasNext()) {
            markSection(it.nextLong());
        }
        hidden.clear();
        hold.clear();
        flushDirty(client);
    }

    private static void collectCameraToPlayerRays(BlockView world, Vec3d cam, ClientPlayerEntity player,
                                                  int minBlockY, LongSet out) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        collectRay(world, cam, new Vec3d(px, py + 0.9, pz), player, minBlockY, out);

        for (double h : PLAYER_RAY_HEIGHTS) {
            double y = py + h;
            for (int a = 0; a < PLAYER_RAY_DIRECTIONS; a++) {
                double angle = a * (Math.PI * 2.0 / PLAYER_RAY_DIRECTIONS);
                Vec3d target = new Vec3d(
                        px + Math.cos(angle) * PLAYER_BODY_RADIUS,
                        y,
                        pz + Math.sin(angle) * PLAYER_BODY_RADIUS
                );
                collectRay(world, cam, target, player, minBlockY, out);
            }
        }
    }

    private static void collectRay(BlockView world, Vec3d start, Vec3d end,
                                   ClientPlayerEntity player, int minBlockY, LongSet out) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.05) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;

        int x = MathHelper.floor(start.x);
        int y = MathHelper.floor(start.y);
        int z = MathHelper.floor(start.z);
        int stepX = dx > 0.0 ? 1 : (dx < 0.0 ? -1 : 0);
        int stepY = dy > 0.0 ? 1 : (dy < 0.0 ? -1 : 0);
        int stepZ = dz > 0.0 ? 1 : (dz < 0.0 ? -1 : 0);
        double tMaxX = intBound(start.x, dx);
        double tMaxY = intBound(start.y, dy);
        double tMaxZ = intBound(start.z, dz);
        double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dx);
        double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dy);
        double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dz);

        Box playerBox = player.getBoundingBox().expand(0.05);
        int max = (int) (len * 3.0) + 4;
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int i = 0; i < max; i++) {
            if (playerBox.contains(x + 0.5, y + 0.5, z + 0.5)) {
                return;
            }

            if (y >= minBlockY) {
                pos.set(x, y, z);
                if (canOcclude(world.getBlockState(pos))) {
                    out.add(BlockPos.asLong(x, y, z));
                }
            }

            if (tMaxX > len && tMaxY > len && tMaxZ > len) {
                return;
            }
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
        }
    }

    private static void expandLocal(BlockView world, LongSet seeds, int minBlockY, LongSet out) {
        if (seeds.isEmpty()) {
            return;
        }
        BlockPos.Mutable pos = new BlockPos.Mutable();
        LongSet visited = new LongOpenHashSet();
        LongSet frontier = new LongOpenHashSet(seeds);

        for (int depth = 0; depth < EXPAND_RADIUS && !frontier.isEmpty(); depth++) {
            LongSet next = new LongOpenHashSet();
            LongIterator it = frontier.iterator();
            while (it.hasNext() && out.size() < MAX_HIDDEN) {
                long key = it.nextLong();
                if (!visited.add(key)) {
                    continue;
                }
                int x = BlockPos.unpackLongX(key);
                int y = BlockPos.unpackLongY(key);
                int z = BlockPos.unpackLongZ(key);

                addIfSolid(world, out, next, pos, x + 1, y, z, minBlockY);
                addIfSolid(world, out, next, pos, x - 1, y, z, minBlockY);
                addIfSolid(world, out, next, pos, x, y + 1, z, minBlockY);
                addIfSolid(world, out, next, pos, x, y - 1, z, minBlockY);
                addIfSolid(world, out, next, pos, x, y, z + 1, minBlockY);
                addIfSolid(world, out, next, pos, x, y, z - 1, minBlockY);
            }
            frontier = next;
        }
    }

    private static void addIfSolid(BlockView world, LongSet out, LongSet next, BlockPos.Mutable pos,
                                   int x, int y, int z, int minBlockY) {
        if (y < minBlockY) {
            return;
        }
        pos.set(x, y, z);
        if (!canOcclude(world.getBlockState(pos))) {
            return;
        }
        long key = pos.asLong();
        if (out.add(key)) {
            next.add(key);
        }
    }

    private static boolean canOcclude(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (!state.getFluidState().isEmpty() && !state.isOpaque()) {
            return false;
        }
        if (state.isOpaque()) {
            return true;
        }
        return !state.getOutlineShape(net.minecraft.world.EmptyBlockView.INSTANCE, BlockPos.ORIGIN).isEmpty();
    }

    private static double intBound(double s, double ds) {
        if (ds == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        double target = ds > 0.0 ? Math.floor(s + 1.0) : Math.floor(s);
        if (ds < 0.0 && target == s) {
            target -= 1.0;
        }
        return (target - s) / ds;
    }

    private static void markSection(long blockKey) {
        int x = BlockPos.unpackLongX(blockKey);
        int y = BlockPos.unpackLongY(blockKey);
        int z = BlockPos.unpackLongZ(blockKey);
        dirtySections.add(ChunkSectionPos.asLong(x >> 4, y >> 4, z >> 4));
    }

    private static void flushDirty(MinecraftClient client) {
        if (dirtySections.isEmpty() || client.worldRenderer == null) {
            return;
        }
        int budget = Math.max(1, DungeonsConfig.get().sectionsPerTick);
        LongIterator it = dirtySections.iterator();
        int n = 0;
        while (it.hasNext() && n < budget) {
            long sec = it.nextLong();
            it.remove();
            int sx = ChunkSectionPos.unpackX(sec);
            int sy = ChunkSectionPos.unpackY(sec);
            int sz = ChunkSectionPos.unpackZ(sec);
            int x0 = sx << 4;
            int y0 = sy << 4;
            int z0 = sz << 4;
            client.worldRenderer.scheduleBlockRenders(x0, y0, z0, x0 + 15, y0 + 15, z0 + 15);
            n++;
        }
    }
}