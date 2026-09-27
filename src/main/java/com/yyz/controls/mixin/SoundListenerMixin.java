package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.SoundListener;
import net.minecraft.client.sound.SoundListenerTransform;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(SoundListener.class)
public abstract class SoundListenerMixin {

    @ModifyVariable(
            method = "setTransform",
            at = @At("HEAD"),
            argsOnly = true
    )
    private SoundListenerTransform dungeons$listenerAtPlayer(SoundListenerTransform transform) {
        if (!DungeonsMode.isEnabled()) {
            return transform;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return transform;
        }

        float tickDelta = client.getRenderTickCounter().getTickDelta(true);
        Vec3d pos = player.getLerpedPos(tickDelta)
                .add(0.0, player.getStandingEyeHeight() * 0.9, 0.0);

        return new SoundListenerTransform(pos, transform.forward(), transform.up());
    }
}