package com.lythroo.wavelength;

import com.lythroo.wavelength.common.network.SyncPacket;
import com.lythroo.wavelength.common.data.ServerFriendData;
import com.lythroo.wavelength.common.data.ServerProjectData;
import com.lythroo.wavelength.common.data.ServerEventData;
import com.lythroo.wavelength.common.data.ProjectData;
import com.lythroo.wavelength.common.data.EventData;
import net.fabricmc.api.DedicatedServerModInitializer;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionPredicate;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WavelengthServer implements DedicatedServerModInitializer {

    private static final Map<UUID, SyncPacket.PlayerDataSyncPayload> playerDataCache = new ConcurrentHashMap<>();
    private static final Map<UUID, SyncPacket.StatDataSyncPayload>   statCache       = new ConcurrentHashMap<>();

    private static final Map<UUID, long[]> debugOverrides = new ConcurrentHashMap<>();
    private static final int IDX_MINED    = 0;
    private static final int IDX_PLACED   = 1;
    private static final int IDX_KILLED   = 2;
    private static final int IDX_CROPS    = 3;
    private static final int IDX_CRAFTED  = 4;
    private static final int IDX_DIST_CM  = 5;
    private static final int IDX_PLAYTIME = 6;

    private static final int STAT_PUSH_INTERVAL = 200;

    private static final ExecutorService STAT_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "wavelength-stat-worker");
        t.setDaemon(true);
        return t;
    });

    private int statPushTimer = 0;

    private static final Set<Block> DOUBLE_HEIGHT_BLOCKS = Set.of(
            Blocks.TALL_GRASS,
            Blocks.LARGE_FERN,
            Blocks.TALL_SEAGRASS,
            Blocks.ROSE_BUSH,
            Blocks.PEONY,
            Blocks.LILAC,
            Blocks.SUNFLOWER,
            Blocks.PITCHER_PLANT
    );

    @Override
    public void onInitializeServer() {
        Wavelength.LOGGER.info("[Wavelength] Server init...");

        ServerFriendData.load();

        ServerProjectData.load();

        ServerEventData.load();

        ServerPlayNetworking.registerGlobalReceiver(
                SyncPacket.EVENT_UPSERT_C2S_ID,
                (payload, context) -> {
                    UUID   senderUuid = context.player().getUuid();
                    String senderName = context.player().getName().getString();
                    MinecraftServer server = context.server();

                    EventData event;
                    if (payload.isCreate()) {
                        event = ServerEventData.create(senderUuid, senderName,
                                payload.name(), payload.description(),
                                payload.hasLocation(), payload.locX(), payload.locY(), payload.locZ(),
                                payload.endMs(), payload.scheduledStartMs());
                        if (event == null) return;
                        broadcastEventSync(server, event);

                        broadcastEventNotify(server, "EVENT_CREATED", event.id, event.name,
                                senderUuid.toString(), senderName);
                    } else {
                        boolean ok = ServerEventData.update(payload.eventId(), senderUuid,
                                payload.name(), payload.description(),
                                payload.hasLocation(), payload.locX(), payload.locY(), payload.locZ(),
                                payload.endMs(), payload.scheduledStartMs());
                        if (!ok) return;
                        event = ServerEventData.get(payload.eventId());
                        if (event == null) return;
                        broadcastEventSync(server, event);
                    }
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SyncPacket.EVENT_ACTION_C2S_ID,
                (payload, context) -> {
                    UUID   senderUuid = context.player().getUuid();
                    String senderName = context.player().getName().getString();
                    MinecraftServer server = context.server();
                    switch (payload.action()) {
                        case "END" -> {
                            EventData ev = ServerEventData.get(payload.eventId());
                            boolean ok = ServerEventData.end(payload.eventId(), senderUuid);
                            if (ok) {
                                broadcastEventRemove(server, payload.eventId());
                                if (ev != null) broadcastEventNotify(server, "EVENT_ENDED",
                                        ev.id, ev.name, senderUuid.toString(), senderName);
                            }
                        }
                        case "DELETE" -> {
                            PermissionPredicate _perms = context.player().getPermissions();
                            boolean isOp = _perms == LeveledPermissionPredicate.GAMEMASTERS
                                    || _perms == LeveledPermissionPredicate.ADMINS
                                    || _perms == LeveledPermissionPredicate.OWNERS;
                            boolean ok = isOp
                                    ? ServerEventData.adminDelete(payload.eventId())
                                    : ServerEventData.delete(payload.eventId(), senderUuid);
                            if (ok) broadcastEventRemove(server, payload.eventId());
                        }
                    }
                }
        );

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            ItemStack held = player.getStackInHand(hand);
            if (held.getItem() instanceof BlockItem) {
                broadcastActivity(player, "BUILDING", world.getServer());

                ProjectData activeProject = ServerProjectData.getActiveProjectFor(player.getUuid());
                if (activeProject != null && !activeProject.completed)
                    ServerProjectData.incrementContribution(activeProject.id, player.getUuid());
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return;
            broadcastActivity(player, "MINING", world.getServer());
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            broadcastActivity(player, "FIGHTING", world.getServer());
            return ActionResult.PASS;
        });

        ServerPlayNetworking.registerGlobalReceiver(
                SyncPacket.FRIEND_ACTION_C2S_ID,
                (payload, context) -> {
                    UUID   senderUuid = context.player().getUuid();
                    String senderName = context.player().getName().getString();
                    UUID   targetUuid = payload.targetUuid();
                    String action     = payload.action();
                    MinecraftServer server = context.server();

                    ServerFriendData.updateName(senderUuid, senderName);

                    switch (action) {
                        case "REQUEST" -> {
                            if (!ServerFriendData.addRequest(senderUuid, senderName, targetUuid)) return;

                            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
                            if (target != null) {
                                ServerPlayNetworking.send(target,
                                        new SyncPacket.FriendNotifyS2CPayload(senderUuid, senderName, "REQUEST"));
                            }

                        }
                        case "ACCEPT" -> {

                            if (!ServerFriendData.acceptRequest(senderUuid, senderName, targetUuid)) return;

                            syncFriendList(server, senderUuid);
                            syncFriendList(server, targetUuid);

                            ServerPlayerEntity requester = server.getPlayerManager().getPlayer(targetUuid);
                            if (requester != null) {
                                ServerPlayNetworking.send(requester,
                                        new SyncPacket.FriendNotifyS2CPayload(senderUuid, senderName, "ACCEPT"));
                            }

                        }
                        case "DECLINE" -> {
                            if (!ServerFriendData.declineRequest(senderUuid, targetUuid)) return;
                            ServerPlayerEntity requester = server.getPlayerManager().getPlayer(targetUuid);
                            if (requester != null) {
                                ServerPlayNetworking.send(requester,
                                        new SyncPacket.FriendNotifyS2CPayload(senderUuid, senderName, "DECLINE"));
                            }

                        }
                        case "CANCEL" -> {
                            if (!ServerFriendData.cancelRequest(senderUuid, targetUuid)) return;
                            ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
                            if (target != null) {
                                ServerPlayNetworking.send(target,
                                        new SyncPacket.FriendNotifyS2CPayload(senderUuid, senderName, "CANCEL"));
                            }
                        }
                        case "UNFRIEND" -> {
                            if (!ServerFriendData.unfriend(senderUuid, targetUuid)) return;

                            syncFriendList(server, senderUuid);
                            syncFriendList(server, targetUuid);
                        }
                    }
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SyncPacket.CLIENT_DATA_PUSH_ID,
                (payload, context) -> {
                    UUID senderUuid = context.player().getUuid();
                    SyncPacket.PlayerDataSyncPayload sync = new SyncPacket.PlayerDataSyncPayload(
                            senderUuid, payload.townName(), payload.rankName(), payload.privacyName(), payload.pvpMode());
                    playerDataCache.put(senderUuid, sync);
                    context.server().getPlayerManager().getPlayerList().forEach(p -> {
                        if (!p.getUuid().equals(senderUuid))
                            ServerPlayNetworking.send(p, sync);
                    });
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SyncPacket.PROJECT_UPSERT_C2S_ID,
                (payload, context) -> {
                    UUID   senderUuid = context.player().getUuid();
                    String senderName = context.player().getName().getString();
                    MinecraftServer server = context.server();

                    ProjectData project;
                    if (payload.isCreate()) {
                        project = ServerProjectData.create(senderUuid, senderName,
                                payload.name(), payload.description(), payload.progress(),
                                payload.materials(),
                                payload.hasLocation(), payload.locX(), payload.locY(), payload.locZ(),
                                payload.collaboration());
                        if (project == null) return;

                        broadcastProjectNotify(server, "CREATED", project.id, project.name,
                                senderUuid.toString(), senderName, "", "");

                        if (project.isHelpWanted() || project.isTeamProject()) {
                            broadcastProjectNotify(server, "HELP_WANTED", project.id, project.name,
                                    senderUuid.toString(), senderName, "", "");
                        }
                    } else {
                        ProjectData before = ServerProjectData.get(payload.projectId());
                        String oldCollab = before != null ? before.collaboration : "SOLO";
                        boolean ok = ServerProjectData.update(payload.projectId(), senderUuid,
                                payload.name(), payload.description(), payload.progress(),
                                payload.materials(),
                                payload.hasLocation(), payload.locX(), payload.locY(), payload.locZ(),
                                payload.collaboration());
                        if (!ok) return;
                        project = ServerProjectData.get(payload.projectId());
                        if (project == null) return;

                        boolean wasOpen = "HELP_WANTED".equals(oldCollab) || "TEAM".equals(oldCollab);
                        boolean nowOpen = project.isHelpWanted() || project.isTeamProject();
                        if (!wasOpen && nowOpen) {
                            broadcastProjectNotify(server, "HELP_WANTED", project.id, project.name,
                                    senderUuid.toString(), senderName, "", "");
                        }
                    }
                    broadcastProjectSync(server, project);
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SyncPacket.PROJECT_ACTION_C2S_ID,
                (payload, context) -> {
                    UUID   senderUuid = context.player().getUuid();
                    String senderName = context.player().getName().getString();
                    MinecraftServer server = context.server();
                    String action    = payload.action();
                    String projectId = payload.projectId();

                    switch (action) {
                        case "DELETE" -> {
                            PermissionPredicate _perms = context.player().getPermissions();
                            boolean isOp = _perms == LeveledPermissionPredicate.GAMEMASTERS
                                    || _perms == LeveledPermissionPredicate.ADMINS
                                    || _perms == LeveledPermissionPredicate.OWNERS;
                            boolean ok = isOp
                                    ? ServerProjectData.adminDelete(projectId)
                                    : ServerProjectData.delete(projectId, senderUuid);
                            if (ok) broadcastProjectRemove(server, projectId);
                        }
                        case "REQUEST_JOIN" -> {
                            ProjectData p = ServerProjectData.get(projectId);
                            if (p == null) return;
                            boolean ok = ServerProjectData.addJoinRequest(projectId, senderUuid, senderName);
                            if (!ok) return;
                            ProjectData updated = ServerProjectData.get(projectId);
                            if (updated == null) return;

                            ServerPlayerEntity owner = server.getPlayerManager()
                                    .getPlayer(java.util.UUID.fromString(updated.ownerUuid));
                            if (owner != null) {
                                ServerPlayNetworking.send(owner,
                                        new SyncPacket.ProjectSyncPayload(updated));
                                ServerPlayNetworking.send(owner,
                                        new SyncPacket.ProjectNotifyS2CPayload(
                                                "JOIN_REQUEST", projectId, updated.name,
                                                senderUuid.toString(), senderName));
                            }
                        }
                        case "ACCEPT_JOIN" -> {
                            String targetUuidStr = payload.targetUuid();
                            if (targetUuidStr.isBlank()) return;
                            UUID   joinerUuid = java.util.UUID.fromString(targetUuidStr);
                            String joinerName = ServerFriendData.getName(joinerUuid);

                            ServerPlayerEntity joinerEntity = server.getPlayerManager().getPlayer(joinerUuid);
                            if (joinerEntity != null) joinerName = joinerEntity.getName().getString();

                            boolean ok = ServerProjectData.acceptJoin(projectId, senderUuid, joinerUuid, joinerName);
                            if (!ok) return;
                            ProjectData updated = ServerProjectData.get(projectId);
                            if (updated == null) return;
                            broadcastProjectSync(server, updated);

                            broadcastProjectNotify(server, "COLLABORATOR_JOINED", projectId, updated.name,
                                    updated.ownerUuid, updated.ownerName,
                                    joinerUuid.toString(), joinerName);
                            if (joinerEntity != null) {
                                ServerPlayNetworking.send(joinerEntity,
                                        new SyncPacket.ProjectNotifyS2CPayload(
                                                "JOIN_ACCEPTED", projectId, updated.name,
                                                senderUuid.toString(), senderName));
                            }
                        }
                        case "DECLINE_JOIN" -> {
                            String targetUuidStr = payload.targetUuid();
                            if (targetUuidStr.isBlank()) return;
                            UUID joinerUuid = java.util.UUID.fromString(targetUuidStr);
                            boolean ok = ServerProjectData.declineJoin(projectId, senderUuid, joinerUuid);
                            if (!ok) return;
                            ProjectData updated = ServerProjectData.get(projectId);
                            if (updated != null) {
                                ServerPlayerEntity ownerEntity = server.getPlayerManager().getPlayer(senderUuid);
                                if (ownerEntity != null)
                                    ServerPlayNetworking.send(ownerEntity, new SyncPacket.ProjectSyncPayload(updated));
                            }
                            ServerPlayerEntity joinerEntity = server.getPlayerManager().getPlayer(joinerUuid);
                            if (joinerEntity != null && updated != null) {
                                ServerPlayNetworking.send(joinerEntity,
                                        new SyncPacket.ProjectNotifyS2CPayload(
                                                "JOIN_DECLINED", projectId, updated.name,
                                                senderUuid.toString(), senderName));
                            }
                        }
                        case "LEAVE" -> {
                            boolean ok = ServerProjectData.leaveProject(projectId, senderUuid);
                            if (!ok) return;
                            ProjectData updated = ServerProjectData.get(projectId);
                            if (updated != null) {
                                broadcastProjectSync(server, updated);

                                broadcastProjectNotify(server, "COLLABORATOR_LEFT", projectId, updated.name,
                                        updated.ownerUuid, updated.ownerName,
                                        senderUuid.toString(), senderName);
                            }
                        }
                        case "KICK" -> {
                            String targetUuidStr = payload.targetUuid();
                            if (targetUuidStr.isBlank()) return;
                            UUID targetUuid = java.util.UUID.fromString(targetUuidStr);
                            String targetName = ServerFriendData.getName(targetUuid);
                            ServerPlayerEntity targetEntity = server.getPlayerManager().getPlayer(targetUuid);
                            if (targetEntity != null) targetName = targetEntity.getName().getString();

                            boolean ok = ServerProjectData.kickCollaborator(projectId, senderUuid, targetUuid);
                            if (!ok) return;
                            ProjectData updated = ServerProjectData.get(projectId);
                            if (updated == null) return;
                            broadcastProjectSync(server, updated);
                            broadcastProjectNotify(server, "COLLABORATOR_LEFT", projectId, updated.name,
                                    updated.ownerUuid, updated.ownerName,
                                    targetUuid.toString(), targetName);

                            if (targetEntity != null) {
                                ServerPlayNetworking.send(targetEntity,
                                        new SyncPacket.ProjectNotifyS2CPayload(
                                                "KICKED", projectId, updated.name,
                                                senderUuid.toString(), senderName));
                            }
                        }
                        case "COMPLETE" -> {
                            boolean ok = ServerProjectData.complete(projectId, senderUuid);
                            if (!ok) return;
                            ProjectData updated = ServerProjectData.get(projectId);
                            if (updated == null) return;
                            broadcastProjectSync(server, updated);

                            broadcastProjectNotify(server, "COMPLETED", projectId, updated.name,
                                    senderUuid.toString(), senderName, "", "");

                            for (ProjectData.Collaborator c : updated.collaborators) {
                                if (c.uuid.equals(senderUuid.toString())) continue;
                                try {
                                    ServerPlayerEntity collab = server.getPlayerManager()
                                            .getPlayer(java.util.UUID.fromString(c.uuid));
                                    if (collab != null) {
                                        ServerPlayNetworking.send(collab,
                                                new SyncPacket.ProjectNotifyS2CPayload(
                                                        "COMPLETED", projectId, updated.name,
                                                        senderUuid.toString(), senderName));
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
        );

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity newPlayer = handler.player;
            UUID newUuid = newPlayer.getUuid();

            Set<UUID> currentlyOnline = server.getPlayerManager().getPlayerList()
                    .stream()
                    .map(ServerPlayerEntity::getUuid)
                    .collect(java.util.stream.Collectors.toSet());
            currentlyOnline.add(newUuid);
            playerDataCache.keySet().retainAll(currentlyOnline);
            statCache.keySet().retainAll(currentlyOnline);

            playerDataCache.remove(newUuid);
            statCache.remove(newUuid);

            playerDataCache.forEach((uuid, cached) -> {
                if (!uuid.equals(newUuid)) ServerPlayNetworking.send(newPlayer, cached);
            });
            statCache.forEach((uuid, cached) -> {
                if (!uuid.equals(newUuid)) ServerPlayNetworking.send(newPlayer, cached);
            });

            for (ProjectData project : ServerProjectData.getActive()) {
                ServerPlayNetworking.send(newPlayer, new SyncPacket.ProjectSyncPayload(project));
            }

            for (EventData event : ServerEventData.getActive()) {
                ServerPlayNetworking.send(newPlayer, new SyncPacket.EventSyncPayload(event));
            }

            pushStatsAsync(newPlayer, server);

            ServerFriendData.updateName(newUuid, newPlayer.getName().getString());

            {
                List<Map.Entry<UUID, String>> friendList = ServerFriendData.getFriendList(newUuid);
                List<String> friendUuids     = new ArrayList<>();
                List<String> friendUsernames = new ArrayList<>();
                for (Map.Entry<UUID, String> entry : friendList) {
                    friendUuids.add(entry.getKey().toString());
                    friendUsernames.add(entry.getValue());
                }
                ServerPlayNetworking.send(newPlayer,
                        new SyncPacket.FriendListSyncS2CPayload(friendUuids, friendUsernames));
            }

            for (UUID requesterUuid : ServerFriendData.incomingRequestsFor(newUuid)) {
                String requesterName = ServerFriendData.getName(requesterUuid);
                ServerPlayNetworking.send(newPlayer,
                        new SyncPacket.FriendNotifyS2CPayload(requesterUuid, requesterName, "REQUEST"));
            }

            for (UUID targetUuid : ServerFriendData.outgoingRequestsFrom(newUuid)) {
                String targetName = ServerFriendData.getName(targetUuid);
                ServerPlayNetworking.send(newPlayer,
                        new SyncPacket.FriendNotifyS2CPayload(targetUuid, targetName, "REQUEST_PENDING"));
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID leavingUuid = handler.player.getUuid();

            playerDataCache.remove(leavingUuid);
            statCache.remove(leavingUuid);
            debugOverrides.remove(leavingUuid);

            SyncPacket.PlayerLeftPayload leftPacket = new SyncPacket.PlayerLeftPayload(leavingUuid);
            server.getPlayerManager().getPlayerList().forEach(p -> {
                if (!p.getUuid().equals(leavingUuid)) {
                    ServerPlayNetworking.send(p, leftPacket);
                }
            });

            Wavelength.LOGGER.info("[Wavelength] Player {} left — notified {} remaining clients to evict cache.",
                    leavingUuid, server.getPlayerManager().getPlayerList().size());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++statPushTimer < STAT_PUSH_INTERVAL) return;
            statPushTimer = 0;

            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
            if (players.isEmpty()) return;

            List<ServerPlayerEntity> snapshot = List.copyOf(players);
            for (ServerPlayerEntity player : snapshot) {
                pushStatsAsync(player, server);
            }
            ServerProjectData.savePeriodic();

            long now = System.currentTimeMillis();
            for (EventData ev : ServerEventData.getActive()) {
                if (ev.endMs > 0 && ev.endMs <= now) {
                    UUID ownerUuid;
                    try { ownerUuid = UUID.fromString(ev.ownerUuid); }
                    catch (Exception ignored) { continue; }
                    boolean ended = ServerEventData.end(ev.id, ownerUuid);
                    if (ended) {
                        broadcastEventRemove(server, ev.id);
                        broadcastEventNotify(server, "EVENT_ENDED",
                                ev.id, ev.name, ev.ownerUuid, ev.ownerName);
                        Wavelength.LOGGER.info(
                                "[Wavelength] Event '{}' ({}) expired and was automatically ended.",
                                ev.name, ev.id);
                    }
                }
            }
        });

        registerDebugCommands();

        Wavelength.LOGGER.info("[Wavelength] Server init complete.");
    }

    static void broadcastProjectSync(MinecraftServer server, ProjectData project) {
        SyncPacket.ProjectSyncPayload payload = new SyncPacket.ProjectSyncPayload(project);
        server.getPlayerManager().getPlayerList()
                .forEach(p -> ServerPlayNetworking.send(p, payload));
    }

    static void broadcastProjectRemove(MinecraftServer server, String projectId) {
        SyncPacket.ProjectRemoveS2CPayload payload = new SyncPacket.ProjectRemoveS2CPayload(projectId);
        server.getPlayerManager().getPlayerList()
                .forEach(p -> ServerPlayNetworking.send(p, payload));
    }

    static void broadcastEventSync(MinecraftServer server, EventData event) {
        SyncPacket.EventSyncPayload payload = new SyncPacket.EventSyncPayload(event);
        server.getPlayerManager().getPlayerList()
                .forEach(p -> ServerPlayNetworking.send(p, payload));
    }

    static void broadcastEventRemove(MinecraftServer server, String eventId) {
        SyncPacket.EventRemoveS2CPayload payload = new SyncPacket.EventRemoveS2CPayload(eventId);
        server.getPlayerManager().getPlayerList()
                .forEach(p -> ServerPlayNetworking.send(p, payload));
    }

    static void broadcastEventNotify(MinecraftServer server,
                                     String action, String eventId, String eventName,
                                     String ownerUuidStr, String ownerName) {
        SyncPacket.ProjectNotifyS2CPayload payload = new SyncPacket.ProjectNotifyS2CPayload(
                "TIMELINE_" + action, eventId, eventName, ownerUuidStr, ownerName);
        server.getPlayerManager().getPlayerList()
                .forEach(p -> ServerPlayNetworking.send(p, payload));
    }

    static void broadcastProjectNotify(MinecraftServer server,
                                       String action, String projectId, String projectName,
                                       String ownerUuidStr, String ownerName,
                                       String extraUuidStr, String extraName) {
        String encodedName = (action.startsWith("COLLABORATOR") && !extraName.isEmpty())
                ? ownerName + "\0" + extraName
                : ownerName;
        SyncPacket.ProjectNotifyS2CPayload payload = new SyncPacket.ProjectNotifyS2CPayload(
                "TIMELINE_" + action, projectId, projectName, ownerUuidStr, encodedName);
        server.getPlayerManager().getPlayerList()
                .forEach(p -> ServerPlayNetworking.send(p, payload));
    }

    static void syncFriendList(MinecraftServer server, UUID playerUuid) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
        if (player == null) return;
        List<Map.Entry<UUID, String>> friendList = ServerFriendData.getFriendList(playerUuid);
        List<String> uuids = new ArrayList<>();
        List<String> usernames = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : friendList) {
            uuids.add(entry.getKey().toString());
            usernames.add(entry.getValue());
        }
        ServerPlayNetworking.send(player, new SyncPacket.FriendListSyncS2CPayload(uuids, usernames));
    }

    private static void pushStatsAsync(ServerPlayerEntity player, MinecraftServer server) {
        final UUID uuid = player.getUuid();

        final boolean hasOverride = debugOverrides.containsKey(uuid);
        final long[]  overrideSnap = hasOverride ? debugOverrides.get(uuid) : null;

        STAT_EXECUTOR.execute(() -> {
            SyncPacket.StatDataSyncPayload stats = hasOverride && overrideSnap != null
                    ? buildFromOverrides(uuid, overrideSnap)
                    : computeStats(player);

            server.execute(() -> {
                if (server.getPlayerManager().getPlayer(uuid) == null) return;
                statCache.put(uuid, stats);
                List<ServerPlayerEntity> recipients = server.getPlayerManager().getPlayerList();
                for (ServerPlayerEntity recipient : recipients) {
                    ServerPlayNetworking.send(recipient, stats);
                }
            });
        });
    }

    private static void broadcastActivity(net.minecraft.entity.player.PlayerEntity player,
                                          String activityName,
                                          MinecraftServer server) {
        if (server == null) return;
        UUID actorUuid = player.getUuid();
        SyncPacket.ActivitySyncPayload packet =
                new SyncPacket.ActivitySyncPayload(actorUuid, activityName);
        for (ServerPlayerEntity recipient : server.getPlayerManager().getPlayerList()) {
            if (!recipient.getUuid().equals(actorUuid)) {
                ServerPlayNetworking.send(recipient, packet);
            }
        }
    }

    private static SyncPacket.StatDataSyncPayload computeStats(ServerPlayerEntity player) {
        var sh = player.getStatHandler();

        long mined = 0;
        for (Block block : Registries.BLOCK) {
            if (!DOUBLE_HEIGHT_BLOCKS.contains(block)) {
                mined += sh.getStat(Stats.MINED, block);
            }
        }
        for (Block block : DOUBLE_HEIGHT_BLOCKS) {
            mined += sh.getStat(Stats.MINED, block) / 2;
        }

        long placed  = 0;
        long crafted = 0;
        for (var item : Registries.ITEM) {
            if (item instanceof BlockItem)
                placed += sh.getStat(Stats.USED, item);
            crafted += sh.getStat(Stats.CRAFTED, item);
        }

        long killed = 0;
        for (var et : Registries.ENTITY_TYPE)
            killed += sh.getStat(Stats.KILLED, et);

        long crops = 0;
        for (Block crop : new Block[]{
                Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
                Blocks.NETHER_WART, Blocks.COCOA, Blocks.SWEET_BERRY_BUSH}) {
            crops += sh.getStat(Stats.MINED, crop);
        }

        long dist =
                sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM))
                        + sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM))
                        + sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SWIM_ONE_CM))
                        + sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.FLY_ONE_CM))
                        + sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.CROUCH_ONE_CM));

        long playtime = sh.getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));

        return new SyncPacket.StatDataSyncPayload(
                player.getUuid(), mined, placed, killed, crops, crafted, dist, playtime);
    }

    private static SyncPacket.StatDataSyncPayload buildFromOverrides(UUID uuid, long[] o) {
        return new SyncPacket.StatDataSyncPayload(
                uuid, o[IDX_MINED], o[IDX_PLACED], o[IDX_KILLED],
                o[IDX_CROPS], o[IDX_CRAFTED], o[IDX_DIST_CM], o[IDX_PLAYTIME]);
    }

    private static final List<String> DEBUG_STATS  =
            List.of("mining", "building", "combat", "farming", "crafting", "exploring");
    private static final List<String> DEBUG_BADGES =
            List.of("NONE", "BRONZE", "SILVER", "GOLD", "DIAMOND");

    private static void registerDebugCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        CommandManager.literal("wldebug")

                                .then(CommandManager.literal("help")
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            src.sendFeedback(() -> Text.literal("§3§l── Wavelength Debug ──"), false);
                                            src.sendFeedback(() -> Text.literal("§e/wldebug setstat §7<stat> <value>"), false);
                                            src.sendFeedback(() -> Text.literal("§e/wldebug addstat §7<stat> <amount>"), false);
                                            src.sendFeedback(() -> Text.literal("§e/wldebug setbadge §7<tier>"), false);
                                            src.sendFeedback(() -> Text.literal("§e/wldebug showstats"), false);
                                            src.sendFeedback(() -> Text.literal("§e/wldebug resetstats"), false);
                                            src.sendFeedback(() -> Text.literal("§7Stats: §fmining  building  combat  farming  crafting  exploring"), false);
                                            src.sendFeedback(() -> Text.literal("§7Tiers: §fNONE  BRONZE  SILVER  GOLD  DIAMOND"), false);
                                            src.sendFeedback(() -> Text.literal("§7'exploring' is in §emeters§7. Thresholds: BRONZE=1k SILVER=5k GOLD=10k DIAMOND=50k"), false);
                                            return 1;
                                        }))

                                .then(CommandManager.literal("showstats")
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            long[] o = getOrInitOverrides(player.getUuid(), player);
                                            ServerCommandSource src = ctx.getSource();
                                            src.sendFeedback(() -> Text.literal("§3§l── Wavelength Stats ──"), false);
                                            src.sendFeedback(() -> Text.literal(String.format("§emining    §f%d  §7→ %s",  o[IDX_MINED],             badgeName(o[IDX_MINED],           "mining"))),       false);
                                            src.sendFeedback(() -> Text.literal(String.format("§ebuilding  §f%d  §7→ %s",  o[IDX_PLACED],            badgeName(o[IDX_PLACED],          "building"))),      false);
                                            src.sendFeedback(() -> Text.literal(String.format("§ecombat    §f%d  §7→ %s",  o[IDX_KILLED],            badgeName(o[IDX_KILLED],          "combat"))),        false);
                                            src.sendFeedback(() -> Text.literal(String.format("§efarming   §f%d  §7→ %s",  o[IDX_CROPS],             badgeName(o[IDX_CROPS],           "farming"))),       false);
                                            src.sendFeedback(() -> Text.literal(String.format("§ecrafting  §f%d  §7→ %s",  o[IDX_CRAFTED],           badgeName(o[IDX_CRAFTED],         "crafting"))),      false);
                                            src.sendFeedback(() -> Text.literal(String.format("§eexploring §f%dm  §7→ %s", o[IDX_DIST_CM] / 1000L,   badgeName(o[IDX_DIST_CM] / 1000L, "exploring"))),     false);
                                            return 1;
                                        }))

                                .then(CommandManager.literal("resetstats")
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            debugOverrides.remove(player.getUuid());
                                            ctx.getSource().sendFeedback(() -> Text.literal("§aDebug overrides cleared — real stats restored."), false);
                                            pushStatsAsync(player, ctx.getSource().getServer());
                                            return 1;
                                        }))

                                .then(CommandManager.literal("setstat")
                                        .then(CommandManager.argument("stat", StringArgumentType.word())
                                                .suggests((ctx, b) -> { DEBUG_STATS.forEach(b::suggest); return b.buildFuture(); })
                                                .then(CommandManager.argument("value", LongArgumentType.longArg(0))
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                            String stat  = StringArgumentType.getString(ctx, "stat");
                                                            long   value = LongArgumentType.getLong(ctx, "value");
                                                            if (!DEBUG_STATS.contains(stat)) {
                                                                ctx.getSource().sendError(Text.literal("§cUnknown stat. Valid: " + String.join(", ", DEBUG_STATS)));
                                                                return 0;
                                                            }
                                                            long[] o = getOrInitOverrides(player.getUuid(), player);
                                                            writeStatToOverrides(o, stat, value);
                                                            long display = readStatFromOverrides(o, stat);
                                                            ctx.getSource().sendFeedback(() -> Text.literal(
                                                                    "§aSet §e" + stat + " §ato §e" + display + unitSuffix(stat)
                                                                            + " §7→ " + badgeName(display, stat)), false);
                                                            pushDebugStats(player, o, ctx.getSource().getServer());
                                                            return 1;
                                                        }))))

                                .then(CommandManager.literal("addstat")
                                        .then(CommandManager.argument("stat", StringArgumentType.word())
                                                .suggests((ctx, b) -> { DEBUG_STATS.forEach(b::suggest); return b.buildFuture(); })
                                                .then(CommandManager.argument("amount", LongArgumentType.longArg(0))
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                            String stat   = StringArgumentType.getString(ctx, "stat");
                                                            long   amount = LongArgumentType.getLong(ctx, "amount");
                                                            if (!DEBUG_STATS.contains(stat)) {
                                                                ctx.getSource().sendError(Text.literal("§cUnknown stat. Valid: " + String.join(", ", DEBUG_STATS)));
                                                                return 0;
                                                            }
                                                            long[] o = getOrInitOverrides(player.getUuid(), player);
                                                            long current = readStatFromOverrides(o, stat);
                                                            writeStatToOverrides(o, stat, current + amount);
                                                            long newVal = readStatFromOverrides(o, stat);
                                                            ctx.getSource().sendFeedback(() -> Text.literal(
                                                                    "§aAdded §e" + amount + unitSuffix(stat) + " §ato §e" + stat
                                                                            + " §7(now " + newVal + ") §7→ " + badgeName(newVal, stat)), false);
                                                            pushDebugStats(player, o, ctx.getSource().getServer());
                                                            return 1;
                                                        }))))

                                .then(CommandManager.literal("setbadge")
                                        .then(CommandManager.argument("tier", StringArgumentType.word())
                                                .suggests((ctx, b) -> { DEBUG_BADGES.forEach(b::suggest); return b.buildFuture(); })
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                    String tierStr = StringArgumentType.getString(ctx, "tier").toUpperCase();
                                                    com.lythroo.wavelength.common.data.Achievement tier;
                                                    try { tier = com.lythroo.wavelength.common.data.Achievement.valueOf(tierStr); }
                                                    catch (IllegalArgumentException e) {
                                                        ctx.getSource().sendError(Text.literal("§cUnknown tier. Valid: " + String.join(", ", DEBUG_BADGES)));
                                                        return 0;
                                                    }
                                                    long[] o = getOrInitOverrides(player.getUuid(), player);

                                                    o[IDX_MINED]   = tier.thresholdFor("mining");
                                                    o[IDX_PLACED]  = tier.thresholdFor("building");
                                                    o[IDX_KILLED]  = tier.thresholdFor("combat");
                                                    o[IDX_CROPS]   = tier.thresholdFor("farming");
                                                    o[IDX_CRAFTED] = tier.thresholdFor("crafting");

                                                    o[IDX_DIST_CM] = (long) tier.thresholdFor("exploring") * 1000L;
                                                    ctx.getSource().sendFeedback(() -> Text.literal(
                                                            "§aAll stats set to §e" + tierStr + " §athresholds (category-specific)."), false);
                                                    pushDebugStats(player, o, ctx.getSource().getServer());
                                                    return 1;
                                                })))
                ));
    }

    private static long[] getOrInitOverrides(UUID uuid, ServerPlayerEntity player) {
        return debugOverrides.computeIfAbsent(uuid, k -> {
            SyncPacket.StatDataSyncPayload real = computeStats(player);
            return new long[]{
                    real.blocksMined(), real.blocksPlaced(), real.mobsKilled(),
                    real.cropsHarvested(), real.itemsCrafted(), real.distanceTraveled(),
                    real.playtimeTicks()
            };
        });
    }

    private static final long DEBUG_STAT_MAX = 10_000_000L;

    private static void writeStatToOverrides(long[] o, String stat, long value) {

        long safe = Math.max(0, Math.min(value, DEBUG_STAT_MAX));
        switch (stat) {
            case "mining"    -> o[IDX_MINED]   = safe;
            case "building"  -> o[IDX_PLACED]  = safe;
            case "combat"    -> o[IDX_KILLED]  = safe;
            case "farming"   -> o[IDX_CROPS]   = safe;
            case "crafting"  -> o[IDX_CRAFTED] = safe;

            case "exploring" -> o[IDX_DIST_CM] = safe * 1000L;
        }
    }

    private static long readStatFromOverrides(long[] o, String stat) {
        return switch (stat) {
            case "mining"    -> o[IDX_MINED];
            case "building"  -> o[IDX_PLACED];
            case "combat"    -> o[IDX_KILLED];
            case "farming"   -> o[IDX_CROPS];
            case "crafting"  -> o[IDX_CRAFTED];
            case "exploring" -> o[IDX_DIST_CM] / 1000L;
            default          -> 0;
        };
    }

    private static void pushDebugStats(ServerPlayerEntity player, long[] o, MinecraftServer server) {
        SyncPacket.StatDataSyncPayload payload = buildFromOverrides(player.getUuid(), o);
        statCache.put(player.getUuid(), payload);
        server.execute(() -> {
            for (ServerPlayerEntity recipient : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(recipient, payload);
            }
        });
    }

    private static String badgeName(long count, String category) {
        com.lythroo.wavelength.common.data.Achievement badge =
                com.lythroo.wavelength.common.data.Achievement.forCount(count, category);
        return switch (badge) {
            case BRONZE  -> "§6Bronze";
            case SILVER  -> "§7Silver";
            case GOLD    -> "§eGold";
            case DIAMOND -> "§bDiamond";
            default      -> "§8None";
        } + "§r";
    }

    private static String unitSuffix(String stat) {
        return stat.equals("exploring") ? "m" : "";
    }
}