package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.common.network.SyncPacket;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.ArrayList;
import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.command.permission.PermissionPredicate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;

final class HubProjectsTab {

    private static final String[] ALL_MATERIALS = {
            "acacia_log", "acacia_planks", "acacia_slab", "acacia_stairs", "acacia_fence",
            "andesite", "andesite_slab", "andesite_stairs", "andesite_wall",
            "bamboo_block", "bamboo_planks", "bamboo_slab", "bamboo_stairs",
            "barrel", "basalt", "birch_log", "birch_planks", "birch_slab", "birch_stairs", "birch_fence",
            "blackstone", "blackstone_slab", "blackstone_stairs", "blackstone_wall",
            "blast_furnace", "bone_block", "bookshelf",
            "brick", "brick_slab", "brick_stairs", "brick_wall",
            "calcite", "campfire", "chain",
            "cherry_log", "cherry_planks", "cherry_slab", "cherry_stairs", "cherry_fence",
            "chest", "chiseled_deepslate", "chiseled_nether_bricks", "chiseled_quartz_block",
            "chiseled_sandstone", "chiseled_stone_bricks",
            "clay", "clay_ball", "coal", "coal_block", "cobblestone",
            "cobblestone_slab", "cobblestone_stairs", "cobblestone_wall",
            "cobbled_deepslate", "cobbled_deepslate_slab", "cobbled_deepslate_stairs",
            "copper_block", "copper_ingot", "cracked_stone_bricks",
            "crimson_planks", "crimson_slab", "crimson_stairs", "crimson_fence",
            "cut_copper", "cut_copper_slab", "cut_copper_stairs",
            "cut_sandstone", "cut_red_sandstone",
            "dark_oak_log", "dark_oak_planks", "dark_oak_slab", "dark_oak_stairs", "dark_oak_fence",
            "dark_prismarine", "dark_prismarine_slab", "dark_prismarine_stairs",
            "deepslate_bricks", "deepslate_brick_slab", "deepslate_brick_stairs",
            "deepslate_tiles", "deepslate_tile_slab", "deepslate_tile_stairs",
            "diamond", "diamond_block",
            "diorite", "diorite_slab", "diorite_stairs", "diorite_wall",
            "dirt", "dripstone_block",
            "emerald", "emerald_block",
            "end_stone", "end_stone_bricks", "end_stone_brick_slab", "end_stone_brick_stairs",
            "furnace", "glass", "glass_pane",
            "gold_block", "gold_ingot", "gold_nugget",
            "granite", "granite_slab", "granite_stairs", "granite_wall",
            "gravel",
            "honey_block", "honeycomb_block",
            "iron_block", "iron_ingot", "iron_nugget",
            "jungle_log", "jungle_planks", "jungle_slab", "jungle_stairs", "jungle_fence",
            "ladder", "lantern", "lapis_block", "lapis_lazuli",
            "magma_block", "mangrove_log", "mangrove_planks", "mangrove_slab", "mangrove_stairs",
            "mossy_cobblestone", "mossy_cobblestone_slab", "mossy_cobblestone_stairs",
            "mossy_stone_bricks", "mossy_stone_brick_slab", "mossy_stone_brick_stairs",
            "mud_bricks", "mud_brick_slab", "mud_brick_stairs",
            "muddy_mangrove_roots",
            "nether_brick", "nether_bricks", "nether_brick_slab", "nether_brick_stairs", "nether_brick_fence",
            "netherite_block", "netherite_ingot",
            "netherrack", "nether_wart_block",
            "oak_log", "oak_planks", "oak_slab", "oak_stairs", "oak_fence",
            "obsidian",
            "packed_ice", "packed_mud",
            "pale_oak_log", "pale_oak_planks", "pale_oak_slab", "pale_oak_stairs",
            "polished_andesite", "polished_andesite_slab", "polished_andesite_stairs",
            "polished_basalt",
            "polished_blackstone", "polished_blackstone_slab", "polished_blackstone_stairs",
            "polished_deepslate", "polished_deepslate_slab", "polished_deepslate_stairs",
            "polished_diorite", "polished_diorite_slab", "polished_diorite_stairs",
            "polished_granite", "polished_granite_slab", "polished_granite_stairs",
            "prismarine", "prismarine_bricks", "prismarine_slab", "prismarine_stairs",
            "purpur_block", "purpur_pillar", "purpur_slab", "purpur_stairs",
            "quartz_block", "quartz_bricks", "quartz_pillar", "quartz_slab", "quartz_stairs",
            "raw_copper_block", "raw_gold_block", "raw_iron_block",
            "red_sandstone", "red_sandstone_slab", "red_sandstone_stairs",
            "redstone_block",
            "sand", "sandstone", "sandstone_slab", "sandstone_stairs", "sandstone_wall",
            "sea_lantern",
            "smooth_basalt", "smooth_quartz", "smooth_quartz_slab", "smooth_quartz_stairs",
            "smooth_red_sandstone", "smooth_red_sandstone_slab", "smooth_red_sandstone_stairs",
            "smooth_sandstone", "smooth_sandstone_slab", "smooth_sandstone_stairs",
            "smooth_stone", "smooth_stone_slab",
            "snow_block", "soul_sand", "soul_soil",
            "spruce_log", "spruce_planks", "spruce_slab", "spruce_stairs", "spruce_fence",
            "stone", "stone_bricks", "stone_brick_slab", "stone_brick_stairs", "stone_brick_wall",
            "stone_slab", "stone_stairs",
            "stripped_acacia_log", "stripped_bamboo_block", "stripped_birch_log",
            "stripped_cherry_log", "stripped_crimson_stem", "stripped_dark_oak_log",
            "stripped_jungle_log", "stripped_mangrove_log", "stripped_oak_log",
            "stripped_spruce_log", "stripped_warped_stem",
            "terracotta",
            "torch", "tuff", "tuff_bricks", "tuff_brick_slab", "tuff_brick_stairs",
            "warped_planks", "warped_slab", "warped_stairs", "warped_fence",
            "white_concrete", "white_concrete_powder",
            "white_terracotta", "white_wool",
            "waxed_copper_block", "waxed_cut_copper", "waxed_cut_copper_slab",
            "wet_sponge",
    };

