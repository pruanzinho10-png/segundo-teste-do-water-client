package dev.waterclient.android;

import android.content.Context;

import java.io.File;

/** Diretórios da instância gerenciada pelo Water Client. */
final class GameInstanceManager {
    private static final String INSTANCE_ROOT = "WaterClient/instances";

    private GameInstanceManager() { }

    static File getInstanceDirectory(Context context, String gameVersion, String loader) throws Exception {
        if (gameVersion == null || !gameVersion.matches("[0-9]+\\.[0-9]+(\\.[0-9]+)?")) {
            throw new IllegalArgumentException("Versão de Minecraft inválida.");
        }
        String normalizedLoader = loader == null ? "" : loader.toLowerCase(java.util.Locale.ROOT);
        if (!normalizedLoader.matches("fabric|forge|neoforge|quilt")) {
            throw new IllegalArgumentException("Mod loader inválido.");
        }
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();
        File root = new File(base, INSTANCE_ROOT);
        if (!root.isDirectory() && !root.mkdirs()) {
            throw new IllegalStateException("Não foi possível criar a pasta de instâncias.");
        }
        File instance = new File(root, "minecraft-" + gameVersion + "-" + normalizedLoader);
        if (!instance.isDirectory() && !instance.mkdirs()) {
            throw new IllegalStateException("Não foi possível criar a pasta da instância.");
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

