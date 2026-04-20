package com.sanitymod.entity.custom;

import com.sanitymod.sound.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import java.util.UUID;

public class MimicEntity extends HostileEntity {
    private UUID targetPlayerUuid;
    private int stareTicks = 0;

    public MimicEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createMimicAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0D); // Mimic doesn't move
    }

    public void setTargetPlayer(UUID uuid) {
        this.targetPlayerUuid = uuid;
    }
    
    public UUID getTargetPlayer() {
        return targetPlayerUuid;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            if (targetPlayerUuid != null) {
                ServerPlayerEntity player = (ServerPlayerEntity) this.getWorld().getPlayerByUuid(targetPlayerUuid);
                if (player != null) {
                    this.lookAtEntity(player, 30.0F, 30.0F);
                    
                    double distance = this.distanceTo(player);
                    if (distance < 5.0) {
                        disappear();
                        return;
                    }

                    net.minecraft.util.math.Vec3d playerLook = player.getRotationVec(1.0f).normalize();
                    net.minecraft.util.math.Vec3d toMimic = this.getPos().subtract(player.getPos()).normalize();
                    double dotProduct = playerLook.dotProduct(toMimic);
                    
                    if (dotProduct > 0.8 && player.canSee(this)) {
                        stareTicks++;
                        if (stareTicks > 30) { // 1.5 seconds
                            disappear();
                        }
                    } else {
                        stareTicks = 0;
                    }
                } else {
                    this.discard();
                }
            } else {
                this.discard();
            }
        }
    }

    private void disappear() {
        this.getWorld().playSound(null, this.getBlockPos(), ModSounds.MIMIC_STATIC, net.minecraft.sound.SoundCategory.HOSTILE, 1.0F, 1.0F);
        this.discard();
    }
}
