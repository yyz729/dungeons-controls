package com.yyz.controls;

import com.yyz.controls.camera.DungeonsCamera;
import com.yyz.controls.input.ClickController;
import com.yyz.controls.occlusion.GhostRenderer;
import com.yyz.controls.occlusion.OcclusionTracker;
import com.yyz.controls.render.TargetHighlighter;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = DungeonsControls.MOD_ID, value = Dist.CLIENT)
public final class DungeonsControlsClient {
	private DungeonsControlsClient() {
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) {
			DungeonsMode.onLeftWorld();
			return;
		}

		DungeonsMode.onWorldTick(client);
		DungeonsKeys.tick(client);
		DungeonsCamera.tick(client);
		ClickController.tick(client);
		OcclusionTracker.tick(client);
	}

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			return;
		}
		if (!DungeonsMode.isGameplayActive()) {
			return;
		}
		GhostRenderer.render(event);
		TargetHighlighter.render(event);
	}
}
