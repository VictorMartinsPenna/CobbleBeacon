package com.cobblebeacon.mixin;

import com.cobblebeacon.effect.ModEffects;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Slots CobbleBeacon's three custom powers into vanilla's existing beacon tier/effect
 * system (BeaconBlockEntity.EFFECTS_BY_LEVEL / EFFECTS) instead of building a parallel
 * one. Everything downstream — the GUI button grid (BeaconScreen reads EFFECTS_BY_LEVEL
 * dynamically, so it lays out however many buttons each tier holds), the "select the same
 * effect twice at full pyramid for level II" mechanic, network sync, and NBT persistence —
 * is written generically against the registry and needs no changes at all.
 *
 * One power per tier keeps each row from getting crowded and gives them the same kind of
 * progression as vanilla's own effects:
 *   Tier 1 (alongside Speed/Haste):        Shiny Luck
 *   Tier 2 (alongside Resistance/JumpBoost): Diligence
 *   Tier 3 (alongside Strength):             Fertility
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
	@Mutable
	@Shadow
	@Final
	public static List<List<RegistryEntry<StatusEffect>>> EFFECTS_BY_LEVEL;

	@Mutable
	@Shadow
	@Final
	private static Set<RegistryEntry<StatusEffect>> EFFECTS;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void cobblebeacon$addPowers(CallbackInfo ci) {
		List<List<RegistryEntry<StatusEffect>>> expanded = new ArrayList<>();
		for (List<RegistryEntry<StatusEffect>> tier : EFFECTS_BY_LEVEL) {
			expanded.add(new ArrayList<>(tier));
		}

		expanded.get(0).add(ModEffects.SHINY_LUCK);
		expanded.get(1).add(ModEffects.DILIGENCE);
		expanded.get(2).add(ModEffects.FERTILITY);

		EFFECTS_BY_LEVEL = expanded.stream().map(List::copyOf).collect(Collectors.toUnmodifiableList());
		// Deliberately a plain (mutable) HashSet via Collectors.toSet(), NOT toUnmodifiableSet():
		// getEffectOrNull(null) is a normal, common call (whenever a player picks only a primary
		// effect with no secondary), and Set.of()-backed immutable sets throw NPE from
		// contains(null) instead of returning false like a HashSet does. Vanilla's own EFFECTS
		// field relies on that HashSet tolerance; losing it here broke every beacon selection
		// that didn't also pick a secondary effect.
		EFFECTS = expanded.stream().flatMap(List::stream).collect(Collectors.toSet());
	}
}
