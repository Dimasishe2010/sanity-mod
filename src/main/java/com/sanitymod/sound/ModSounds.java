package com.sanitymod.sound;

import com.sanitymod.SanityMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent HEAVY_BREATHING = registerSoundEvent("heavy_breathing");
    public static final SoundEvent MIMIC_STATIC = registerSoundEvent("mimic_static");
    public static final SoundEvent AMBIENT_WHISPER = registerSoundEvent("ambient_whisper");
    public static final SoundEvent AMBIENT_STEPS = registerSoundEvent("ambient_steps");
    public static final SoundEvent AMBIENT_SCRAPE = registerSoundEvent("ambient_scrape");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier(SanityMod.MOD_ID, name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        SanityMod.LOGGER.info("Registering Sounds for " + SanityMod.MOD_ID);
    }
}
