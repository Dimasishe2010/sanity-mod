package com.sanitymod.client;

import com.sanitymod.sanity.SanityData;
import com.sanitymod.sound.ModSounds;
import com.sanitymod.util.IEntityDataSaver;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;

import java.util.Random;

public class SoundEngine {
    private static final Random random = new Random();
    private static int breathingTimer = 0;
    private static int ambientTimer = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null)
                return;

            float sanity = SanityData.getSanity((IEntityDataSaver) client.player);

            if (sanity < 50.0f) {
                breathingTimer++;
                // Play breathing every ~4 seconds (80 ticks) when sanity < 50
                // Frequency increases as sanity drops
                int threshold = (int) (40 + sanity); // At 50 sanity = 90 ticks. At 0 sanity = 40 ticks.

                if (breathingTimer >= threshold) {
                    breathingTimer = 0;
                    // Volume scales from 0.1 (at 50 sanity) to 1.0 (at 0 sanity)
                    float volume = 1.0f - (sanity / 50.0f);
                    if (volume < 0.1f)
                        volume = 0.1f;

                    client.world.playSound(
                            client.player.getX(), client.player.getY(), client.player.getZ(),
                            ModSounds.HEAVY_BREATHING,
                            SoundCategory.AMBIENT,
                            volume,
                            1.0f,
                            false);
                }
            } else {
                breathingTimer = 0;
            }

            if (sanity < 30.0f) {
                ambientTimer++;
                // Random ambient sounds
                if (ambientTimer >= 100 && random.nextInt(200) == 0) {
                    ambientTimer = 0;
                    int soundChoice = random.nextInt(3);
                    net.minecraft.sound.SoundEvent sound = ModSounds.AMBIENT_WHISPER;
                    if (soundChoice == 1)
                        sound = ModSounds.AMBIENT_STEPS;
                    else if (soundChoice == 2)
                        sound = ModSounds.AMBIENT_SCRAPE;

                    float volume = 1.0f - (sanity / 30.0f);

                    client.world.playSound(
                            client.player.getX(), client.player.getY(), client.player.getZ(),
                            sound,
                            SoundCategory.AMBIENT,
                            volume,
                            0.8f + random.nextFloat() * 0.4f,
                            false);
                }
            } else {
                ambientTimer = 0;
            }
        });
    }
}
