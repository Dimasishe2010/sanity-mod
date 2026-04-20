package com.sanitymod.sanity;

import com.sanitymod.util.IEntityDataSaver;
import net.minecraft.nbt.NbtCompound;

public class SanityData {
    public static float addSanity(IEntityDataSaver player, float amount) {
        NbtCompound nbt = player.getPersistentData();
        float sanity = nbt.getFloat("sanity");
        if (!nbt.contains("sanity")) {
            sanity = 100.0f; // Default starting sanity
        }
        
        sanity += amount;
        
        // Clamp between 0 and 100
        if(sanity < 0.0f) {
            sanity = 0.0f;
        } else if (sanity > 100.0f) {
            sanity = 100.0f;
        }
        
        nbt.putFloat("sanity", sanity);
        return sanity;
    }
    
    public static float getSanity(IEntityDataSaver player) {
        NbtCompound nbt = player.getPersistentData();
        if (!nbt.contains("sanity")) {
            nbt.putFloat("sanity", 100.0f);
            return 100.0f;
        }
        return nbt.getFloat("sanity");
    }
    
    public static float setSanity(IEntityDataSaver player, float amount) {
        NbtCompound nbt = player.getPersistentData();
        
        float sanity = amount;
        if(sanity < 0.0f) {
            sanity = 0.0f;
        } else if (sanity > 100.0f) {
            sanity = 100.0f;
        }
        
        nbt.putFloat("sanity", sanity);
        return sanity;
    }
}
