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
                if (player.isCreative() || player.isSpectator())
                    continue;

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
                    net.minecraft.nbt.NbtCompound nbt = dataSaver.getPersistentData();
                    int mimicTimer = nbt.getInt("mimic_timer");

                    if (sanity < 30.0f) {
                        mimicTimer += 100;
                        // Снизили таймер до 2-3 минут для тестов (3600 тиков)
                        if (mimicTimer >= 3600) {
                            mimicTimer = 0;
                            com.sanitymod.entity.custom.MimicEntity mimic = com.sanitymod.entity.ModEntities.MIMIC
                                    .create(player.getWorld());
                            if (mimic != null) {
                                float yaw = player.getYaw() * ((float) Math.PI / 180F);
                                double hX = -Math.sin(yaw);
                                double hZ = Math.cos(yaw);

                                // Выбор состояния
                                com.sanitymod.entity.custom.MimicEntity.MimicState[] possibleStates;
                                boolean isCave = player.getBlockPos().getY() < 50;
                                if (isCave) {
                                    possibleStates = new com.sanitymod.entity.custom.MimicEntity.MimicState[] {
                                            com.sanitymod.entity.custom.MimicEntity.MimicState.STARE,
                                            com.sanitymod.entity.custom.MimicEntity.MimicState.CHASE,
                                            com.sanitymod.entity.custom.MimicEntity.MimicState.RUN_ACROSS,
                                            com.sanitymod.entity.custom.MimicEntity.MimicState.BREAK_BLOCK
                                    };
                                } else {
                                    possibleStates = new com.sanitymod.entity.custom.MimicEntity.MimicState[] {
                                            com.sanitymod.entity.custom.MimicEntity.MimicState.STARE,
                                            com.sanitymod.entity.custom.MimicEntity.MimicState.CHASE,
                                            com.sanitymod.entity.custom.MimicEntity.MimicState.RUN_ACROSS
                                    };
                                }

                                com.sanitymod.entity.custom.MimicEntity.MimicState chosenState = possibleStates[player
                                        .getRandom().nextInt(possibleStates.length)];
                                mimic.setMimicState(chosenState);

                                double spawnDirX, spawnDirZ;
                                int dist = 15 + player.getRandom().nextInt(5);

                                if (chosenState == com.sanitymod.entity.custom.MimicEntity.MimicState.RUN_ACROSS) {
                                    // Спавн сбоку (перпендикулярно взгляду)
                                    boolean left = player.getRandom().nextBoolean();
                                    spawnDirX = left ? -hZ : hZ;
                                    spawnDirZ = left ? hX : -hX;

                                    // Точка, куда он побежит (на другую сторону)
                                    BlockPos targetPos = player.getBlockPos().add((int) (-spawnDirX * dist), 0,
                                            (int) (-spawnDirZ * dist));
                                    mimic.setTargetPos(targetPos);
                                } else {
                                    // Строго за спиной
                                    spawnDirX = -hX;
                                    spawnDirZ = -hZ;
                                }

                                BlockPos spawnPos = player.getBlockPos().add((int) (spawnDirX * dist), 0,
                                        (int) (spawnDirZ * dist));

                                if (chosenState == com.sanitymod.entity.custom.MimicEntity.MimicState.BREAK_BLOCK) {
                                    mimic.setTargetPos(spawnPos.down());
                                }

                                // Ищем свободное место (воздух) по высоте в пределах +/- 5 блоков
                                boolean foundPos = false;
                                for (int yOffset = 0; yOffset <= 10; yOffset++) {
                                    int y = (yOffset % 2 == 0) ? (yOffset / 2) : -(yOffset / 2);
                                    BlockPos checkPos = spawnPos.add(0, y, 0);
                                    if (player.getWorld().getBlockState(checkPos).isAir()
                                            && player.getWorld().getBlockState(checkPos.up()).isAir()) {
                                        spawnPos = checkPos;
                                        foundPos = true;
                                        break;
                                    }
                                }

                                if (foundPos) {
                                    mimic.refreshPositionAndAngles(spawnPos, player.getYaw(), 0);
                                    mimic.setTargetPlayer(player.getUuid());
                                    player.getWorld().spawnEntity(mimic);
                                } else {
                                    // Откладываем спавн на 10 секунд
                                    mimicTimer = 3400;
                                }
                            }
                        }
                    } else {
                        // Медленно сбрасываем таймер, если рассудок поднялся, а не обнуляем сразу
                        mimicTimer = Math.max(0, mimicTimer - 50);
                    }
                    nbt.putInt("mimic_timer", mimicTimer);
                }
            }
        });
    }
}
