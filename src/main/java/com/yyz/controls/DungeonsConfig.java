package com.yyz.controls;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DungeonsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static DungeonsConfig instance = new DungeonsConfig();

	public boolean enableOnWorldJoin = true;
	public boolean replaceWasd = true;
	public boolean cameraRelativeWasd = true;
	public boolean autoSprint = true;

	public float cameraPitch = 50.0F;
	public float cameraYaw = 45.0F;
	public float cameraDistance = 12.0F;
	public float minDistance = 4.0F;
	public float maxDistance = 24.0F;
	public float cameraFov = 50.0F;
	public float cameraSmooth = 0.28F;
	public float orbitSensitivity = 0.22F;
	public boolean orbitInvertX = false;
	public boolean orbitInvertY = false;

	public boolean fadeOccluders = true;
	public float ghostAlpha = 0.10F;
	public int ghostMaxBlocks = 160;
	public int occlusionHoldTicks = 4;
	public int sectionsPerTick = 8;

	public boolean hideCrosshair = true;
	public boolean showGroundMarker = true;

	public static DungeonsConfig get() {
		return instance;
	}

	public static Path path() {
		return FMLPaths.CONFIGDIR.get().resolve("dungeons_controls.json");
	}

	public static void load() {
		Path file = path();
		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				DungeonsConfig loaded = GSON.fromJson(reader, DungeonsConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (Exception e) {
				DungeonsControls.LOGGER.warn("Failed to read config, using defaults", e);
			}
		}
		save();
	}

	public static void save() {
		Path file = path();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			DungeonsControls.LOGGER.warn("Failed to write config", e);
		}
	}
}
