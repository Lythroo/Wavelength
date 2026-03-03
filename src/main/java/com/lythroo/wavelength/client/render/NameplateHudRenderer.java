package com.lythroo.wavelength.client.render;

import com.lythroo.wavelength.client.tracking.ActivityDetector;
import com.lythroo.wavelength.common.data.*;
import com.lythroo.wavelength.config.WavelengthConfig;
import com.lythroo.wavelength.mixin.GameRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public final class NameplateHudRenderer {

    private static final float VANILLA_WORLD_SCALE = 0.025f;

    private static final double MAX_RENDER_DIST        = 64.0;

    private static final double MAX_RENDER_DIST_SNEAK  = 32.0;

    private static final double FADE_RANGE             = 8.0;

    private static final float NEAR = 0.05f;
    private static final float FAR  = 100f;

    private static final int NAME_PAD_X = 6;
    private static final int NAME_PAD_Y = 3;

    private static final int   SUB_PAD_X  = 3;
    private static final int   SUB_PAD_Y  = 1;
    private static final float SUB_SCALE  = 0.80f;

    private static final int PANEL_GAP = 2;

    private static final int NAME_BG     = 0x55101018;
    private static final int NAME_BORDER = 0xFF223344;
    private static final int NAME_TEXT   = 0xFFFFFFFF;

    private static final int RANK_BG     = 0x55201800;
    private static final int RANK_BORDER = 0xFF886622;
    private static final int RANK_TEXT   = 0xFFDDBB55;

    private static final float FLOAT_AMP_PX = 1.4f;
    private static final long  FLOAT_PERIOD = 3600L;

    private NameplateHudRenderer() {}

    public static void register() {  }

    public static void render(DrawContext ctx, MinecraftClient client, RenderTickCounter tickCounter) {
        if (client.player == null || client.world == null) return;

        float tickDelta = tickCounter.getTickProgress(true);
        int   guiW      = ctx.getScaledWindowWidth();
        int   guiH      = ctx.getScaledWindowHeight();

        float baseFov = (float)(int) client.options.getFov().getValue();
        GameRendererAccessor gra = (GameRendererAccessor) client.gameRenderer;
        float fovMult = MathHelper.lerp(tickDelta, gra.getLastFovMultiplier(), gra.getFovMultiplier());
        float fovRad  = (float) Math.toRadians(baseFov * fovMult);
        float aspect  = (float) guiW / (float) guiH;

        Matrix4f projMat = new Matrix4f().perspective(fovRad, aspect, NEAR, FAR);
        Camera   camera  = client.gameRenderer.getCamera();
        Vec3d    camPos  = camera.getCameraPos();

        Quaternionf invRot = camera.getRotation().conjugate(new Quaternionf());
        Matrix4f    rotMat = new Matrix4f().rotate(invRot);

        Matrix4f bobMat = new Matrix4f();
        if (client.options.getBobView().getValue()) {
            MatrixStack scratchStack = new MatrixStack();
            gra.invokedBobView(scratchStack, tickDelta);
            bobMat = new Matrix4f(scratchStack.peek().getPositionMatrix());
        }
        Matrix4f viewProj = new Matrix4f(projMat).mul(bobMat).mul(rotMat);

        float tanHalfFov = (float) Math.tan(fovRad / 2.0);

        float floatOffset = AnimationHelper.bobOffset(FLOAT_AMP_PX, FLOAT_PERIOD);

        WavelengthConfig cfg = WavelengthConfig.get();
        TextRenderer     tr  = client.textRenderer;

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;

            if (player.isSneaking()) continue;

            PrivacyMode privacy = RemotePlayerCache.privacy(player.getUuid());

            double dist = Math.sqrt(player.squaredDistanceTo(client.player));

            double maxDist = MAX_RENDER_DIST;
            if (dist > maxDist) continue;

            float distAlpha = 1.0f;
            double fadeStart = maxDist - FADE_RANGE;
            if (dist > fadeStart) {
                distAlpha = 1.0f - (float) ((dist - fadeStart) / FADE_RANGE);
                distAlpha = MathHelper.clamp(distAlpha, 0f, 1f);
            }

            float privacyAlphaMul = (privacy == PrivacyMode.GHOST) ? 0.45f : 1.0f;
            float finalAlpha = distAlpha * privacyAlphaMul;

            Vec3d lerpedPos  = player.getLerpedPos(tickDelta);
            Vec3d labelWorld = new Vec3d(lerpedPos.x,
                    lerpedPos.y + player.getHeight() + 0.35, lerpedPos.z);
            Vec3d rel = labelWorld.subtract(camPos);

            Vector4f clip = new Vector4f((float) rel.x, (float) rel.y, (float) rel.z, 1f);
            viewProj.transform(clip);
            if (clip.w() <= 0.001f) continue;

            float ndcX = clip.x() / clip.w();
            float ndcY = clip.y() / clip.w();
            float sx   = (ndcX *  0.5f + 0.5f) * guiW;
            float sy   = (0.5f - ndcY *  0.5f) * guiH;

            if (sx < -400 || sx > guiW + 400 || sy < -200 || sy > guiH + 200) continue;

            float distScale = (VANILLA_WORLD_SCALE * guiH)
                    / (Math.max(0.5f, clip.w()) * 2f * tanHalfFov);
            distScale = MathHelper.clamp(distScale, 0f, 1.0f) * distAlpha;

            float finalSy = sy + floatOffset * distScale;

            ActivityType act = ActivityDetector.getActivity(player.getUuid());
            RemotePlayerCache.Entry entry = RemotePlayerCache.get(player.getUuid());
            String rankLine = (entry != null) ? entry.rankLine() : "";

            boolean hasActivity = cfg.showActivityTag
                    && !RemotePlayerCache.pvpMode(player.getUuid())
                    && act != ActivityType.IDLE && act != ActivityType.AFK;
            boolean hasRank = cfg.showRankUnderName
                    && rankLine != null && !rankLine.isBlank();

            Achievement badge = computeTopBadgeForPlayer(player.getUuid());

            String privacyIcon = privacyIcon(privacy);
            String baseName    = privacyIcon.isEmpty()
                    ? player.getName().getString()
                    : player.getName().getString() + " " + privacyIcon;

            String nameStr = decorateName(baseName, badge);
            String actStr  = hasActivity ? activityIcon(act) + " " + act.displayName : "";
            String rankStr = hasRank     ? "\u2605 " + rankLine : "";

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(sx, finalSy);
            ctx.getMatrices().scale(distScale, distScale);
            ctx.getMatrices().translate(-sx, -finalSy);

            drawNameplate(ctx, tr, (int) sx, (int) finalSy,
                    nameStr, actStr, rankStr,
                    act, hasActivity, hasRank, finalAlpha, privacy, badge);

            ctx.getMatrices().popMatrix();
        }
    }

    private static void drawNameplate(DrawContext ctx, TextRenderer tr,
                                      int cx, int cy,
                                      String nameStr, String actStr, String rankStr,
                                      ActivityType act,
                                      boolean hasActivity, boolean hasRank,
                                      float alpha, PrivacyMode privacy,
                                      Achievement badge) {

        int fontH = tr.fontHeight;

        int namePanelH = fontH + NAME_PAD_Y * 2;
        int nameW      = tr.getWidth(nameStr) + NAME_PAD_X * 2;
        int nameY      = cy - namePanelH;

        int subPanelH = fontH + SUB_PAD_Y * 2;
        int actW      = hasActivity ? tr.getWidth(actStr)  + SUB_PAD_X * 2 : 0;
        int rankW     = hasRank     ? tr.getWidth(rankStr) + SUB_PAD_X * 2 : 0;

        int rankCenterY = (int)((nameY - PANEL_GAP) - subPanelH / 2f * SUB_SCALE);
        int rankY       = rankCenterY - subPanelH / 2;

        int actCenterY = (int)((cy + PANEL_GAP) + subPanelH / 2f * SUB_SCALE);
        int actY       = actCenterY - subPanelH / 2;

        int nameBorder = computeNameBorder(alpha, privacy, badge);

        float rankPulse  = AnimationHelper.sine(1900L);
        int   rankBorder = blendColors(
                applyAlpha(RANK_BORDER, alpha),
                applyAlpha(0xFFFFEE88, alpha),
                rankPulse * 0.45f);

        int nameTextColor = applyAlpha(privacy.dotColor, alpha);

        if (hasActivity) {
            float actPulse  = AnimationHelper.sine(1400L);
            int   actBg     = tintBg(act.color, alpha * (0.85f + actPulse * 0.15f));
            int   actBorder = tintBorder(act.color, alpha * (0.80f + actPulse * 0.20f));
            int   actText   = applyAlpha(act.color, alpha * (0.90f + actPulse * 0.10f));
            drawScaledSubPanel(ctx, tr, cx, actY, actCenterY, actW, subPanelH,
                    actBg, actBorder, actStr, actText);
        }

        drawPanel(ctx, cx, nameY, nameW, namePanelH,
                applyAlpha(NAME_BG, alpha), nameBorder);
        drawCenteredText(ctx, tr, nameStr, cx, nameY + NAME_PAD_Y, nameTextColor);

        if (hasRank) {
            drawScaledSubPanel(ctx, tr, cx, rankY, rankCenterY, rankW, subPanelH,
                    applyAlpha(RANK_BG, alpha), rankBorder,
                    rankStr, applyAlpha(RANK_TEXT, alpha));
        }
    }

    private static String decorateName(String baseName, Achievement badge) {
        return switch (badge) {
            case DIAMOND -> "\u25C6 " + baseName + " \u25C6";
            case GOLD    -> "\u2736 " + baseName + " \u2736";
            default      -> baseName;
        };
    }

    private static int computeNameBorder(float alpha, PrivacyMode privacy, Achievement badge) {

        float ghostFade = (privacy == PrivacyMode.GHOST)
                ? Math.max(0f, (alpha - 0.3f) / 0.7f)
                : 1.0f;

        return switch (badge) {
            case DIAMOND -> {

                float t   = (System.currentTimeMillis() % 7000L) / 7000.0f;
                float hue = 0.48f + t * 0.14f;

                float sat = 0.55f * (0.35f + ghostFade * 0.65f);
                float val = 0.75f * (0.40f + ghostFade * 0.60f);

                float breathe = AnimationHelper.sine(3200L);
                val = val * (0.88f + breathe * 0.12f);
                yield hsvToArgb(hue, sat, val, alpha);
            }
            case GOLD -> {
                float pulse   = AnimationHelper.sine(900L);
                int   blended = blendColors(
                        applyAlpha(0xFFAA7700, alpha),
                        applyAlpha(0xFFFFDD44, alpha),
                        0.45f + pulse * 0.55f);
                yield desaturateToward(blended, 0.5f * (1f - ghostFade));
            }
            case SILVER -> {
                float pulse = AnimationHelper.sine(2200L);
                yield blendColors(
                        applyAlpha(NAME_BORDER, alpha),
                        applyAlpha(0xFFCCCCCC, alpha),
                        0.15f + pulse * 0.35f);
            }
            default -> {
                float namePulse = AnimationHelper.sine(2800L);
                yield blendColors(
                        applyAlpha(NAME_BORDER, alpha),
                        applyAlpha(privacy.dotColor, alpha * 0.55f),
                        namePulse * 0.35f);
            }
        };
    }

    private static Achievement computeTopBadgeForPlayer(java.util.UUID uuid) {

        RemoteStatCache.Entry live = RemoteStatCache.get(uuid);
        if (live != null) {
            Achievement best = Achievement.NONE;
            for (Achievement a : new Achievement[]{
                    Achievement.forCount(live.blocksMined),
                    Achievement.forCount(live.blocksPlaced),
                    Achievement.forCount(live.mobsKilled),
                    Achievement.forCount(live.cropsHarvested),
                    Achievement.forCount(live.itemsCrafted),
                    Achievement.forCount(live.distanceTraveled / 1000)
            }) { if (a.ordinal() > best.ordinal()) best = a; }
            return best;
        }

        PersistentPlayerCache.Entry pc = PersistentPlayerCache.get(uuid);
        if (pc != null) {
            Achievement best = Achievement.NONE;
            for (Achievement a : new Achievement[]{
                    Achievement.forCount(pc.blocksMined),
                    Achievement.forCount(pc.blocksPlaced),
                    Achievement.forCount(pc.mobsKilled),
                    Achievement.forCount(pc.cropsHarvested),
                    Achievement.forCount(pc.itemsCrafted),
                    Achievement.forCount(pc.distanceTraveled / 1000)
            }) { if (a.ordinal() > best.ordinal()) best = a; }
            return best;
        }
        return Achievement.NONE;
    }

    private static int hsvToArgb(float h, float s, float v, float a) {
        float hh = (h - (float) Math.floor(h)) * 6f;
        int   i  = (int) hh;
        float f  = hh - i;
        float p  = v * (1f - s);
        float q  = v * (1f - s * f);
        float t_ = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i) {
            case 0  -> { r = v; g = t_; b = p;  }
            case 1  -> { r = q; g = v;  b = p;  }
            case 2  -> { r = p; g = v;  b = t_; }
            case 3  -> { r = p; g = q;  b = v;  }
            case 4  -> { r = t_; g = p; b = v;  }
            default -> { r = v;  g = p; b = q;  }
        }
        return ((int)(a * 255) << 24)
                | ((int)(r * 255) << 16)
                | ((int)(g * 255) <<  8)
                |  (int)(b * 255);
    }

    private static void drawScaledSubPanel(DrawContext ctx, TextRenderer tr,
                                           int cx, int panelY, int centerY,
                                           int w, int h,
                                           int bgColor, int borderColor,
                                           String text, int textColor) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, centerY);
        ctx.getMatrices().scale(SUB_SCALE, SUB_SCALE);
        ctx.getMatrices().translate(-cx, -centerY);

        drawPanel(ctx, cx, panelY, w, h, bgColor, borderColor);
        drawCenteredText(ctx, tr, text, cx, panelY + SUB_PAD_Y, textColor);

        ctx.getMatrices().popMatrix();
    }

    private static void drawPanel(DrawContext ctx, int cx, int y, int w, int h,
                                  int bgColor, int borderColor) {
        if (w <= 0 || h <= 0) return;
        int x = cx - w / 2;
        fillCutCorner(ctx, x,     y,     w,     h,     borderColor);
        fillCutCorner(ctx, x + 1, y + 1, w - 2, h - 2, bgColor);
    }

    private static void fillCutCorner(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;

        ctx.fillGradient(x + 1, y,     x + w - 1, y + h,     color, color);
        ctx.fillGradient(x,     y + 1, x + w,     y + h - 1, color, color);
    }

    private static void drawCenteredText(DrawContext ctx, TextRenderer tr,
                                         String text, int cx, int textY, int color) {
        ctx.drawText(tr, text, cx - tr.getWidth(text) / 2, textY, color, true);
    }

    private static int tintBg(int argb, float alpha) {
        int r = (int)(((argb >> 16) & 0xFF) * 0.18f);
        int g = (int)(((argb >>  8) & 0xFF) * 0.18f);
        int b = (int)(( argb        & 0xFF) * 0.18f);
        return ((int)(0x55 * alpha) << 24) | (r << 16) | (g << 8) | b;
    }

    private static int tintBorder(int argb, float alpha) {
        int r = Math.min(255, (int)(((argb >> 16) & 0xFF) * 0.65f));
        int g = Math.min(255, (int)(((argb >>  8) & 0xFF) * 0.65f));
        int b = Math.min(255, (int)(( argb        & 0xFF) * 0.65f));
        return ((int)(0xFF * alpha) << 24) | (r << 16) | (g << 8) | b;
    }

    private static int applyAlpha(int argb, float alpha) {
        int origA = (argb >> 24) & 0xFF;
        int newA  = (int)(origA * alpha);
        return (argb & 0x00FFFFFF) | (newA << 24);
    }

    private static int desaturateToward(int argb, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        int luma = (int)(r * 0.299f + g * 0.587f + b * 0.114f);
        r = (int)(r + (luma - r) * amount);
        g = (int)(g + (luma - g) * amount);
        b = (int)(b + (luma - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blendColors(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa=(a>>24)&0xFF, ba=(b>>24)&0xFF;
        int ar=(a>>16)&0xFF, br=(b>>16)&0xFF;
        int ag=(a>> 8)&0xFF, bg_=(b>> 8)&0xFF;
        int ab= a     &0xFF, bb= b     &0xFF;
        return (((int)(aa+(ba-aa)*t))<<24)|(((int)(ar+(br-ar)*t))<<16)
                |(((int)(ag+(bg_-ag)*t))<<8) |((int)(ab+(bb-ab)*t));
    }

    private static String activityIcon(ActivityType act) {
        return switch (act) {
            case MINING    -> "\u26CF";
            case BUILDING  -> "\u2302";
            case FIGHTING  -> "\u2694";
            case FARMING   -> "\u2698";
            case EXPLORING -> "\u21D7";
            default        -> "\u25B8";
        };
    }

    private static String privacyIcon(PrivacyMode mode) {
        return switch (mode) {
            case GHOST   -> "\u25CB";
            case FRIENDS -> "\u2605";
            case PUBLIC  -> "";
        };
    }
}