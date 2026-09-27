package com.yyz.controls.input;

import com.yyz.controls.DungeonsMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ClickController {
	private static Vec3 walkTarget;
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

	public static Vec3 walkTarget() { return walkTarget; }
	public static HitResult lastHit() { return lastHit; }
	public static boolean isMining() { return mining; }
	public static boolean hasDesiredYaw() { return hasDesiredYaw; }
	public static float desiredYaw() { return desiredYaw; }

	public static boolean isHorizontalSwim() { return depthLocked; }

	public static boolean isHorizontalSwimEnabled() { return horizontalSwim; }

	public static void toggleHorizontalSwim() {
		horizontalSwim = !horizontalSwim;
	}

	public static void tick(Minecraft client) {
		if (dodgeCooldown > 0) dodgeCooldown--;
		if (attackSwingDelay > 0) attackSwingDelay--;

		LocalPlayer player = client.player;
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

		float tickDelta = client.getTimer().getGameTimeDeltaPartialTick(true);
		HitResult hit = WorldPicker.pick(client, tickDelta);
		lastHit = hit;
		client.hitResult = hit;

		boolean inWater = player.isInWater();
		boolean elytra = player.isFallFlying();
		boolean creativeFly = player.getAbilities().flying;
		boolean clickingHigherBlock = false;
		if (inWater && hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
			BlockState state = client.level.getBlockState(bhr.getBlockPos());
			boolean fluidSurface = !state.getFluidState().isEmpty()
					&& state.getCollisionShape(client.level, bhr.getBlockPos()).isEmpty();
			if (!fluidSurface) {
				clickingHigherBlock = bhr.getLocation().y > player.getY() + 0.5;
			}
		}

		if (creativeFly) {
			depthLocked = true;
		} else if (elytra) {
			depthLocked = horizontalSwim && !clickingHigherBlock;
		} else if (inWater) {
			depthLocked = horizontalSwim && !clickingHigherBlock && player.isUnderWater();
		} else {
			depthLocked = false;
		}

		boolean lmb = client.options.keyAttack.isDown() && DungeonsMode.screenGraceTicks() <= 0;

		if (!lmb) {
			if (holdTicks > 0) {
				if (holdTicks < HOLD_THRESHOLD_TICKS && hit != null) {
					walkTarget = flatten(player, hit.getLocation());
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
		boolean rooted = client.options.keyShift.isDown();

		if (longPressWalking) {
			if (hit != null) {
				Vec3 target = hit instanceof EntityHitResult e ? e.getEntity().position() : hit.getLocation();
				walkTarget = rooted ? null : flatten(player, target);
				faceTowards(player, target);
			}
			return;
		}

		if (longPress && shouldStartLongPressWalk(client, player, hit)) {
			longPressWalking = true;
			if (hit != null) {
				walkTarget = rooted ? null : flatten(player, hit.getLocation());
				faceTowards(player, hit.getLocation());
			}
			return;
		}

		if (WorldPicker.isLivingTarget(hit)) {
			stopMining(client);
			Entity entity = ((EntityHitResult) hit).getEntity();
			if (longPress) {
				walkTarget = null;
			} else if (walkTarget == null) {
				walkTarget = rooted ? null : entity.position();
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
				walkTarget = rooted ? null : item.position();
			}
			faceTowards(player, item.position());
			return;
		}

		if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
			double distSq = player.distanceToSqr(blockHit.getLocation());
			double reach = player.blockInteractionRange();

			if (longPress) {
				if (distSq <= reach * reach
						&& !client.level.getBlockState(blockHit.getBlockPos()).isAir()) {
					mining = true;
					walkTarget = null;
					faceTowards(player, blockHit.getLocation());
					driveMining(client, player, blockHit.getBlockPos(), blockHit.getDirection());
				} else {
					stopMining(client);
					walkTarget = rooted ? null : flatten(player, blockHit.getLocation());
					faceTowards(player, walkTarget != null ? walkTarget : blockHit.getLocation());
				}
				return;
			}

			faceTowards(player, blockHit.getLocation());
			return;
		}

		stopMining(client);
		if (longPress) {
			Vec3 target = hit != null ? hit.getLocation() : player.position();
			walkTarget = rooted ? null : flatten(player, target);
			faceTowards(player, walkTarget != null ? walkTarget : target);
		} else {
			faceTowards(player, hit != null ? hit.getLocation() : player.position());
		}
	}

	private static boolean shouldStartLongPressWalk(Minecraft client, LocalPlayer player, HitResult hit) {
		if (hit instanceof EntityHitResult) {
			return false;
		}
		if (hit instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
			double distSq = player.distanceToSqr(bhr.getLocation());
			double reach = player.blockInteractionRange();
			if (distSq <= reach * reach && !client.level.getBlockState(bhr.getBlockPos()).isAir()) {
				return false;
			}
		}
		return true;
	}

	public static float aimPitch(LocalPlayer player) {
		HitResult hit = lastHit;
		if (hit == null || hit.getType() == HitResult.Type.MISS) {
			return 0.0F;
		}
		Vec3 target = hit instanceof EntityHitResult e
				? e.getEntity().getBoundingBox().getCenter()
				: hit.getLocation();

		Item active = player.getUseItem().getItem();
		Item item = active != Items.AIR ? active : player.getMainHandItem().getItem();

		Throwables.Params params = Throwables.get(item);
		if (params == null) {
			return straight(player, target);
		}

		if (Throwables.isCharged(item)) {
			if (item instanceof BowItem) {
				float pull = player.getTicksUsingItem() / 20.0F;
				pull = (pull * pull + pull * 2.0F) / 3.0F;
				if (pull > 1.0F) pull = 1.0F;
				return ballistic(player, target, pull * params.speed, params.gravity);
			}
			if (player.getTicksUsingItem() < 10) {
				return straight(player, target);
			}
			return ballistic(player, target, params.speed, params.gravity);
		}

		if (Throwables.isFixedSpeed(item)) {
			return ballistic(player, target, params.speed, params.gravity);
		}
		return straight(player, target);
	}

	public static Throwables.Params getThrowParams(LocalPlayer player) {
		Item item = player.getMainHandItem().getItem();
		if (!Throwables.isFixedSpeed(item)) {
			return null;
		}
		return Throwables.get(item);
	}

	private static float straight(LocalPlayer player, Vec3 target) {
		Vec3 aim = target.subtract(player.getEyePosition());
		double len = aim.length();
		if (len < 1.0E-4) {
			return 0.0F;
		}
		return (float) Math.toDegrees(-Math.asin(Mth.clamp(aim.y / len, -1.0, 1.0)));
	}

	private static float ballistic(LocalPlayer player, Vec3 target, double speed, double g) {
		Vec3 eye = player.getEyePosition();
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

	private static void driveMining(Minecraft client, LocalPlayer player,
									BlockPos pos, Direction side) {
		if (client.gameMode == null) return;
		client.gameMode.continueDestroyBlock(pos, side);
		player.swing(InteractionHand.MAIN_HAND);
	}

	private static void stopMining(Minecraft client) {
		if (!mining) return;
		if (client.gameMode != null) {
			client.gameMode.stopDestroyBlock();
		}
		mining = false;
	}

	public static void tryInteract(Minecraft client) {
		if (client.player == null || client.gameMode == null) return;
		if (lastHit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
			client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, blockHit);
			client.player.swing(InteractionHand.MAIN_HAND);
		} else if (lastHit instanceof EntityHitResult entityHit) {
			client.gameMode.interact(client.player, entityHit.getEntity(), InteractionHand.MAIN_HAND);
			client.player.swing(InteractionHand.MAIN_HAND);
		}
	}

	private static void tryAttack(Minecraft client, LocalPlayer player, Entity entity) {
		if (client.gameMode == null || attackSwingDelay > 0) return;
		double reach = player.entityInteractionRange();
		if (player.distanceToSqr(entity) > reach * reach) return;
		if (player.getAttackStrengthScale(0.5F) < 1.0F) return;
		client.gameMode.attack(player, entity);
		player.swing(InteractionHand.MAIN_HAND);
		attackSwingDelay = 4;
	}

	private static void faceHit(LocalPlayer player, HitResult hit) {
		if (hit == null) {
			hasDesiredYaw = false;
			return;
		}
		Vec3 pos = hit instanceof EntityHitResult e
				? e.getEntity().getBoundingBox().getCenter()
				: hit.getLocation();
		faceTowards(player, pos);
	}

	private static void faceTowards(LocalPlayer player, Vec3 world) {
		double dx = world.x - player.getX();
		double dz = world.z - player.getZ();
		if (dx * dx + dz * dz < 1.0E-6) return;
		desiredYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
		hasDesiredYaw = true;
	}

	private static Vec3 flatten(LocalPlayer player, Vec3 pos) {
		if (player.getAbilities().flying && depthLocked) {
			return new Vec3(pos.x, player.getEyeY(), pos.z);
		}
		if (player.isFallFlying()) {
			return new Vec3(pos.x, pos.y, pos.z);
		}
		boolean onSolidGround = !player.isInWater();
		boolean lockY = onSolidGround || depthLocked;
		return new Vec3(pos.x, lockY ? player.getY() : pos.y, pos.z);
	}

	public static boolean fallbackVanillaMove(LocalPlayer player) {
		return player.onClimbable();
	}
}
