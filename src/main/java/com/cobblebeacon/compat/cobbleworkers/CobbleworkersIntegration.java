package com.cobblebeacon.compat.cobbleworkers;

import com.cobblebeacon.CobbleBeacon;
import com.cobblebeacon.config.CobbleBeaconConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Cobbleworkers (accieo.cobbleworkers) ships no public API and is not published to any
 * Maven repository, so this integration talks to its config purely through reflection
 * instead of a compile-time dependency. That keeps this project buildable even if
 * Cobbleworkers isn't installed, and lets it degrade gracefully (logging a warning and
 * turning itself off) instead of crashing the server if a future Cobbleworkers update
 * renames the handful of symbols we touch:
 *   accieo.cobbleworkers.config.ConfigManager (Kotlin object, static field "config")
 *   accieo.cobbleworkers.config.CobbleworkersConfig (Kotlin data class, 6-int constructor:
 *       navigationTimeout, depositTimeout, areaScanCooldown, areaScanRadius, areaScanHeight,
 *       scannedBlocksPerTick — all of which the worker job system reads live, so swapping
 *       the config object takes effect on the very next job tick)
 *
 * That global config, however, does NOT cover every source of delay: as of 2.0.x, each job
 * definition (data/cobbleworkers/jobs/*.json, e.g. the fire/water/powder-snow cauldron
 * generators) also bakes in its own fixed "cooldown" (in seconds, applied per-entity after a
 * successful completion) that is completely separate from the config above and was previously
 * unaffected by this power. Those compiled accieo.cobbleworkers.job.definition.Job instances
 * are plain (mutable-via-reflection) objects held by shared reference in both
 * JobRegistry.jobsById and JobRegistry.jobs, so this scales each job's private "cooldown" int
 * field in place (same multiplier as the config fields) instead of trying to replace the
 * registry's map/list contents.
 *
 * Cobbleworkers pasture jobs aren't scoped to a location the way vanilla beacon effects
 * are (a pasture works whether or not a player is nearby), so this can't be limited to
 * "pastures within beacon range" without much deeper, far more fragile hooks. Instead the
 * boost is a single server-wide toggle, switched on for as long as at least one online
 * player currently holds the Diligence beacon effect (see BeaconPowerTicker), and switched
 * back off the moment none do.
 */
public final class CobbleworkersIntegration {
	private static final String MOD_ID = "cobbleworkers";

	private static boolean available;
	private static boolean boosted = false;

	private static Class<?> configManagerClass;
	private static Class<?> configClass;
	private static Object configManagerInstance;
	private static Method getConfigMethod;
	private static Field configField;
	private static Constructor<?> configConstructor;
	private static Method getNavigationTimeout;
	private static Method getDepositTimeout;
	private static Method getAreaScanCooldown;
	private static Method getAreaScanRadius;
	private static Method getAreaScanHeight;
	private static Method getScannedBlocksPerTick;

	private static Object originalConfig;

	private static boolean jobCooldownAvailable;
	private static Field jobsByIdField;
	private static Field jobCooldownField;
	private static final Map<String, Integer> originalJobCooldowns = new HashMap<>();

	private CobbleworkersIntegration() {
	}

	public static void init() {
		if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
			available = false;
			return;
		}

		try {
			configManagerClass = Class.forName("accieo.cobbleworkers.config.ConfigManager");
			configClass = Class.forName("accieo.cobbleworkers.config.CobbleworkersConfig");

			Field instanceField = configManagerClass.getField("INSTANCE");
			configManagerInstance = instanceField.get(null);

			getConfigMethod = configManagerClass.getMethod("getConfig");
			configField = configManagerClass.getDeclaredField("config");
			configField.setAccessible(true);

			configConstructor = configClass.getConstructor(
					int.class, int.class, int.class, int.class, int.class, int.class);

			getNavigationTimeout = configClass.getMethod("getNavigationTimeout");
			getDepositTimeout = configClass.getMethod("getDepositTimeout");
			getAreaScanCooldown = configClass.getMethod("getAreaScanCooldown");
			getAreaScanRadius = configClass.getMethod("getAreaScanRadius");
			getAreaScanHeight = configClass.getMethod("getAreaScanHeight");
			getScannedBlocksPerTick = configClass.getMethod("getScannedBlocksPerTick");

			available = true;
			CobbleBeacon.LOGGER.info("Cobbleworkers detected, worker-speed beacon power enabled.");
		} catch (ReflectiveOperationException e) {
			available = false;
			CobbleBeacon.LOGGER.warn(
					"Cobbleworkers is installed but its config layout has changed; the worker-speed "
							+ "beacon power is disabled until CobbleBeacon is updated for the new version.", e);
			return;
		}

		try {
			Class<?> jobRegistryClass = Class.forName("accieo.cobbleworkers.job.JobRegistry");
			jobsByIdField = jobRegistryClass.getDeclaredField("jobsById");
			jobsByIdField.setAccessible(true);

			Class<?> jobClass = Class.forName("accieo.cobbleworkers.job.definition.Job");
			jobCooldownField = jobClass.getDeclaredField("cooldown");
			jobCooldownField.setAccessible(true);

			jobCooldownAvailable = true;
		} catch (ReflectiveOperationException e) {
			jobCooldownAvailable = false;
			CobbleBeacon.LOGGER.warn(
					"Cobbleworkers is installed but its job registry layout has changed; per-job cooldowns "
							+ "(e.g. the fire/water/powder-snow cauldron generator jobs) won't be sped up by "
							+ "Diligence until CobbleBeacon is updated for the new version.", e);
		}
	}

	public static boolean isAvailable() {
		return available;
	}

	public static boolean isBoosted() {
		return boosted;
	}

	/** Re-applies the boost with the current config's multiplier, e.g. after a /cobblebeacon reload. */
	public static void refresh() {
		if (boosted) {
			setBoosted(false);
			setBoosted(true);
		}
	}

	/**
	 * @param shouldBoost whether at least one online player currently holds the Diligence effect
	 */
	public static void setBoosted(boolean shouldBoost) {
		if (!available || boosted == shouldBoost) {
			return;
		}

		try {
			if (shouldBoost) {
				Object current = getConfigMethod.invoke(configManagerInstance);
				originalConfig = current;

				double multiplier = CobbleBeaconConfig.get().workerCooldownMultiplier;
				int navigation = scale((int) getNavigationTimeout.invoke(current), multiplier);
				int deposit = scale((int) getDepositTimeout.invoke(current), multiplier);
				int scan = scale((int) getAreaScanCooldown.invoke(current), multiplier);
				int radius = (int) getAreaScanRadius.invoke(current);
				int height = (int) getAreaScanHeight.invoke(current);
				int perTick = (int) getScannedBlocksPerTick.invoke(current);

				Object boostedConfig = configConstructor.newInstance(navigation, deposit, scan, radius, height, perTick);
				configField.set(null, boostedConfig);

				applyJobCooldowns(multiplier);
			} else if (originalConfig != null) {
				configField.set(null, originalConfig);
				originalConfig = null;

				revertJobCooldowns();
			}
			boosted = shouldBoost;
		} catch (ReflectiveOperationException e) {
			available = false;
			CobbleBeacon.LOGGER.warn(
					"Failed to apply the Cobbleworkers worker-speed beacon power; disabling it.", e);
		}
	}

	/** Scales every registered job's per-entity cooldown in place (e.g. the cauldron generators' 90s default). */
	private static void applyJobCooldowns(double multiplier) {
		if (!jobCooldownAvailable) {
			return;
		}

		try {
			Map<?, ?> jobsById = (Map<?, ?>) jobsByIdField.get(null);
			originalJobCooldowns.clear();
			for (Map.Entry<?, ?> entry : jobsById.entrySet()) {
				String id = (String) entry.getKey();
				Object job = entry.getValue();
				int original = jobCooldownField.getInt(job);
				originalJobCooldowns.put(id, original);
				// A handful of jobs (the harvesters) are defined with cooldown 0 -- no per-entity
				// throttle at all, only gated by areaScanCooldown/navigationTimeout instead. scale()'s
				// floor of 1 would turn that "no cooldown" into a brand new 20-tick cooldown, which is
				// a slowdown, not a speedup, so 0 is left untouched.
				jobCooldownField.setInt(job, original <= 0 ? 0 : scale(original, multiplier));
			}
		} catch (ReflectiveOperationException | ClassCastException e) {
			jobCooldownAvailable = false;
			CobbleBeacon.LOGGER.warn(
					"Failed to speed up Cobbleworkers job cooldowns; disabling that part of Diligence.", e);
		}
	}

	private static void revertJobCooldowns() {
		if (!jobCooldownAvailable || originalJobCooldowns.isEmpty()) {
			return;
		}

		try {
			Map<?, ?> jobsById = (Map<?, ?>) jobsByIdField.get(null);
			for (Map.Entry<?, ?> entry : jobsById.entrySet()) {
				Integer original = originalJobCooldowns.get(entry.getKey());
				if (original != null) {
					jobCooldownField.setInt(entry.getValue(), original);
				}
			}
		} catch (ReflectiveOperationException | ClassCastException e) {
			CobbleBeacon.LOGGER.warn("Failed to restore Cobbleworkers job cooldowns.", e);
		} finally {
			originalJobCooldowns.clear();
		}
	}

	private static int scale(int ticks, double multiplier) {
		return Math.max(1, (int) Math.round(ticks * multiplier));
	}
}
