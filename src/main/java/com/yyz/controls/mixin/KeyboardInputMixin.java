package com.yyz.controls.mixin;

import com.yyz.controls.DungeonsConfig;
import com.yyz.controls.DungeonsMode;
import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
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
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		if (!DungeonsMode.isEnabled() || player == null || ClickController.fallbackVanillaMove(player)) {
			return;
		}

		if (DungeonsMode.isGameplayActive() && DungeonsConfig.get().replaceWasd) {
			this.pressingForward = false;
			this.pressingBack = false;
			this.pressingLeft = false;
			this.pressingRight = false;
			this.movementForward = 0.0F;
			this.movementSideways = 0.0F;

			Vec3d target = ClickController.walkTarget();
			if (target != null) {
				boolean drawingBow = player.isUsingItem()
						&& player.getActiveItem().getItem() instanceof BowItem;

				boolean aiming = drawingBow
						|| client.options.useKey.isPressed()
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
						double relative = Math.toRadians(targetYaw - player.getYaw());
						this.movementForward = (float) Math.cos(relative);
						this.movementSideways = (float) -Math.sin(relative);
						this.pressingForward = this.movementForward > 0.0F;
						this.pressingBack = this.movementForward < 0.0F;
						this.pressingLeft = this.movementSideways < 0.0F;
						this.pressingRight = this.movementSideways > 0.0F;
					} else {
						this.movementForward = 1.0F;
						this.pressingForward = true;
					}

					if (slowDown) {
						this.movementForward *= slowDownFactor;
						this.movementSideways *= slowDownFactor;
					}
					if (DungeonsConfig.get().autoSprint && dist > 1.4
							&& player.getHungerManager().getFoodLevel() > 6) {
						player.setSprinting(true);
					}
				}
			}

			if (player.getAbilities().flying && target != null
					&& !ClickController.isHorizontalSwim()) {
				double dy = target.y - player.getY();
				this.jumping = dy > 0.6;
				this.sneaking = dy < -0.6;
			}

			if (player.isTouchingWater() && target != null
					&& !ClickController.isHorizontalSwim()) {
				if (!this.jumping) {
					double dy = target.y - player.getEyeY();
					if (dy > 0.5) {
						this.jumping = true;
					}
				}
			}

			if (!this.jumping && !this.sneaking && target != null
					&& (player.isOnGround() || player.isTouchingWater())
					&& !player.getAbilities().flying
					&& (this.movementForward > 0.05F || this.movementSideways != 0.0F)) {
				if (shouldStepUp(client, player, target)) {
					this.jumping = true;
				}
			}

			return;
		}

		if (DungeonsConfig.get().cameraRelativeWasd) {
			float yawDelta = DungeonsCamera.yaw() - player.getYaw();
			Vector2f movement = new Vector2f(this.movementForward, this.movementSideways);
			movement.mul(new Matrix2f().rotate((float) Math.toRadians(-yawDelta)));
			this.movementForward = movement.x;
			this.movementSideways = movement.y;
		}
	}

	private static boolean shouldStepUp(MinecraftClient client, ClientPlayerEntity player, Vec3d target) {
		if (client.world == null) {
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

		double probe = player.isTouchingWater() ? 0.5 : 0.7;
		int probeX = MathHelper.floor(player.getX() + dx * probe);
		int probeZ = MathHelper.floor(player.getZ() + dz * probe);

		double feetY = player.getY();

		int standingBlockY = MathHelper.floor(feetY - 1.0E-3);

		double bestTop = Double.NEGATIVE_INFINITY;
		int bestBlockY = standingBlockY;
		BlockPos.Mutable pos = new BlockPos.Mutable();

		for (int dyBlock = 0; dyBlock <= 1; dyBlock++) {
			int y = standingBlockY + dyBlock;
			pos.set(probeX, y, probeZ);
			BlockState state = client.world.getBlockState(pos);
			VoxelShape shape = state.getCollisionShape(client.world, pos);
			if (shape.isEmpty()) {
				continue;
			}
			double top = y + shape.getMax(Direction.Axis.Y);
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
		if (!client.world.getBlockState(above1).getCollisionShape(client.world, above1).isEmpty()) {
			return false;
		}
		if (!client.world.getBlockState(above2).getCollisionShape(client.world, above2).isEmpty()) {
			return false;
		}

		return true;
	}
}