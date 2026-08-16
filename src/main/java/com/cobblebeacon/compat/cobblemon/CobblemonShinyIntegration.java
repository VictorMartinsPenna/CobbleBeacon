package com.cobblebeacon.compat.cobblemon;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblebeacon.config.CobbleBeaconConfig;
import com.cobblebeacon.effect.ModEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Random;

/**
 * Both powers here are pure Cobblemon-API integrations (CobblemonEvents.SHINY_CHANCE_CALCULATION
 * / HATCH_EGG_PRE) with zero dependency on Cobbreeding, even though the egg-shiny power exists
 * for Cobbreeding's benefit: Cobbreeding eggs are just Cobblemon Pokemon that fire Cobblemon's
 * own hatch event, so hooking Cobblemon's event covers them (and anything else using the same
 * event) without touching Cobbreeding internals at all. This is the most future-proof of the
 * three integrations since CobblemonEvents is documented public API.
 */
public final class CobblemonShinyIntegration {
	private static final Random RANDOM = new Random();

	private CobblemonShinyIntegration() {
	}

	public static void init() {
		CobblemonEvents.SHINY_CHANCE_CALCULATION.subscribe(event -> {
			event.addModificationFunction((chance, player, pokemon) -> {
				if (!(player instanceof ServerPlayerEntity serverPlayer) || !serverPlayer.hasStatusEffect(ModEffects.SHINY_LUCK)) {
					return chance;
				}

				// Cobblemon's "chance" here is a 1-in-N rate (default ~4096), rolled internally
				// as Random.nextFloat() < 1f / rate — a LOWER rate means MORE common, and a
				// rate <= 0 means "never shiny". So making shiny more common means dividing the
				// rate down, not adding to it (adding only makes it rarer).
				StatusEffectInstance instance = serverPlayer.getStatusEffect(ModEffects.SHINY_LUCK);
				int amplifier = instance == null ? 0 : instance.getAmplifier();
				double divisor = Math.max(1.0, CobbleBeaconConfig.get().shinyLuckRateDivisor * (amplifier + 1));
				return (float) (chance / divisor);
			});
		});

		CobblemonEvents.HATCH_EGG_PRE.subscribe(event -> {
			ServerPlayerEntity player = event.getPlayer();
			if (player == null || !player.hasStatusEffect(ModEffects.FERTILITY)) {
				return;
			}

			PokemonProperties egg = event.getEgg();
			if (egg == null || Boolean.TRUE.equals(egg.getShiny())) {
				return;
			}

			StatusEffectInstance instance = player.getStatusEffect(ModEffects.FERTILITY);
			int amplifier = instance == null ? 0 : instance.getAmplifier();
			double chance = CobbleBeaconConfig.get().eggShinyBonusChance * (amplifier + 1);
			if (RANDOM.nextDouble() < chance) {
				egg.setShiny(true);
			}
		});
	}
}
