package com.cobblebeacon;

import com.cobblebeacon.compat.cobbleworkers.CobbleworkersIntegration;
import com.cobblebeacon.compat.cobbreeding.CobbreedingEggTimerIntegration;
import com.cobblebeacon.compat.cobbreeding.CobbreedingPastureIntegration;
import com.cobblebeacon.config.CobbleBeaconConfig;
import com.cobblebeacon.effect.ModEffects;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** /cobblebeacon reload and /cobblebeacon status — both operator-only (permission level 2). */
public final class CobbleBeaconCommands {
	private CobbleBeaconCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				CommandManager.literal("cobblebeacon")
						.requires(source -> source.hasPermissionLevel(2))
						.then(CommandManager.literal("reload").executes(CobbleBeaconCommands::reload))
						.then(CommandManager.literal("status").executes(CobbleBeaconCommands::status))
		));
	}

	private static int reload(CommandContext<ServerCommandSource> context) {
		CobbleBeaconConfig.reload();
		CobbleworkersIntegration.refresh();

		context.getSource().sendFeedback(() -> Text.literal("[CobbleBeacon] config reloaded from cobblebeacon.json."), true);
		return 1;
	}

	private static int status(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();

		source.sendFeedback(() -> Text.literal("[CobbleBeacon] Shiny Luck / Fertility (shiny): Cobblemon event API - always active."), false);
		source.sendFeedback(() -> Text.literal("[CobbleBeacon] Diligence (Cobbleworkers): " + describe(
				"cobbleworkers", CobbleworkersIntegration.isAvailable(),
				CobbleworkersIntegration.isAvailable() ? (CobbleworkersIntegration.isBoosted() ? "active, currently boosted" : "active, idle (no player nearby)") : null
		)), false);
		source.sendFeedback(() -> Text.literal("[CobbleBeacon] Fertility (Cobbreeding egg speed): " + describe(
				"cobbreeding", CobbreedingEggTimerIntegration.isAvailable(), null
		)), false);
		source.sendFeedback(() -> Text.literal("[CobbleBeacon] Fertility (Cobbreeding pasture breeding speed): " + describe(
				"cobbreeding", CobbreedingPastureIntegration.isAvailable(), null
		)), false);

		ServerPlayerEntity player = source.getPlayer();
		if (player != null) {
			source.sendFeedback(() -> Text.literal("[CobbleBeacon] Your active effects right now (beacon effects expire ~9-13s "
					+ "after you leave its power range, so this is the real source of truth):"), false);
			source.sendFeedback(() -> Text.literal("  " + describeEffect(player, "Shiny Luck", ModEffects.SHINY_LUCK)), false);
			source.sendFeedback(() -> Text.literal("  " + describeEffect(player, "Diligence", ModEffects.DILIGENCE)), false);
			source.sendFeedback(() -> Text.literal("  " + describeEffect(player, "Fertility", ModEffects.FERTILITY)), false);
		}

		return 1;
	}

	private static String describeEffect(ServerPlayerEntity player, String name, RegistryEntry<StatusEffect> effect) {
		StatusEffectInstance instance = player.getStatusEffect(effect);
		if (instance == null) {
			return name + ": not active";
		}
		return name + ": active, amplifier " + instance.getAmplifier() + ", " + (instance.getDuration() / 20) + "s left";
	}

	private static String describe(String modId, boolean available, String activeExtra) {
		if (!FabricLoader.getInstance().isModLoaded(modId)) {
			return "mod not installed";
		}
		if (!available) {
			return "mod installed, but integration disabled (see server log for why)";
		}
		return activeExtra != null ? activeExtra : "active";
	}
}
