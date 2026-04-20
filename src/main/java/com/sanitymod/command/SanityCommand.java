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
                                context.getSource().sendFeedback(() -> Text.literal("Рассудок установлен на: " + amount), false);
                                return 1;
                            }
                            return 0;
                        })
                    )
                )
                .then(CommandManager.literal("get")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            float sanity = SanityData.getSanity((IEntityDataSaver) player);
                            context.getSource().sendFeedback(() -> Text.literal("Текущий рассудок: " + sanity), false);
                            return 1;
                        }
                        return 0;
                    })
                )
            );
        });
    }
}
