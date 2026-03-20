package com.lythroo.wavelength.client.render;

import com.lythroo.wavelength.common.data.ActivityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

public class SkinImageLoader {

    public static void evict(String username) {}

    public static void evictAll() {}

    public static void reload(String username) {}

    public static Identifier getOrLoad(String username, ActivityType activity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(username)) {
                    return player.getSkin().body().texturePath();
                }
            }
        }

        return Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    }

    public static Identifier getFromEntity(AbstractClientPlayerEntity player) {
        return player.getSkin().body().texturePath();
    }

    public static String poseFor(ActivityType activity) {
        return "default";
    }
}