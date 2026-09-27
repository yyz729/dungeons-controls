package com.yyz.controls.input;

import com.yyz.controls.occlusion.OcclusionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class WorldPicker {
	private WorldPicker() {
	}

	public static HitResult pick(Minecraft client, float tickDelta) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null) {
			return BlockHitResult.miss(player != null ? player.position() : Vec3.ZERO, Direction.UP, BlockPos.ZERO);
		}

		double reach = Math.max(player.blockInteractionRange(), player.entityInteractionRange()) + 48.0;
		CursorRay ray = CursorRay.fromMouse(client, tickDelta, reach);

		EntityHitResult entityHit = pickEntity(client, player, ray);
		BlockHitResult blockHit = pickBlock(client, ray);

		if (entityHit != null) {
			if (blockHit.getType() == HitResult.Type.MISS) {
				return entityHit;
			}
			double entityDist = entityHit.getLocation().distanceToSqr(ray.origin);
			double blockDist = blockHit.getLocation().distanceToSqr(ray.origin);
			if (entityDist <= blockDist + 0.15) {
				return entityHit;
			}
		}
		return blockHit;
	}

	private static EntityHitResult pickEntity(Minecraft client, LocalPlayer player, CursorRay ray) {
		Entity vehicle = player.getVehicle();
		AABB search = new AABB(ray.origin, ray.end).inflate(1.0);
		double maxDistSq = ray.origin.distanceToSqr(ray.end);
		EntityHitResult best = ProjectileUtil.getEntityHitResult(
				player,
				ray.origin,
				ray.end,
				search,
				entity -> entity != null
						&& entity.isAlive()
						&& entity.isAttackable()
						&& !entity.isSpectator()
						&& entity != player
						&& entity != vehicle
						&& !(entity instanceof ExperienceOrb),
				maxDistSq
		);
		if (best != null) {
			return best;
		}

		EntityHitResult loose = null;
		double bestDist = maxDistSq;
		for (Entity entity : client.level.getEntities(player, search, e -> e.isAlive() && !e.isSpectator())) {
			if (entity == vehicle) {
				continue;
			}
			AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.45);
			var hit = box.clip(ray.origin, ray.end);
			if (hit.isEmpty()) {
				continue;
			}
			double dist = ray.origin.distanceToSqr(hit.get());
			if (dist < bestDist) {
				bestDist = dist;
				loose = new EntityHitResult(entity, hit.get());
			}
		}
		return loose;
	}

	private static BlockHitResult pickBlock(Minecraft client, CursorRay ray) {
		BlockHitResult hit = traverse(client.level, ray.origin, ray.end);
		if (hit.getType() != HitResult.Type.MISS) {
			return hit;
		}
		return groundPlane(client.player, ray);
	}

	private static BlockHitResult traverse(BlockGetter world, Vec3 start, Vec3 end) {
		if (start.equals(end)) {
			return BlockHitResult.miss(end, Direction.UP, BlockPos.containing(end));
		}

		double dx = end.x - start.x;
		double dy = end.y - start.y;
		double dz = end.z - start.z;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		dx /= len;
		dy /= len;
		dz /= len;

		int x = Mth.floor(start.x);
		int y = Mth.floor(start.y);
		int z = Mth.floor(start.z);

		int stepX = dx > 0.0 ? 1 : (dx < 0.0 ? -1 : 0);
		int stepY = dy > 0.0 ? 1 : (dy < 0.0 ? -1 : 0);
		int stepZ = dz > 0.0 ? 1 : (dz < 0.0 ? -1 : 0);

		double tMaxX = intBound(start.x, dx);
		double tMaxY = intBound(start.y, dy);
		double tMaxZ = intBound(start.z, dz);
		double tDeltaX = stepX == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dx);
		double tDeltaY = stepY == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dy);
		double tDeltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : 1.0 / Math.abs(dz);

		Direction last = Direction.getNearest(dx, dy, dz).getOpposite();
		int maxSteps = (int) (len * 3.0) + 8;
		for (int i = 0; i < maxSteps; i++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!OcclusionTracker.shouldHide(pos)) {
				BlockState state = world.getBlockState(pos);
				if (!state.isAir()) {
					VoxelShape shape = state.getShape(world, pos);
					BlockHitResult shapeHit = shape.clip(start, end, pos);
					if (shapeHit != null) {
						return shapeHit;
					}
				}
			}

			if (tMaxX > len && tMaxY > len && tMaxZ > len) {
				break;
			}
			if (tMaxX < tMaxY) {
				if (tMaxX < tMaxZ) {
					x += stepX;
					tMaxX += tDeltaX;
					last = stepX > 0 ? Direction.WEST : Direction.EAST;
				} else {
					z += stepZ;
					tMaxZ += tDeltaZ;
					last = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
				}
			} else if (tMaxY < tMaxZ) {
				y += stepY;
				tMaxY += tDeltaY;
				last = stepY > 0 ? Direction.DOWN : Direction.UP;
			} else {
				z += stepZ;
				tMaxZ += tDeltaZ;
				last = stepZ > 0 ? Direction.NORTH : Direction.SOUTH;
			}
		}
		return BlockHitResult.miss(end, last, BlockPos.containing(end));
	}

	private static BlockHitResult groundPlane(LocalPlayer player, CursorRay ray) {
		double planeY = player.getY();
		if (Math.abs(ray.direction.y) < 1.0E-4) {
			Vec3 p = new Vec3(player.getX(), planeY, player.getZ());
			return BlockHitResult.miss(p, Direction.UP, BlockPos.containing(p));
		}
		double t = (planeY - ray.origin.y) / ray.direction.y;
		if (t < 0.0) {
			t = (planeY + 1.0 - ray.origin.y) / ray.direction.y;
		}
		if (t < 0.0) {
			Vec3 p = player.position();
			return BlockHitResult.miss(p, Direction.UP, BlockPos.containing(p));
		}
		Vec3 point = ray.origin.add(ray.direction.scale(t));
		return BlockHitResult.miss(point, Direction.UP, BlockPos.containing(point));
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

	public static boolean isItem(HitResult hit) {
		return hit instanceof EntityHitResult e && e.getEntity() instanceof ItemEntity;
	}

	public static boolean isLivingTarget(HitResult hit) {
		return hit instanceof EntityHitResult e
				&& e.getEntity().isAlive()
				&& e.getEntity().isAttackable()
				&& !(e.getEntity() instanceof ItemEntity)
				&& !(e.getEntity() instanceof ExperienceOrb);
	}
}
