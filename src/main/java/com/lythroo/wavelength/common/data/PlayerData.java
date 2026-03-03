package com.lythroo.wavelength.common.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class PlayerData {

    public String townName  = "";
    public Rank   rank      = Rank.NONE;
    public PrivacyMode privacy = PrivacyMode.PUBLIC;

    public boolean pvpMode  = false;

    private static PlayerData INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wavelength_player.json";

    public static PlayerData get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static void reload() {
        INSTANCE = load();
    }

    private static PlayerData load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        File file = path.toFile();
        if (!file.exists()) {

            PlayerData fresh = new PlayerData();
            fresh.pvpMode = com.lythroo.wavelength.config.WavelengthConfig.get().defaultPvpMode;
            return fresh;
        }
        try (Reader r = new FileReader(file)) {
            PlayerData data = GSON.fromJson(r, PlayerData.class);
            return data != null ? data : new PlayerData();
        } catch (Exception e) {
            return new PlayerData();
        }
    }

    public void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (Writer w = new FileWriter(path.toFile())) {
            GSON.toJson(this, w);
        } catch (Exception e) {

        }
    }

    public String rankLine() {
        return rank.format(townName);
    }

    public void cyclePrivacy() {
        privacy = privacy.next();
        save();
    }

    public void togglePvpMode() {
        pvpMode = !pvpMode;
        save();
    }
}