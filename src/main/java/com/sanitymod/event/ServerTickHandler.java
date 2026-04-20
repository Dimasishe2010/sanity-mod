package com.sanitymod.event;

import com.sanitymod.network.ModMessages;
import com.sanitymod.sanity.SanityData;
import com.sanitymod.util.IEntityDataSaver;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.LightType;
import net.minecraft.util.math.BlockPos;

public class ServerTickHandler {
    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.isCreative() || player.isSpectator()) continue;

                IEntityDataSaver dataSaver = (IEntityDataSaver) player;
                
                if (player.isSleeping()) {
                    if (server.getTicks() % 20 == 0) {
                        SanityData.addSanity(dataSaver, 5.0f);
                        ModMessages.syncSanity(SanityData.getSanity(dataSaver), player);
                    }
                    continue;
                }

                if (server.getTicks() % 20 == 0) {
                    BlockPos pos = player.getBlockPos();
                    int blockLight = player.getWorld().getLightLevel(LightType.BLOCK, pos);
                    int skyLight = player.getWorld().getLightLevel(LightType.SKY, pos);
                    boolean isNight = player.getWorld().isNight();
                    boolean isDeep = pos.getY() <= 10;

                    float sanityChange = 0.0f;

                    if (isDeep) {
                        sanityChange -= 0.5f;
                    }

                    if (blockLight < 4 && (skyLight < 4 || isNight)) {
                        sanityChange -= 0.2f;
                    }
                    
                    if (!isDeep && blockLight >= 10 || (!isNight && skyLight >= 10)) {
                        sanityChange += 0.1f;
                    }

                    if (sanityChange != 0) {
                        SanityData.addSanity(dataSaver, sanityChange);
                        ModMessages.syncSanity(SanityData.getSanity(dataSaver), player);
                    }
                }
                
                if (server.getTicks() % 100 == 0) {
                    float sanity = SanityData.getSanity(dataSaver);
                    if (sanity < 30.0f) {
                        net.minecraft.nbt.NbtCompound nbt = dataSaver.getPersistentData();
                        int mimicTimer = nbt.getInt("mimic_timer");
                        mimicTimer += 100;
                        if (mimicTimer >= 12000) {
                            mimicTimer = 0;
                            com.sanitymod.entity.custom.MimicEntity mimic = com.sanitymod.entity.ModEntities.MIMIC.create(player.getWorld());
                            if (mimic != null) {
                                net.minecraft.util.math.Vec3d look = player.getRotationVec(1.0f).multiply(-1);
                                look = look.multiply(15 + player.getRandom().nextInt(5));
                                net.minecraft.util.math.BlockPos spawnPos = player.getBlockPos().add((int)look.x, 0, (int)look.z);
                                while(!player.getWorld().getBlockState(spawnPos).isAir() && spawnPos.getY() < 319) {
                                    spawnPos = spawnPos.up();
                                }
                                mimic.refreshPositionAndAngles(spawnPos, 0, 0);
                                mimic.setTargetPlayer(player.getUuid());
                                player.getWorld().spawnEntity(mimic);
                            }
                        }
                        nbt.putInt("mimic_timer", mimicTimer);
                    } else {
                        dataSaver.getPersistentData().putInt("mimic_timer", 0);
                    }
                }
            }
        });
    }
}
