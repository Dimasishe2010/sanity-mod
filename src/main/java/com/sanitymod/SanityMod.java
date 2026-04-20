package com.sanitymod;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SanityMod implements ModInitializer {
	public static final String MOD_ID = "sanitymod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Sanity Mod initialized");
		com.sanitymod.entity.ModEntities.registerEntities();
		com.sanitymod.sound.ModSounds.registerSounds();
		com.sanitymod.network.ModMessages.registerC2SPackets();
		com.sanitymod.event.ServerTickHandler.register();
		com.sanitymod.command.SanityCommand.register();
	}
}
