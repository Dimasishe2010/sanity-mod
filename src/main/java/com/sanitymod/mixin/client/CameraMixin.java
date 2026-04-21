package com.sanitymod.mixin.client;

import com.sanitymod.client.SanityHudOverlay;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private float pitch;
    @Shadow
    private float yaw;

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdate(net.minecraft.world.BlockView area, net.minecraft.entity.Entity focusedEntity,
            boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (SanityHudOverlay.jumpscareTicks > 0) {
            float intensity = ((float) SanityHudOverlay.jumpscareTicks / SanityHudOverlay.jumpscareDuration) * 3.0f; // Сила тряски
            float shakeYaw = this.yaw + (float) ((Math.random() - 0.5) * intensity);
            float shakePitch = this.pitch + (float) ((Math.random() - 0.5) * intensity);
            this.setRotation(shakeYaw, shakePitch);
        } else if (SanityHudOverlay.pulsationTicks > 0) {
            float fadeOut = (float) SanityHudOverlay.pulsationTicks / 540.0f;
            float time = SanityHudOverlay.pulsationTicks + tickDelta;
            float pulse = (float) Math.max(0.0, Math.sin(time * 0.15f));
            float intensity = pulse * fadeOut * 1.5f; // Дрожь камеры при пульсации
            float shakeYaw = this.yaw + (float) ((Math.random() - 0.5) * intensity);
            float shakePitch = this.pitch + (float) ((Math.random() - 0.5) * intensity);
            this.setRotation(shakeYaw, shakePitch);
        }
    }
}
