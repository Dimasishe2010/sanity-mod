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

    public static void registerC2SPackets() {
        
    }

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(SANITY_SYNC_ID, (client, handler, buf, responseSender) -> {
            float sanity = buf.readFloat();
            client.execute(() -> {
                if (client.player != null) {
                    ((com.sanitymod.util.IEntityDataSaver) client.player).getPersistentData().putFloat("sanity", sanity);
                }
            });
        });
    }
    
    public static void syncSanity(float sanity, ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(sanity);
        ServerPlayNetworking.send(player, SANITY_SYNC_ID, buf);
    }
}
