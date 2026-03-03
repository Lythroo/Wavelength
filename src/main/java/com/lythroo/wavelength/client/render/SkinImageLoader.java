package com.lythroo.wavelength.client.render;

import com.lythroo.wavelength.Wavelength;
import com.lythroo.wavelength.common.data.ActivityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SkinImageLoader {

    private static final String API_BASE =
            "https://starlightskins.lunareclipse.studio/skin-render/";

    private static final Map<String, Identifier> ready = new ConcurrentHashMap<>();

    private static final Set<String> pending = ConcurrentHashMap.newKeySet();

    public static final Identifier LOADING_PLACEHOLDER =
            Identifier.of(Wavelength.MOD_ID, "textures/gui/skin_loading.png");

    public static Identifier getOrLoad(String username, ActivityType activity) {
        String key = username.toLowerCase();
        Identifier cached = ready.get(key);
        if (cached != null) return cached;

        if (pending.add(key)) {
            String pose = poseFor(activity);
            String urlStr = API_BASE + pose + "/" + username + "/full";
            CompletableFuture.runAsync(() -> download(key, username, urlStr));
        }
        return LOADING_PLACEHOLDER;
    }

    public static void reload(String username) {
        evict(username);
    }

    public static void evict(String username) {
        String key = username.toLowerCase();
        Identifier id = ready.remove(key);
        if (id != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
        }
        pending.remove(key);
    }

    public static void evictAll() {
        for (String key : ready.keySet()) evict(key);
    }

    private static void download(String cacheKey, String username, String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("User-Agent", "Wavelength-Mod/1.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                Wavelength.LOGGER.warn("[Wavelength] Starlight API returned {} for {}", status, urlStr);
                pending.remove(cacheKey);
                return;
            }

            NativeImage img;
            try (InputStream in = conn.getInputStream()) {
                img = NativeImage.read(in);
            }

            Identifier texId = Identifier.of(Wavelength.MOD_ID, "skin_render/" + cacheKey);

            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "wavelength_skin_" + cacheKey, img);
                client.getTextureManager().registerTexture(texId, tex);
                ready.put(cacheKey, texId);
                pending.remove(cacheKey);
            });

        } catch (Exception e) {
            Wavelength.LOGGER.warn("[Wavelength] Failed to load skin for {}: {}", username, e.getMessage());
            pending.remove(cacheKey);
        }
    }

    public static String poseFor(ActivityType activity) {
        return switch (activity) {
            case EXPLORING                      -> "walking";
            case FIGHTING                       -> "marching";
            case MINING, BUILDING, FARMING      -> "crouching";
            default                             -> "default";
        };
    }
}