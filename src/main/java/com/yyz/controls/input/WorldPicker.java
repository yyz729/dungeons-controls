package com.yyz.controls.input;

import com.yyz.controls.occlusion.OcclusionTracker;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

public final class WorldPicker {
	private WorldPicker() {
	}

	public static HitResult pick(MinecraftClient client, float tickDelta) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.world == null) {
			return BlockHitResult.createMissed(player != null ? player.getPos() : Vec3d.ZERO, Direction.UP, BlockPos.ORIGIN);
		}

		double reach = Math.max(player.getBlockInteractionRange(), player.getEntityInteractionRange()) + 48.0;
		CursorRay ray = CursorRay.fromMouse(client, tickDelta, reach);

		EntityHitResult entityHit = pickEntity(client, player, ray);
		BlockHitResult blockHit = pickBlock(client, ray);

		if (entityHit != null) {
			if (blockHit.getType() == HitResult.Type.MISS) {
				return entityHit;
			}
			double entityDist = entityHit.getPos().squaredDistanceTo(ray.origin);
			double blockDist = blockHit.getPos().squaredDistanceTo(ray.origin);
			if (entityDist <= blockDist + 0.15) {
				return entityHit;
			}
		}
		return blockHit;
	}

	private static EntityHitResult pickEntity(MinecraftClient client, ClientPlayerEntity player, CursorRay ray) {
		Entity vehicle = player.getVehicle();
		Box search = new Box(ray.origin, ray.end).expand(1.0);
		double maxDistSq = ray.origin.squaredDistanceTo(ray.end);
		EntityHitResult best = ProjectileUtil.raycast(
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
						&& !(entity instanceof ExperienceOrbEntity),
				maxDistSq
		);
		if (best != null) {
			return best;
		}

		EntityHitResult loose = null;
		double bestDist = maxDistSq;
		for (Entity entity : client.world.getOtherEntities(player, search, e -> e.isAlive() && !e.isSpectator())) {
			if (entity == vehicle) {
				continue;
			}
			Box box = entity.getBoundingBox().expand(entity.getTargetingMargin() + 0.45);
			var hit = box.raycast(ray.origin, ray.end);
			if (hit.isEmpty()) {
				continue;
			}
			double dist = ray.origin.squaredDistanceTo(hit.get());
			if (dist < bestDist) {
				bestDist = dist;
				loose = new EntityHitResult(entity, hit.get());
			}
		}
		return loose;
	}

	private static BlockHitResult pickBlock(MinecraftClient client, CursorRay ray) {
		BlockHitResult hit = traverse(client.world, ray.origin, ray.end);
		if (hit.getType() != HitResult.Type.MISS) {
			return hit;
		}
		return groundPlane(client.player, ray);
	}

	private static BlockHitResult traverse(BlockView world, Vec3d start, Vec3d end) {
		if (start.equals(end)) {
			return BlockHitResult.createMissed(end, Direction.UP, BlockPos.ofFloored(end));
		}

		double dx = end.x - start.x;
		double dy = end.y - start.y;
		double dz = end.z - start.z;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
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

		Direction last = Direction.getFacing(dx, dy, dz).getOpposite();
		int maxSteps = (int) (len * 3.0) + 8;
		for (int i = 0; i < maxSteps; i++) {
			BlockPos pos = new BlockPos(x, y, z);
			if (!OcclusionTracker.shouldHide(pos)) {
				BlockState state = world.getBlockState(pos);
				if (!state.isAir()) {
					VoxelShape shape = state.getOutlineShape(world, pos);
					BlockHitResult shapeHit = shape.raycast(start, end, pos);
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
		return BlockHitResult.createMissed(end, last, BlockPos.ofFloored(end));
	}

	private static BlockHitResult groundPlane(ClientPlayerEntity player, CursorRay ray) {
		double planeY = player.getY();
		if (Math.abs(ray.direction.y) < 1.0E-4) {
			Vec3d p = new Vec3d(player.getX(), planeY, player.getZ());
			return BlockHitResult.createMissed(p, Direction.UP, BlockPos.ofFloored(p));
		}
		double t = (planeY - ray.origin.y) / ray.direction.y;
		if (t < 0.0) {
			t = (planeY + 1.0 - ray.origin.y) / ray.direction.y;
		}
		if (t < 0.0) {
			Vec3d p = player.getPos();
			return BlockHitResult.createMissed(p, Direction.UP, BlockPos.ofFloored(p));
		}
		Vec3d point = ray.origin.add(ray.direction.multiply(t));
		return BlockHitResult.createMissed(point, Direction.UP, BlockPos.ofFloored(point));
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
				&& !(e.getEntity() instanceof ExperienceOrbEntity);
	}
}