package com.yyz.controls.input;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TridentItem;

import java.util.HashMap;
import java.util.Map;

public final class Throwables {
	public static final class Params {
		public final double speed;
		public final double gravity;

		public Params(double speed, double gravity) {
			this.speed = speed;
			this.gravity = gravity;
		}
	}

	private static final Map<Item, Params> PROJECTILES = new HashMap<>();
	private static boolean bootstrapped;

	private Throwables() {
	}

	public static void bootstrap() {
		if (bootstrapped) {
			return;
		}
		bootstrapped = true;

		register(Items.BOW, 3.0, 0.05);
		register(Items.TRIDENT, 2.5, 0.05);

		register(Items.SNOWBALL, 1.5, 0.03);
		register(Items.EGG, 1.5, 0.03);
		register(Items.ENDER_PEARL, 1.5, 0.03);
		register(Items.POTION, 0.5, 0.03);
		register(Items.SPLASH_POTION, 0.5, 0.03);
		register(Items.LINGERING_POTION, 0.5, 0.03);
		register(Items.EXPERIENCE_BOTTLE, 0.7, 0.03);

		register(Items.WIND_CHARGE, 1.5, 0.03);
	}

	public static void register(Item item, double speed, double gravity) {
		if (item == null) {
			return;
		}
		PROJECTILES.put(item, new Params(speed, gravity));
	}

	public static void unregister(Item item) {
		if (item != null) {
			PROJECTILES.remove(item);
		}
	}

	public static Params get(Item item) {
		if (item == null) {
			return null;
		}
		return PROJECTILES.get(item);
	}

	public static boolean isCharged(Item item) {
		return item instanceof BowItem || item instanceof TridentItem;
	}

	public static boolean isFixedSpeed(Item item) {
		return item instanceof SnowballItem
				|| item instanceof EggItem
				|| item instanceof EnderpearlItem
				|| item instanceof PotionItem
				|| item instanceof ExperienceBottleItem;
	}
}
