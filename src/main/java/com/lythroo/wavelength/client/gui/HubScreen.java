package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.config.WavelengthConfig;
import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.common.data.AchievementTimeline;
import com.lythroo.wavelength.common.data.ProjectData;
import com.lythroo.wavelength.common.network.ServerModDetector;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.*;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

import static com.lythroo.wavelength.client.gui.HubUiHelper.*;

public class HubScreen extends BaseOwoScreen<FlowLayout> {

    enum Tab      { PLAYERS, STATS, LEADERBOARD, PROJECTS, EVENTS, FRIENDS, TOWN, TIMELINE }
    enum SortMode { DISTANCE, ACTIVITY, NAME }

    static int panelW = 310;

    int      contentH         = 200;
    Tab      activeTab        = Tab.PLAYERS;
    SortMode sortMode         = switch (WavelengthConfig.get().defaultSortMode) {
        case "ACTIVITY" -> SortMode.ACTIVITY;
        case "NAME"     -> SortMode.NAME;
        default         -> SortMode.DISTANCE;
    };

    String searchQuery       = "";
    UUID   selectedStatsUuid = null;
    String leaderboardCat    = "mining";

    String         pendingTownName    = "";
    Rank           pendingRank        = Rank.NONE;
    boolean        pendingInitialized = false;
    LabelComponent previewLabel       = null;
    FlowLayout     rankGridRef        = null;

    boolean            privacyDropdownOpen = false;
    FlowLayout         privacyControlsWrap = null;
    FlowLayout         playersGridRef      = null;
    ScrollContainer<?> playersScrollRef    = null;
    double             playersScrollY      = 0;

    boolean eventFormOpen         = false;
    String  eventFormId           = "";
    String  eventFormName         = "";
    String  eventFormDesc         = "";
    boolean eventFormHasLoc       = false;
    long    eventFormLocX = 0, eventFormLocY = 64, eventFormLocZ = 0;
    String  eventFormEndTime      = "";
    String  eventFormSchedStart   = "";

    boolean    projectFormOpen       = false;
    String     projectFormId         = null;
    String     projectFormName       = "";
    String     projectFormDesc       = "";
    int        projectFormProgress   = 0;
    String     projectFormCollab     = "SOLO";
    boolean    projectFormHasLoc     = false;
    String     projectFormLocXStr    = "0";
    String     projectFormLocYStr    = "64";
    String     projectFormLocZStr    = "0";
    java.util.List<ProjectData.Material> projectFormMaterials = new ArrayList<>();
    String     projectFormNewMatName   = "";
    String     projectFormNewMatReq    = "";
    String     projectFormNewMatCur    = "";

    String     projectFormMatFilter    = "";

    LabelComponent projectProgressLabel  = null;
    FlowLayout     projectCollabGridRef  = null;
    FlowLayout     projectMatsGridRef    = null;

    private final HubPlayersTab     playersTab     = new HubPlayersTab(this);
    private final HubStatsTab       statsTab       = new HubStatsTab(this);
    private final HubLeaderboardTab leaderboardTab = new HubLeaderboardTab(this);
    private final HubProjectsTab    projectsTab    = new HubProjectsTab(this);
    private final HubEventsTab      eventsTab      = new HubEventsTab(this);
    private final HubFriendsTab     friendsTab     = new HubFriendsTab(this);
    private final HubTownTab        townTab        = new HubTownTab(this);
    private final HubTimelineTab    timelineTab    = new HubTimelineTab(this);

    private FlowLayout tabBarContainer;
    private FlowLayout contentArea;

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        HudManager.setSuppressed(true);
        root.surface(Surface.flat(COL_BG));
        root.horizontalAlignment(HorizontalAlignment.CENTER);
        root.verticalAlignment(VerticalAlignment.TOP);
        root.sizing(Sizing.fill(100), Sizing.fill(100));
        root.padding(Insets.of(0));
        root.gap(0);

        buildHeader(root);

        boolean isSingleplayer = MinecraftClient.getInstance().getServer() != null;
        if (isSingleplayer) {
            root.child(warningBanner(
                    "⚠  Singleplayer — multiplayer features require the Wavelength server mod",
                    0xFF1A1000, 0xFFFFAA44));
        } else if (!ServerModDetector.isPresent()) {
            root.child(warningBanner(
                    "⚠  Wavelength server mod not detected — multiplayer features disabled",
                    0xFF3A2000, 0xFFFFAA44));
        }

