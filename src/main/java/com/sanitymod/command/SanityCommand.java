package com.sanitymod.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.sanitymod.network.ModMessages;
import com.sanitymod.sanity.SanityData;
import com.sanitymod.util.IEntityDataSaver;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SanityCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sanity")
                    .requires(source -> source.hasPermissionLevel(2)) // Needs OP for setting
                    .then(CommandManager.literal("set")
                            .then(CommandManager.argument("amount", FloatArgumentType.floatArg(0.0f, 100.0f))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        if (player != null) {
                                            float amount = FloatArgumentType.getFloat(context, "amount");
                                            IEntityDataSaver dataSaver = (IEntityDataSaver) player;
                                            SanityData.setSanity(dataSaver, amount);
                                            ModMessages.syncSanity(amount, player);
                                            context.getSource().sendFeedback(
                                                    () -> Text.literal("Рассудок установлен на: " + amount), false);
                                            return 1;
                                        }
                                        return 0;
                                    })))
                    .then(CommandManager.literal("get")
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    float sanity = SanityData.getSanity((IEntityDataSaver) player);
                                    context.getSource().sendFeedback(() -> Text.literal("Текущий рассудок: " + sanity),
                                            false);
                                    return 1;
                                }
                                return 0;
                            })));

            dispatcher.register(CommandManager.literal("mimic")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("state", com.mojang.brigadier.arguments.StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                if (player != null) {
                                    String stateStr = com.mojang.brigadier.arguments.StringArgumentType
                                            .getString(context, "state");
                                    com.sanitymod.entity.custom.MimicEntity.MimicState state;
                                    try {
                                        state = com.sanitymod.entity.custom.MimicEntity.MimicState
                                                .valueOf(stateStr.toUpperCase());
                                    } catch (IllegalArgumentException e) {
                                        context.getSource().sendError(Text.literal(
                                                "Неизвестное состояние. Доступные: STARE, CHASE, RUN_ACROSS, BREAK_BLOCK"));
                                        return 0;
                                    }

                                    com.sanitymod.entity.custom.MimicEntity mimic = com.sanitymod.entity.ModEntities.MIMIC
                                            .create(player.getWorld());
                                    if (mimic != null) {
                                        float yaw = player.getYaw() * ((float) Math.PI / 180F);
                                        double hX = -Math.sin(yaw);
                                        double hZ = Math.cos(yaw);
                                        double spawnDirX, spawnDirZ;
                                        int dist = 10;

                                        if (state == com.sanitymod.entity.custom.MimicEntity.MimicState.RUN_ACROSS) {
                                            boolean left = player.getRandom().nextBoolean();
                                            spawnDirX = left ? -hZ : hZ;
                                            spawnDirZ = left ? hX : -hX;
                                            net.minecraft.util.math.BlockPos targetPos = player.getBlockPos()
                                                    .add((int) (-spawnDirX * dist), 0, (int) (-spawnDirZ * dist));
                                            mimic.setTargetPos(targetPos);
                                        } else {
                                            spawnDirX = -hX;
                                            spawnDirZ = -hZ;
                                        }

                                        net.minecraft.util.math.BlockPos spawnPos = player.getBlockPos()
                                                .add((int) (spawnDirX * dist), 0, (int) (spawnDirZ * dist));
                                        if (state == com.sanitymod.entity.custom.MimicEntity.MimicState.BREAK_BLOCK) {
                                            mimic.setTargetPos(spawnPos.down());
                                        }

                                        boolean foundPos = false;
                                        for (int yOffset = 0; yOffset <= 10; yOffset++) {
                                            int y = (yOffset % 2 == 0) ? (yOffset / 2) : -(yOffset / 2);
                                            net.minecraft.util.math.BlockPos checkPos = spawnPos.add(0, y, 0);
                                            if (player.getWorld().getBlockState(checkPos).isAir()
                                                    && player.getWorld().getBlockState(checkPos.up()).isAir()) {
                                                spawnPos = checkPos;
                                                foundPos = true;
                                                break;
                                            }
                                        }

                                        if (foundPos) {
                                            mimic.setMimicState(state);
                                            mimic.refreshPositionAndAngles(spawnPos, player.getYaw(), 0);
                                            mimic.setTargetPlayer(player.getUuid());
                                            player.getWorld().spawnEntity(mimic);
                                            context.getSource().sendFeedback(
                                                    () -> Text.literal("Мимик заспавнен в состоянии: " + state.name()),
                                                    false);
                                            return 1;
                                        } else {
                                            context.getSource()
                                                    .sendError(Text.literal("Не удалось найти место для спавна!"));
                                            return 0;
                                        }
                                    }
                                }
                                return 0;
                            })));
        });
    }
}
