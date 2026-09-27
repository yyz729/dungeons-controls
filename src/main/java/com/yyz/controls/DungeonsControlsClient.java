package com.yyz.controls;

import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import com.yyz.controls.input.Throwables;
import com.yyz.controls.occlusion.GhostRenderer;
import com.yyz.controls.occlusion.OcclusionTracker;
import com.yyz.controls.render.TargetHighlighter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DungeonsControlsClient implements ClientModInitializer {
	public static final String MOD_ID = "dungeons_controls";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		DungeonsConfig.load();
		Throwables.bootstrap();
		DungeonsKeys.register();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.world == null || client.player == null) {
				DungeonsMode.onLeftWorld();
				return;
			}

			DungeonsMode.onWorldTick(client);
			DungeonsKeys.tick(client);
			DungeonsCamera.tick(client);
			ClickController.tick(client);
			OcclusionTracker.tick(client);
		});

		WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
			if (!DungeonsMode.isGameplayActive()) {
				return;
			}
			GhostRenderer.render(context);
			TargetHighlighter.render(context);
		});

		LOGGER.info("Dungeons Controls ready");
	}
}