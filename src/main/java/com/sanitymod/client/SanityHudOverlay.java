package com.sanitymod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sanitymod.util.IEntityDataSaver;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

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
                // Увеличим продолжительность для плавности: от 8 до 20 тиков (0.4 - 1.0 сек)
                blinkTicks = 8 + (int)(((70.0f - sanity) / 70.0f) * 12);
                maxBlinkTicks = blinkTicks;
                blinkCooldown = 20 + client.player.getRandom().nextInt(100);
            }
        }

        if (blinkTicks > 0) {
            // Вычисляем прогресс моргания от 0.0 до 1.0 (вперед и назад)
            float smoothTicks = (maxBlinkTicks - blinkTicks) + tickDelta;
            float timeProgress = smoothTicks / (float) maxBlinkTicks;
            timeProgress = Math.max(0.0f, Math.min(1.0f, timeProgress));

            // Синусоида даст плавный эффект: 0 -> 1 -> 0 (полное закрытие посередине анимации)
            float smoothProgress = (float) Math.sin(timeProgress * Math.PI);

            drawCurvedEyelids(drawContext, width, height, smoothProgress);

            blinkTicks--;
        }
    }

    private void drawCurvedEyelids(DrawContext drawContext, int width, int height, float progress) {
        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int segments = 20; // Количество сегментов для гладкости кривой
        float segmentWidth = (float) width / segments;

        for (int i = 0; i < segments; i++) {
            float x1 = i * segmentWidth;
            float x2 = (i + 1) * segmentWidth;
            
            // Нормализация координат от -1 до 1 (0 по центру экрана)
            float nx1 = (x1 / width) * 2.0f - 1.0f;
            float nx2 = (x2 / width) * 2.0f - 1.0f;
            
            // Формула для век: когда глаза полуоткрыты, края выше центра (эффект миндалевидных глаз).
            // Когда глаза закрываются полностью (progress = 1), линия становится прямой.
            float topEdge1 = (height / 2.0f) * progress + (height / 4.0f) * (float)(Math.sin(progress * Math.PI)) * (float)Math.cos(nx1 * Math.PI / 2.0);
            float topEdge2 = (height / 2.0f) * progress + (height / 4.0f) * (float)(Math.sin(progress * Math.PI)) * (float)Math.cos(nx2 * Math.PI / 2.0);
            
            // Верхнее веко
            bufferBuilder.vertex(matrix, x1, 0, 0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(matrix, x1, topEdge1, 0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(matrix, x2, topEdge2, 0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(matrix, x2, 0, 0).color(0, 0, 0, 255).next();
            
            // Нижнее веко (симметрично верхнему)
            float bottomEdge1 = height - topEdge1;
            float bottomEdge2 = height - topEdge2;
            
            bufferBuilder.vertex(matrix, x1, bottomEdge1, 0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(matrix, x1, height, 0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(matrix, x2, height, 0).color(0, 0, 0, 255).next();
            bufferBuilder.vertex(matrix, x2, bottomEdge2, 0).color(0, 0, 0, 255).next();
        }

        tessellator.draw();
        RenderSystem.disableBlend();
    }
}
