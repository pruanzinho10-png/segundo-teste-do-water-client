package dev.waterclient.android;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Cliente mínimo da API pública de busca do Modrinth; nunca baixa mods automaticamente. */
final class ModrinthApi {
    private static final long CACHE_MILLIS = 15 * 60 * 1000L;
    private static final Map<String, CacheEntry> CACHE = new HashMap<>();

    private static final class CacheEntry {
        final long savedAt;
        final List<Project> projects;
        CacheEntry(List<Project> projects) { this.savedAt = System.currentTimeMillis(); this.projects = projects; }
    }

    static final class Project {
        final String title;
        final String description;
        final String slug;
        final long downloads;

        Project(String title, String description, String slug, long downloads) {
            this.title = title;
            this.description = description;
            this.slug = slug;
            this.downloads = downloads;
        }
    }

    private ModrinthApi() { }

    static List<Project> searchMods(String query) throws Exception {
        String cacheKey = query.trim().toLowerCase(Locale.ROOT);
        synchronized (CACHE) {
            CacheEntry cached = CACHE.get(cacheKey);
            if (cached != null && System.currentTimeMillis() - cached.savedAt < CACHE_MILLIS) {
                return new ArrayList<>(cached.projects);
            }
        }
        String facets = URLEncoder.encode("[[\"project_type:mod\"]]", "UTF-8");
        String encoded = URLEncoder.encode(query, "UTF-8");
        URL url = new URL("https://api.modrinth.com/v2/search?query=" + encoded + "&facets=" + facets + "&limit=20");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "dev.waterclient.android/0.1.0");
        connection.setRequestProperty("Accept", "application/json");
        try {
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) throw new IllegalStateException("Modrinth respondeu HTTP " + code);
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            if (code < 200 || code >= 300) throw new IllegalStateException("Modrinth respondeu HTTP " + code);
            JSONArray hits = new JSONObject(body.toString()).optJSONArray("hits");
            List<Project> projects = new ArrayList<>();
            if (hits == null) return projects;
            for (int i = 0; i < hits.length(); i++) {
                JSONObject item = hits.optJSONObject(i);
                if (item == null) continue;
                projects.add(new Project(item.optString("title"), item.optString("description"),
                        item.optString("slug"), item.optLong("downloads")));
            }
            synchronized (CACHE) { CACHE.put(cacheKey, new CacheEntry(new ArrayList<>(projects))); }
            return projects;
        } finally {
            connection.disconnect();
        }
    }
}
