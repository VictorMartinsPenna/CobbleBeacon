package com.cobblebeacon.effect;

import com.cobblebeacon.CobbleBeacon;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Three inert marker StatusEffects with no tick behaviour of their own. Vanilla's beacon
 * already re-applies whatever effect is selected to every player in range every 4 seconds
 * (see BeaconBlockEntity#tick / #applyPlayerEffects) and removes it once they leave range or
 * the beacon deactivates, so simply being a valid beacon power is enough to get "is this
 * player currently near an active beacon with this power selected" for free via
 * LivingEntity#hasStatusEffect — no separate range tracking needed.
 */
public final class ModEffects {
	public static final RegistryEntry<StatusEffect> SHINY_LUCK = register("shiny_luck", new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xFFD700) {
	});
	public static final RegistryEntry<StatusEffect> DILIGENCE = register("diligence", new StatusEffect(StatusEffectCategory.BENEFICIAL, 0x62C370) {
	});
	public static final RegistryEntry<StatusEffect> FERTILITY = register("fertility", new StatusEffect(StatusEffectCategory.BENEFICIAL, 0xF7A6C1) {
	});

	private ModEffects() {
	}

	public static void init() {
		// Referencing the fields is enough to force class init / registration.
	}

	private static RegistryEntry<StatusEffect> register(String path, StatusEffect effect) {
		return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of(CobbleBeacon.MOD_ID, path), effect);
	}
}
