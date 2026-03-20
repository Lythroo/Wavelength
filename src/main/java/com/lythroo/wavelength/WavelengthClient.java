package com.lythroo.wavelength;

import com.lythroo.wavelength.client.gui.HubScreen;
import com.lythroo.wavelength.client.gui.TopBarHud;
import com.lythroo.wavelength.client.render.NameplateHudRenderer;
import com.lythroo.wavelength.client.tracking.ActivityDetector;
import com.lythroo.wavelength.client.tracking.StatTracker;
import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.common.data.AchievementTimeline;
import com.lythroo.wavelength.common.data.ClientProjectCache;
import com.lythroo.wavelength.common.data.ClientEventCache;

import java.util.UUID;

import com.lythroo.wavelength.common.data.PersistentPlayerCache;
import com.lythroo.wavelength.common.network.ServerModDetector;
import com.lythroo.wavelength.common.network.SyncPacket;
import com.lythroo.wavelength.config.WavelengthConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class WavelengthClient implements ClientModInitializer {

    public static KeyBinding menuKey;
    public static KeyBinding pvpKey;

    private static final int INITIAL_PUSH_DELAY = 5;
    private static final int REPUSH_DELAY       = 100;

    private static int pushCountdown = -1;
    private static int repushesLeft  = 0;

    private static final java.util.Set<String> announcedStartedEvents =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static int eventStartCheckTick = 0;

    private static int eventsTabRefreshTick = 0;

    private static final KeyBinding.Category KEY_CATEGORY =
            new KeyBinding.Category(Identifier.of(Wavelength.MOD_ID, "keys"));

    @Override
    public void onInitializeClient() {
        Wavelength.LOGGER.info("[Wavelength] Client init started...");

        NameplateHudRenderer.register();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wavelength.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                KEY_CATEGORY
        ));
        pvpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wavelength.pvp_toggle",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                KEY_CATEGORY
        ));

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.PLAYER_DATA_SYNC_ID,
                (payload, context) -> {
                    ServerModDetector.markPresent();
                    RemotePlayerCache.put(payload);
                    String cachedName = null;
                    MinecraftClient mc2 = context.client();
                    if (mc2.world != null) {
                        for (var p : mc2.world.getPlayers()) {
                            if (p.getUuid().equals(payload.playerUuid())) {
                                cachedName = p.getName().getString(); break;
                            }
                        }
                    }
                    PersistentPlayerCache.putIdentity(payload, cachedName);
                    Wavelength.LOGGER.debug(
                            "[Wavelength] PlayerData for {}: town='{}' rank={} privacy={}",
                            payload.playerUuid(), payload.townName(),
                            payload.rankName(), payload.privacyName());
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.STAT_DATA_SYNC_ID,
                (payload, context) -> {
                    ServerModDetector.markPresent();
                    RemoteStatCache.put(payload);
                    PersistentPlayerCache.putStats(payload);

                    MinecraftClient mc3 = context.client();
                    boolean isLocalPlayer = mc3.player != null
                            && mc3.player.getUuid().equals(payload.playerUuid());
                    String statName = null;
                    if (!isLocalPlayer && mc3.world != null) {
                        for (var p : mc3.world.getPlayers()) {
                            if (p.getUuid().equals(payload.playerUuid())) {
                                statName = p.getName().getString(); break;
                            }
                        }
                    }
                    if (!isLocalPlayer && statName == null) {
                        PersistentPlayerCache.Entry pce =
                                PersistentPlayerCache.get(payload.playerUuid());
                        if (pce != null && !pce.username.isBlank()) statName = pce.username;
                    }
                    if (!isLocalPlayer && statName != null) {
                        AchievementTimeline.checkAndRecord(
                                payload.playerUuid(), statName,
                                payload.blocksMined(),   payload.blocksPlaced(),
                                payload.mobsKilled(),    payload.cropsHarvested(),
                                payload.itemsCrafted(),  payload.distanceTraveled() / 1000);
                    }

                    Wavelength.LOGGER.debug(
                            "[Wavelength] Stats for {}: mined={} placed={} killed={}",
                            payload.playerUuid(), payload.blocksMined(),
                            payload.blocksPlaced(), payload.mobsKilled());
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.PLAYER_LEFT_ID,
                (payload, context) -> {
                    RemotePlayerCache.remove(payload.playerUuid());
                    RemoteStatCache.remove(payload.playerUuid());
                    AchievementTimeline.removePlayer(payload.playerUuid());
                    Wavelength.LOGGER.debug("[Wavelength] Evicted cache for {} (left).",
                            payload.playerUuid());
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.ACTIVITY_SYNC_ID,
                (payload, context) -> {
                    try {
                        ActivityType type = ActivityType.valueOf(payload.activityName());
                        ActivityDetector.putServerActivity(payload.playerUuid(), type);
                    } catch (IllegalArgumentException ignored) {}
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.FRIEND_NOTIFY_S2C_ID,
                (payload, context) -> {
                    ServerModDetector.markPresent();
                    final UUID   from   = payload.fromUuid();
                    final String name   = payload.fromUsername();
                    final String action = payload.action();
                    context.client().execute(() -> {
                        MinecraftClient mc = context.client();
                        switch (action) {
                            case "REQUEST" -> {

                                FriendRequests.addIncoming(from, name);
                                if (mc.player != null && !pvpMode() && !muteChat()) {
                                    mc.player.playSound(SoundEvents.UI_TOAST_IN);
                                    mc.inGameHud.getChatHud().addMessage(
                                            Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                                    .append(Text.literal(name).formatted(Formatting.AQUA))
                                                    .append(Text.literal(" sent you a friend request! Open the Menu (H) to accept.")
                                                            .withColor(0xFFCCCCCC)));
                                }
                                HubScreen.refreshIfOpen();
                            }
                            case "REQUEST_PENDING" -> {

                                FriendRequests.addOutgoing(from, name);
                                HubScreen.refreshIfOpen();
                            }
                            case "ACCEPT" -> {

                                FriendRequests.removeOutgoing(from);
                                FriendList.add(from, name);
                                if (mc.player != null && !pvpMode() && !muteChat()) {
                                    mc.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
                                    mc.inGameHud.getChatHud().addMessage(
                                            Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                                    .append(Text.literal(name).formatted(Formatting.GREEN))
                                                    .append(Text.literal(" accepted your friend request!")
                                                            .withColor(0xFFCCCCCC)));
                                }
                                HubScreen.refreshIfOpen();
                            }
                            case "DECLINE" -> {
                                FriendRequests.removeOutgoing(from);
                                if (mc.player != null && !pvpMode() && !muteChat()) {
                                    mc.player.playSound(SoundEvents.UI_TOAST_OUT, 0.6f, 0.9f);
                                    mc.inGameHud.getChatHud().addMessage(
                                            Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                                    .append(Text.literal(name).formatted(Formatting.RED))
                                                    .append(Text.literal(" declined your friend request.")
                                                            .withColor(0xFFAAAAAA)));
                                }
                                HubScreen.refreshIfOpen();
                            }
                            case "CANCEL" -> {
                                FriendRequests.removeIncoming(from);
                                HubScreen.refreshIfOpen();
                            }
                        }
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.FRIEND_LIST_SYNC_S2C_ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        FriendList.replaceAll(payload.friendUuids(), payload.friendUsernames());

                        FriendRequests.cleanConfirmedFriends(payload.friendUuids());

                        HubScreen.refreshIfOpen();
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.PROJECT_SYNC_ID,
                (payload, context) -> context.client().execute(() -> {
                    ClientProjectCache.put(payload.project());
                    HubScreen.refreshIfOpen();
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.EVENT_SYNC_ID,
                (payload, context) -> context.client().execute(() -> {
                    ClientEventCache.put(payload.event());
                    HubScreen.refreshIfOpen();
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.EVENT_REMOVE_S2C_ID,
                (payload, context) -> context.client().execute(() -> {
                    ClientEventCache.remove(payload.eventId());
                    HubScreen.refreshIfOpen();
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.PROJECT_REMOVE_S2C_ID,
                (payload, context) -> context.client().execute(() -> {
                    ClientProjectCache.remove(payload.projectId());
                    HubScreen.refreshIfOpen();
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.PROJECT_NOTIFY_S2C_ID,
                (payload, context) -> context.client().execute(() -> {
                    MinecraftClient mc2 = context.client();
                    String projName = payload.projectName();
                    String action   = payload.action();

                    if (action.startsWith("TIMELINE_")) {
                        String timelineAction = action.substring("TIMELINE_".length());

                        String fromName = payload.fromName();
                        String ownerName = fromName;
                        String extraName = "";
                        int sep = fromName.indexOf('\0');
                        if (sep >= 0) {
                            ownerName = fromName.substring(0, sep);
                            extraName = fromName.substring(sep + 1);
                        }
                        UUID ownerUuid;
                        try { ownerUuid = UUID.fromString(payload.fromUuid()); }
                        catch (Exception e) { ownerUuid = new UUID(0, 0); }

                        AchievementTimeline.addProjectEvent(
                                new AchievementTimeline.ProjectTimelineEvent(
                                        ownerUuid, ownerName, projName,
                                        timelineAction, extraName,
                                        System.currentTimeMillis()));

                        if (mc2.player != null && !timelineAction.equals("COLLABORATOR_LEFT")
                                && !WavelengthConfig.get().muteTimelineToasts) {
                            String msg = switch (timelineAction) {
                                case "CREATED"             -> "🏗 " + ownerName + " started \"" + projName + "\"";
                                case "HELP_WANTED"         -> "🤝 " + ownerName + " needs help on \"" + projName + "\"";
                                case "COLLABORATOR_JOINED" -> "✦ " + extraName + " joined \"" + projName + "\"";
                                case "COMPLETED"           -> "🎉 \"" + projName + "\" complete! Congrats " + ownerName + "!";
                                case "EVENT_CREATED"       -> "📅 " + ownerName + " created event \"" + projName + "\"";
                                case "EVENT_ENDED"         -> "✓ " + ownerName + "'s event \"" + projName + "\" has ended";
                                default -> null;
                            };
                            if (msg != null && !pvpMode() && !muteChat()) {
                                int color = switch (timelineAction) {
                                    case "COMPLETED"     -> 0xFFFFD700;
                                    case "HELP_WANTED"   -> 0xFFFFAA00;
                                    case "EVENT_CREATED" -> 0xFF55DDFF;
                                    case "EVENT_ENDED"   -> 0xFF8899AA;
                                    default              -> 0xFFBBAAFF;
                                };
                                mc2.inGameHud.getChatHud().addMessage(
                                        Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                                .append(Text.literal(msg).withColor(color)));
                                if (timelineAction.equals("COMPLETED")) {
                                    mc2.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f, 1.1f);
                                }
                            }
                        }
                        HubScreen.refreshIfOpen();
                        return;
                    }

                    switch (action) {
                        case "JOIN_REQUEST" -> {
                            if (mc2.player != null && !pvpMode() && !muteChat()) {
                                mc2.player.playSound(SoundEvents.UI_TOAST_IN);
                                mc2.inGameHud.getChatHud().addMessage(
                                        Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                                .append(Text.literal(payload.fromName()).formatted(Formatting.AQUA))
                                                .append(Text.literal(" wants to join \"" + projName + "\"! Check the Projects tab.")
                                                        .withColor(0xFFCCCCCC)));
                            }
                            HubScreen.refreshIfOpen();
                        }
                        case "JOIN_ACCEPTED" -> {
                            if (mc2.player != null && !pvpMode() && !muteChat()) {
                                mc2.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
                                mc2.inGameHud.getChatHud().addMessage(
                                        Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                                .append(Text.literal("Accepted into \"" + projName + "\"!")
                                                        .withColor(0xFF88FF88)));
                            }
                            HubScreen.refreshIfOpen();
                        }
                        case "JOIN_DECLINED" -> {
                            if (mc2.player != null && !pvpMode() && !muteChat()) {
                                mc2.player.playSound(SoundEvents.UI_TOAST_OUT, 0.6f, 0.9f);
                                mc2.inGameHud.getChatHud().addMessage(
                                        Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                                .append(Text.literal("Request to join \"" + projName + "\" declined.")
                                                        .withColor(0xFFAAAAAA)));
                            }
                        }
                        case "COMPLETED" -> {
                            if (mc2.player != null && !muteChat()) {
                                if (!pvpMode()) {
                                    mc2.player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
                                    mc2.inGameHud.getChatHud().addMessage(
                                            Text.literal("✦ [Wavelength] 🎉 \"" + projName
                                                            + "\" is complete! Congrats to the whole team!")
                                                    .withColor(0xFFFFD700));
                                    com.lythroo.wavelength.client.gui.HubUiHelper.spawnCompletionParticles();
                                }
                            }
                            HubScreen.refreshIfOpen();
                        }
                        case "KICKED" -> {
                            if (mc2.player != null && !pvpMode() && !muteChat()) {
                                mc2.player.playSound(SoundEvents.UI_TOAST_OUT, 0.8f, 0.7f);
                                mc2.inGameHud.getChatHud().addMessage(
                                        Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                                .append(Text.literal("You were removed from \"" + projName + "\".")
                                                        .withColor(0xFFFF8855)));
                            }
                            HubScreen.refreshIfOpen();
                        }
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncPacket.DIMENSION_GATE_SYNC_ID,
                (payload, context) -> context.client().execute(() -> {

                    boolean wasNetherOpen    = ClientDimensionGateCache.isNetherOpen();
                    boolean wasEndOpen       = ClientDimensionGateCache.isEndOpen();
                    boolean wasAlreadySynced = ClientDimensionGateCache.hasSynced();
                    MinecraftClient mc3      = context.client();

                    ClientDimensionGateCache.update(
                            payload.netherOpen(),      payload.endOpen(),
                            payload.netherBase(),      payload.endBase(),
                            payload.netherEventId(),   payload.endEventId(),
                            payload.netherEventName(), payload.endEventName());

                    if (mc3.player != null && wasAlreadySynced && !pvpMode()) {
                        boolean netherNowOpen = ClientDimensionGateCache.isNetherOpen();
                        boolean endNowOpen    = ClientDimensionGateCache.isEndOpen();

                        if (!wasNetherOpen && netherNowOpen) {

                            mc3.inGameHud.setTitle(
                                    net.minecraft.text.Text.literal("🔥 Nether Unlocked")
                                            .withColor(0xFFFF6622));
                            mc3.inGameHud.setSubtitle(
                                    net.minecraft.text.Text.literal("The Nether is now open")
                                            .withColor(0xFFFFCC88));
                            mc3.inGameHud.setDefaultTitleFade();
                            mc3.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_BLAZE_AMBIENT,
                                    0.6f, 0.75f);
                        } else if (wasNetherOpen && !netherNowOpen) {

                            mc3.player.sendMessage(
                                    net.minecraft.text.Text.literal("🔥 The Nether is now closed")
                                            .withColor(0xFFFF6622), true);
                            mc3.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_LAVA_EXTINGUISH,
                                    0.5f, 1.1f);
                        }

                        if (!wasEndOpen && endNowOpen) {

                            mc3.inGameHud.setTitle(
                                    net.minecraft.text.Text.literal("✦ The End Unlocked")
                                            .withColor(0xFF8844EE));
                            mc3.inGameHud.setSubtitle(
                                    net.minecraft.text.Text.literal("The End is now open")
                                            .withColor(0xFFCCAAFF));
                            mc3.inGameHud.setDefaultTitleFade();
                            mc3.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                                    0.7f, 0.85f);
                            mc3.player.playSound(net.minecraft.sound.SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                                    0.4f, 1.6f);
                        } else if (wasEndOpen && !endNowOpen) {

                            mc3.player.sendMessage(
                                    net.minecraft.text.Text.literal("✦ The End is now closed")
                                            .withColor(0xFF8844EE), true);
                            mc3.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                                    0.5f, 0.6f);
                        }
                    }

                    HubScreen.refreshIfOpen();
                })
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Wavelength.LOGGER.info("[Wavelength] Joined — scheduling identity push.");
            pushCountdown = INITIAL_PUSH_DELAY;
            repushesLeft  = 1;
            if (client.player != null) {
                UUID localUuid = client.player.getUuid();
                FriendRequests.onPlayerJoin(localUuid);
                FriendList.onPlayerJoin(localUuid);
                PersistentPlayerCache.onPlayerJoin(localUuid);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerModDetector.reset();
            RemotePlayerCache.clear();
            RemoteStatCache.clear();
            StatTracker.reset();
            AchievementTimeline.clear();
            FriendRequests.clearAll();
            FriendList.clearAll();
            ClientProjectCache.clear();
            ClientEventCache.clear();
            ClientDimensionGateCache.reset();
            PersistentPlayerCache.onDisconnect();
            pushCountdown = -1;
            repushesLeft  = 0;
            announcedStartedEvents.clear();
            eventStartCheckTick = 0;
            Wavelength.LOGGER.info("[Wavelength] Cleared caches on disconnect.");
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            if (pushCountdown > 0) {
                pushCountdown--;
            } else if (pushCountdown == 0) {
                pushCountdown = -1;
                pushLocalPlayerData();
                if (repushesLeft > 0) {
                    repushesLeft--;
                    pushCountdown = REPUSH_DELAY;
                }
            }

            if (menuKey.wasPressed()) {
                client.setScreen(new HubScreen());
            }

            if (pvpKey.wasPressed()) {
                PlayerData.get().togglePvpMode();
                pushLocalPlayerData();
            }

            if (++eventStartCheckTick >= 20) {
                eventStartCheckTick = 0;
                long now = System.currentTimeMillis();
                for (com.lythroo.wavelength.common.data.EventData ev
                        : com.lythroo.wavelength.common.data.ClientEventCache.getActive()) {
                    if (ev.scheduledStartMs > 0
                            && ev.scheduledStartMs <= now
                            && !announcedStartedEvents.contains(ev.id)) {
                        announcedStartedEvents.add(ev.id);
                        java.util.UUID ownerUuid;
                        try { ownerUuid = java.util.UUID.fromString(ev.ownerUuid); }
                        catch (Exception ex) { ownerUuid = new java.util.UUID(0, 0); }

                        com.lythroo.wavelength.common.data.AchievementTimeline.addProjectEvent(
                                new com.lythroo.wavelength.common.data.AchievementTimeline.ProjectTimelineEvent(
                                        ownerUuid, ev.ownerName, ev.name,
                                        "EVENT_STARTED", "", now));

                        if (client.player != null && !muteChat() && !WavelengthConfig.get().muteTimelineToasts && !pvpMode()) {
                            client.inGameHud.getChatHud().addMessage(
                                    Text.literal("✦ [Wavelength] ").withColor(0xFFBBAAFF)
                                            .append(Text.literal("🔔 " + ev.ownerName
                                                            + "'s event \"" + ev.name + "\" has begun!")
                                                    .withColor(0xFF55FFBB)));
                        }

                        if (client.player != null && !pvpMode()) {
                            client.player.playSound(net.minecraft.sound.SoundEvents.UI_TOAST_IN, 0.8f, 1.0f);
                            client.player.playSound(
                                    net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                    0.6f, 1.4f);
                        }
                        HubScreen.refreshIfOpen();
                    }
                }
            }

            if (++eventsTabRefreshTick >= 20) {
                eventsTabRefreshTick = 0;
                com.lythroo.wavelength.client.gui.HubScreen.refreshEventsIfOpen();
            }

            ActivityDetector.tick(client);
            StatTracker.tick(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;
            if (client.getDebugHud().shouldShowDebugHud()) return;

            if (client.getServer() != null) return;

            NameplateHudRenderer.render(drawContext, client, tickCounter);

            if (WavelengthConfig.get().showTopBar) {
                com.lythroo.wavelength.client.gui.PlayersHud.render(drawContext, client);
                TopBarHud.render(drawContext, client);
            }
            com.lythroo.wavelength.client.gui.EventsHud.render(drawContext, client);
        });

        Wavelength.LOGGER.info("[Wavelength] Client init complete.");
    }

    private static boolean pvpMode() {
        return com.lythroo.wavelength.common.data.PlayerData.get().pvpMode;
    }

    private static boolean muteChat() {
        return WavelengthConfig.get().muteAllChatMessages;
    }

    private static final java.util.UUID DEVELOPER_UUID =
            java.util.UUID.fromString("6b5c34ea-ad7e-4717-931e-b6861a5efc8c");

    public static void pushLocalPlayerData() {
        PlayerData data    = PlayerData.get();
        MinecraftClient mc = MinecraftClient.getInstance();

        boolean isDeveloper = mc.player != null
                && DEVELOPER_UUID.equals(mc.player.getUuid());

        String rankName    = isDeveloper
                ? Rank.DEVELOPER.name()
                : (data.rank != null ? data.rank.name() : Rank.NONE.name());
        String privacyName = data.privacy != null ? data.privacy.name() : PrivacyMode.PUBLIC.name();
        String townName    = data.townName != null ? data.townName : "";

        SyncPacket.ClientDataPushPayload payload =
                new SyncPacket.ClientDataPushPayload(townName, rankName, privacyName, data.pvpMode);

        try {
            ClientPlayNetworking.send(payload);
            Wavelength.LOGGER.info("[Wavelength] Identity pushed: town='{}' rank={} privacy={}",
                    townName, rankName, privacyName);
        } catch (Exception e) {
            Wavelength.LOGGER.debug("[Wavelength] Identity push skipped (no channel): {}", e.getMessage());
        }
    }
}