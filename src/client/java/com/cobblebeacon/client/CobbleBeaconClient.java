package com.cobblebeacon.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * No client-only logic is needed: the three custom beacon powers slot into vanilla's own
 * BeaconBlockEntity.EFFECTS_BY_LEVEL (see BeaconBlockEntityMixin), and BeaconScreen already
 * builds its button grid by reading that list at render time, so the GUI adapts on its own.
 */
public class CobbleBeaconClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
	}
}