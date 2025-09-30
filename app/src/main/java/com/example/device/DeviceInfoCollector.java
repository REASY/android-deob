package com.example.device;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.Locale;

public class DeviceInfoCollector {

    private final Context context;
    private final String appInstallId;

    public DeviceInfoCollector(Context context, String appInstallId) {
        this.context = context;
        this.appInstallId = appInstallId;
    }

    public DeviceInfo collectDeviceInfo() {
        DeviceInfo.Builder builder = new DeviceInfo.Builder();

        try {
            // Build Information (always available)
            collectBuildInfo(builder);

            // Display Information (usually available without permissions)
            collectDisplayInfo(builder);

            // Device Identifiers (may require permissions)
            collectDeviceIdentifiers(builder);

            // Locale and Timezone (always available)
            collectLocaleInfo(builder);

            // Storage Information (usually available)
            collectStorageInfo(builder);

            // Memory Information (always available)
            collectMemoryInfo(builder);

            builder.setUserAgent(safeGet(System.getProperty("http.agent")));

            builder.setAppInstallId(appInstallId);

        } catch (Exception e) {
            // Continue with whatever data we could collect
            System.out.println("Partial collection completed with errors: " + e.getMessage());
        }

        return builder.build();
    }

    private void collectBuildInfo(DeviceInfo.Builder builder) {
        try {
            builder.setManufacturer(safeGet(Build.MANUFACTURER))
                    .setModel(safeGet(Build.MODEL))
                    .setProduct(safeGet(Build.PRODUCT))
                    .setDevice(safeGet(Build.DEVICE))
                    .setBoard(safeGet(Build.BOARD))
                    .setHardware(safeGet(Build.HARDWARE))
                    .setBrand(safeGet(Build.BRAND))
                    .setAndroidVersion(safeGet(Build.VERSION.RELEASE))
                    .setApiLevel(Build.VERSION.SDK_INT)
                    .setBuildId(safeGet(Build.DISPLAY))
                    .setFingerprint(safeGet(Build.FINGERPRINT))
                    .setSupportedAbis(Build.SUPPORTED_ABIS);
        } catch (Exception e) {
            System.out.println("Error collecting build info: " + e.getMessage());
        }
    }

    private void collectDisplayInfo(DeviceInfo.Builder builder) {
        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) {
                Display display = wm.getDefaultDisplay();
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);

                builder.setScreenResolution(metrics.widthPixels + "x" + metrics.heightPixels)
                        .setDensity(metrics.density)
                        .setDensityDpi(metrics.densityDpi);
            }
        } catch (Exception e) {
            System.out.println("Error collecting display info: " + e.getMessage());
        }
    }

    private void collectDeviceIdentifiers(DeviceInfo.Builder builder) {
        try {
            // Android ID
            try {
                String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                builder.setAndroidId(androidId);
            } catch (Exception e) {
                System.out.println("Android ID not available: " + e.getMessage());
            }

            // Serial Number
            builder.setSerialNumber(safeGet(Build.SERIAL));

        } catch (Exception e) {
            System.out.println("Error collecting device identifiers: " + e.getMessage());
        }
    }

    private void collectLocaleInfo(DeviceInfo.Builder builder) {
        try {
            builder.setLanguage(Locale.getDefault().getLanguage())
                    .setCountry(Locale.getDefault().getCountry())
                    .setTimezone(java.util.TimeZone.getDefault().getID());
        } catch (Exception e) {
            System.out.println("Error collecting locale info: " + e.getMessage());
        }
    }

    private void collectStorageInfo(DeviceInfo.Builder builder) {
        try {
            android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();

            builder.setTotalStorage(formatFileSize(totalBlocks * blockSize))
                    .setAvailableStorage(formatFileSize(availableBlocks * blockSize));
        } catch (Exception e) {
            System.out.println("Error collecting storage info: " + e.getMessage());
        }
    }

    private void collectMemoryInfo(DeviceInfo.Builder builder) {
        try {
            Runtime runtime = Runtime.getRuntime();
            builder.setMaxMemory(formatFileSize(runtime.maxMemory()))
                    .setTotalMemory(formatFileSize(runtime.totalMemory()))
                    .setFreeMemory(formatFileSize(runtime.freeMemory()))
                    .setUsedMemory(formatFileSize(runtime.totalMemory() - runtime.freeMemory()));
        } catch (Exception e) {
            System.out.println("Error collecting memory info: " + e.getMessage());
        }
    }

    // Helper methods
    private String safeGet(String value) {
        return value != null ? value : "Unknown";
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}