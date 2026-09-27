package com.yyz.controls.input;

import com.yyz.controls.DungeonsMode;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class ClickController {
	private static Vec3d walkTarget;
	private static boolean mining;
	private static int dodgeCooldown;
	private static float desiredYaw;
	private static boolean hasDesiredYaw;
	private static HitResult lastHit;
	private static int attackSwingDelay;
	private static int holdTicks;
	private static boolean longPressWalking;

	private static boolean horizontalSwim;
	private static boolean depthLocked;

	private static final int HOLD_THRESHOLD_TICKS = 6;

	private ClickController() {
	}

	public static void reset() {
		walkTarget = null;
		mining = false;
		hasDesiredYaw = false;
		lastHit = null;
		holdTicks = 0;
		longPressWalking = false;
		depthLocked = false;
	}

	public static Vec3d walkTarget() { return walkTarget; }
	public static HitResult lastHit() { return lastHit; }
	public static boolean isMining() { return mining; }
	public static boolean hasDesiredYaw() { return hasDesiredYaw; }
	public static float desiredYaw() { return desiredYaw; }

	public static boolean isHorizontalSwim() { return depthLocked; }

	public static boolean isHorizontalSwimEnabled() { return horizontalSwim; }

	public static void toggleHorizontalSwim() {
		horizontalSwim = !horizontalSwim;
	}

	public static void tick(MinecraftClient client) {
		if (dodgeCooldown > 0) dodgeCooldown--;
		if (attackSwingDelay > 0) attackSwingDelay--;

		ClientPlayerEntity player = client.player;
		if (!DungeonsMode.isGameplayActive() || player == null || fallbackVanillaMove(player)) {
			stopMining(client);
			walkTarget = null;
			hasDesiredYaw = false;
			holdTicks = 0;
			longPressWalking = false;
			depthLocked = false;
			return;
		}

		if (walkTarget != null) {
			double dx = walkTarget.x - player.getX();
			double dz = walkTarget.z - player.getZ();
			if (dx * dx + dz * dz < 0.36) {
				walkTarget = null;
			}
		}

		float tickDelta = client.getRenderTickCounter().getTickDelta(true);
		HitResult hit = WorldPicker.pick(client, tickDelta);
		lastHit = hit;
		client.crosshairTarget = hit;

		boolean inWater = player.isTouchingWater();
		boolean elytra = player.isFallFlying();
		boolean creativeFly = player.getAbilities().flying;
		boolean clickingHigherBlock = false;
		if (inWater && hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
			BlockState state = client.world.getBlockState(bhr.getBlockPos());
			boolean fluidSurface = !state.getFluidState().isEmpty()
					&& state.getCollisionShape(client.world, bhr.getBlockPos()).isEmpty();
			if (!fluidSurface) {
				clickingHigherBlock = bhr.getPos().y > player.getY() + 0.5;
			}
		}

		if (creativeFly) {
			depthLocked = true;
		} else if (elytra) {
			depthLocked = horizontalSwim && !clickingHigherBlock;
		} else if (inWater) {
			depthLocked = horizontalSwim && !clickingHigherBlock && player.isSubmergedInWater();
		} else {
			depthLocked = false;
		}

		boolean lmb = client.options.attackKey.isPressed() && DungeonsMode.screenGraceTicks() <= 0;

		if (!lmb) {
			if (holdTicks > 0) {
				if (holdTicks < HOLD_THRESHOLD_TICKS && hit != null) {
					walkTarget = flatten(player, hit.getPos());
				}
			}
			holdTicks = 0;
			longPressWalking = false;
			stopMining(client);

			if (walkTarget != null) {
				faceTowards(player, walkTarget);
			} else {
				faceHit(player, hit);
			}
			return;
		}

		holdTicks++;
		boolean longPress = holdTicks >= HOLD_THRESHOLD_TICKS;
		boolean rooted = client.options.sneakKey.isPressed();

		if (longPressWalking) {
			if (hit != null) {
				Vec3d target = hit instanceof EntityHitResult e ? e.getEntity().getPos() : hit.getPos();
				walkTarget = rooted ? null : flatten(player, target);
				faceTowards(player, target);
			}
			return;
		}

		if (longPress && shouldStartLongPressWalk(client, player, hit)) {
			longPressWalking = true;
			if (hit != null) {
				walkTarget = rooted ? null : flatten(player, hit.getPos());
				faceTowards(player, hit.getPos());
			}
			return;
		}

		if (WorldPicker.isLivingTarget(hit)) {
			stopMining(client);
			Entity entity = ((EntityHitResult) hit).getEntity();
			if (longPress) {
				walkTarget = null;
			} else if (walkTarget == null) {
				walkTarget = rooted ? null : entity.getPos();
			}
			faceTowards(player, entity.getBoundingBox().getCenter());
			tryAttack(client, player, entity);
			return;
		}

		if (WorldPicker.isItem(hit)) {
			stopMining(client);
			Entity item = ((EntityHitResult) hit).getEntity();
			if (longPress) {
				walkTarget = null;
			} else if (walkTarget == null) {
				walkTarget = rooted ? null : item.getPos();
			}
			faceTowards(player, item.getPos());
			return;
		}

		if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
			double distSq = player.squaredDistanceTo(blockHit.getPos());
			double reach = player.getBlockInteractionRange();

			if (longPress) {
				if (distSq <= reach * reach
						&& !client.world.getBlockState(blockHit.getBlockPos()).isAir()) {
					mining = true;
					walkTarget = null;
					faceTowards(player, blockHit.getPos());
					driveMining(client, player, blockHit.getBlockPos(), blockHit.getSide());
				} else {
					stopMining(client);
					walkTarget = rooted ? null : flatten(player, blockHit.getPos());
					faceTowards(player, walkTarget != null ? walkTarget : blockHit.getPos());
				}
				return;
			}

			faceTowards(player, blockHit.getPos());
			return;
		}

		stopMining(client);
		if (longPress) {
			Vec3d target = hit != null ? hit.getPos() : player.getPos();
			walkTarget = rooted ? null : flatten(player, target);
			faceTowards(player, walkTarget != null ? walkTarget : target);
		} else {
			faceTowards(player, hit != null ? hit.getPos() : player.getPos());
		}
	}

	private static boolean shouldStartLongPressWalk(MinecraftClient client, ClientPlayerEntity player, HitResult hit) {
		if (hit instanceof EntityHitResult) {
			return false;
		}
		if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
			double distSq = player.squaredDistanceTo(bhr.getPos());
			double reach = player.getBlockInteractionRange();
			if (distSq <= reach * reach && !client.world.getBlockState(bhr.getBlockPos()).isAir()) {
				return false;
			}
		}
		return true;
	}

	public static float aimPitch(ClientPlayerEntity player) {
		HitResult hit = lastHit;
		if (hit == null || hit.getType() == HitResult.Type.MISS) {
			return 0.0F;
		}
		Vec3d target = hit instanceof EntityHitResult e
				? e.getEntity().getBoundingBox().getCenter()
				: hit.getPos();

		Item active = player.getActiveItem().getItem();
		Item item = active != Items.AIR ? active : player.getMainHandStack().getItem();

		Throwables.Params params = Throwables.get(item);
		if (params == null) {
			return straight(player, target);
		}

		if (Throwables.isCharged(item)) {
			if (item instanceof BowItem) {
				float pull = player.getItemUseTime() / 20.0F;
				pull = (pull * pull + pull * 2.0F) / 3.0F;
				if (pull > 1.0F) pull = 1.0F;
				return ballistic(player, target, pull * params.speed, params.gravity);
			}
			if (player.getItemUseTime() < 10) {
				return straight(player, target);
			}
			return ballistic(player, target, params.speed, params.gravity);
		}

		if (Throwables.isFixedSpeed(item)) {
			return ballistic(player, target, params.speed, params.gravity);
		}
		return straight(player, target);
	}

	public static Throwables.Params getThrowParams(ClientPlayerEntity player) {
		Item item = player.getMainHandStack().getItem();
		if (!Throwables.isFixedSpeed(item)) {
			return null;
		}
		return Throwables.get(item);
	}

	private static float straight(ClientPlayerEntity player, Vec3d target) {
		Vec3d aim = target.subtract(player.getEyePos());
		double len = aim.length();
		if (len < 1.0E-4) {
			return 0.0F;
		}
		return (float) Math.toDegrees(-Math.asin(MathHelper.clamp(aim.y / len, -1.0, 1.0)));
	}

	private static float ballistic(ClientPlayerEntity player, Vec3d target, double speed, double g) {
		Vec3d eye = player.getEyePos();
		double dx = target.x - eye.x;
		double dy = target.y - eye.y;
		double dz = target.z - eye.z;
		double dist = Math.sqrt(dx * dx + dz * dz);
		if (dist < 0.05 || speed < 0.1) {
			return 0.0F;
		}

		double k = g * dist * dist / (2.0 * speed * speed);
		double disc = dist * dist - 4.0 * k * (dy + k);
		if (disc < 0.0) {
			return (float) -Math.toDegrees(Math.atan2(dy, dist));
		}
		double u = (dist - Math.sqrt(disc)) / (2.0 * k);
		return (float) -Math.toDegrees(Math.atan(u));
	}

	private static void driveMining(MinecraftClient client, ClientPlayerEntity player,
									BlockPos pos, Direction side) {
		if (client.interactionManager == null) return;
		client.interactionManager.updateBlockBreakingProgress(pos, side);
		player.swingHand(Hand.MAIN_HAND);
	}

	private static void stopMining(MinecraftClient client) {
		if (!mining) return;
		if (client.interactionManager != null) {
			client.interactionManager.cancelBlockBreaking();
		}
		mining = false;
	}

	public static void tryInteract(MinecraftClient client) {
		if (client.player == null || client.interactionManager == null) return;
		if (lastHit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
			client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, blockHit);
			client.player.swingHand(Hand.MAIN_HAND);
		} else if (lastHit instanceof EntityHitResult entityHit) {
			client.interactionManager.interactEntity(client.player, entityHit.getEntity(), Hand.MAIN_HAND);
			client.player.swingHand(Hand.MAIN_HAND);
		}
	}

	private static void tryAttack(MinecraftClient client, ClientPlayerEntity player, Entity entity) {
		if (client.interactionManager == null || attackSwingDelay > 0) return;
		double reach = player.getEntityInteractionRange();
		if (player.squaredDistanceTo(entity) > reach * reach) return;
		if (player.getAttackCooldownProgress(0.5F) < 1.0F) return;
		client.interactionManager.attackEntity(player, entity);
		player.swingHand(Hand.MAIN_HAND);
		attackSwingDelay = 4;
	}

	private static void faceHit(ClientPlayerEntity player, HitResult hit) {
		if (hit == null) {
			hasDesiredYaw = false;
			return;
		}
		Vec3d pos = hit instanceof EntityHitResult e
				? e.getEntity().getBoundingBox().getCenter()
				: hit.getPos();
		faceTowards(player, pos);
	}

	private static void faceTowards(ClientPlayerEntity player, Vec3d world) {
		double dx = world.x - player.getX();
		double dz = world.z - player.getZ();
		if (dx * dx + dz * dz < 1.0E-6) return;
		desiredYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
		hasDesiredYaw = true;
	}

	private static Vec3d flatten(ClientPlayerEntity player, Vec3d pos) {
		if (player.getAbilities().flying && depthLocked) {
			return new Vec3d(pos.x, player.getEyeY(), pos.z);
		}
		if (player.isFallFlying()) {
			return new Vec3d(pos.x, pos.y, pos.z);
		}
		boolean onSolidGround = !player.isTouchingWater();
		boolean lockY = onSolidGround || depthLocked;
		return new Vec3d(pos.x, lockY ? player.getY() : pos.y, pos.z);
	}

	public static boolean fallbackVanillaMove(ClientPlayerEntity player) {
		return player.isClimbing();
	}
}