        tabBarContainer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(22));
        root.child(tabBarContainer);
        fillTabBar();

        panelW   = Math.min(310, this.width - 30);
        int warningH = (isSingleplayer || !ServerModDetector.isPresent()) ? 18 : 0;
        contentH = Math.max(50, this.height - 26 - 22 - warningH);
        contentArea = UIContainers.verticalFlow(Sizing.fill(100), Sizing.fixed(contentH));
        root.child(contentArea);
        fillContent();
    }

    private void buildHeader(FlowLayout root) {

        String modVersion = "";
        try {
            modVersion = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer(com.lythroo.wavelength.Wavelength.MOD_ID)
                    .map(cv -> cv.getMetadata().getVersion().getFriendlyString())
                    .orElse("");
        } catch (Exception ignored) {}

        String titleStr  = "✦ Wavelength" + (modVersion.isBlank() ? "" : " v" + modVersion);
        String creditStr = "Mod by Lythroo_";

        net.minecraft.client.font.TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int creditPx = tr.getWidth(creditStr);

        int innerW      = this.width - 28;
        int rightWrapW  = creditPx + 8 + 20;
        int titleWrapW  = Math.max(40, innerW - 10 - rightWrapW);

        FlowLayout header = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(26));
        header.surface(Surface.flat(0xEE080815));
        header.verticalAlignment(VerticalAlignment.CENTER);
        header.padding(Insets.horizontal(14));
        header.gap(10);

        FlowLayout titleWrap = UIContainers.horizontalFlow(
                Sizing.fixed(titleWrapW), Sizing.fixed(20));
        titleWrap.verticalAlignment(VerticalAlignment.CENTER);
        LabelComponent title = UIComponents.label(
                Text.literal(titleStr).withColor(0xFFBBAAFF));
        title.shadow(true);
        titleWrap.child(title);
        header.child(titleWrap);

        FlowLayout rightWrap = UIContainers.horizontalFlow(
                Sizing.fixed(rightWrapW), Sizing.fixed(20));
        rightWrap.gap(0);
        rightWrap.verticalAlignment(VerticalAlignment.CENTER);

        LabelComponent credit = UIComponents.label(
                Text.literal(creditStr).withColor(0xFF7755AA));
        credit.shadow(false);
        rightWrap.child(credit);

        rightWrap.child(UIComponents.label(Text.literal("")).sizing(Sizing.fixed(8), Sizing.fixed(1)));
        rightWrap.child(styledBtn(20, 20, Text.literal("✕").withColor(0xFFFF6666),
                0xFF2A0A0A, 0xFF4A1515, this::close));
        header.child(rightWrap);

        root.child(header);
    }

    private static FlowLayout warningBanner(String text, int bg, int color) {
        FlowLayout warn = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(18));
        warn.surface(Surface.flat(bg));
        warn.verticalAlignment(VerticalAlignment.CENTER);
        warn.horizontalAlignment(HorizontalAlignment.CENTER);
        warn.gap(6);
        LabelComponent lbl = UIComponents.label(Text.literal(text).withColor(color));
        lbl.shadow(true);
        warn.child(lbl);
        return warn;
    }

    void fillTabBar() {
        tabBarContainer.clearChildren();
        tabBarContainer.surface(Surface.flat(0xCC0E0E22));
        tabBarContainer.verticalAlignment(VerticalAlignment.CENTER);
        tabBarContainer.padding(Insets.horizontal(0));
        tabBarContainer.gap(0);

        boolean isSingleplayer = MinecraftClient.getInstance().getServer() != null;
        boolean serverPresent  = ServerModDetector.isPresent();
        Tab[] visibleTabs = (serverPresent || isSingleplayer)
                ? Tab.values()
                : new Tab[]{Tab.PLAYERS, Tab.STATS, Tab.LEADERBOARD, Tab.EVENTS, Tab.TIMELINE};

        boolean activeVisible = false;
        for (Tab t : visibleTabs) if (t == activeTab) { activeVisible = true; break; }
        if (!activeVisible) activeTab = Tab.PLAYERS;

        final int TAB_PAD_X = 10;
        final int TAB_H     = 18;
        final int TAB_GAP   = 3;
        net.minecraft.client.font.TextRenderer tr =
                MinecraftClient.getInstance().textRenderer;

        int[] tabWidths = new int[visibleTabs.length];
        int totalW = TAB_GAP;
        for (int i = 0; i < visibleTabs.length; i++) {
            String lbl = "● " + tabLabel(visibleTabs[i]);
            tabWidths[i] = tr.getWidth(lbl) + TAB_PAD_X * 2;
            totalW += tabWidths[i] + TAB_GAP;
        }

        int availableW = this.width - 0;

        boolean needsScroll = totalW > availableW;
        if (!needsScroll) {

            int extra = (availableW - totalW) / visibleTabs.length;
            for (int i = 0; i < tabWidths.length; i++) tabWidths[i] += extra;
        }

        FlowLayout innerRow = UIContainers.horizontalFlow(
                needsScroll ? Sizing.content() : Sizing.fill(100),
                Sizing.fixed(TAB_H + 4));
        innerRow.verticalAlignment(VerticalAlignment.CENTER);
        innerRow.gap(TAB_GAP);
        innerRow.padding(Insets.horizontal(TAB_GAP));

        FlowLayout activeWrapper = null;
        for (int i = 0; i < visibleTabs.length; i++) {
            Tab tab = visibleTabs[i];
            int tw = tabWidths[i];
            boolean selected = (tab == activeTab);
            int pending  = (tab == Tab.FRIENDS) ? FriendRequests.incomingCount() : 0;
            int normalBg = selected ? COL_TAB_ACTIVE : (pending > 0 ? 0xFF552222 : COL_TAB_IDLE);
            int hoverBg  = selected ? COL_TAB_ACTIVE : (pending > 0 ? 0xFF773333 : 0xFF383870);
            int txtColor = selected ? 0xFFEEEEFF  : (pending > 0 ? 0xFFFFAA88 : 0xFFAAAAAA);

            Text labelText = Text.literal(selected ? "● " + tabLabel(tab) : tabLabel(tab)).withColor(txtColor);
            FlowLayout wrapper = UIContainers.horizontalFlow(Sizing.fixed(tw), Sizing.fixed(TAB_H));
            wrapper.surface(Surface.flat(normalBg));
            wrapper.horizontalAlignment(HorizontalAlignment.CENTER);
            wrapper.verticalAlignment(VerticalAlignment.CENTER);
            LabelComponent tabLbl = UIComponents.label(labelText);
            tabLbl.shadow(true);
            wrapper.child(tabLbl);

            if (!selected) {
                wrapper.mouseEnter().subscribe(() -> wrapper.surface(Surface.flat(hoverBg)));
                wrapper.mouseLeave().subscribe(() -> wrapper.surface(Surface.flat(normalBg)));
            } else {
                activeWrapper = wrapper;
            }
            Tab tabRef = tab;
            wrapper.mouseDown().subscribe((x, y) -> {
                playTabClick();
                MinecraftClient.getInstance().execute(() -> {
                    activeTab = tabRef; fillTabBar(); fillContent();
                });
                return true;
            });
            innerRow.child(wrapper);
        }

        if (needsScroll) {

            TabScrollContainer scroller = new TabScrollContainer(
                    Sizing.fill(100), Sizing.fixed(TAB_H + 4), innerRow);
            tabBarContainer.child(scroller);

            final FlowLayout scrollTarget = activeWrapper;
            if (scrollTarget != null) {
                MinecraftClient.getInstance().execute(() -> scroller.snapTo(scrollTarget));
            }
        } else {
            tabBarContainer.child(innerRow);
        }
    }

    void fillContent() {
        panelW = Math.min(310, this.width - 30);
        if (activeTab != Tab.PLAYERS) playersScrollY = 0;
        contentArea.clearChildren();
        contentArea.child(switch (activeTab) {
            case PLAYERS     -> playersTab.build();
            case STATS       -> statsTab.build();
            case LEADERBOARD -> leaderboardTab.build();
            case PROJECTS    -> projectsTab.build();
            case EVENTS      -> eventsTab.build();
            case FRIENDS     -> friendsTab.build();
            case TOWN        -> townTab.build();
            case TIMELINE    -> timelineTab.build();
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        if (activeTab == Tab.PLAYERS) playersScrollY = Math.max(0, playersScrollY - v * 12);
        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    private static String tabLabel(Tab t) {
        return switch (t) {
            case PROJECTS     -> "Projects";
            case EVENTS       -> "Events";
            case PLAYERS     -> "Players";
            case STATS       -> "Stats";
            case LEADERBOARD -> "Leaderboard";
            case FRIENDS -> {
                int n = FriendRequests.incomingCount();
                yield n > 0 ? "Friends [" + n + "]" : "Friends";
            }
            case TOWN     -> "Town";
            case TIMELINE -> {
                int n = AchievementTimeline.getEvents().size();
                yield n > 0 ? "Timeline [" + n + "]" : "Timeline";
            }
        };
    }

    @Override public void close()          { HudManager.setSuppressed(false); super.close(); }
    @Override public boolean shouldPause() { return false; }

    private static final class TabScrollContainer extends ScrollContainer<FlowLayout> {
        TabScrollContainer(Sizing horizontal, Sizing vertical, FlowLayout child) {
            super(ScrollDirection.HORIZONTAL, horizontal, vertical, child);
        }

        void snapTo(UIComponent component) {
            scrollTo(component);
            currentScrollPosition = scrollOffset;
        }
    }

    public static void refreshIfOpen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof HubScreen hub) {
            hub.fillTabBar();
            if (hub.activeTab == Tab.FRIENDS
                    || hub.activeTab == Tab.PROJECTS
                    || hub.activeTab == Tab.EVENTS) {
                hub.fillContent();
            }
        }
    }
}