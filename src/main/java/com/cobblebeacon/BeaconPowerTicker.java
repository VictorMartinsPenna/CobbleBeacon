package com.cobblebeacon;

import com.cobblebeacon.compat.cobbleworkers.CobbleworkersIntegration;
import com.cobblebeacon.compat.cobbreeding.CobbreedingEggTimerIntegration;
import com.cobblebeacon.compat.cobbreeding.CobbreedingPastureIntegration;
import com.cobblebeacon.effect.ModEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Drives the two beacon powers that can't be handled by a single self-contained event
 * listener:
 *  - Fertility's egg-hatch-speed half needs to touch every Fertility-holding player's
 *    inventory periodically (the shiny-chance half is a plain Cobblemon event listener).
 *  - Diligence (Cobbleworkers speed) is Cobbleworkers' own limitation, not ours: pasture jobs
 *    run regardless of whether a player is nearby, so there's no per-player or per-location
 *    hook to attach to. This ticker turns the server-wide speed-up on for as long as at least
 *    one online player currently holds Diligence (i.e. is standing near an active beacon with
 *    it selected) and back off the instant none do, which is the closest equivalent to "near
 *    an active beacon" that Cobbleworkers' architecture allows.
 */
public final class BeaconPowerTicker {
	private static final int INTERVAL_TICKS = 20;
	private int tickCounter = 0;

	private BeaconPowerTicker() {
	}

	public static void init() {
		BeaconPowerTicker ticker = new BeaconPowerTicker();
		ServerTickEvents.END_SERVER_TICK.register(ticker::onServerTick);
	}

	private void onServerTick(MinecraftServer server) {
		if (++this.tickCounter < INTERVAL_TICKS) {
			return;
		}
		this.tickCounter = 0;

		boolean anyoneDiligent = false;

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (CobbleworkersIntegration.isAvailable() && player.hasStatusEffect(ModEffects.DILIGENCE)) {
				anyoneDiligent = true;
			}

			if (player.hasStatusEffect(ModEffects.FERTILITY)) {
				StatusEffectInstance instance = player.getStatusEffect(ModEffects.FERTILITY);
				int amplifier = instance == null ? 0 : instance.getAmplifier();

				if (CobbreedingEggTimerIntegration.isAvailable()) {
					CobbreedingEggTimerIntegration.speedUpEggs(player, amplifier);
				}
				if (CobbreedingPastureIntegration.isAvailable()) {
					CobbreedingPastureIntegration.speedUpNearbyBreeding(player, amplifier);
				}
			}
		}

		CobbleworkersIntegration.setBoosted(anyoneDiligent);
	}
}
