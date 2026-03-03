package com.lythroo.wavelength.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public class WavelengthConfig {

    public boolean showTopBar            = true;
    public int     topBarOpacity         = 85;

    public boolean showPrivacySection    = true;
    public boolean showNearbySection     = true;
    public boolean showTownSection       = true;
    public boolean showEventSection      = true;

    public static final int NEARBY_RANGE_BLOCKS = 100;
    public int     maxNearbyShown        = 5;

    public boolean showOfflineFriends    = true;

    public String  defaultSortMode       = "DISTANCE";

    public boolean showActivityTag       = true;
    public boolean showRankUnderName     = true;

    public boolean muteTimelineToasts    = false;

    public boolean defaultPvpMode        = false;

    private static WavelengthConfig INSTANCE;
    private static final Gson   GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "wavelength_config.json";

    public static WavelengthConfig get() {
        if (INSTANCE == null) INSTANCE = load();
        return INSTANCE;
    }

    public static void init() {
        INSTANCE = load();
    }

    private static WavelengthConfig load() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve(FILE);
        if (p.toFile().exists()) {
            try (Reader r = new FileReader(p.toFile())) {
                WavelengthConfig c = GSON.fromJson(r, WavelengthConfig.class);
                return c != null ? c : new WavelengthConfig();
            } catch (Exception ignored) {}
        }
        return new WavelengthConfig();
    }

    public void save() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve(FILE);
        try (Writer w = new FileWriter(p.toFile())) {
            GSON.toJson(this, w);
        } catch (Exception ignored) {}
    }

    public static Screen buildScreen(Screen parent) {
        WavelengthConfig cfg = get();

        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("Wavelength Settings"))

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Top Bar"))
                        .tooltip(Text.literal("Controls the HUD bar shown at the top of your screen."))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Top Bar"))
                                .description(OptionDescription.of(Text.literal(
                                        "Toggles the entire top bar HUD on or off.")))
                                .binding(true, () -> cfg.showTopBar, v -> cfg.showTopBar = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Bar Opacity (%)"))
                                .description(OptionDescription.of(Text.literal(
                                        "How opaque the bar background is. Lower = more transparent.")))
                                .binding(85, () -> cfg.topBarOpacity, v -> cfg.topBarOpacity = v)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(10, 100).step(5))
                                .build())

                        .group(OptionGroup.createBuilder()
                                .name(Text.literal("Bar Sections"))
                                .description(OptionDescription.of(Text.literal(
                                        "Toggle individual sections of the top bar on or off.")))

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Privacy Section"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Shows your current privacy mode (Public / Friends / Ghost).")))
                                        .binding(true, () -> cfg.showPrivacySection, v -> cfg.showPrivacySection = v)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Nearby Players Section"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Shows a count of and names of nearby players.")))
                                        .binding(true, () -> cfg.showNearbySection, v -> cfg.showNearbySection = v)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Town Section"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Shows your rank and town name in the top bar, "
                                                        + "e.g. \"Mayor of Riverside\".")))
                                        .binding(true, () -> cfg.showTownSection, v -> cfg.showTownSection = v)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())

                                .option(Option.<Boolean>createBuilder()
                                        .name(Text.literal("Event Section"))
                                        .description(OptionDescription.of(Text.literal(
                                                "Shows the name of the currently active server event, "
                                                        + "if one exists.")))
                                        .binding(true, () -> cfg.showEventSection, v -> cfg.showEventSection = v)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())

                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Nearby Players"))
                        .tooltip(Text.literal("Controls how nearby players are displayed in the top bar."))

                        .option(Option.<Integer>createBuilder()
                                .name(Text.literal("Max Players Shown in Bar"))
                                .description(OptionDescription.of(Text.literal(
                                        "Maximum number of nearby player names shown in the top bar HUD. "
                                                + "Does not affect the Players tab.")))
                                .binding(5, () -> cfg.maxNearbyShown, v -> cfg.maxNearbyShown = v)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 10).step(1))
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Players Tab"))
                        .tooltip(Text.literal("Settings for the Players tab inside the Wavelength Menu."))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Offline Friends"))
                                .description(OptionDescription.of(Text.literal(
                                        "Show an Offline Friends section at the bottom of the Players tab "
                                                + "for friends who are not currently in the world.")))
                                .binding(true, () -> cfg.showOfflineFriends, v -> cfg.showOfflineFriends = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<String>createBuilder()
                                .name(Text.literal("Default Sort Mode"))
                                .description(OptionDescription.of(Text.literal(
                                        "How to sort players in the Players tab when the menu is first opened.\n"
                                                + "Distance = closest first,  Activity = grouped by activity,  Name = alphabetical.")))
                                .binding("DISTANCE", () -> cfg.defaultSortMode, v -> cfg.defaultSortMode = v)
                                .controller(opt -> CyclingListControllerBuilder.create(opt)
                                        .values(List.of("DISTANCE", "ACTIVITY", "NAME"))
                                        .valueFormatter(v -> Text.literal(switch (v) {
                                            case "ACTIVITY" -> "Activity";
                                            case "NAME"     -> "Name";
                                            default         -> "Distance";
                                        })))
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Nameplates"))
                        .tooltip(Text.literal("Controls the extra information shown above other players' heads."))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Activity Tag"))
                                .description(OptionDescription.of(Text.literal(
                                        "Show a colored tag like [Mining] or [Fighting] above each player's name.")))
                                .binding(true, () -> cfg.showActivityTag, v -> cfg.showActivityTag = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Show Rank Under Name"))
                                .description(OptionDescription.of(Text.literal(
                                        "Show a player's rank and town beneath their name, e.g. \"Mayor of Riverside\".")))
                                .binding(true, () -> cfg.showRankUnderName, v -> cfg.showRankUnderName = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Notifications"))
                        .tooltip(Text.literal("Controls chat messages triggered by server-wide events."))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Mute Timeline Toasts"))
                                .description(OptionDescription.of(Text.literal(
                                        "When enabled, suppresses the chat notifications for badge unlocks, "
                                                + "project updates, and event start/end announcements.\n"
                                                + "The Timeline tab still records everything — only the chat messages are silenced.")))
                                .binding(false, () -> cfg.muteTimelineToasts, v -> cfg.muteTimelineToasts = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Privacy & Identity"))
                        .tooltip(Text.literal("Controls default identity settings on a fresh install."))

                        .option(Option.<Boolean>createBuilder()
                                .name(Text.literal("Default PVP Mode"))
                                .description(OptionDescription.of(Text.literal(
                                        "When enabled, PVP Mode is on by default for new installs. "
                                                + "PVP Mode hides your activity tag from other players' nameplates.")))
                                .binding(false, () -> cfg.defaultPvpMode, v -> cfg.defaultPvpMode = v)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Keybinds"))
                        .tooltip(Text.literal("Information about configuring Wavelength keybindings."))

                        .option(LabelOption.create(Text.literal(
                                "§aWavelength Menu§r — default: H\n"
                                        + "§aPVP Mode Toggle§r — default: unbound\n\n"
                                        + "§7To change them, go to:§r\n"
                                        + "Options → Controls → Wavelength")))

                        .build())

                .save(cfg::save)
                .build()
                .generateScreen(parent);
    }
}