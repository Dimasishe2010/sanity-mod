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
                                // Получаем горизонтальный вектор взгляда (чтобы избежать спавна в самом игроке
                                // при взгляде вниз/вверх)
                                float yaw = player.getYaw() * ((float) Math.PI / 180F);
                                double hX = -Math.sin(yaw);
                                double hZ = Math.cos(yaw);

                                // Строго за спиной (вектор инвертирован)
                                double behindX = -hX;
                                double behindZ = -hZ;

                                int dist = 15 + player.getRandom().nextInt(5);
                                BlockPos spawnPos = player.getBlockPos().add((int) (behindX * dist), 0,
                                        (int) (behindZ * dist));

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
                                    // Если место не найдено (например в узком туннеле), откладываем спавн на 10
                                    // секунд
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
