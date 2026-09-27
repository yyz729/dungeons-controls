package com.yyz.controls;

import com.yyz.controls.input.Throwables;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = DungeonsControls.MOD_ID, dist = Dist.CLIENT)
public final class DungeonsControls {
	public static final String MOD_ID = "dungeons_controls";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public DungeonsControls(IEventBus modBus) {
		DungeonsConfig.load();
		Throwables.bootstrap();
		modBus.addListener(DungeonsControls::onRegisterKeys);
		LOGGER.info("Dungeons Controls ready");
	}

	private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
		DungeonsKeys.register(event);
	}
}
