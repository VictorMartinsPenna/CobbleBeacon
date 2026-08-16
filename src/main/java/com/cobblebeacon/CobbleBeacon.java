package com.cobblebeacon;

import com.cobblebeacon.compat.cobbleworkers.CobbleworkersIntegration;
import com.cobblebeacon.compat.cobblemon.CobblemonShinyIntegration;
import com.cobblebeacon.compat.cobbreeding.CobbreedingEggTimerIntegration;
import com.cobblebeacon.compat.cobbreeding.CobbreedingPastureIntegration;
import com.cobblebeacon.effect.ModEffects;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleBeacon implements ModInitializer {
	public static final String MOD_ID = "cobblebeacon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEffects.init();

		CobblemonShinyIntegration.init();
		CobbleworkersIntegration.init();
		CobbreedingEggTimerIntegration.init();
		CobbreedingPastureIntegration.init();

		BeaconPowerTicker.init();
		CobbleBeaconCommands.init();

		LOGGER.info("CobbleBeacon ready: Shiny Luck, Diligence and Fertility are now valid beacon powers.");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
