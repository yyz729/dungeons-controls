package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityRenderDistanceMixin {

    @Inject(method = "shouldRender(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void dungeons$playerCentricRender(double cameraX, double cameraY, double cameraZ,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!DungeonsMode.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (self == player) {
            return;
        }

        float tickDelta = client.getRenderTickCounter().getTickDelta(true);
        Vec3d p = player.getLerpedPos(tickDelta);

        double d = self.getBoundingBox().getAverageSideLength();
        if (Double.isNaN(d)) {
            d = 1.0;
        }
        d *= 64.0;

        double distSq = self.squaredDistanceTo(p.x, p.y, p.z);
        cir.setReturnValue(distSq < d * d);
    }
}