package com.sanitymod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class VeinRenderer {
    private static class VeinSegment {
        float x1, y1, x2, y2;
        float thickness;
        float birthProgress; // 0.0 to 1.0 (когда этот сегмент должен появиться)

        VeinSegment(float x1, float y1, float x2, float y2, float thickness, float birthProgress) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.thickness = thickness;
            this.birthProgress = birthProgress;
        }
    }

    private static List<VeinSegment> segments = new ArrayList<>();
    private static int lastWidth = 0;
    private static int lastHeight = 0;

    public static void render(DrawContext drawContext, int width, int height, float progress) {
        if (width != lastWidth || height != lastHeight || segments.isEmpty()) {
            generateVeins(width, height);
            lastWidth = width;
            lastHeight = height;
        }

        Matrix4f matrix = drawContext.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // Рисуем линиями или полигонами. Полигоны лучше для толщины.
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (VeinSegment seg : segments) {
            if (progress >= seg.birthProgress) {
                // Если сегмент только начал расти, вычисляем его текущую длину
                float grow = 1.0f;
                if (progress - seg.birthProgress < 0.1f) {
                    grow = (progress - seg.birthProgress) / 0.1f;
                }

                float currX2 = seg.x1 + (seg.x2 - seg.x1) * grow;
                float currY2 = seg.y1 + (seg.y2 - seg.y1) * grow;

                // Вектор перпендикуляра для толщины
                float dx = currX2 - seg.x1;
                float dy = currY2 - seg.y1;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len == 0)
                    continue;

                float px = (-dy / len) * (seg.thickness / 2.0f);
                float py = (dx / len) * (seg.thickness / 2.0f);

                bufferBuilder.vertex(matrix, seg.x1 - px, seg.y1 - py, 0).color(0, 0, 0, 255).next();
                bufferBuilder.vertex(matrix, seg.x1 + px, seg.y1 + py, 0).color(0, 0, 0, 255).next();
                bufferBuilder.vertex(matrix, currX2 + px, currY2 + py, 0).color(0, 0, 0, 255).next();
                bufferBuilder.vertex(matrix, currX2 - px, currY2 - py, 0).color(0, 0, 0, 255).next();
            }
        }

        tessellator.draw();
        RenderSystem.disableBlend();
    }

    private static void generateVeins(int width, int height) {
        segments.clear();
        Random rand = new Random();

        // Генерация вен с 4 сторон
        for (int i = 0; i < 15; i++) {
            // Верх
            generateBranch(rand.nextFloat() * width, 0, width / 2.0f, height / 2.0f, 6.0f, 0.0f, rand, 0);
            // Низ
            generateBranch(rand.nextFloat() * width, height, width / 2.0f, height / 2.0f, 6.0f, 0.0f, rand, 0);
            // Лево
            generateBranch(0, rand.nextFloat() * height, width / 2.0f, height / 2.0f, 6.0f, 0.0f, rand, 0);
            // Право
            generateBranch(width, rand.nextFloat() * height, width / 2.0f, height / 2.0f, 6.0f, 0.0f, rand, 0);
        }
    }

    private static void generateBranch(float x, float y, float targetX, float targetY, float thickness,
            float birthProgress, Random rand, int depth) {
        if (depth > 5 || thickness < 1.0f || birthProgress >= 1.0f)
            return;

        // Направление к центру с шумом
        float dx = targetX - x;
        float dy = targetY - y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        // Длина сегмента
        float segmentLen = 20.0f + rand.nextFloat() * 30.0f;
        if (segmentLen > len)
            segmentLen = len;

        float dirX = dx / len;
        float dirY = dy / len;

        // Добавляем шум в направление
        float angleNoise = (rand.nextFloat() - 0.5f) * 1.5f; // +- радианы
        float newDirX = (float) (dirX * Math.cos(angleNoise) - dirY * Math.sin(angleNoise));
        float newDirY = (float) (dirX * Math.sin(angleNoise) + dirY * Math.cos(angleNoise));

        float nextX = x + newDirX * segmentLen;
        float nextY = y + newDirY * segmentLen;

        segments.add(new VeinSegment(x, y, nextX, nextY, thickness, birthProgress));

        // Основная ветка
        generateBranch(nextX, nextY, targetX, targetY, thickness * 0.8f,
                birthProgress + 0.1f + rand.nextFloat() * 0.05f, rand, depth + 1);

        // Побочные ветки (шанс 40%)
        if (rand.nextFloat() < 0.4f) {
            float branchTargetX = targetX + (rand.nextFloat() - 0.5f) * 200;
            float branchTargetY = targetY + (rand.nextFloat() - 0.5f) * 200;
            generateBranch(nextX, nextY, branchTargetX, branchTargetY, thickness * 0.5f, birthProgress + 0.15f, rand,
                    depth + 1);
        }
    }
}
