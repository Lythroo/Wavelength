package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.common.data.AchievementTimeline;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;

final class HubTimelineTab {

    private final HubScreen hub;

    HubTimelineTab(HubScreen hub) { this.hub = hub; }

    UIComponent build() {
        int innerW = Math.min(hub.panelW, hub.width - 40);
        FlowLayout panel = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        panel.padding(Insets.of(14));
        panel.gap(4);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);

        List<AchievementTimeline.ProjectTimelineEvent> projEvents = AchievementTimeline.getProjectEvents();
        panel.child(sectionHeader("🏗 PROJECT & EVENT ACTIVITY"));
        panel.child(divider(hub.panelW));
        if (projEvents.isEmpty()) {
            panel.child(spacer(4));
            panel.child(UIComponents.label(
                    Text.literal("No project activity this session yet.").withColor(0xFF444466)));
        } else {
            for (AchievementTimeline.ProjectTimelineEvent event : projEvents) {
                panel.child(buildProjectEntry(event, innerW));
            }
        }

        panel.child(spacer(12));

        panel.child(sectionHeader("\u231B RECENT ACHIEVEMENTS"));
        panel.child(divider(hub.panelW));

        List<AchievementTimeline.TimelineEvent> events = AchievementTimeline.getEvents();
        if (events.isEmpty()) {
            panel.child(spacer(8));
            panel.child(UIComponents.label(
                    Text.literal("No achievements unlocked this session yet.").withColor(0xFF444466)));
            panel.child(spacer(4));
            panel.child(UIComponents.label(
                    Text.literal("Badge unlocks across the server will appear here.").withColor(0xFF333355)));
        } else {
            for (AchievementTimeline.TimelineEvent event : events) {
                panel.child(buildEntry(event, innerW));
            }
        }

        panel.child(spacer(16));
        return UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(hub.contentH), panel);
    }

    private FlowLayout buildProjectEntry(AchievementTimeline.ProjectTimelineEvent event, int innerW) {
        int accentColor = event.accentColor();
        int cardBg      = event.cardBg();

        FlowLayout card = UIContainers.verticalFlow(Sizing.fixed(innerW), Sizing.content());
        card.surface(Surface.flat(cardBg));
        card.padding(Insets.of(6, 6, 6, 8));
        card.gap(2);
        card.margins(Insets.vertical(2));

        FlowLayout accentRow = UIContainers.horizontalFlow(Sizing.fixed(innerW - 14), Sizing.content());
        accentRow.gap(8);
        accentRow.verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout accentBar = UIContainers.verticalFlow(Sizing.fixed(3), Sizing.fixed(24));
        accentBar.surface(Surface.flat(accentColor));
        accentRow.child(accentBar);

        FlowLayout textCol = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        textCol.gap(2);
        textCol.child(UIComponents.label(Text.literal(event.timeAgo()).withColor(0xFF444466)));
        LabelComponent descLbl = UIComponents.label(Text.literal(event.description()).withColor(accentColor));
        descLbl.shadow(true);
        textCol.child(descLbl);
        accentRow.child(textCol);
        card.child(accentRow);

        int hoverBg = (cardBg & 0x00FFFFFF) | 0xDD000000;
        card.mouseEnter().subscribe(() -> card.surface(Surface.flat(hoverBg)));
        card.mouseLeave().subscribe(() -> card.surface(Surface.flat(cardBg)));

        HubScreen.Tab clickTarget = event.action().startsWith("EVENT_")
                ? HubScreen.Tab.EVENTS
                : HubScreen.Tab.PROJECTS;
        card.mouseDown().subscribe((mx, my) -> {
            long win = MinecraftClient.getInstance().getWindow().getHandle();
            if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(win, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) != org.lwjgl.glfw.GLFW.GLFW_PRESS) return false;
            playClick();
            MinecraftClient.getInstance().execute(() -> {
                hub.activeTab = clickTarget;
                hub.fillTabBar();
                hub.fillContent();
            });
            return true;
        });

        return card;
    }

    private FlowLayout buildEntry(AchievementTimeline.TimelineEvent event, int innerW) {
        int cardBg = switch (event.badge()) {
            case DIAMOND -> 0xCC060E12; case GOLD   -> 0xCC130F00;
            case SILVER  -> 0xCC0D0D0D; case BRONZE -> 0xCC100A00;
            default -> 0xCC0D0D1A;
        };
        int accentColor = switch (event.badge()) {
            case DIAMOND -> 0xFF00DDEE; case GOLD   -> 0xFFCC9900;
            case SILVER  -> 0xFF999999; case BRONZE -> 0xFF996633;
            default -> 0xFF444466;
        };

        FlowLayout card = UIContainers.verticalFlow(Sizing.fixed(innerW), Sizing.content());
        card.surface(Surface.flat(cardBg));
        card.padding(Insets.of(6, 6, 6, 8));
        card.gap(2);
        card.margins(Insets.vertical(3));

        FlowLayout accentRow = UIContainers.horizontalFlow(Sizing.fixed(innerW - 14), Sizing.content());
        accentRow.gap(8);
        accentRow.verticalAlignment(VerticalAlignment.CENTER);

        FlowLayout accentBar = UIContainers.verticalFlow(Sizing.fixed(3), Sizing.fixed(28));
        accentBar.surface(Surface.flat(accentColor));
        accentRow.child(accentBar);

        FlowLayout textCol = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        textCol.gap(2);
        textCol.child(UIComponents.label(Text.literal(event.timeAgo()).withColor(0xFF444466)));
        LabelComponent descLbl = UIComponents.label(Text.literal(event.description()).withColor(accentColor));
        descLbl.shadow(true);
        textCol.child(descLbl);
        accentRow.child(textCol);
        card.child(accentRow);

        int hoverBg = (cardBg & 0x00FFFFFF) | 0xDD000000;
        card.mouseEnter().subscribe(() -> card.surface(Surface.flat(hoverBg)));
        card.mouseLeave().subscribe(() -> card.surface(Surface.flat(cardBg)));

        final UUID targetUuid = event.playerUuid();
        card.mouseDown().subscribe((mx, my) -> {
            long win = MinecraftClient.getInstance().getWindow().getHandle();
            if (org.lwjgl.glfw.GLFW.glfwGetMouseButton(win, org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) != org.lwjgl.glfw.GLFW.GLFW_PRESS) return false;
            playClick();
            MinecraftClient.getInstance().execute(() -> {
                hub.selectedStatsUuid = targetUuid;
                hub.activeTab = HubScreen.Tab.STATS;
                hub.fillTabBar();
                hub.fillContent();
            });
            return true;
        });

        return card;
    }
}