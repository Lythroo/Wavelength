package com.lythroo.wavelength.client.gui;

import com.lythroo.wavelength.config.WavelengthConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends Screen {

    private static final int ID_NONE    = 0;
    private static final int ID_PLAYERS = 1;
    private static final int ID_IDENTITY= 2;
    private static final int ID_EVENTS  = 3;

    private static final int SNAP_DIST  = 10;

    private int     dragging   = ID_NONE;
    private boolean wasDown    = false;
    private double  dragStartMX, dragStartMY;
    private int     dragStartEX, dragStartEY;

    private boolean snapH, snapV;

    private static final int BTN_W    = 100;
    private static final int BTN_H    = 18;
    private static final int BTN_GAP  = 6;
    private static final int INFL     = 4;

    private static final int COL_IDLE  = 0x88AAAAFF;
    private static final int COL_HOV   = 0xCCDDDDFF;
    private static final int COL_DRAG  = 0xFFFFFF44;
    private static final int COL_GUIDE = 0x55FFFFFF;
    private static final int COL_SNAP  = 0x99FFFF44;
    private static final int COL_LABEL = 0xFFCCCCFF;
    private static final int COL_HINT  = 0xFF667788;

    public HudEditorScreen() { super(Text.literal("HUD Editor")); }

    @Override public boolean shouldPause() { return false; }

    @Override protected void init() { HudManager.setSuppressed(false); }

    @Override
    public void close() {
        WavelengthConfig.get().save();
        HudManager.setSuppressed(false);
        super.close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        WavelengthConfig cfg   = WavelengthConfig.get();

        long win   = client.getWindow().getHandle();
        boolean dn = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        double[] rx = new double[1], ry = new double[1];
        GLFW.glfwGetCursorPos(win, rx, ry);
        double scale = client.getWindow().getScaleFactor();
        int mx = (int)(rx[0] / scale);
        int my = (int)(ry[0] / scale);

        int cx = width / 2;
        int cy = height / 2;

        if (dn && !wasDown) {

            int[] btnXs = resetBtnXs();
            String[] labels = resetLabels();
            for (int i = 0; i < 3; i++) {
                if (over(mx, my, btnXs[i], height - 40, BTN_W, BTN_H)) {
                    applyReset(cfg, i);
                    dragging = ID_NONE;
                    wasDown  = true;
                    super.render(ctx, mouseX, mouseY, delta);
                    return;
                }
            }

            if (PlayersHud.lastW > 0 && over(mx, my, PlayersHud.lastX, PlayersHud.lastY,
                    PlayersHud.lastW, PlayersHud.lastH)) {
                dragging = ID_PLAYERS;
                dragStartMX = mx; dragStartMY = my;
                dragStartEX = PlayersHud.lastX; dragStartEY = PlayersHud.lastY;
            } else if (TopBarHud.lastW > 0 && over(mx, my, TopBarHud.lastX, TopBarHud.lastY,
                    TopBarHud.lastW, TopBarHud.lastH)) {
                dragging = ID_IDENTITY;
                dragStartMX = mx; dragStartMY = my;
                dragStartEX = TopBarHud.lastX; dragStartEY = TopBarHud.lastY;
            } else if (EventsHud.lastW > 0 && over(mx, my, EventsHud.lastX, EventsHud.lastY,
                    EventsHud.lastW, EventsHud.lastH)) {
                dragging = ID_EVENTS;
                dragStartMX = mx; dragStartMY = my;
                dragStartEX = EventsHud.lastX; dragStartEY = EventsHud.lastY;
            }
        }

        snapH = false; snapV = false;
        if (dn && dragging != ID_NONE) {
            int dx = (int)(mx - dragStartMX);
            int dy = (int)(my - dragStartMY);
            int newEX = dragStartEX + dx;
            int newEY = dragStartEY + dy;

            if (dragging == ID_PLAYERS) {
                int defX = (width - PlayersHud.lastW) / 2;
                int defY = 4;
                int rawOffX = newEX - defX;
                int rawOffY = newEY - defY;

                int elemCX = newEX + PlayersHud.lastW / 2;
                int elemCY = newEY + PlayersHud.lastH / 2;
                if (Math.abs(elemCX - cx) <= SNAP_DIST) { rawOffX = 0; snapV = true; }
                if (Math.abs(elemCY - cy) <= SNAP_DIST) { rawOffY = cy - defY - PlayersHud.lastH / 2; snapH = true; }
                cfg.playersHudOffsetX = rawOffX;
                cfg.playersHudOffsetY = rawOffY;
            } else if (dragging == ID_IDENTITY) {
                int defX = (width - TopBarHud.lastW) / 2;
                int defY = 4 + 13 + 2;
                int rawOffX = newEX - defX;
                int rawOffY = newEY - defY;
                int elemCX = newEX + TopBarHud.lastW / 2;
                int elemCY = newEY + TopBarHud.lastH / 2;
                if (Math.abs(elemCX - cx) <= SNAP_DIST) { rawOffX = 0; snapV = true; }
                if (Math.abs(elemCY - cy) <= SNAP_DIST) { rawOffY = cy - defY - TopBarHud.lastH / 2; snapH = true; }
                cfg.topBarOffsetX = rawOffX;
                cfg.topBarOffsetY = rawOffY;
            } else if (dragging == ID_EVENTS) {
                int defX = width - EventsHud.lastW - 6;
                int defY = 4;
                int rawOffX = newEX - defX;
                int rawOffY = newEY - defY;
                int elemCX = newEX + EventsHud.lastW / 2;
                int elemCY = newEY + EventsHud.lastH / 2;
                if (Math.abs(elemCX - cx) <= SNAP_DIST) { rawOffX = cx - defX - EventsHud.lastW / 2; snapV = true; }
                if (Math.abs(elemCY - cy) <= SNAP_DIST) { rawOffY = cy - defY - EventsHud.lastH / 2; snapH = true; }
                cfg.eventsHudOffsetX = rawOffX;
                cfg.eventsHudOffsetY = rawOffY;
            }
        }

        if (!dn) dragging = ID_NONE;
        wasDown = dn;

        ctx.fill(0, 0, width, height, 0x99000000);

        if (client.player != null) {
            PlayersHud.render(ctx, client);
            TopBarHud.render(ctx, client);
            EventsHud.render(ctx, client);
        }

        int guideColV = snapV ? COL_SNAP : COL_GUIDE;
        ctx.fill(cx, 0, cx + 1, height, guideColV);

        int guideColH = snapH ? COL_SNAP : COL_GUIDE;
        ctx.fill(0, cy, width, cy + 1, guideColH);

        ctx.drawTextWithShadow(textRenderer, "CENTER", cx + 3, cy - textRenderer.fontHeight - 2, COL_GUIDE);

        drawOutline(ctx, mx, my, PlayersHud.lastX, PlayersHud.lastY,
                PlayersHud.lastW, PlayersHud.lastH, ID_PLAYERS, "Players");
        drawOutline(ctx, mx, my, TopBarHud.lastX, TopBarHud.lastY,
                TopBarHud.lastW, TopBarHud.lastH, ID_IDENTITY, "Identity");
        drawOutline(ctx, mx, my, EventsHud.lastX, EventsHud.lastY,
                EventsHud.lastW, EventsHud.lastH, ID_EVENTS, "Events");

        int[] btnXs = resetBtnXs();
        String[] labels = resetLabels();
        for (int i = 0; i < 3; i++) {
            drawBtn(ctx, btnXs[i], height - 40, labels[i],
                    over(mx, my, btnXs[i], height - 40, BTN_W, BTN_H));
        }

        String hint = "Drag to move  •  Snaps to center  •  Esc to save & close";
        int hw = textRenderer.getWidth(hint);
        ctx.drawTextWithShadow(textRenderer, hint, (width - hw) / 2, height - 18, COL_HINT);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawOutline(DrawContext ctx, int mx, int my,
                             int ex, int ey, int ew, int eh,
                             int id, String label) {
        if (ew <= 0 || eh <= 0) return;
        boolean hov = dragging == ID_NONE && over(mx, my, ex, ey, ew, eh);
        int col = dragging == id ? COL_DRAG : hov ? COL_HOV : COL_IDLE;
        int x1 = ex - INFL, y1 = ey - INFL, x2 = ex + ew + INFL, y2 = ey + eh + INFL;
        ctx.fill(x1, y1, x2, y1 + 1, col);
        ctx.fill(x1, y2 - 1, x2, y2, col);
        ctx.fill(x1, y1, x1 + 1, y2, col);
        ctx.fill(x2 - 1, y1, x2, y2, col);
        int lw = textRenderer.getWidth(label);
        ctx.drawTextWithShadow(textRenderer, label,
                ex + (ew - lw) / 2, y1 - textRenderer.fontHeight - 2, col);
    }

    private void drawBtn(DrawContext ctx, int bx, int by, String label, boolean hov) {
        ctx.fill(bx, by, bx + BTN_W, by + BTN_H, hov ? 0xFF2A2A55 : 0xFF181830);
        ctx.fill(bx,           by,           bx + BTN_W, by + 1,        COL_IDLE);
        ctx.fill(bx,           by + BTN_H - 1, bx + BTN_W, by + BTN_H, COL_IDLE);
        ctx.fill(bx,           by,           bx + 1,     by + BTN_H,   COL_IDLE);
        ctx.fill(bx + BTN_W - 1, by,         bx + BTN_W, by + BTN_H,   COL_IDLE);
        int lw = textRenderer.getWidth(label);
        ctx.drawTextWithShadow(textRenderer, label,
                bx + (BTN_W - lw) / 2, by + (BTN_H - textRenderer.fontHeight) / 2, COL_LABEL);
    }

    private int[] resetBtnXs() {
        int totalW = BTN_W * 3 + BTN_GAP * 2;
        int startX = (width - totalW) / 2;
        return new int[]{ startX, startX + BTN_W + BTN_GAP, startX + (BTN_W + BTN_GAP) * 2 };
    }

    private String[] resetLabels() {
        return new String[]{ "Reset Players", "Reset Identity", "Reset Events" };
    }

    private void applyReset(WavelengthConfig cfg, int index) {
        switch (index) {
            case 0 -> { cfg.playersHudOffsetX = 0; cfg.playersHudOffsetY = 0; }
            case 1 -> { cfg.topBarOffsetX     = 0; cfg.topBarOffsetY     = 0; }
            case 2 -> { cfg.eventsHudOffsetX  = 0; cfg.eventsHudOffsetY  = 0; }
        }
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}