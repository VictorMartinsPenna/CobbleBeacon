package com.cobblebeacon.compat.cobbreeding;

import com.cobblebeacon.CobbleBeacon;
import com.cobblebeacon.config.CobbleBeaconConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Cobbreeding stores each egg's remaining hatch time as a plain, registered data component
 * ("cobbreeding:timer", an Integer) on the egg ItemStack, decremented by Cobbreeding's own
 * PokemonEgg#inventoryTick every 20 ticks. Rather than mixin into that private method, this
 * looks the component type up generically by its registry id and nudges the same value down
 * a bit further ourselves on the same cadence — Cobbreeding's own tick then hatches the egg
 * whenever it next sees the value at or below zero, same as it always does. The component id
 * is part of Cobbreeding's persisted item/save data, so it's far less likely to change across
 * updates than an internal method signature would be, and this needs no compile-time
 * dependency on Cobbreeding at all: if it's absent (or too old/new to have this component),
 * this simply never finds it and stays a no-op.
 *
 * The lookup is done lazily on first real use rather than in init(): Fabric doesn't guarantee
 * mod initialization order, and CobbleBeacon only "suggests" (not "depends on") Cobbreeding,
 * so Cobbreeding can easily register its component after our own onInitialize() already ran,
 * which made an eager lookup at init time fail even with Cobbreeding installed and working.
 */
public final class CobbreedingEggTimerIntegration {
	private static final Identifier TIMER_ID = Identifier.of("cobbreeding", "timer");
	private static final String MOD_ID = "cobbreeding";

	private static boolean modPresent;
	private static ComponentType<Integer> timerType;
	private static boolean warnedMissing;

	private CobbreedingEggTimerIntegration() {
	}

	public static void init() {
		modPresent = FabricLoader.getInstance().isModLoaded(MOD_ID);
	}

	public static boolean isAvailable() {
		if (!modPresent) {
			return false;
		}
		if (timerType == null) {
			resolveTimerType();
		}
		return timerType != null;
	}

	private static void resolveTimerType() {
		ComponentType<?> found = Registries.DATA_COMPONENT_TYPE.get(TIMER_ID);
		if (found == null) {
			if (!warnedMissing) {
				warnedMissing = true;
				CobbleBeacon.LOGGER.warn(
						"Cobbreeding is installed but its egg timer component ({}) wasn't found; "
								+ "the egg-hatch-speed beacon power is disabled until CobbleBeacon is updated.", TIMER_ID);
			}
			return;
		}

		@SuppressWarnings("unchecked")
		ComponentType<Integer> typed = (ComponentType<Integer>) found;
		timerType = typed;
		CobbleBeacon.LOGGER.info("Cobbreeding detected, egg-hatch-speed beacon power enabled.");
	}

	/** Called roughly once a second for every online player holding the Fertility effect. */
	public static void speedUpEggs(ServerPlayerEntity player, int amplifier) {
		if (!isAvailable()) {
			return;
		}

		double extraRate = CobbleBeaconConfig.get().eggHatchSpeedMultiplier - 1.0D;
		if (extraRate <= 0) {
			return;
		}

		int extraTicks = Math.max(1, (int) Math.round(20 * extraRate * (amplifier + 1)));

		PlayerInventory inventory = player.getInventory();
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}

			Integer timer = stack.get(timerType);
			if (timer == null || timer <= 0) {
				continue;
			}

			stack.set(timerType, Math.max(0, timer - extraTicks));
		}
	}
}
