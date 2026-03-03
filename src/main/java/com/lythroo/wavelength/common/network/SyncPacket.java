package com.lythroo.wavelength.common.network;

import com.lythroo.wavelength.Wavelength;
import com.lythroo.wavelength.common.data.ProjectData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SyncPacket {

    public static final CustomPayload.Id<PlayerDataSyncPayload> PLAYER_DATA_SYNC_ID =
            new CustomPayload.Id<>(Identifier.of(Wavelength.MOD_ID, "player_data_sync"));

    public static final CustomPayload.Id<StatDataSyncPayload> STAT_DATA_SYNC_ID =
            new CustomPayload.Id<>(Identifier.of(Wavelength.MOD_ID, "stat_data_sync"));

    public static final CustomPayload.Id<ClientDataPushPayload> CLIENT_DATA_PUSH_ID =
            new CustomPayload.Id<>(Identifier.of(Wavelength.MOD_ID, "client_data_push"));

    public static final CustomPayload.Id<PlayerLeftPayload> PLAYER_LEFT_ID =
            new CustomPayload.Id<>(Identifier.of(Wavelength.MOD_ID, "player_left"));

    public static Identifier CLIENT_DATA_PUSH = CLIENT_DATA_PUSH_ID.id();

    public static final CustomPayload.Id<ProjectSyncPayload>
            PROJECT_SYNC_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "project_sync"));

    public static final CustomPayload.Id<ProjectRemoveS2CPayload>
            PROJECT_REMOVE_S2C_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "project_remove"));

    public static final CustomPayload.Id<ProjectNotifyS2CPayload>
            PROJECT_NOTIFY_S2C_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "project_notify"));

    public static final CustomPayload.Id<ProjectUpsertC2SPayload>
            PROJECT_UPSERT_C2S_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "project_upsert"));

    public static final CustomPayload.Id<ProjectActionC2SPayload>
            PROJECT_ACTION_C2S_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "project_action"));

    private SyncPacket() {}

    public record PlayerDataSyncPayload(
            UUID   playerUuid,
            String townName,
            String rankName,
            String privacyName,
            boolean pvpMode
    ) implements CustomPayload {

        public static final PacketCodec<PacketByteBuf, PlayerDataSyncPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
                        PlayerDataSyncPayload::playerUuid,
                        PacketCodecs.STRING,  PlayerDataSyncPayload::townName,
                        PacketCodecs.STRING,  PlayerDataSyncPayload::rankName,
                        PacketCodecs.STRING,  PlayerDataSyncPayload::privacyName,
                        PacketCodecs.BOOLEAN, PlayerDataSyncPayload::pvpMode,
                        PlayerDataSyncPayload::new
                );

        @Override public Id<? extends CustomPayload> getId() { return PLAYER_DATA_SYNC_ID; }
    }

    public record StatDataSyncPayload(
            UUID  playerUuid,
            long  blocksMined,
            long  blocksPlaced,
            long  mobsKilled,
            long  cropsHarvested,
            long  itemsCrafted,
            long  distanceTraveled,
            long  playtimeTicks
    ) implements CustomPayload {

        public static final PacketCodec<PacketByteBuf, StatDataSyncPayload> CODEC =
                new PacketCodec<>() {
                    @Override
                    public StatDataSyncPayload decode(PacketByteBuf buf) {
                        UUID uuid             = UUID.fromString(buf.readString());
                        long blocksMined      = buf.readVarLong();
                        long blocksPlaced     = buf.readVarLong();
                        long mobsKilled       = buf.readVarLong();
                        long cropsHarvested   = buf.readVarLong();
                        long itemsCrafted     = buf.readVarLong();
                        long distanceTraveled = buf.readVarLong();
                        long playtimeTicks    = buf.readVarLong();
                        return new StatDataSyncPayload(uuid, blocksMined, blocksPlaced,
                                mobsKilled, cropsHarvested, itemsCrafted,
                                distanceTraveled, playtimeTicks);
                    }

                    @Override
                    public void encode(PacketByteBuf buf, StatDataSyncPayload p) {
                        buf.writeString(p.playerUuid().toString());
                        buf.writeVarLong(p.blocksMined());
                        buf.writeVarLong(p.blocksPlaced());
                        buf.writeVarLong(p.mobsKilled());
                        buf.writeVarLong(p.cropsHarvested());
                        buf.writeVarLong(p.itemsCrafted());
                        buf.writeVarLong(p.distanceTraveled());
                        buf.writeVarLong(p.playtimeTicks());
                    }
                };

        @Override public Id<? extends CustomPayload> getId() { return STAT_DATA_SYNC_ID; }
    }

    public record ClientDataPushPayload(
            String townName,
            String rankName,
            String privacyName,
            boolean pvpMode
    ) implements CustomPayload {

        public static final PacketCodec<PacketByteBuf, ClientDataPushPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING,  ClientDataPushPayload::townName,
                        PacketCodecs.STRING,  ClientDataPushPayload::rankName,
                        PacketCodecs.STRING,  ClientDataPushPayload::privacyName,
                        PacketCodecs.BOOLEAN, ClientDataPushPayload::pvpMode,
                        ClientDataPushPayload::new
                );

        @Override public Id<? extends CustomPayload> getId() { return CLIENT_DATA_PUSH_ID; }
    }

    public static final CustomPayload.Id<FriendActionC2SPayload> FRIEND_ACTION_C2S_ID =
            new CustomPayload.Id<>(Identifier.of(Wavelength.MOD_ID, "friend_action"));

    public record FriendActionC2SPayload(UUID targetUuid, String action) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, FriendActionC2SPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
                        FriendActionC2SPayload::targetUuid,
                        PacketCodecs.STRING, FriendActionC2SPayload::action,
                        FriendActionC2SPayload::new
                );
        @Override public Id<? extends CustomPayload> getId() { return FRIEND_ACTION_C2S_ID; }
    }

    public static final CustomPayload.Id<FriendNotifyS2CPayload> FRIEND_NOTIFY_S2C_ID =
            new CustomPayload.Id<>(Identifier.of(Wavelength.MOD_ID, "friend_notify"));

    public record FriendNotifyS2CPayload(UUID fromUuid, String fromUsername, String action) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, FriendNotifyS2CPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
                        FriendNotifyS2CPayload::fromUuid,
                        PacketCodecs.STRING, FriendNotifyS2CPayload::fromUsername,
                        PacketCodecs.STRING, FriendNotifyS2CPayload::action,
                        FriendNotifyS2CPayload::new
                );
        @Override public Id<? extends CustomPayload> getId() { return FRIEND_NOTIFY_S2C_ID; }
    }

    public static final CustomPayload.Id<FriendListSyncS2CPayload> FRIEND_LIST_SYNC_S2C_ID =
            new CustomPayload.Id<>(Identifier.of(Wavelength.MOD_ID, "friend_list_sync"));

    public record FriendListSyncS2CPayload(
            List<String> friendUuids,
            List<String> friendUsernames
    ) implements CustomPayload {

        public static final PacketCodec<PacketByteBuf, FriendListSyncS2CPayload> CODEC =
                new PacketCodec<>() {
                    @Override
                    public FriendListSyncS2CPayload decode(PacketByteBuf buf) {
                        int count = buf.readVarInt();
                        List<String> uuids = new ArrayList<>(count);
                        List<String> usernames = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            uuids.add(buf.readString());
                            usernames.add(buf.readString());
                        }
                        return new FriendListSyncS2CPayload(uuids, usernames);
                    }

                    @Override
                    public void encode(PacketByteBuf buf, FriendListSyncS2CPayload p) {
                        buf.writeVarInt(p.friendUuids().size());
                        for (int i = 0; i < p.friendUuids().size(); i++) {
                            buf.writeString(p.friendUuids().get(i));
                            buf.writeString(p.friendUsernames().get(i));
                        }
                    }
                };

        @Override public Id<? extends CustomPayload> getId() { return FRIEND_LIST_SYNC_S2C_ID; }
    }

    public static final CustomPayload.Id<ActivitySyncPayload> ACTIVITY_SYNC_ID =
            new CustomPayload.Id<>(Identifier.of(Wavelength.MOD_ID, "activity_sync"));

    public record ActivitySyncPayload(UUID playerUuid, String activityName) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ActivitySyncPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
                        ActivitySyncPayload::playerUuid,
                        PacketCodecs.STRING, ActivitySyncPayload::activityName,
                        ActivitySyncPayload::new
                );
        @Override public Id<? extends CustomPayload> getId() { return ACTIVITY_SYNC_ID; }
    }

    public record PlayerLeftPayload(UUID playerUuid) implements CustomPayload {

        public static final PacketCodec<PacketByteBuf, PlayerLeftPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
                        PlayerLeftPayload::playerUuid,
                        PlayerLeftPayload::new
                );

        @Override public Id<? extends CustomPayload> getId() { return PLAYER_LEFT_ID; }
    }

    static void encodeProject(PacketByteBuf buf, ProjectData p) {
        buf.writeString(p.id);
        buf.writeString(p.ownerUuid);
        buf.writeString(p.ownerName);
        buf.writeString(p.name);
        buf.writeString(p.description != null ? p.description : "");
        buf.writeVarInt(p.progress);
        List<ProjectData.Material> mats = p.materials != null ? p.materials : List.of();
        buf.writeVarInt(mats.size());
        for (ProjectData.Material m : mats) {
            buf.writeString(m.name); buf.writeVarInt(m.required); buf.writeVarInt(m.current);
        }
        buf.writeBoolean(p.hasLocation);
        if (p.hasLocation) { buf.writeLong(p.locX); buf.writeLong(p.locY); buf.writeLong(p.locZ); }
        buf.writeString(p.collaboration != null ? p.collaboration : "SOLO");
        List<ProjectData.Collaborator> collabs = p.collaborators != null ? p.collaborators : List.of();
        buf.writeVarInt(collabs.size());
        for (ProjectData.Collaborator c : collabs) {
            buf.writeString(c.uuid); buf.writeString(c.name); buf.writeVarLong(c.blocksContributed);
        }
        List<ProjectData.PendingJoin> pending = p.pendingJoins != null ? p.pendingJoins : List.of();
        buf.writeVarInt(pending.size());
        for (ProjectData.PendingJoin j : pending) { buf.writeString(j.uuid); buf.writeString(j.name); }
        buf.writeVarLong(p.startedMs);
        buf.writeBoolean(p.completed);
    }

    static ProjectData decodeProject(PacketByteBuf buf) {
        ProjectData p = new ProjectData();
        p.id = buf.readString(); p.ownerUuid = buf.readString(); p.ownerName = buf.readString();
        p.name = buf.readString(); p.description = buf.readString(); p.progress = buf.readVarInt();
        int matCount = buf.readVarInt();
        p.materials = new ArrayList<>(matCount);
        for (int i = 0; i < matCount; i++)
            p.materials.add(new ProjectData.Material(buf.readString(), buf.readVarInt(), buf.readVarInt()));
        p.hasLocation = buf.readBoolean();
        if (p.hasLocation) { p.locX = buf.readLong(); p.locY = buf.readLong(); p.locZ = buf.readLong(); }
        p.collaboration = buf.readString();
        int collabCount = buf.readVarInt();
        p.collaborators = new ArrayList<>(collabCount);
        for (int i = 0; i < collabCount; i++)
            p.collaborators.add(new ProjectData.Collaborator(buf.readString(), buf.readString(), buf.readVarLong()));
        int pendingCount = buf.readVarInt();
        p.pendingJoins = new ArrayList<>(pendingCount);
        for (int i = 0; i < pendingCount; i++)
            p.pendingJoins.add(new ProjectData.PendingJoin(buf.readString(), buf.readString()));
        p.startedMs = buf.readVarLong(); p.completed = buf.readBoolean();
        return p;
    }

    public record ProjectSyncPayload(ProjectData project) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ProjectSyncPayload> CODEC =
                new PacketCodec<>() {
                    @Override public ProjectSyncPayload decode(PacketByteBuf buf) {
                        return new ProjectSyncPayload(decodeProject(buf));
                    }
                    @Override public void encode(PacketByteBuf buf, ProjectSyncPayload p) {
                        encodeProject(buf, p.project());
                    }
                };
        @Override public Id<? extends CustomPayload> getId() { return PROJECT_SYNC_ID; }
    }

    public record ProjectRemoveS2CPayload(String projectId) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ProjectRemoveS2CPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, ProjectRemoveS2CPayload::projectId,
                        ProjectRemoveS2CPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return PROJECT_REMOVE_S2C_ID; }
    }

    public record ProjectNotifyS2CPayload(
            String action, String projectId, String projectName,
            String fromUuid, String fromName
    ) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ProjectNotifyS2CPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, ProjectNotifyS2CPayload::action,
                        PacketCodecs.STRING, ProjectNotifyS2CPayload::projectId,
                        PacketCodecs.STRING, ProjectNotifyS2CPayload::projectName,
                        PacketCodecs.STRING, ProjectNotifyS2CPayload::fromUuid,
                        PacketCodecs.STRING, ProjectNotifyS2CPayload::fromName,
                        ProjectNotifyS2CPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return PROJECT_NOTIFY_S2C_ID; }
    }

    public record ProjectUpsertC2SPayload(
            boolean isCreate, String projectId,
            String name, String description, int progress,
            List<ProjectData.Material> materials,
            boolean hasLocation, long locX, long locY, long locZ,
            String collaboration
    ) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ProjectUpsertC2SPayload> CODEC =
                new PacketCodec<>() {
                    @Override public ProjectUpsertC2SPayload decode(PacketByteBuf buf) {
                        boolean isCreate = buf.readBoolean(); String pid = buf.readString();
                        String  name = buf.readString(); String desc = buf.readString();
                        int progress = buf.readVarInt();
                        int matCount = buf.readVarInt();
                        List<ProjectData.Material> mats = new ArrayList<>(matCount);
                        for (int i = 0; i < matCount; i++)
                            mats.add(new ProjectData.Material(buf.readString(), buf.readVarInt(), buf.readVarInt()));
                        boolean hasLoc = buf.readBoolean();
                        long lx = 0, ly = 64, lz = 0;
                        if (hasLoc) { lx = buf.readLong(); ly = buf.readLong(); lz = buf.readLong(); }
                        return new ProjectUpsertC2SPayload(isCreate, pid, name, desc, progress,
                                mats, hasLoc, lx, ly, lz, buf.readString());
                    }
                    @Override public void encode(PacketByteBuf buf, ProjectUpsertC2SPayload p) {
                        buf.writeBoolean(p.isCreate()); buf.writeString(p.projectId());
                        buf.writeString(p.name()); buf.writeString(p.description()); buf.writeVarInt(p.progress());
                        List<ProjectData.Material> mats = p.materials() != null ? p.materials() : List.of();
                        buf.writeVarInt(mats.size());
                        for (ProjectData.Material m : mats) {
                            buf.writeString(m.name); buf.writeVarInt(m.required); buf.writeVarInt(m.current);
                        }
                        buf.writeBoolean(p.hasLocation());
                        if (p.hasLocation()) { buf.writeLong(p.locX()); buf.writeLong(p.locY()); buf.writeLong(p.locZ()); }
                        buf.writeString(p.collaboration());
                    }
                };
        @Override public Id<? extends CustomPayload> getId() { return PROJECT_UPSERT_C2S_ID; }
    }

    public static final CustomPayload.Id<EventSyncPayload>
            EVENT_SYNC_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "event_sync"));

    public static final CustomPayload.Id<EventRemoveS2CPayload>
            EVENT_REMOVE_S2C_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "event_remove"));

    public static final CustomPayload.Id<EventUpsertC2SPayload>
            EVENT_UPSERT_C2S_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "event_upsert"));

    public static final CustomPayload.Id<EventActionC2SPayload>
            EVENT_ACTION_C2S_ID = new CustomPayload.Id<>(
            Identifier.of(Wavelength.MOD_ID, "event_action"));

    public record ProjectActionC2SPayload(
            String projectId, String action, String targetUuid
    ) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ProjectActionC2SPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.STRING, ProjectActionC2SPayload::projectId,
                        PacketCodecs.STRING, ProjectActionC2SPayload::action,
                        PacketCodecs.STRING, ProjectActionC2SPayload::targetUuid,
                        ProjectActionC2SPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return PROJECT_ACTION_C2S_ID; }
    }

    static void encodeEvent(PacketByteBuf buf, com.lythroo.wavelength.common.data.EventData e) {
        buf.writeString(e.id); buf.writeString(e.ownerUuid); buf.writeString(e.ownerName);
        buf.writeString(e.name); buf.writeString(e.description != null ? e.description : "");
        buf.writeBoolean(e.hasLocation);
        if (e.hasLocation) { buf.writeLong(e.locX); buf.writeLong(e.locY); buf.writeLong(e.locZ); }
        buf.writeVarLong(e.startMs); buf.writeVarLong(e.endMs); buf.writeBoolean(e.active);
        buf.writeVarLong(e.scheduledStartMs);
    }

    static com.lythroo.wavelength.common.data.EventData decodeEvent(PacketByteBuf buf) {
        com.lythroo.wavelength.common.data.EventData e = new com.lythroo.wavelength.common.data.EventData();
        e.id = buf.readString(); e.ownerUuid = buf.readString(); e.ownerName = buf.readString();
        e.name = buf.readString(); e.description = buf.readString();
        e.hasLocation = buf.readBoolean();
        if (e.hasLocation) { e.locX = buf.readLong(); e.locY = buf.readLong(); e.locZ = buf.readLong(); }
        e.startMs = buf.readVarLong(); e.endMs = buf.readVarLong(); e.active = buf.readBoolean();
        e.scheduledStartMs = buf.readVarLong();
        return e;
    }

    public record EventSyncPayload(com.lythroo.wavelength.common.data.EventData event) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, EventSyncPayload> CODEC = new PacketCodec<>() {
            @Override public EventSyncPayload decode(PacketByteBuf buf) { return new EventSyncPayload(decodeEvent(buf)); }
            @Override public void encode(PacketByteBuf buf, EventSyncPayload p) { encodeEvent(buf, p.event()); }
        };
        @Override public Id<? extends CustomPayload> getId() { return EVENT_SYNC_ID; }
    }

    public record EventRemoveS2CPayload(String eventId) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, EventRemoveS2CPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, EventRemoveS2CPayload::eventId, EventRemoveS2CPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return EVENT_REMOVE_S2C_ID; }
    }

    public record EventUpsertC2SPayload(boolean isCreate, String eventId, String name, String description,
                                        boolean hasLocation, long locX, long locY, long locZ,
                                        long endMs, long scheduledStartMs) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, EventUpsertC2SPayload> CODEC = new PacketCodec<>() {
            @Override public EventUpsertC2SPayload decode(PacketByteBuf buf) {
                boolean c = buf.readBoolean(); String id = buf.readString();
                String n = buf.readString(); String d = buf.readString();
                boolean hl = buf.readBoolean(); long lx=0,ly=64,lz=0;
                if (hl){lx=buf.readLong();ly=buf.readLong();lz=buf.readLong();}
                long end = buf.readVarLong();
                long schedStart = buf.readVarLong();
                return new EventUpsertC2SPayload(c,id,n,d,hl,lx,ly,lz,end,schedStart);
            }
            @Override public void encode(PacketByteBuf buf, EventUpsertC2SPayload p) {
                buf.writeBoolean(p.isCreate()); buf.writeString(p.eventId());
                buf.writeString(p.name()); buf.writeString(p.description());
                buf.writeBoolean(p.hasLocation());
                if(p.hasLocation()){buf.writeLong(p.locX());buf.writeLong(p.locY());buf.writeLong(p.locZ());}
                buf.writeVarLong(p.endMs());
                buf.writeVarLong(p.scheduledStartMs());
            }
        };
        @Override public Id<? extends CustomPayload> getId() { return EVENT_UPSERT_C2S_ID; }
    }

    public record EventActionC2SPayload(String eventId, String action) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, EventActionC2SPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.STRING, EventActionC2SPayload::eventId,
                        PacketCodecs.STRING, EventActionC2SPayload::action, EventActionC2SPayload::new);
        @Override public Id<? extends CustomPayload> getId() { return EVENT_ACTION_C2S_ID; }
    }
}