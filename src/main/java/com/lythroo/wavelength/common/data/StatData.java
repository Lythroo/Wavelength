package com.lythroo.wavelength.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class StatData {

    public long blocksMined    = 0;
    public long blocksPlaced   = 0;
    public long mobsKilled     = 0;
    public long damageDealt    = 0;
    public long cropsHarvested = 0;
    public long itemsCrafted   = 0;
    public long distanceTraveled = 0;
    public long biomesSeen     = 0;

    public transient long sessionStartMs = System.currentTimeMillis();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_stats.json";

    public static StatData load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        File file = path.toFile();
        if (!file.exists()) return new StatData();
        try (Reader r = new FileReader(file)) {
            StatData data = GSON.fromJson(r, StatData.class);
            return data != null ? data : new StatData();
        } catch (Exception e) {
            return new StatData();
        }
    }

    public void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (Writer w = new FileWriter(path.toFile())) {
            GSON.toJson(this, w);
        } catch (Exception e) {

        }
    }

    public long sessionMs() {
        return System.currentTimeMillis() - sessionStartMs;
    }

    public long countForCategory(String category) {
        long raw = switch (category) {
            case "mining"   -> blocksMined;
            case "building" -> blocksPlaced;
            case "combat"   -> mobsKilled;
            case "farming"  -> cropsHarvested;
            case "crafting" -> itemsCrafted;
            case "exploring"-> distanceTraveled / 1000;
            default         -> 0;
        };
        return Math.max(0, raw);
    }

    public Achievement badgeForCategory(String category) {
        return Achievement.forCount(countForCategory(category), category);
    }

    public Achievement topBadge() {
        Achievement best = Achievement.NONE;
        for (String cat : new String[]{"mining","building","combat","farming","crafting","exploring"}) {
            Achievement a = badgeForCategory(cat);
            if (a.ordinal() > best.ordinal()) best = a;
        }
        return best;
    }
}