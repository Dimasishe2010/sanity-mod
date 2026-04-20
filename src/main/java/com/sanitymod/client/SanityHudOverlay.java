package com.sanitymod.client;

import com.sanitymod.util.IEntityDataSaver;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class SanityHudOverlay implements HudRenderCallback {
    private int blinkTicks = 0;
    private int maxBlinkTicks = 0;
    private int blinkCooldown = 0;

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        float sanity = ((IEntityDataSaver) client.player).getPersistentData().getFloat("sanity");
        
        if (sanity > 70.0f) {
            blinkTicks = 0;
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        if (blinkCooldown > 0) {
            blinkCooldown--;
        } else if (blinkTicks <= 0) {
            float chance = 0.01f + ((70.0f - sanity) / 70.0f) * 0.05f;
            if (client.player.getRandom().nextFloat() < chance) {
                blinkTicks = 2 + (int)(((70.0f - sanity) / 70.0f) * 10);
                maxBlinkTicks = blinkTicks;
                blinkCooldown = 20 + client.player.getRandom().nextInt(100);
            }
        }

        if (blinkTicks > 0) {
            float alpha = 1.0f;
            
            int color = ((int)(alpha * 255.0f) << 24) | 0x000000;
            drawContext.fill(0, 0, width, height, color);

            blinkTicks--;
        }
    }
}
