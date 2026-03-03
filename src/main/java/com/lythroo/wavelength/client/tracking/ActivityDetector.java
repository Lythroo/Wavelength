package com.lythroo.wavelength.client.tracking;

import com.lythroo.wavelength.common.data.ActivityType;
import com.lythroo.wavelength.common.data.PlayerData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActivityDetector {

    private static final int   AFK_TICKS        = 6_000;
    private static final int   EVAL_INTERVAL    = 3;
    private static final long  ACTION_LINGER_MS = 3_000;
    private static final long  SERVER_LINGER_MS = 5_000;

    private static final Map<UUID, ActivityType> activities     = new HashMap<>();

    private static final Map<UUID, Long>         lastActionMs   = new HashMap<>();

    private static final Map<UUID, ActivityType> lastAction     = new HashMap<>();
    private static final Map<UUID, Vec3d>        lastPos        = new HashMap<>();

    private static final Map<UUID, ActivityType> serverActivity = new HashMap<>();

    private static final Map<UUID, Long>         serverPushedMs = new HashMap<>();

    private static int  localIdleTicks     = 0;
    private static int  evalCooldown       = 0;
    private static int  localPrevDamage    = -1;
    private static Item localPrevHeldBlock = null;
    private static int  localPrevUsed      = 0;

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        activities.put(client.player.getUuid(), detectLocal(client.player, client));

        if (--evalCooldown <= 0) {
            evalCooldown = EVAL_INTERVAL;
            for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                if (p == client.player) continue;

                if (!hasServerActivity(p.getUuid())) {
                    activities.put(p.getUuid(), detectObservedFallback(p));
                }
                lastPos.put(p.getUuid(), pos(p));
            }
        }
    }

    public static void putServerActivity(UUID uuid, ActivityType type) {
        serverActivity.put(uuid, type);
        serverPushedMs.put(uuid, System.currentTimeMillis());

        activities.put(uuid, type);
    }

    private static boolean hasServerActivity(UUID uuid) {
        Long ms = serverPushedMs.get(uuid);
        return ms != null && (System.currentTimeMillis() - ms) < SERVER_LINGER_MS;
    }

    private static ActivityType detectLocal(PlayerEntity player, MinecraftClient client) {
        if (PlayerData.get().pvpMode) return ActivityType.IDLE;

        UUID  uuid = player.getUuid();
        Vec3d cur  = pos(player);
        Vec3d prev = lastPos.getOrDefault(uuid, cur);
        lastPos.put(uuid, cur);

        if (cur.squaredDistanceTo(prev) > 0.001) localIdleTicks = 0;
        else                                      localIdleTicks++;
        if (localIdleTicks >= AFK_TICKS) return ActivityType.AFK;

        ActivityType linger = linger(uuid);
        if (linger != null) return linger;

        StatHandler sh = client.player.getStatHandler();

        if (sh != null) {
            try {
                int dmg = sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.DAMAGE_DEALT));
                if (localPrevDamage < 0) {
                    localPrevDamage = dmg;
                } else if (dmg > localPrevDamage) {
                    localPrevDamage = dmg;
                    stamp(uuid, ActivityType.FIGHTING);
                    return ActivityType.FIGHTING;
                }
                localPrevDamage = dmg;
            } catch (Exception ignored) {}
        }

        ItemStack held = player.getMainHandStack();

        if (player.handSwinging && isMiningTool(held)) {
            stamp(uuid, ActivityType.MINING);
            return ActivityType.MINING;
        }

        if (!held.isEmpty() && held.getItem() instanceof BlockItem && sh != null) {
            Item heldItem = held.getItem();
            try {
                int usedNow = sh.getStat(Stats.USED.getOrCreateStat(heldItem));
                if (heldItem != localPrevHeldBlock) {

                    localPrevHeldBlock = heldItem;
                    localPrevUsed      = usedNow;
                } else if (usedNow > localPrevUsed) {
                    localPrevUsed = usedNow;
                    stamp(uuid, ActivityType.BUILDING);
                    return ActivityType.BUILDING;
                }
            } catch (Exception ignored) {
                localPrevHeldBlock = null;
            }
        } else {
            localPrevHeldBlock = null;
            localPrevUsed      = 0;
        }

        return passiveActivity(held);
    }

    private static ActivityType detectObservedFallback(AbstractClientPlayerEntity player) {
        UUID uuid = player.getUuid();

        Vec3d cur  = pos(player);
        Vec3d last = lastPos.get(uuid);
        boolean moved = last != null && cur.squaredDistanceTo(last) > 0.001;

        ActivityType linger = linger(uuid);
        if (linger != null) return linger;

        ItemStack held = player.getMainHandStack();

        if (player.handSwinging && isWeapon(held)) {
            stamp(uuid, ActivityType.FIGHTING);
            return ActivityType.FIGHTING;
        }

        if (player.handSwinging && isMiningTool(held)) {
            stamp(uuid, ActivityType.MINING);
            return ActivityType.MINING;
        }

        if (held.isEmpty()) return moved ? ActivityType.EXPLORING : ActivityType.AFK;
        ActivityType passive = passiveActivity(held);
        return passive == ActivityType.IDLE
                ? (moved ? ActivityType.EXPLORING : ActivityType.AFK)
                : passive;
    }

    private static ActivityType linger(UUID uuid) {
        Long ms = lastActionMs.get(uuid);
        if (ms == null) return null;
        if (System.currentTimeMillis() - ms < ACTION_LINGER_MS) return lastAction.get(uuid);
        lastActionMs.remove(uuid);
        lastAction.remove(uuid);
        return null;
    }

    private static void stamp(UUID uuid, ActivityType type) {
        lastActionMs.put(uuid, System.currentTimeMillis());
        lastAction.put(uuid, type);
    }

    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.endsWith("_sword") || path.endsWith("_axe");
    }

    private static boolean isMiningTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.endsWith("_pickaxe") || path.endsWith("_shovel");
    }

    private static ActivityType passiveActivity(ItemStack stack) {
        if (stack.isEmpty()) return ActivityType.IDLE;
        Item item = stack.getItem();
        if (item instanceof FilledMapItem) return ActivityType.EXPLORING;
        try {
            if (item.getClass().getSimpleName().equals("ShearsItem")) return ActivityType.FARMING;
        } catch (Exception ignored) {}
        String path = Registries.ITEM.getId(item).getPath();
        if (path.endsWith("_hoe"))    return ActivityType.FARMING;
        if (path.endsWith("_shears")) return ActivityType.FARMING;
        if (path.equals("compass") || path.equals("filled_map")) return ActivityType.EXPLORING;
        return ActivityType.IDLE;
    }

    private static Vec3d pos(net.minecraft.entity.Entity e) {
        return new Vec3d(e.getX(), e.getY(), e.getZ());
    }

    public static ActivityType getActivity(UUID uuid) {

        if (hasServerActivity(uuid)) {
            return serverActivity.getOrDefault(uuid, ActivityType.IDLE);
        }
        return activities.getOrDefault(uuid, ActivityType.IDLE);
    }

    public static void clear() {
        activities.clear();
        lastActionMs.clear();
        lastAction.clear();
        lastPos.clear();
        serverActivity.clear();
        serverPushedMs.clear();
        localIdleTicks     = 0;
        evalCooldown       = 0;
        localPrevDamage    = -1;
        localPrevHeldBlock = null;
        localPrevUsed      = 0;
    }
}