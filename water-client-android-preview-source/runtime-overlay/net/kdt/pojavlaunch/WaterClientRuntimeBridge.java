package net.kdt.pojavlaunch;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/** Connects Water Client's local profile selection to Amethyst without bypassing its license gate. */
public final class WaterClientRuntimeBridge {
    private static final String TAG = "WaterClientBridge";
    private static final String EXTRA_PROFILE_NAME = "dev.waterclient.android.PROFILE_NAME";
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private WaterClientRuntimeBridge() { }

    public static void prepare(Context context, Intent source) {
        if (source == null) return;
        String username = source.getStringExtra(EXTRA_PROFILE_NAME);
        if (username == null || !VALID_NAME.matcher(username).matches()) return;

        // Amethyst intentionally requires a licensed online Java account before enabling local auth.
        // Keep that check intact: Water Client profiles never grant or simulate Minecraft ownership.
        if (!Tools.hasOnlineProfile()) {
            Log.i(TAG, "No licensed online profile is saved; leaving account selection to Amethyst.");
            return;
        }

        MinecraftAccount existing = MinecraftAccount.load(username);
        if (existing != null && !existing.isLocal()) {
            Log.w(TAG, "Water profile name matches an online account; not replacing its credentials.");
            Toast.makeText(context, "Esse nome já pertence a uma conta conectada no runtime.", Toast.LENGTH_LONG).show();
            return;
        }

        if (existing == null) {
            MinecraftAccount local = new MinecraftAccount();
            local.username = username;
            local.accessToken = "0";
            local.clientToken = "0";
            local.profileId = UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8))
                    .toString().replace("-", "");
            try {
                local.save();
            } catch (IOException error) {
                Log.e(TAG, "Could not create local profile from Water Client", error);
                Toast.makeText(context, "Não foi possível sincronizar o perfil com o runtime.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        PojavProfile.setCurrentProfile(context, username);
        Log.i(TAG, "Selected Water Client local profile in the integrated runtime.");
    }
}
