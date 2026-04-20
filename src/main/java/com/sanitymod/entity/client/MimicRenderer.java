package com.sanitymod.entity.client;

import com.sanitymod.entity.custom.MimicEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class MimicRenderer extends BipedEntityRenderer<MimicEntity, BipedEntityModel<MimicEntity>> {
    public MimicRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(MimicEntity entity) {
        if (entity.getTargetPlayer() != null) {
            AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) MinecraftClient.getInstance().world.getPlayerByUuid(entity.getTargetPlayer());
            if (player != null) {
                return player.getSkinTexture();
            }
        }
        return net.minecraft.client.util.DefaultSkinHelper.getTexture();
    }

    @Override
    public void render(MimicEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
