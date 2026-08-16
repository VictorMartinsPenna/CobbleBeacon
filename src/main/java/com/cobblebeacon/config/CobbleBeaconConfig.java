package com.cobblebeacon.config;

import com.cobblebeacon.CobbleBeacon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plain, dependency-free config (own JSON file) so tuning the beacon powers never
 * requires recompiling. Values are per-amplifier-0; the tier-4 "dual select" (amplifier 1)
 * multiplies the bonus by (amplifier + 1), mirroring vanilla's Speed II / Haste II.
 */
public final class CobbleBeaconConfig {
	/**
	 * Cobblemon's shiny odds are a "1 in N" rate (default ~4096), not a 0-1 probability, so
	 * this divides that rate instead of adding to it: 4.0 means wild shinies become roughly
	 * 4x as common (e.g. ~1 in 1024) while the effect is active.
	 */
	public double shinyLuckRateDivisor = 4.0D;

	/** Extra flat chance added to Cobbreeding's hatched-egg shiny roll. */
	public double eggShinyBonusChance = 0.002D;

	/** Multiplier applied to Cobbreeding's egg TIMER countdown speed (2.0 = twice as fast). */
	public double eggHatchSpeedMultiplier = 1.5D;

	/** Multiplier applied to Cobbleworkers' navigation/deposit/scan cooldowns (0.5 = half the wait). */
	public double workerCooldownMultiplier = 0.6D;

	private static CobbleBeaconConfig instance;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static synchronized CobbleBeaconConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	/** Re-reads cobblebeacon.json from disk and swaps it in for every future {@link #get()} call. */
	public static synchronized CobbleBeaconConfig reload() {
		instance = load();
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("cobblebeacon.json");
	}

	private static CobbleBeaconConfig load() {
		Path path = path();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				CobbleBeaconConfig loaded = GSON.fromJson(reader, CobbleBeaconConfig.class);
				if (loaded != null) {
					return loaded;
				}
			} catch (IOException | RuntimeException e) {
				CobbleBeacon.LOGGER.warn("Failed to read cobblebeacon.json, regenerating defaults", e);
			}
		}
		CobbleBeaconConfig defaults = new CobbleBeaconConfig();
		defaults.save();
		return defaults;
	}

	public void save() {
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			CobbleBeacon.LOGGER.warn("Failed to save cobblebeacon.json", e);
		}
	}
}
