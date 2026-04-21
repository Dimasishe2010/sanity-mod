package com.sanitymod.entity.custom;

import com.sanitymod.network.ModMessages;
import com.sanitymod.sound.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.UUID;

public class MimicEntity extends HostileEntity {
    public enum MimicState {
        STARE, CHASE, RUN_ACROSS, BREAK_BLOCK
    }

    private UUID targetPlayerUuid;
    private int stateTicks = 0;
    private MimicState currentState = MimicState.STARE;
    private BlockPos targetPos = null;

    public MimicEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createMimicAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.4D); // Выдали скорость!
    }

    public void setTargetPlayer(UUID uuid) {
        this.targetPlayerUuid = uuid;
    }

    public UUID getTargetPlayer() {
        return targetPlayerUuid;
    }

    public void setMimicState(MimicState state) {
        this.currentState = state;
    }

    public void setTargetPos(BlockPos pos) {
        this.targetPos = pos;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient)
            return;

        if (targetPlayerUuid == null) {
            this.discard();
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) this.getWorld().getPlayerByUuid(targetPlayerUuid);
        if (player == null) {
            this.discard();
            return;
        }

        double distance = this.distanceTo(player);
        stateTicks++;

        net.minecraft.util.math.Vec3d playerLook = player.getRotationVec(1.0f).normalize();
        net.minecraft.util.math.Vec3d toMimic = this.getPos().subtract(player.getPos()).normalize();
        double dotProduct = playerLook.dotProduct(toMimic);
        boolean isLookingAt = dotProduct > 0.8 && player.canSee(this);

        switch (currentState) {
            case STARE:
                this.getNavigation().stop();
                this.lookAtEntity(player, 30.0F, 30.0F);
                if (distance < 5.0 || (isLookingAt && stateTicks > 30)) {
                    disappear();
                }
                break;

            case CHASE:
                this.lookAtEntity(player, 30.0F, 30.0F);
                // Бежит за игроком только если игрок на него НЕ смотрит (эффект SCP-173 или
                // плачущего ангела, но он бежит всегда)
                // Или просто бежит напролом. Сделаем, чтобы бежал всегда.
                this.getNavigation().startMovingTo(player, 1.0D);

                if (distance < 2.5) { // Добежал! Скример!
                    ModMessages.sendJumpscare(player, 100); // 5 секунд эффектов
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0, false, false));
                    disappear();
                } else if (isLookingAt && stateTicks > 20) {
                    // Если игрок вовремя обернулся и смотрит дольше 1 сек - мимик исчезает без
                    // скримера
                    disappear();
                }

                // Таймаут преследования
                if (stateTicks > 400)
                    disappear();
                break;

            case RUN_ACROSS:
                // Бежит в целевую точку
                if (targetPos != null) {
                    this.getNavigation().startMovingTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.2D);
                    if (this.getBlockPos().isWithinDistance(targetPos, 2.0) || stateTicks > 100) {
                        this.discard(); // Просто пропадает, без звука
                    }
                } else {
                    this.discard();
                }
                break;

            case BREAK_BLOCK:
                // Ломает блок рядом с собой
                if (stateTicks > 20) {
                    if (targetPos != null && !this.getWorld().getBlockState(targetPos).isAir()) {
                        this.getWorld().breakBlock(targetPos, false, this);
                        // Звук поломки блока и так воспроизводится ванильным методом breakBlock
                    }
                    disappear();
                }
                break;
        }
    }

    private void disappear() {
        this.getWorld().playSound(null, this.getBlockPos(), ModSounds.MIMIC_STATIC,
                net.minecraft.sound.SoundCategory.HOSTILE, 1.0F, 1.0F);
        this.discard();
    }
}
