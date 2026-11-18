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

/** Cliente mínimo da API pública do Modrinth. */
final class ModrinthApi {
    private static final String API = "https://api.modrinth.com/v2";
    private static final String USER_AGENT = "WaterClient/0.1.0 (dev.waterclient.android)";
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

    static final class ModFile {
        final String projectTitle;
        final String version;
        final String filename;
        final String url;
        final String sha1;
        final long size;

        ModFile(String projectTitle, String version, String filename, String url, String sha1, long size) {
            this.projectTitle = projectTitle;
            this.version = version;
            this.filename = filename;
            this.url = url;
            this.sha1 = sha1;
            this.size = size;
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
        JSONObject response = getJson(new URL(API + "/search?query=" + encoded + "&facets=" + facets + "&limit=20"));
        JSONArray hits = response.optJSONArray("hits");
        List<Project> projects = new ArrayList<>();
        if (hits != null) {
            for (int i = 0; i < hits.length(); i++) {
                JSONObject item = hits.optJSONObject(i);
                if (item == null) continue;
                projects.add(new Project(item.optString("title"), item.optString("description"),
                        item.optString("slug"), item.optLong("downloads")));
            }
        }
        synchronized (CACHE) { CACHE.put(cacheKey, new CacheEntry(new ArrayList<>(projects))); }
        return projects;
    }

    static ModFile findCompatibleFile(Project project, String gameVersion, String loader) throws Exception {
        String encodedVersions = URLEncoder.encode(new JSONArray().put(gameVersion).toString(), "UTF-8");
        String encodedLoaders = URLEncoder.encode(new JSONArray().put(loader.toLowerCase(Locale.ROOT)).toString(), "UTF-8");
        String slug = URLEncoder.encode(project.slug, "UTF-8");
        URL url = new URL(API + "/project/" + slug + "/version?game_versions=" + encodedVersions
                + "&loaders=" + encodedLoaders + "&include_changelog=false");
        JSONArray versions = getArray(url);
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.optJSONObject(i);
            if (version == null) continue;
            JSONArray files = version.optJSONArray("files");
            if (files == null) continue;
            JSONObject selected = null;
            for (int j = 0; j < files.length(); j++) {
                JSONObject file = files.optJSONObject(j);
                if (file == null) continue;
                String name = file.optString("filename", "");
                if (!name.toLowerCase(Locale.ROOT).endsWith(".jar")) continue;
                if (selected == null || file.optBoolean("primary", false)) selected = file;
                if (file.optBoolean("primary", false)) break;
            }
            if (selected == null) continue;
            JSONObject hashes = selected.optJSONObject("hashes");
            return new ModFile(project.title, version.optString("version_number", "latest"),
                    selected.optString("filename"), selected.optString("url"),
                    hashes == null ? "" : hashes.optString("sha1", ""), selected.optLong("size", -1));
        }
        return null;
    }

    private static JSONObject getJson(URL url) throws Exception {
        String body = getText(url);
        return new JSONObject(body);
    }

    private static JSONArray getArray(URL url) throws Exception {
        String body = getText(url);
        return new JSONArray(body);
    }

    private static String getText(URL url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("User-Agent", USER_AGENT);
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
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }
}
