package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) {
			return;
		}
		Entity self = (Entity) (Object) this;
		if (self == player) {
			return;
		}

		float tickDelta = client.getTimer().getGameTimeDeltaPartialTick(true);
		Vec3 p = player.getPosition(tickDelta);

		double d = self.getBoundingBox().getSize();
		if (Double.isNaN(d)) {
			d = 1.0;
		}
		d *= 64.0;

		double distSq = self.distanceToSqr(p.x, p.y, p.z);
		cir.setReturnValue(distSq < d * d);
	}
}
