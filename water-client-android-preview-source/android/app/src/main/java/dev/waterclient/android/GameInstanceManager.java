package dev.waterclient.android;

import android.content.Context;

import java.io.File;

/** Pasta de mods compartilhada com o runtime Amethyst integrado ao APK. */
final class GameInstanceManager {
    private GameInstanceManager() { }

    static File getInstanceDirectory(Context context, String gameVersion, String loader) throws Exception {
        if (gameVersion == null || !gameVersion.matches("[0-9]+\\.[0-9]+(\\.[0-9]+)?")) {
            throw new IllegalArgumentException("Versão de Minecraft inválida.");
        }
        String normalizedLoader = loader == null ? "" : loader.toLowerCase(java.util.Locale.ROOT);
        if (!normalizedLoader.matches("fabric|forge|neoforge|quilt")) {
            throw new IllegalArgumentException("Mod loader inválido.");
        }
        // Amethyst resolves its game root to getExternalFilesDir(null) on Android 10+.
        // Put the JAR under that same .minecraft directory so the launched game sees it.
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();
        File instance = new File(base, ".minecraft");
        if (!instance.isDirectory() && !instance.mkdirs()) {
            throw new IllegalStateException("Não foi possível criar a pasta .minecraft do runtime.");
        }
        return instance;
    }

    static File getModsDirectory(Context context, String gameVersion, String loader) throws Exception {
        File mods = new File(getInstanceDirectory(context, gameVersion, loader), "mods");
        if (!mods.isDirectory() && !mods.mkdirs()) {
            throw new IllegalStateException("Não foi possível criar a pasta mods da instância.");
        }
        return mods;
    }
}
