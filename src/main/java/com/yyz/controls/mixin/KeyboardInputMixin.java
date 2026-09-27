package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsConfig;
import com.yyz.controls.DungeonsMode;
import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
	@Inject(method = "tick", at = @At("TAIL"))
	private void dungeons$clickMove(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (!DungeonsMode.isEnabled() || player == null || ClickController.fallbackVanillaMove(player)) {
			return;
		}

		if (DungeonsMode.isGameplayActive() && DungeonsConfig.get().replaceWasd) {
			this.up = false;
			this.down = false;
			this.left = false;
			this.right = false;
			this.forwardImpulse = 0.0F;
			this.leftImpulse = 0.0F;

			Vec3 target = ClickController.walkTarget();
			if (target != null) {
				boolean drawingBow = player.isUsingItem()
						&& player.getUseItem().getItem() instanceof BowItem;
				boolean aiming = drawingBow
						|| client.options.keyUse.isDown()
						|| player.isUsingItem();

				double dx = target.x - player.getX();
				double dz = target.z - player.getZ();
				double dy = target.y - player.getY();
				double distSq = dx * dx + dz * dz;

				boolean arrived = distSq < 0.16;
				if (!ClickController.isHorizontalSwim()) {
					arrived = arrived && Math.abs(dy) < 0.5;
				}

				if (!arrived) {
					double dist = Math.sqrt(distSq);
					if (aiming) {
						double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
						double relative = Math.toRadians(targetYaw - player.getYRot());
						this.forwardImpulse = (float) Math.cos(relative);
						this.leftImpulse = (float) -Math.sin(relative);
						this.up = this.forwardImpulse > 0.0F;
						this.down = this.forwardImpulse < 0.0F;
						this.left = this.leftImpulse < 0.0F;
						this.right = this.leftImpulse > 0.0F;
					} else {
						this.forwardImpulse = 1.0F;
						this.up = true;
					}

					if (slowDown) {
						this.forwardImpulse *= slowDownFactor;
						this.leftImpulse *= slowDownFactor;
					}
					if (DungeonsConfig.get().autoSprint && dist > 1.4
							&& player.getFoodData().getFoodLevel() > 6) {
						player.setSprinting(true);
					}
				}
			}

			if (player.getAbilities().flying && target != null
					&& !ClickController.isHorizontalSwim()) {
				double dy = target.y - player.getY();
				this.jumping = dy > 0.6;
				this.shiftKeyDown = dy < -0.6;
			}

			if (player.isInWater() && target != null
					&& !ClickController.isHorizontalSwim()) {
				if (!this.jumping) {
					double dy = target.y - player.getEyeY();
					if (dy > 0.5) {
						this.jumping = true;
					}
				}
			}

			if (!this.jumping && !this.shiftKeyDown && target != null
					&& (player.onGround() || player.isInWater())
					&& !player.getAbilities().flying
					&& (this.forwardImpulse > 0.05F || this.leftImpulse != 0.0F)) {
				if (shouldStepUp(client, player, target)) {
					this.jumping = true;
				}
			}

			return;
		}

		if (DungeonsConfig.get().cameraRelativeWasd) {
			float yawDelta = DungeonsCamera.yaw() - player.getYRot();
			Vector2f movement = new Vector2f(this.forwardImpulse, this.leftImpulse);
			movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yawDelta)));
			this.forwardImpulse = movement.x;
			this.leftImpulse = movement.y;
		}
	}

	private static boolean shouldStepUp(Minecraft client, LocalPlayer player, Vec3 target) {
		if (client.level == null) {
			return false;
		}
		double dx = target.x - player.getX();
		double dz = target.z - player.getZ();
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 0.4) {
			return false;
		}
		dx /= len;
		dz /= len;

		double probe = player.isInWater() ? 0.5 : 0.7;
		int probeX = Mth.floor(player.getX() + dx * probe);
		int probeZ = Mth.floor(player.getZ() + dz * probe);

		double feetY = player.getY();

		int standingBlockY = Mth.floor(feetY - 1.0E-3);

		double bestTop = Double.NEGATIVE_INFINITY;
		int bestBlockY = standingBlockY;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int dyBlock = 0; dyBlock <= 1; dyBlock++) {
			int y = standingBlockY + dyBlock;
			pos.set(probeX, y, probeZ);
			BlockState state = client.level.getBlockState(pos);
			VoxelShape shape = state.getCollisionShape(client.level, pos);
			if (shape.isEmpty()) {
				continue;
			}
			double top = y + shape.max(Direction.Axis.Y);
			if (top > bestTop) {
				bestTop = top;
				bestBlockY = y;
			}
		}

		if (bestTop == Double.NEGATIVE_INFINITY) {
			return false;
		}

		if (bestTop - feetY < 0.6) {
			return false;
		}

		BlockPos above1 = new BlockPos(probeX, bestBlockY + 1, probeZ);
		BlockPos above2 = new BlockPos(probeX, bestBlockY + 2, probeZ);
		if (!client.level.getBlockState(above1).getCollisionShape(client.level, above1).isEmpty()) {
			return false;
		}
		if (!client.level.getBlockState(above2).getCollisionShape(client.level, above2).isEmpty()) {
			return false;
		}

		return true;
	}
}
