package com.example.obfuscate;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class AppInstallIdProvider {
    private static final String PREFS_NAME = "app_install_prefs";
    private static final String KEY_INSTALL_ID = "app_install_id";

    private static String cachedId;

    public static synchronized String getAppInstallId(Context context) {
        if (cachedId != null) {
            return cachedId;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY_INSTALL_ID, null);

        if (id == null) {
            // Generate new UUID for this install
            id = UUID.randomUUID().toString();

            prefs.edit()
                    .putString(KEY_INSTALL_ID, id)
                    .apply();
        }

        cachedId = id;
        return id;
    }
}
