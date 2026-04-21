package com.sanitymod.network;

import com.sanitymod.SanityMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModMessages {
    public static final Identifier SANITY_SYNC_ID = new Identifier(SanityMod.MOD_ID, "sanity_sync");
    public static final Identifier JUMPSCARE_SYNC_ID = new Identifier(SanityMod.MOD_ID, "jumpscare_sync");

    public static void registerC2SPackets() {

    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SANITY_SYNC_ID, (client, handler, buf, responseSender) -> {
            float sanity = buf.readFloat();
            client.execute(() -> {
                if (client.player != null) {
                    ((com.sanitymod.util.IEntityDataSaver) client.player).getPersistentData().putFloat("sanity",
                            sanity);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(JUMPSCARE_SYNC_ID, (client, handler, buf, responseSender) -> {
            int durationTicks = buf.readInt();
            client.execute(() -> {
                com.sanitymod.client.SanityHudOverlay.triggerJumpscare(durationTicks);
                client.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(com.sanitymod.sound.ModSounds.MIMIC_CHASE, 1.0F, 1.0F));
            });
        });
    }

    public static void syncSanity(float sanity, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(sanity);
        ServerPlayNetworking.send(player, SANITY_SYNC_ID, buf);
    }

    public static void sendJumpscare(ServerPlayerEntity player, int durationTicks) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(durationTicks);
        ServerPlayNetworking.send(player, JUMPSCARE_SYNC_ID, buf);
    }
}
