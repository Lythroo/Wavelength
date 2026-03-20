package com.lythroo.wavelength.client.tracking;

import com.lythroo.wavelength.client.render.LevelUpAnimator;
import com.lythroo.wavelength.common.data.RemoteStatCache;
import com.lythroo.wavelength.common.data.StatData;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.client.MinecraftClient;

public class StatTracker {

    private static final int REQUEST_INTERVAL_TICKS = 100;

    private static final int POLL_TICKS             = 20;

    private static final int SAVE_TICKS             = 100;

    private static StatData cachedData  = null;
    private static int      tickCounter = 0;

    public static StatData getData() {
        if (cachedData == null) cachedData = StatData.load();
        return cachedData;
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) return;
        if (cachedData == null) cachedData = StatData.load();

        tickCounter++;

        RemoteStatCache.Entry remote = RemoteStatCache.get(client.player.getUuid());
        if (remote != null) {
            cachedData.blocksMined      = remote.blocksMined;
            cachedData.blocksPlaced     = remote.blocksPlaced;
            cachedData.mobsKilled       = remote.mobsKilled;
            cachedData.cropsHarvested   = remote.cropsHarvested;
            cachedData.itemsCrafted     = remote.itemsCrafted;
            cachedData.distanceTraveled = remote.distanceTraveled;
        }

        if (remote == null) {

            if (tickCounter == 1 || tickCounter % REQUEST_INTERVAL_TICKS == 0) {
                client.getNetworkHandler().sendPacket(
                        new net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket(
                                net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode.REQUEST_STATS));
            }
            if (tickCounter % POLL_TICKS == 0) {
                pollVanillaStats(client);
            }
        }

        String[] cats = {"mining","building","combat","farming","crafting","exploring"};
        for (String cat : cats) {
            LevelUpAnimator.checkAndTrigger(cat, cachedData.badgeForCategory(cat));
        }

        if (tickCounter % SAVE_TICKS == 0) {
            cachedData.save();
        }
    }

    public static void reset() {
        if (cachedData != null) cachedData.save();
        cachedData  = null;
        tickCounter = 0;
        LevelUpAnimator.reset();
    }

    private static void pollVanillaStats(MinecraftClient client) {
        if (client.player == null) return;
        StatHandler sh = client.player.getStatHandler();
        if (sh == null) return;

        try {
            cachedData.mobsKilled =
                    (long) sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.MOB_KILLS))
                            + (long) sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAYER_KILLS));

            cachedData.distanceTraveled =
                    (long) sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM))
                            + (long) sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM))
                            + (long) sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SWIM_ONE_CM))
                            + (long) sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM))
                            + (long) sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM));

            long mined = 0;
            for (Block block : Registries.BLOCK)
                mined += sh.getStat(Stats.MINED.getOrCreateStat(block));
            cachedData.blocksMined = mined;

            long placed = 0;
            for (Item item : Registries.ITEM)
                if (item instanceof BlockItem)
                    placed += sh.getStat(Stats.USED.getOrCreateStat(item));
            cachedData.blocksPlaced = placed;

            long crafted = 0;
            for (Item item : Registries.ITEM)
                crafted += sh.getStat(Stats.CRAFTED.getOrCreateStat(item));
            cachedData.itemsCrafted = crafted;

        } catch (Exception ignored) {}
    }
}