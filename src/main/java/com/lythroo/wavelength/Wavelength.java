package com.lythroo.wavelength;

import com.lythroo.wavelength.common.network.SyncPacket;
import com.lythroo.wavelength.config.WavelengthConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wavelength implements ModInitializer {

    public static final String MOD_ID = "wavelength";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("");
        LOGGER.info(" _    _                 _                  _   _      ");
        LOGGER.info("| |  | |               | |                | | | |     ");
        LOGGER.info("| |  | | __ ___   _____| | ___ _ __   __ _| |_| |__  ");
        LOGGER.info("| |/\\| |/ _` \\ \\ / / _ \\ |/ _ \\ '_ \\ / _` | __| '_ \\          Version: {}", getVersion());
        LOGGER.info("\\  /\\  / (_| |\\ V /  __/ |  __/ | | | (_| | |_| | | |");
        LOGGER.info(" \\/  \\/ \\__,_| \\_/ \\___|_|\\___|_| |_|\\__, |\\__|_| |_|");
        LOGGER.info("                                      __/ |          ");
        LOGGER.info("                                     |___/           ");

        WavelengthConfig.init();
        LOGGER.info("[Wavelength] Config loaded: showTopBar={}, nearbyRange={}",
                WavelengthConfig.get().showTopBar,
                WavelengthConfig.NEARBY_RANGE_BLOCKS);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.PLAYER_DATA_SYNC_ID,
                SyncPacket.PlayerDataSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(
                SyncPacket.STAT_DATA_SYNC_ID,
                SyncPacket.StatDataSyncPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.PLAYER_LEFT_ID,
                SyncPacket.PlayerLeftPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.ACTIVITY_SYNC_ID,
                SyncPacket.ActivitySyncPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SyncPacket.CLIENT_DATA_PUSH_ID,
                SyncPacket.ClientDataPushPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SyncPacket.FRIEND_ACTION_C2S_ID,
                SyncPacket.FriendActionC2SPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.FRIEND_NOTIFY_S2C_ID,
                SyncPacket.FriendNotifyS2CPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.FRIEND_LIST_SYNC_S2C_ID,
                SyncPacket.FriendListSyncS2CPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.PROJECT_SYNC_ID,
                SyncPacket.ProjectSyncPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.PROJECT_REMOVE_S2C_ID,
                SyncPacket.ProjectRemoveS2CPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.PROJECT_NOTIFY_S2C_ID,
                SyncPacket.ProjectNotifyS2CPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SyncPacket.PROJECT_UPSERT_C2S_ID,
                SyncPacket.ProjectUpsertC2SPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SyncPacket.PROJECT_ACTION_C2S_ID,
                SyncPacket.ProjectActionC2SPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.EVENT_SYNC_ID,
                SyncPacket.EventSyncPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.EVENT_REMOVE_S2C_ID,
                SyncPacket.EventRemoveS2CPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SyncPacket.EVENT_UPSERT_C2S_ID,
                SyncPacket.EventUpsertC2SPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SyncPacket.EVENT_ACTION_C2S_ID,
                SyncPacket.EventActionC2SPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(
                SyncPacket.DIMENSION_GATE_SYNC_ID,
                SyncPacket.DimensionGateSyncPayload.CODEC);

        PayloadTypeRegistry.playC2S().register(
                SyncPacket.DIMENSION_GATE_ACTION_C2S_ID,
                SyncPacket.DimensionGateActionC2SPayload.CODEC);

        LOGGER.info("[Wavelength] Network payload types registered " +
                "(S2C: player_data_sync, stat_data_sync, player_left, friend_notify, friend_list_sync | C2S: client_data_push, friend_action).");
        LOGGER.info("[Wavelength] Common init complete.");
    }

    private static String getVersion() {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer(MOD_ID)
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }
}