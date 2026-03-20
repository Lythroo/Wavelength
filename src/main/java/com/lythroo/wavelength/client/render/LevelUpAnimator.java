package com.lythroo.wavelength.client.render;

import com.lythroo.wavelength.common.data.Achievement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.Random;

public class LevelUpAnimator {

    private static final java.util.Map<String, Achievement> lastBadges =
            new java.util.HashMap<>();
    private static final Random RANDOM = new Random();

    public static void checkAndTrigger(String category, Achievement current) {
        Achievement last = lastBadges.get(category);
        if (last == null) {

            lastBadges.put(category, current);
            return;
        }
        if (current.ordinal() > last.ordinal()) {
            trigger(category, current);
        }
        lastBadges.put(category, current);
    }

    public static void reset() {
        lastBadges.clear();
    }

    private static void trigger(String category, Achievement badge) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (com.lythroo.wavelength.common.data.PlayerData.get().pvpMode) return;

        com.lythroo.wavelength.common.data.AchievementTimeline.addEvent(
                new com.lythroo.wavelength.common.data.AchievementTimeline.TimelineEvent(
                        client.player.getUuid(),
                        client.player.getName().getString(),
                        category,
                        badge,
                        System.currentTimeMillis()));

        PlayerEntity player = client.player;
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        float pitch = 0.85f + badge.ordinal() * 0.12f;
        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, pitch);

        int helixCount = 18 + badge.ordinal() * 14;
        for (int i = 0; i < helixCount; i++) {
            double t      = i / (double) helixCount;
            double angle  = t * Math.PI * 5.0;
            double radius = 0.7 + t * 0.7;
            double height = t * 2.8;
            double vx = Math.cos(angle) * 0.03;
            double vz = Math.sin(angle) * 0.03;
            client.world.addParticleClient(ParticleTypes.END_ROD,
                    true, false,
                    px + Math.cos(angle) * radius,
                    py + height,
                    pz + Math.sin(angle) * radius,
                    vx, 0.05, vz);
        }

        var burstType = switch (badge) {
            case BRONZE  -> ParticleTypes.CRIT;
            case SILVER  -> ParticleTypes.ENCHANT;
            case GOLD    -> ParticleTypes.CRIT;
            case DIAMOND -> ParticleTypes.ENCHANT;
            default      -> ParticleTypes.CRIT;
        };
        int burstCount = 25 + badge.ordinal() * 12;
        for (int i = 0; i < burstCount; i++) {
            double vx = (RANDOM.nextDouble() - 0.5) * 0.7;
            double vy = RANDOM.nextDouble() * 0.45 + 0.1;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.7;
            client.world.addParticleClient(burstType,
                    true, false,
                    px, py + 1.0, pz,
                    vx, vy, vz);
        }

        if (badge == Achievement.GOLD || badge == Achievement.DIAMOND) {
            int ringCount = 24;
            for (int i = 0; i < ringCount; i++) {
                double angle = (i / (double) ringCount) * Math.PI * 2;
                double vx = Math.cos(angle) * 0.22;
                double vz = Math.sin(angle) * 0.22;
                client.world.addParticleClient(ParticleTypes.END_ROD,
                        true, false,
                        px + Math.cos(angle) * 0.4,
                        py + 1.9,
                        pz + Math.sin(angle) * 0.4,
                        vx, 0.12, vz);
            }
        }

        if (badge == Achievement.DIAMOND) {
            for (int i = 0; i < 30; i++) {
                double offX = (RANDOM.nextDouble() - 0.5) * 0.6;
                double offZ = (RANDOM.nextDouble() - 0.5) * 0.6;
                double vy   = 0.1 + RANDOM.nextDouble() * 0.25;
                client.world.addParticleClient(ParticleTypes.REVERSE_PORTAL,
                        true, false,
                        px + offX,
                        py + RANDOM.nextDouble() * 2.2,
                        pz + offZ,
                        (RANDOM.nextDouble() - 0.5) * 0.08,
                        vy,
                        (RANDOM.nextDouble() - 0.5) * 0.08);
            }
        }
    }
}