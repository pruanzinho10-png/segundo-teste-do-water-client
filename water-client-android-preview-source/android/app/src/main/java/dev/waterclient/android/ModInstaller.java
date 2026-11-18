package dev.waterclient.android;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/** Baixa e valida um JAR do CDN oficial do Modrinth antes de colocá-lo na instância. */
final class ModInstaller {
    private static final long MAX_FILE_SIZE = 200L * 1024L * 1024L;
    private static final int MAX_REDIRECTS = 5;

    private ModInstaller() { }

    static File install(ModrinthApi.ModFile mod, File modsDirectory) throws Exception {
        String safeName = safeJarName(mod.filename);
        if (mod.size > MAX_FILE_SIZE) throw new IllegalStateException("O arquivo passa do limite de 200 MB.");
        if (mod.sha1 == null || !mod.sha1.matches("(?i)[0-9a-f]{40}")) {
            throw new IllegalStateException("O Modrinth não forneceu um SHA-1 válido para conferir o arquivo.");
        }
        if (!modsDirectory.isDirectory() && !modsDirectory.mkdirs()) {
            throw new IllegalStateException("Não foi possível acessar a pasta mods da instância.");
        }

        File target = new File(modsDirectory, safeName);
        File temporary = new File(modsDirectory, "." + UUID.randomUUID() + ".part");
        try {
            download(mod.url, temporary, mod.size);
            if (!isJarArchive(temporary)) throw new IllegalStateException("O download não parece ser um arquivo JAR válido.");
            String actualSha1 = sha1(temporary);
            if (!actualSha1.equalsIgnoreCase(mod.sha1)) {
                throw new IllegalStateException("A verificação SHA-1 do download falhou; o arquivo foi descartado.");
            }
            if (target.exists() && !target.delete()) {
                throw new IllegalStateException("Não foi possível substituir a versão anterior do mod.");
            }
            if (!temporary.renameTo(target)) {
                try (InputStream input = new BufferedInputStream(new FileInputStream(temporary));
                     BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
                    byte[] buffer = new byte[32 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
                if (!temporary.delete()) temporary.deleteOnExit();
            }
            return target;
        } finally {
            if (temporary.exists()) temporary.delete();
        }
    }

    private static void download(String urlText, File destination, long expectedSize) throws Exception {
        URL current = new URL(urlText);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            validateCdnUrl(current);
            HttpURLConnection connection = (HttpURLConnection) current.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "WaterClient/0.1.0 (dev.waterclient.android)");
            connection.setRequestProperty("Accept", "application/java-archive, application/octet-stream");
            try {
                int code = connection.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP
                        || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null || redirects == MAX_REDIRECTS) {
                        throw new IllegalStateException("Redirecionamento inválido no CDN do Modrinth.");
                    }
                    current = new URL(current, location);
                    continue;
                }
                if (code < 200 || code >= 300) throw new IllegalStateException("Download respondeu HTTP " + code);
                long total = 0;
                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(destination))) {
                    byte[] buffer = new byte[32 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > MAX_FILE_SIZE) throw new IllegalStateException("O download passou do limite de 200 MB.");
                        output.write(buffer, 0, count);
                    }
                }
                if (expectedSize >= 0 && total != expectedSize) {
                    throw new IllegalStateException("O tamanho do download não corresponde ao informado pelo Modrinth.");
                }
                return;
            } finally {
                connection.disconnect();
            }
        }
        throw new IllegalStateException("Não foi possível completar o download.");
    }

    private static void validateCdnUrl(URL url) {
        if (!"https".equalsIgnoreCase(url.getProtocol()) || !"cdn.modrinth.com".equalsIgnoreCase(url.getHost())) {
            throw new IllegalStateException("O Modrinth retornou um endereço de download fora do CDN oficial.");
        }
    }

    private static String safeJarName(String filename) {
        if (filename == null || filename.trim().isEmpty() || filename.contains("/") || filename.contains("\\")
                || filename.equals(".") || filename.equals("..")) {
            throw new IllegalStateException("Nome de arquivo inválido recebido do Modrinth.");
        }
        String safe = filename.replaceAll("[^A-Za-z0-9._+() -]", "_").trim();
        if (safe.length() > 180) safe = safe.substring(safe.length() - 180);
        if (!safe.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw new IllegalStateException("O projeto não forneceu um arquivo .jar instalável.");
        }
        return safe;
    }

    private static boolean isJarArchive(File file) throws Exception {
        try (InputStream input = new FileInputStream(file)) {
            return input.read() == 0x50 && input.read() == 0x4b;
        }
    }

    private static String sha1(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        StringBuilder result = new StringBuilder(40);
        for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }
}
