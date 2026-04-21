package com.sanitymod;

import net.fabricmc.api.ClientModInitializer;

public class SanityModClient implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
                com.sanitymod.network.ModMessages.registerS2CPackets();
                net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT
                                .register(new com.sanitymod.client.SanityHudOverlay());
                net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK
                                .register(client -> com.sanitymod.client.SanityHudOverlay.clientTick());
                com.sanitymod.client.SoundEngine.register();
                net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
                                .register(com.sanitymod.entity.ModEntities.MIMIC,
                                                com.sanitymod.entity.client.MimicRenderer::new);
        }
}
