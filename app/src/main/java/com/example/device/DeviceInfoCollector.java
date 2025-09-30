package com.example.device;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
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

            // Network Information (may require permissions)
            collectNetworkInfo(builder);

            // WiFi Information (may require permissions)
            collectWifiInfo(builder);

            // IP Address Information
            collectIpAddressInfo(builder);

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
                    .setFingerprint(safeGet(Build.FINGERPRINT));
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

    private void collectNetworkInfo(DeviceInfo.Builder builder) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                try {
                    builder.setNetworkOperator(safeGet(tm.getNetworkOperatorName()))
                            .setNetworkCountry(safeGet(tm.getNetworkCountryIso()))
                            .setSimOperator(safeGet(tm.getSimOperatorName()))
                            .setSimCountry(safeGet(tm.getSimCountryIso()))
                            .setSimState(getSimState(tm.getSimState()));
                } catch (SecurityException e) {
                    System.out.println("Telephony permissions not granted");
                }
            }

        } catch (Exception e) {
            System.out.println("Error collecting network info: " + e.getMessage());
        }
    }

    private void collectWifiInfo(DeviceInfo.Builder builder) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                try {
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    if (wifiInfo != null) {
                        builder.setSsid(safeGet(wifiInfo.getSSID()))
                                .setBssid(safeGet(wifiInfo.getBSSID()))
                                .setMacAddress(safeGet(wifiInfo.getMacAddress()))
                                .setLinkSpeed(wifiInfo.getLinkSpeed())
                                .setIpAddress(intToIp(wifiInfo.getIpAddress()))
                                .setNetworkId(wifiInfo.getNetworkId())
                                .setRssi(wifiInfo.getRssi());
                    }
                } catch (SecurityException e) {
                    System.out.println("WiFi permissions not granted");
                }
            }
        } catch (Exception e) {
            System.out.println("Error collecting WiFi info: " + e.getMessage());
        }
    }

    private void collectIpAddressInfo(DeviceInfo.Builder builder) {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces == null) return;

            List<NetworkInterface> interfaces = Collections.list(networkInterfaces);
            for (NetworkInterface intf : interfaces) {
                if (intf == null) continue;

                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        // Just take the first non-loopback address for simplicity
                        builder.setIpAddress(addr.getHostAddress());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error collecting IP address info: " + e.getMessage());
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

    private String getSimState(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT: return "ABSENT";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: return "NETWORK_LOCKED";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: return "PIN_REQUIRED";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: return "PUK_REQUIRED";
            case TelephonyManager.SIM_STATE_READY: return "READY";
            case TelephonyManager.SIM_STATE_UNKNOWN: return "UNKNOWN";
            default: return "UNKNOWN (" + simState + ")";
        }
    }

    private String intToIp(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.US, "%.2f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}