package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Listener.class)
public abstract class SoundListenerMixin {

	@ModifyVariable(
			method = "setTransform",
			at = @At("HEAD"),
			argsOnly = true
	)
	private ListenerTransform dungeons$listenerAtPlayer(ListenerTransform transform) {
		if (!DungeonsMode.isEnabled()) {
			return transform;
		}
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return transform;
		}

		float tickDelta = client.getTimer().getGameTimeDeltaPartialTick(true);
		Vec3 pos = player.getPosition(tickDelta)
				.add(0.0, player.getEyeHeight() * 0.9, 0.0);

		return new ListenerTransform(pos, transform.forward(), transform.up());
	}
}