    private static final int AUTOCOMPLETE_LIMIT = 7;

    private static final java.util.Set<String> pendingJoinProjectIds =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    private final HubScreen hub;
    private FlowLayout autocompleteBox = null;

    HubProjectsTab(HubScreen hub) { this.hub = hub; }

    UIComponent build() {
        if (hub.projectFormOpen) return buildFormMode();
        return buildViewMode();
    }

    private UIComponent buildViewMode() {
        int innerW = Math.min(hub.panelW - 32, hub.width - 50);
        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(12));
        panel.gap(6);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            panel.child(UIComponents.label(Text.literal("Not in a world.").withColor(COL_GRAY)));
            return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
        }
        UUID localUuid = client.player.getUuid();

        pendingJoinProjectIds.removeIf(id -> {
            ProjectData p = ClientProjectCache.get(id);
            return p == null || p.isCollaborator(localUuid) || p.isOwner(localUuid);
        });

        panel.child(sectionHeader("🏗  MY PROJECT"));
        panel.child(divider(hub.panelW));

        ProjectData myProject = ClientProjectCache.getByOwner(localUuid);
        if (myProject == null) {
            panel.child(UIComponents.label(
                    Text.literal("You don't have an active project yet.").withColor(COL_GRAY)));
            panel.child(spacer(4));
            panel.child(styledBtn(160, 22,
                    Text.literal("＋  Create Project").withColor(0xFF88FF88),
                    0xFF153A15, 0xFF1F5A1F,
                    () -> openForm(null)));
        } else {
            panel.child(buildProjectCard(myProject, localUuid, innerW, true));
            if (!myProject.pendingJoins.isEmpty()) {
                panel.child(spacer(6));
                panel.child(sectionHeader("📩  JOIN REQUESTS  (" + myProject.pendingJoins.size() + ")"));
                panel.child(divider(hub.panelW));
                for (ProjectData.PendingJoin join : myProject.pendingJoins)
                    panel.child(buildJoinRequestRow(myProject, join, innerW));
            }
        }

        List<ProjectData> collaborating = ClientProjectCache.getCollaborating(localUuid);
        if (!collaborating.isEmpty()) {
            panel.child(spacer(10));
            panel.child(sectionHeader("🤝  COLLABORATING ON  (" + collaborating.size() + ")"));
            panel.child(divider(hub.panelW));
            for (ProjectData p : collaborating) {
                panel.child(buildProjectCard(p, localUuid, innerW, false));
                panel.child(spacer(4));
            }
        }

        List<ProjectData> open = ClientProjectCache.getOpenProjects(localUuid);
        panel.child(spacer(10));
        panel.child(sectionHeader("🌐  OPEN PROJECTS  (" + open.size() + ")"));
        panel.child(divider(hub.panelW));
        if (open.isEmpty()) {
            panel.child(UIComponents.label(
                    Text.literal("No open projects on this server yet.").withColor(COL_GRAY)));
        } else {
            for (ProjectData p : open) {
                panel.child(buildProjectCard(p, localUuid, innerW, false));
                panel.child(spacer(4));
            }
        }

        panel.child(spacer(16));
        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    private FlowLayout buildProjectCard(ProjectData p, UUID viewerUuid, int innerW, boolean isOwn) {
        int normalBg = 0xCC0E1520;
        int hoverBg  = 0xCC162030;

        FlowLayout card = UIContainers.verticalFlow(Sizing.fixed(innerW), Sizing.content());
        card.surface(Surface.flat(normalBg));
        card.padding(Insets.of(8));
        card.gap(4);
        card.margins(Insets.vertical(2));

        if (isOwn && !p.completed) {
            card.mouseEnter().subscribe(() -> card.surface(Surface.flat(hoverBg)));
            card.mouseLeave().subscribe(() -> card.surface(Surface.flat(normalBg)));
            card.mouseDown().subscribe((mx, my) -> {
                playClick();
                MinecraftClient.getInstance().execute(() -> openForm(p));
                return true;
            });
        }

        FlowLayout header = UIContainers.horizontalFlow(Sizing.fixed(innerW - 16), Sizing.content());
        header.gap(6);
        header.verticalAlignment(VerticalAlignment.CENTER);
        FlowLayout titleGroup = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        LabelComponent titleLbl = UIComponents.label(
                Text.literal("🏗  " + p.name).withColor(0xFFDDEEFF));
        titleLbl.shadow(true);
        titleGroup.child(titleLbl);
        header.child(titleGroup);
        if (isOwn && !p.completed)
            header.child(UIComponents.label(Text.literal("✎ edit").withColor(0xFF444466)));
        card.child(header);

        if (!isOwn)
            card.child(UIComponents.label(Text.literal("Owner: " + p.ownerName).withColor(COL_GRAY)));
        if (!p.description.isBlank())
            card.child(UIComponents.label(Text.literal(p.description).withColor(0xFF888899)));

        card.child(buildProgressBar(p, innerW - 16));

        if (!p.materials.isEmpty()) {
            card.child(UIComponents.label(Text.literal("Materials:").withColor(0xFF9988BB)));
            for (ProjectData.Material m : p.materials)
                card.child(UIComponents.label(Text.literal(
                                "  • " + m.name + ": " + m.current + "/" + m.required)
                        .withColor(0xFFAABBCC)));
        }

        if (p.hasLocation)
            card.child(UIComponents.label(Text.literal(
                    "📍 (" + p.locX + ", " + p.locY + ", " + p.locZ + ")").withColor(0xFF55DDFF)));

        if (!p.collaborators.isEmpty()) {
            card.child(UIComponents.label(
                    Text.literal("Collaborators (" + p.collaborators.size() + "):").withColor(0xFF9988BB)));
            for (ProjectData.Collaborator c : p.collaborators) {
                boolean isOwner_ = c.uuid.equals(p.ownerUuid);
                String contrib = c.blocksContributed > 0
                        ? " — " + formatNum(c.blocksContributed) + " blocks" : "";

                if (isOwn && !isOwner_ && !p.completed) {

                    FlowLayout collabRow = UIContainers.horizontalFlow(
                            Sizing.fixed(innerW - 16), Sizing.fixed(16));
                    collabRow.gap(4);
                    collabRow.verticalAlignment(VerticalAlignment.CENTER);
                    LabelComponent collabLbl = UIComponents.label(
                            Text.literal("  • " + c.name + contrib).withColor(0xFFCCCCDD));
                    collabLbl.sizing(Sizing.fixed(innerW - 16 - 38 - 4), Sizing.content());
                    collabRow.child(collabLbl);
                    final String kickUuid = c.uuid;
                    final String kickName = c.name;
                    collabRow.child(styledBtn(38, 12,
                            Text.literal("✗ Kick").withColor(0xFFFF6655),
                            0xFF2A0E0E, 0xFF3A1515,
                            () -> {
                                playNegative();
                                sendAction(p.id, "KICK", kickUuid);
                            }));
                    card.child(collabRow);
                } else {

                    card.child(UIComponents.label(
                            Text.literal("  • " + c.name + (isOwner_ ? " (Owner)" : "") + contrib)
                                    .withColor(isOwner_ ? COL_GOLD : 0xFFCCCCDD)));
                }
            }
        }

        FlowLayout footer = UIContainers.horizontalFlow(Sizing.fixed(innerW - 16), Sizing.content());
        footer.gap(8);
        int collabColor = switch (p.collaboration) {
            case "HELP_WANTED" -> 0xFF55FF55;
            case "TEAM"        -> 0xFF55AAFF;
            default            -> COL_GRAY;
        };
        footer.child(UIComponents.label(Text.literal(p.collaborationLabel()).withColor(collabColor)));
        LabelComponent timeLbl = UIComponents.label(
                Text.literal("Started " + p.timeAgo()).withColor(0xFF444466));
        timeLbl.sizing(Sizing.fill(100), Sizing.content());
        footer.child(timeLbl);
        card.child(footer);

        card.child(buildCardButtons(p, viewerUuid, isOwn, innerW - 16));
        return card;
    }

    private FlowLayout buildProgressBar(ProjectData p, int barW) {
        int pct = Math.max(0, Math.min(100, p.progress));
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(barW), Sizing.content());
        row.gap(6);
        row.verticalAlignment(VerticalAlignment.CENTER);
        int trackW = (int)(barW * 0.6f);
        int fillW  = Math.max(0, (int)(trackW * pct / 100f));
        FlowLayout track = UIContainers.horizontalFlow(Sizing.fixed(trackW), Sizing.fixed(6));
        track.surface(Surface.flat(COL_BAR_BG));
        if (fillW > 0) {
            FlowLayout fill = UIContainers.horizontalFlow(
                    Sizing.fixed(Math.min(fillW, trackW)), Sizing.fixed(6));
            fill.surface(Surface.flat(pct >= 100 ? 0xFF55FF55 : 0xFF5588FF));
            track.child(fill);
        }
        row.child(track);
        row.child(UIComponents.label(
                Text.literal(pct + "%").withColor(pct >= 100 ? 0xFF55FF55 : 0xFFCCCCFF)));
        return row;
    }

    private FlowLayout buildCardButtons(ProjectData p, UUID viewerUuid, boolean isOwn, int btnAreaW) {
        FlowLayout btnRow = UIContainers.horizontalFlow(Sizing.fixed(btnAreaW), Sizing.content());
        btnRow.gap(4);
        MinecraftClient _mc = MinecraftClient.getInstance();
        PermissionPredicate _perms = _mc.player != null ? _mc.player.getPermissions() : PermissionPredicate.NONE;
        boolean isOp = _perms == LeveledPermissionPredicate.GAMEMASTERS
                || _perms == LeveledPermissionPredicate.ADMINS
                || _perms == LeveledPermissionPredicate.OWNERS;

        if (isOwn) {
            if (!p.completed) {
                btnRow.child(styledBtn(116, 18, Text.literal("✔ Mark Complete").withColor(0xFF55FF55),
                        0xFF153A15, 0xFF1F5A1F, () -> sendAction(p.id, "COMPLETE", "")));
                btnRow.child(styledBtn(80, 18, Text.literal("🗑 Delete").withColor(0xFFFF5555),
                        0xFF3A1515, 0xFF5A1F1F, () -> sendAction(p.id, "DELETE", "")));
            }
        } else {
            if (isOp) {

                btnRow.child(styledBtn(90, 18, Text.literal("🗑 Admin Del").withColor(0xFFFF4444),
                        0xFF2A0808, 0xFF3A1010, () -> sendAction(p.id, "DELETE", "")));
            }
            if (p.isCollaborator(viewerUuid)) {
                btnRow.child(styledBtn(100, 18, Text.literal("Leave Project").withColor(0xFFFF8855),
                        0xFF2A1510, 0xFF3A1F15, () -> sendAction(p.id, "LEAVE", "")));
            } else if (p.hasPendingFrom(viewerUuid) || pendingJoinProjectIds.contains(p.id)) {
                btnRow.child(UIComponents.label(
                        Text.literal("⏳ Join request sent…").withColor(0xFF888888)));
            } else if (p.isHelpWanted() || p.isTeamProject()) {
                btnRow.child(styledBtn(110, 18, Text.literal("⚑ Join Project").withColor(0xFFAAFFAA),
                        0xFF153A15, 0xFF1F5A1F, () -> {
                            pendingJoinProjectIds.add(p.id);
                            sendAction(p.id, "REQUEST_JOIN", "");
                        }));
            }
        }
        return btnRow;
    }

    private FlowLayout buildJoinRequestRow(ProjectData project, ProjectData.PendingJoin join, int innerW) {
        FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(24));
        row.surface(Surface.flat(0xFF1A1A2E));
        row.padding(Insets.horizontal(6));
        row.gap(6);
        row.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent nameLbl = UIComponents.label(
                Text.literal("⭐ " + join.name).withColor(0xFFCCCCFF));
        nameLbl.sizing(Sizing.fixed(Math.max(40, innerW - 12 - 64 - 6 - 66 - 6)), Sizing.content());
        row.child(nameLbl);
        row.child(styledBtn(64, 18, Text.literal("✓ Accept").withColor(0xFF55FF55), 0xFF153A15, 0xFF1F5A1F,
                () -> { playSuccess(); sendAction(project.id, "ACCEPT_JOIN", join.uuid); }));
        row.child(styledBtn(66, 18, Text.literal("✗ Decline").withColor(0xFFFF5555), 0xFF3A1515, 0xFF5A1F1F,
                () -> { playNegative(); sendAction(project.id, "DECLINE_JOIN", join.uuid); }));
        return row;
    }

    private UIComponent buildFormMode() {

        int innerW = Math.min(hub.panelW, hub.width - 40);
        int fieldW = Math.min(220, innerW - 20);

        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(16));
        panel.gap(8);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        boolean isNew = (hub.projectFormId == null);
        panel.child(sectionHeader(isNew ? "＋  CREATE PROJECT" : "✎  EDIT PROJECT"));
        panel.child(divider(hub.panelW));

        panel.child(UIComponents.label(Text.literal("Project Name:").withColor(COL_HEADER_TXT)));
        TextBoxComponent nameField = UIComponents.textBox(Sizing.fixed(fieldW));
        nameField.setMaxLength(40);
        nameField.setText(hub.projectFormName);
        nameField.onChanged().subscribe(s -> hub.projectFormName = s);
        panel.child(nameField);
        LabelComponent nameLive = UIComponents.label(
                Text.literal(hub.projectFormName.isBlank() ? "(empty)" : hub.projectFormName)
                        .withColor(0xFF555577));
        nameField.onChanged().subscribe(s ->
                nameLive.text(Text.literal(s.isBlank() ? "(empty)" : s).withColor(0xFF555577)));
        panel.child(nameLive);

        panel.child(UIComponents.label(Text.literal("Description (optional):").withColor(COL_HEADER_TXT)));
        TextBoxComponent descField = UIComponents.textBox(Sizing.fixed(fieldW));
        descField.setMaxLength(80);
        descField.setText(hub.projectFormDesc);
        descField.onChanged().subscribe(s -> hub.projectFormDesc = s);
        panel.child(descField);
        LabelComponent descLive = UIComponents.label(
                Text.literal(hub.projectFormDesc.isBlank() ? "(empty)" : hub.projectFormDesc)
                        .withColor(0xFF555577));
        descField.onChanged().subscribe(s ->
                descLive.text(Text.literal(s.isBlank() ? "(empty)" : s).withColor(0xFF555577)));
        panel.child(descLive);

        panel.child(UIComponents.label(Text.literal("Progress:").withColor(COL_HEADER_TXT)));

        hub.projectProgressLabel = UIComponents.label(
                Text.literal(hub.projectFormProgress + "%").withColor(0xFFCCCCFF));

        FlowLayout progressRow = UIContainers.horizontalFlow(Sizing.fixed(fieldW), Sizing.fixed(22));
        progressRow.gap(6);
        progressRow.verticalAlignment(VerticalAlignment.CENTER);
        progressRow.horizontalAlignment(HorizontalAlignment.CENTER);
        progressRow.child(styledBtn(22, 18, Text.literal("-").withColor(0xFFFFAAAA),
                0xFF2A1510, 0xFF3A2015,
                () -> {
                    hub.projectFormProgress = Math.max(0, hub.projectFormProgress - 5);
                    if (hub.projectProgressLabel != null)
                        hub.projectProgressLabel.text(
                                Text.literal(hub.projectFormProgress + "%").withColor(0xFFCCCCFF));
                }));
        progressRow.child(hub.projectProgressLabel);
        progressRow.child(styledBtn(22, 18, Text.literal("+").withColor(0xFFAAFFAA),
                0xFF102A10, 0xFF154A15,
                () -> {
                    hub.projectFormProgress = Math.min(100, hub.projectFormProgress + 5);
                    if (hub.projectProgressLabel != null)
                        hub.projectProgressLabel.text(
                                Text.literal(hub.projectFormProgress + "%").withColor(0xFFCCCCFF));
                }));
        panel.child(progressRow);

        panel.child(UIComponents.label(Text.literal("Collaboration:").withColor(COL_HEADER_TXT)));
        FlowLayout collabGrid = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        collabGrid.gap(4);
        hub.projectCollabGridRef = collabGrid;
        buildCollabButtons(collabGrid);
        panel.child(collabGrid);

        panel.child(UIComponents.label(Text.literal("Location (optional):").withColor(COL_HEADER_TXT)));
        boolean locOn = hub.projectFormHasLoc;
        panel.child(styledBtn(104, 18,
                Text.literal(locOn ? "☑ Location On" : "☐ Location Off")
                        .withColor(locOn ? 0xFF55FF88 : COL_GRAY),
                locOn ? 0xFF153A15 : 0xFF1E1E3A,
                locOn ? 0xFF1F5A1F : 0xFF2E2E55,
                () -> { hub.projectFormHasLoc = !hub.projectFormHasLoc; hub.fillContent(); }));

        if (hub.projectFormHasLoc) {

            FlowLayout coordRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
            coordRow.gap(6);
            coordRow.verticalAlignment(VerticalAlignment.CENTER);

            coordRow.child(UIComponents.label(Text.literal("X").withColor(COL_GRAY)));
            TextBoxComponent xField = UIComponents.textBox(Sizing.fixed(52));
            xField.setMaxLength(8); xField.setText(hub.projectFormLocXStr);
            xField.onChanged().subscribe(s -> hub.projectFormLocXStr = s);
            coordRow.child(xField);

            coordRow.child(UIComponents.label(Text.literal("Y").withColor(COL_GRAY)));
            TextBoxComponent yField = UIComponents.textBox(Sizing.fixed(46));
            yField.setMaxLength(8); yField.setText(hub.projectFormLocYStr);
            yField.onChanged().subscribe(s -> hub.projectFormLocYStr = s);
            coordRow.child(yField);

            coordRow.child(UIComponents.label(Text.literal("Z").withColor(COL_GRAY)));
            TextBoxComponent zField = UIComponents.textBox(Sizing.fixed(52));
            zField.setMaxLength(8); zField.setText(hub.projectFormLocZStr);
            zField.onChanged().subscribe(s -> hub.projectFormLocZStr = s);
            coordRow.child(zField);
            panel.child(coordRow);

            panel.child(UIComponents.label(
                    Text.literal("→ " + hub.projectFormLocXStr
                                    + ", " + hub.projectFormLocYStr
                                    + ", " + hub.projectFormLocZStr)
                            .withColor(0xFF55DDFF)));

            panel.child(styledBtn(128, 18, Text.literal("📍 Use My Position").withColor(0xFF55DDFF),
                    0xFF0D1E2A, 0xFF152A3A, () -> {
                        MinecraftClient mc = MinecraftClient.getInstance();
                        if (mc.player != null) {
                            hub.projectFormLocXStr = String.valueOf((long) mc.player.getX());
                            hub.projectFormLocYStr = String.valueOf((long) mc.player.getY());
                            hub.projectFormLocZStr = String.valueOf((long) mc.player.getZ());
                            hub.fillContent();
                        }
                    }));
        }

        panel.child(divider(hub.panelW));
        panel.child(sectionHeader("📦  Materials"));

        hub.projectMatsGridRef = UIContainers.verticalFlow(Sizing.fixed(fieldW), Sizing.content());
        hub.projectMatsGridRef.gap(2);
        refreshMatsGrid(fieldW);
        panel.child(hub.projectMatsGridRef);

        panel.child(spacer(2));
        panel.child(UIComponents.label(Text.literal("Add Material:").withColor(COL_HEADER_TXT)));

        panel.child(UIComponents.label(Text.literal("Name:").withColor(COL_GRAY)));
        TextBoxComponent matNameField = UIComponents.textBox(Sizing.fixed(fieldW));
        matNameField.setMaxLength(40);
        matNameField.setText(hub.projectFormMatFilter);
        matNameField.onChanged().subscribe(s -> {
            hub.projectFormMatFilter = s;
            hub.projectFormNewMatName = s;
            rebuildAutocomplete(s, fieldW);
        });
        panel.child(matNameField);

        LabelComponent matNameLive = UIComponents.label(
                Text.literal(hub.projectFormMatFilter.isBlank()
                                ? "type to search blocks…" : "→ " + hub.projectFormMatFilter)
                        .withColor(0xFF555577));
        matNameField.onChanged().subscribe(s -> matNameLive.text(
                Text.literal(s.isBlank() ? "type to search blocks…" : "→ " + s)
                        .withColor(0xFF555577)));
        panel.child(matNameLive);

        autocompleteBox = UIContainers.verticalFlow(Sizing.fixed(fieldW), Sizing.content());
        autocompleteBox.gap(1);
        rebuildAutocomplete(hub.projectFormMatFilter, fieldW);
        panel.child(autocompleteBox);

        FlowLayout matNumRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        matNumRow.gap(8);
        matNumRow.verticalAlignment(VerticalAlignment.CENTER);
        matNumRow.child(UIComponents.label(Text.literal("Required:").withColor(COL_GRAY)));
        TextBoxComponent reqField = UIComponents.textBox(Sizing.fixed(52));
        reqField.setMaxLength(6); reqField.setText(hub.projectFormNewMatReq);
        reqField.onChanged().subscribe(s -> hub.projectFormNewMatReq = s);
        matNumRow.child(reqField);
        matNumRow.child(UIComponents.label(Text.literal("Have:").withColor(COL_GRAY)));
        TextBoxComponent curField = UIComponents.textBox(Sizing.fixed(52));
        curField.setMaxLength(6); curField.setText(hub.projectFormNewMatCur);
        curField.onChanged().subscribe(s -> hub.projectFormNewMatCur = s);
        matNumRow.child(curField);
        panel.child(matNumRow);

        panel.child(spacer(2));
        panel.child(styledBtn(70, 20, Text.literal("＋ Add").withColor(0xFF55FF88),
                0xFF0D280D, 0xFF174A17, () -> {
                    if (hub.projectFormNewMatName.isBlank()) return;
                    int req = Math.max(1, parseIntSafe(hub.projectFormNewMatReq, 1));
                    int cur = Math.max(0, parseIntSafe(hub.projectFormNewMatCur, 0));
                    hub.projectFormMaterials.add(new ProjectData.Material(
                            hub.projectFormNewMatName.trim(), req, cur));
                    hub.projectFormNewMatName = "";
                    hub.projectFormMatFilter  = "";
                    hub.projectFormNewMatReq  = "";
                    hub.projectFormNewMatCur  = "";
                    hub.fillContent();
                }));

        panel.child(spacer(8));
        panel.child(divider(hub.panelW));
        panel.child(spacer(4));
        FlowLayout btnRow = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        btnRow.gap(8);
        btnRow.child(styledBtn(110, 22, Text.literal("✔  Save").withColor(0xFF55FF55),
                0xFF153A15, 0xFF1F5A1F, this::submitForm));
        btnRow.child(styledBtn(90, 22, Text.literal("✗  Cancel").withColor(0xFFFF5555),
                0xFF3A1515, 0xFF5A1F1F,
                () -> { hub.projectFormOpen = false; hub.fillContent(); }));
        if (!isNew) {
            btnRow.child(styledBtn(86, 22, Text.literal("🗑 Delete").withColor(0xFFFF4444),
                    0xFF3A0A0A, 0xFF4A1010,
                    () -> {
                        sendAction(hub.projectFormId, "DELETE", "");
                        hub.projectFormOpen = false;
                        hub.fillContent();
                    }));
        }
        panel.child(btnRow);
        panel.child(spacer(16));
        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    private void rebuildAutocomplete(String filter, int innerW) {
        if (autocompleteBox == null) return;
        autocompleteBox.clearChildren();
        if (filter == null || filter.isBlank()) return;

        String lower = filter.toLowerCase();
        List<String> matches = Arrays.stream(ALL_MATERIALS)
                .filter(b -> b.contains(lower))
                .sorted((a, b) -> {
                    boolean as = a.startsWith(lower), bs = b.startsWith(lower);
                    if (as && !bs) return -1;
                    if (!as && bs) return 1;
                    return a.compareTo(b);
                })
                .limit(AUTOCOMPLETE_LIMIT)
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            autocompleteBox.child(UIComponents.label(
                    Text.literal("  No matches — custom name will be used.").withColor(0xFF444466)));
            return;
        }

        for (String match : matches) {
            FlowLayout row = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(16));
            row.surface(Surface.flat(0xFF141428));
            row.padding(Insets.of(1, 0, 1, 6));
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.mouseEnter().subscribe(() -> row.surface(Surface.flat(0xFF1E1E44)));
            row.mouseLeave().subscribe(() -> row.surface(Surface.flat(0xFF141428)));
            row.child(UIComponents.label(Text.literal(match).withColor(0xFF88AAFF)));
            row.mouseDown().subscribe((mx, my) -> {
                playTick();
                hub.projectFormNewMatName = match;
                hub.projectFormMatFilter  = match;
                MinecraftClient.getInstance().execute(hub::fillContent);
                return true;
            });
            autocompleteBox.child(row);
        }
    }

    private void openForm(ProjectData existing) {
        hub.projectFormOpen     = true;
        hub.projectFormMatFilter = "";
        if (existing == null) {
            hub.projectFormId         = null;
            hub.projectFormName       = "";
            hub.projectFormDesc       = "";
            hub.projectFormProgress   = 0;
            hub.projectFormCollab     = "SOLO";
            hub.projectFormHasLoc     = false;
            hub.projectFormLocXStr    = "0";
            hub.projectFormLocYStr    = "64";
            hub.projectFormLocZStr    = "0";
            hub.projectFormMaterials  = new ArrayList<>();
            hub.projectFormNewMatName = "";
            hub.projectFormNewMatReq  = "";
            hub.projectFormNewMatCur  = "";
        } else {
            hub.projectFormId         = existing.id;
            hub.projectFormName       = existing.name;
            hub.projectFormDesc       = existing.description;
            hub.projectFormProgress   = existing.progress;
            hub.projectFormCollab     = existing.collaboration;
            hub.projectFormHasLoc     = existing.hasLocation;
            hub.projectFormLocXStr    = String.valueOf(existing.locX);
            hub.projectFormLocYStr    = String.valueOf(existing.locY);
            hub.projectFormLocZStr    = String.valueOf(existing.locZ);
            hub.projectFormMaterials  = new ArrayList<>(existing.materials);
            hub.projectFormNewMatName = "";
            hub.projectFormNewMatReq  = "";
            hub.projectFormNewMatCur  = "";
        }
        hub.fillContent();
    }

    void buildCollabButtons(FlowLayout row) {
        row.clearChildren();
        String[] keys   = {"SOLO", "HELP_WANTED", "TEAM"};
        String[] labels = {"Solo", "Help Wanted", "Team"};
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final String lbl = labels[i];
            boolean sel = key.equals(hub.projectFormCollab);
            row.child(styledBtn(74, 18,
                    Text.literal(sel ? "▶ " + lbl : lbl).withColor(sel ? 0xFFDDDDFF : 0xFF888899),
                    sel ? 0xFF3A3A7A : 0xFF1E1E3A,
                    sel ? 0xFF3A3A7A : 0xFF2A2A55,
                    () -> {
                        hub.projectFormCollab = key;
                        if (hub.projectCollabGridRef != null) {
                            hub.projectCollabGridRef.clearChildren();
                            buildCollabButtons(hub.projectCollabGridRef);
                        }
                    }));
        }
    }

    void refreshMatsGrid(int innerW) {
        if (hub.projectMatsGridRef == null) return;
        hub.projectMatsGridRef.clearChildren();
        for (int idx = 0; idx < hub.projectFormMaterials.size(); idx++) {
            ProjectData.Material m = hub.projectFormMaterials.get(idx);
            final int i = idx;
            FlowLayout matRow = UIContainers.horizontalFlow(Sizing.fixed(innerW), Sizing.fixed(22));
            matRow.gap(6);
            matRow.verticalAlignment(VerticalAlignment.CENTER);
            matRow.surface(Surface.flat(0xFF111122));
            matRow.padding(Insets.of(2, 4, 2, 4));
            FlowLayout matNameGroup = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
            LabelComponent matLbl = UIComponents.label(
                    Text.literal("• " + m.name + "  " + m.current + " / " + m.required)
                            .withColor(0xFFAABBCC));
            matNameGroup.child(matLbl);
            matRow.child(matNameGroup);

            matRow.child(styledBtn(44, 18, Text.literal("✗ Del").withColor(0xFFFF6666),
                    0xFF2A1010, 0xFF3A1515,
                    () -> {
                        hub.projectFormMaterials.remove(i);
                        hub.fillContent();
                    }));
            hub.projectMatsGridRef.child(matRow);
        }
        if (hub.projectFormMaterials.isEmpty())
            hub.projectMatsGridRef.child(UIComponents.label(
                    Text.literal("  No materials added yet.").withColor(0xFF444466)));
    }

    private void submitForm() {
        int finalProgress = Math.max(0, Math.min(100, hub.projectFormProgress));
        long lx = parseLongSafe(hub.projectFormLocXStr, 0);
        long ly = parseLongSafe(hub.projectFormLocYStr, 64);
        long lz = parseLongSafe(hub.projectFormLocZStr, 0);
        try {
            ClientPlayNetworking.send(new SyncPacket.ProjectUpsertC2SPayload(
                    hub.projectFormId == null,
                    hub.projectFormId != null ? hub.projectFormId : "",
                    hub.projectFormName.isBlank() ? "Untitled Project" : hub.projectFormName,
                    hub.projectFormDesc,
                    finalProgress,
                    new ArrayList<>(hub.projectFormMaterials),
                    hub.projectFormHasLoc, lx, ly, lz,
                    hub.projectFormCollab
            ));
        } catch (Exception ignored) {}
        hub.projectFormOpen = false;
        hub.fillContent();
    }

    private void sendAction(String projectId, String action, String targetUuid) {
        try {
            ClientPlayNetworking.send(new SyncPacket.ProjectActionC2SPayload(
                    projectId, action, targetUuid));
        } catch (Exception ignored) {}
        hub.fillContent();
    }

    private static int    parseIntSafe(String s, int def)   { try { return Integer.parseInt(s.trim());  } catch (Exception e) { return def; } }
    private static long   parseLongSafe(String s, long def) { try { return Long.parseLong(s.trim());    } catch (Exception e) { return def; } }
    private static String formatNum(long v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000)     return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    static void spawnCompletionParticles() {
        HubUiHelper.spawnCompletionParticles();
    }
}