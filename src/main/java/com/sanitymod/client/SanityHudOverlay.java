package com.sanitymod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sanitymod.client.render.VeinRenderer;
import com.sanitymod.util.IEntityDataSaver;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

public class SanityHudOverlay implements HudRenderCallback {
    public static int blinkTicks = 0;
    public static int maxBlinkTicks = 0;
    public static int blinkCooldown = 0;

    public static int pulsationTicks = 0;
    public static int pulsationCooldown = 0;
    private static final int MAX_PULSATION_TICKS = 540;

    public static int jumpscareTicks = 0;
    public static int jumpscareDuration = 0;

    public static void triggerJumpscare(int duration) {
        jumpscareDuration = duration;
        jumpscareTicks = duration;
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Jumpscare Effect (Вены)
        if (jumpscareTicks > 0) {
            float progress = 1.0f - ((float) jumpscareTicks / jumpscareDuration);
            VeinRenderer.render(drawContext, width, height, progress);
        }

        if (pulsationTicks > 0) {
            float fadeOut = (float) pulsationTicks / MAX_PULSATION_TICKS;
            float time = pulsationTicks + tickDelta;
            float pulse = (float) Math.max(0.0, Math.sin(time * 0.15f));
            float intensity = pulse * fadeOut * 0.8f;
            
            if (intensity > 0) {
                drawPulsationVignette(drawContext, width, height, intensity);
            }
        }

        float sanity = ((IEntityDataSaver) client.player).getPersistentData().getFloat("sanity");

        if (sanity > 70.0f) {
            blinkTicks = 0;
            return;
        }

        if (blinkTicks > 0) {
            float smoothTicks = (maxBlinkTicks - blinkTicks) + tickDelta;
            float timeProgress = smoothTicks / (float) maxBlinkTicks;
            timeProgress = Math.max(0.0f, Math.min(1.0f, timeProgress));
            float smoothProgress = (float) Math.sin(timeProgress * Math.PI);
            drawCurvedEyelids(drawContext, width, height, smoothProgress);
        }
    }

    public static void clientTick() {
        if (jumpscareTicks > 0) {
            jumpscareTicks--;
        }

        if (pulsationTicks > 0) {
            pulsationTicks--;
        }
        if (pulsationCooldown > 0) {
            pulsationCooldown--;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null)
            return;

        float sanity = ((IEntityDataSaver) client.player).getPersistentData().getFloat("sanity");

        if (sanity < 40.0f && pulsationCooldown <= 0 && pulsationTicks <= 0) {
            if (client.player.getRandom().nextFloat() < 0.002f) {
                pulsationTicks = MAX_PULSATION_TICKS;
                pulsationCooldown = 1200; // 60s cooldown
                client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(com.sanitymod.sound.ModSounds.PULSATION, 1.0F, 1.0F));
            }
        }

        if (sanity > 70.0f)
            return;

        if (blinkCooldown > 0) {
            blinkCooldown--;
        } else if (blinkTicks <= 0) {
            // Тик происходит 20 раз в секунду. Шанс нужно адаптировать под тики.
            float chance = 0.005f + ((70.0f - sanity) / 70.0f) * 0.02f;
            if (client.player.getRandom().nextFloat() < chance) {
                blinkTicks = 8 + (int) (((70.0f - sanity) / 70.0f) * 12);
                maxBlinkTicks = blinkTicks;
                blinkCooldown = 20 + client.player.getRandom().nextInt(100);
            }
        }

        if (blinkTicks > 0) {
            blinkTicks--;
        }
    }

    private void drawPulsationVignette(DrawContext drawContext, int width, int height, float intensity) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        int alpha = (int)(intensity * 255);
        int cw = width / 2;
        int ch = height / 2;
        
        bufferBuilder.vertex(matrix, 0, 0, 0).color(100, 0, 0, alpha).next();
        bufferBuilder.vertex(matrix, 0, ch, 0).color(100, 0, 0, 0).next();
        bufferBuilder.vertex(matrix, width, ch, 0).color(100, 0, 0, 0).next();
        bufferBuilder.vertex(matrix, width, 0, 0).color(100, 0, 0, alpha).next();

        bufferBuilder.vertex(matrix, 0, ch, 0).color(100, 0, 0, 0).next();
        bufferBuilder.vertex(matrix, 0, height, 0).color(100, 0, 0, alpha).next();
        bufferBuilder.vertex(matrix, width, height, 0).color(100, 0, 0, alpha).next();
        bufferBuilder.vertex(matrix, width, ch, 0).color(100, 0, 0, 0).next();

        bufferBuilder.vertex(matrix, 0, 0, 0).color(100, 0, 0, alpha).next();
        bufferBuilder.vertex(matrix, 0, height, 0).color(100, 0, 0, alpha).next();
        bufferBuilder.vertex(matrix, cw, height, 0).color(100, 0, 0, 0).next();
        bufferBuilder.vertex(matrix, cw, 0, 0).color(100, 0, 0, 0).next();

        bufferBuilder.vertex(matrix, cw, 0, 0).color(100, 0, 0, 0).next();
        bufferBuilder.vertex(matrix, cw, height, 0).color(100, 0, 0, 0).next();
        bufferBuilder.vertex(matrix, width, height, 0).color(100, 0, 0, alpha).next();
        bufferBuilder.vertex(matrix, width, 0, 0).color(100, 0, 0, alpha).next();

        tessellator.draw();
        RenderSystem.disableBlend();
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

            // Формула для век: когда глаза полуоткрыты, края выше центра (эффект
            // миндалевидных глаз).
            // Когда глаза закрываются полностью (progress = 1), линия становится прямой.
            float topEdge1 = (height / 2.0f) * progress
                    + (height / 4.0f) * (float) (Math.sin(progress * Math.PI)) * (float) Math.cos(nx1 * Math.PI / 2.0);
            float topEdge2 = (height / 2.0f) * progress
                    + (height / 4.0f) * (float) (Math.sin(progress * Math.PI)) * (float) Math.cos(nx2 * Math.PI / 2.0);

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
