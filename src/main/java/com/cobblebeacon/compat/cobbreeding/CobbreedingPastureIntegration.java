package com.cobblebeacon.compat.cobbreeding;

import com.cobblebeacon.CobbleBeacon;
import com.cobblebeacon.config.CobbleBeaconConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@link CobbreedingEggTimerIntegration} only speeds up an egg's post-lay hatch countdown; it
 * never touches the separate, earlier countdown a Pasture block runs BEFORE an egg exists (the
 * two compatible tethered Pokemon actually breeding). Cobbreeding tracks that one per pasture
 * in a plain, public static registry: ludichat.cobbreeding.PastureBreedingData.registry, a
 * Map<BlockPos, PastureBreedingData> with public getRequiredTicks()/setRequiredTicks(int) --
 * completion fires once (world time - data.time) reaches requiredTicks, so lowering
 * requiredTicks makes that happen sooner without touching world time or any egg-selection
 * logic. Reflection (not a compile-time dependency) is used because this is an internal
 * implementation detail of Cobbreeding, not published API, and is more likely to change shape
 * across updates than the egg's data-component id.
 *
 * The registry has no world/dimension reference on either the key or the value, so proximity
 * is checked in raw coordinates only (matching a limitation already present in Cobbreeding's
 * own tick logic) -- a false-positive cross-dimension match is a corner case rare enough to
 * accept rather than build extra plumbing around.
 */
public final class CobbreedingPastureIntegration {
	private static final String MOD_ID = "cobbreeding";
	private static final String DATA_CLASS_NAME = "ludichat.cobbreeding.PastureBreedingData";
	private static final double RANGE = 32.0D;

	private static boolean modPresent;
	private static boolean resolved;
	private static boolean available;

	private static Field registryField;
	private static Method getRequiredTicksMethod;
	private static Method setRequiredTicksMethod;

	private CobbreedingPastureIntegration() {
	}

	public static void init() {
		modPresent = FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static boolean isAvailable() {
		if (!modPresent) {
			return false;
		}
		if (!resolved) {
			resolve();
		}
		return available;
	}

	private static void resolve() {
		resolved = true;
		try {
			Class<?> dataClass = Class.forName(DATA_CLASS_NAME);
			registryField = dataClass.getField("registry");
			getRequiredTicksMethod = dataClass.getMethod("getRequiredTicks");
			setRequiredTicksMethod = dataClass.getMethod("setRequiredTicks", int.class);
			available = true;
			CobbleBeacon.LOGGER.info("Cobbreeding detected, pasture-breeding-speed beacon power enabled.");
		} catch (ReflectiveOperationException e) {
			CobbleBeacon.LOGGER.warn(
					"Cobbreeding is installed but its pasture breeding data ({}) has changed shape; "
							+ "the pasture-breeding-speed beacon power is disabled until CobbleBeacon is updated.",
					DATA_CLASS_NAME, e);
		}
	}

	/** Called roughly once a second for every online player holding the Fertility effect. */
	public static void speedUpNearbyBreeding(ServerPlayerEntity player, int amplifier) {
		if (!isAvailable()) {
			return;
		}

		double extraRate = CobbleBeaconConfig.get().eggHatchSpeedMultiplier - 1.0D;
		if (extraRate <= 0) {
			return;
		}

		int extraTicks = Math.max(1, (int) Math.round(20 * extraRate * (amplifier + 1)));
		double rangeSq = RANGE * RANGE;

		try {
			@SuppressWarnings("unchecked")
			Map<BlockPos, Object> registry = (Map<BlockPos, Object>) registryField.get(null);

			for (Map.Entry<BlockPos, Object> entry : registry.entrySet()) {
				BlockPos pos = entry.getKey();
				double dx = pos.getX() + 0.5D - player.getX();
				double dy = pos.getY() + 0.5D - player.getY();
				double dz = pos.getZ() + 0.5D - player.getZ();
				if (dx * dx + dy * dy + dz * dz > rangeSq) {
					continue;
				}

				Object data = entry.getValue();
				int required = (int) getRequiredTicksMethod.invoke(data);
				if (required <= 0) {
					continue;
				}

				setRequiredTicksMethod.invoke(data, Math.max(0, required - extraTicks));

				if (player.getWorld() instanceof ServerWorld serverWorld) {
					serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
							pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
							4, 0.3D, 0.2D, 0.3D, 0.0D);
				}
			}
		} catch (ReflectiveOperationException | ClassCastException e) {
			available = false;
			CobbleBeacon.LOGGER.warn("Failed to speed up nearby pasture breeding; disabling it.", e);
		}
	}
}
