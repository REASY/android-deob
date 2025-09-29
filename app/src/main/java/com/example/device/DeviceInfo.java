package com.example.device;

import java.util.Optional;

public class DeviceInfo {
    // Build Information
    private final Optional<String> manufacturer;
    private final Optional<String> model;
    private final Optional<String> product;
    private final Optional<String> device;
    private final Optional<String> board;
    private final Optional<String> hardware;
    private final Optional<String> brand;
    private final Optional<String> androidVersion;
    private final Optional<Integer> apiLevel;
    private final Optional<String> buildId;
    private final Optional<String> fingerprint;

    // Display Information
    private final Optional<String> screenResolution;
    private final Optional<Float> density;
    private final Optional<Integer> densityDpi;

    // Device Identifiers
    private final Optional<String> androidId;
    private final Optional<String> serialNumber;

    // Network Information
    private final Optional<String> networkOperator;
    private final Optional<String> networkCountry;
    private final Optional<String> simOperator;
    private final Optional<String> simCountry;
    private final Optional<String> simState;

    // WiFi Information
    private final Optional<String> ssid;
    private final Optional<String> bssid;
    private final Optional<String> macAddress;
    private final Optional<Integer> linkSpeed;
    private final Optional<String> ipAddress;
    private final Optional<Integer> networkId;
    private final Optional<Integer> rssi;

    // Locale & Storage
    private final Optional<String> language;
    private final Optional<String> country;
    private final Optional<String> timezone;
    private final Optional<String> totalStorage;
    private final Optional<String> availableStorage;

    // Memory Information
    private final Optional<String> maxMemory;
    private final Optional<String> totalMemory;
    private final Optional<String> freeMemory;
    private final Optional<String> usedMemory;

    private final Optional<String> userAgent;

    private final Optional<String> appInstallId;

    // Builder pattern for easy construction
    private DeviceInfo(Builder builder) {
        this.manufacturer = Optional.ofNullable(builder.manufacturer);
        this.model = Optional.ofNullable(builder.model);
        this.product = Optional.ofNullable(builder.product);
        this.device = Optional.ofNullable(builder.device);
        this.board = Optional.ofNullable(builder.board);
        this.hardware = Optional.ofNullable(builder.hardware);
        this.brand = Optional.ofNullable(builder.brand);
        this.androidVersion = Optional.ofNullable(builder.androidVersion);
        this.apiLevel = Optional.ofNullable(builder.apiLevel);
        this.buildId = Optional.ofNullable(builder.buildId);
        this.fingerprint = Optional.ofNullable(builder.fingerprint);
        this.screenResolution = Optional.ofNullable(builder.screenResolution);
        this.density = Optional.ofNullable(builder.density);
        this.densityDpi = Optional.ofNullable(builder.densityDpi);
        this.androidId = Optional.ofNullable(builder.androidId);
        this.serialNumber = Optional.ofNullable(builder.serialNumber);
        this.networkOperator = Optional.ofNullable(builder.networkOperator);
        this.networkCountry = Optional.ofNullable(builder.networkCountry);
        this.simOperator = Optional.ofNullable(builder.simOperator);
        this.simCountry = Optional.ofNullable(builder.simCountry);
        this.simState = Optional.ofNullable(builder.simState);
        this.ssid = Optional.ofNullable(builder.ssid);
        this.bssid = Optional.ofNullable(builder.bssid);
        this.macAddress = Optional.ofNullable(builder.macAddress);
        this.linkSpeed = Optional.ofNullable(builder.linkSpeed);
        this.ipAddress = Optional.ofNullable(builder.ipAddress);
        this.networkId = Optional.ofNullable(builder.networkId);
        this.rssi = Optional.ofNullable(builder.rssi);
        this.language = Optional.ofNullable(builder.language);
        this.country = Optional.ofNullable(builder.country);
        this.timezone = Optional.ofNullable(builder.timezone);
        this.totalStorage = Optional.ofNullable(builder.totalStorage);
        this.availableStorage = Optional.ofNullable(builder.availableStorage);
        this.maxMemory = Optional.ofNullable(builder.maxMemory);
        this.totalMemory = Optional.ofNullable(builder.totalMemory);
        this.freeMemory = Optional.ofNullable(builder.freeMemory);
        this.usedMemory = Optional.ofNullable(builder.usedMemory);
        this.userAgent = Optional.ofNullable(builder.userAgent);
        this.appInstallId = Optional.ofNullable(builder.appInstallId);
    }

    public static class Builder {
        private String manufacturer;
        private String model;
        private String product;
        private String device;
        private String board;
        private String hardware;
        private String brand;
        private String androidVersion;
        private Integer apiLevel;
        private String buildId;
        private String fingerprint;
        private String screenResolution;
        private Float density;
        private Integer densityDpi;
        private String androidId;
        private String serialNumber;
        private String networkOperator;
        private String networkCountry;
        private String simOperator;
        private String simCountry;
        private String simState;
        private String ssid;
        private String bssid;
        private String macAddress;
        private Integer linkSpeed;
        private String ipAddress;
        private Integer networkId;
        private Integer rssi;
        private String language;
        private String country;
        private String timezone;
        private String totalStorage;
        private String availableStorage;
        private String maxMemory;
        private String totalMemory;
        private String freeMemory;
        private String usedMemory;

        private String userAgent;

        private String appInstallId;

        public Builder setManufacturer(String manufacturer) { this.manufacturer = manufacturer; return this; }
        public Builder setModel(String model) { this.model = model; return this; }
        public Builder setProduct(String product) { this.product = product; return this; }
        public Builder setDevice(String device) { this.device = device; return this; }
        public Builder setBoard(String board) { this.board = board; return this; }
        public Builder setHardware(String hardware) { this.hardware = hardware; return this; }
        public Builder setBrand(String brand) { this.brand = brand; return this; }
        public Builder setAndroidVersion(String androidVersion) { this.androidVersion = androidVersion; return this; }
        public Builder setApiLevel(Integer apiLevel) { this.apiLevel = apiLevel; return this; }
        public Builder setBuildId(String buildId) { this.buildId = buildId; return this; }
        public Builder setFingerprint(String fingerprint) { this.fingerprint = fingerprint; return this; }
        public Builder setScreenResolution(String screenResolution) { this.screenResolution = screenResolution; return this; }
        public Builder setDensity(Float density) { this.density = density; return this; }
        public Builder setDensityDpi(Integer densityDpi) { this.densityDpi = densityDpi; return this; }
        public Builder setAndroidId(String androidId) { this.androidId = androidId; return this; }
        public Builder setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; return this; }
        public Builder setNetworkOperator(String networkOperator) { this.networkOperator = networkOperator; return this; }
        public Builder setNetworkCountry(String networkCountry) { this.networkCountry = networkCountry; return this; }
        public Builder setSimOperator(String simOperator) { this.simOperator = simOperator; return this; }
        public Builder setSimCountry(String simCountry) { this.simCountry = simCountry; return this; }
        public Builder setSimState(String simState) { this.simState = simState; return this; }
        public Builder setSsid(String ssid) { this.ssid = ssid; return this; }
        public Builder setBssid(String bssid) { this.bssid = bssid; return this; }
        public Builder setMacAddress(String macAddress) { this.macAddress = macAddress; return this; }
        public Builder setLinkSpeed(Integer linkSpeed) { this.linkSpeed = linkSpeed; return this; }
        public Builder setIpAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder setNetworkId(Integer networkId) { this.networkId = networkId; return this; }
        public Builder setRssi(Integer rssi) { this.rssi = rssi; return this; }
        public Builder setLanguage(String language) { this.language = language; return this; }
        public Builder setCountry(String country) { this.country = country; return this; }
        public Builder setTimezone(String timezone) { this.timezone = timezone; return this; }
        public Builder setTotalStorage(String totalStorage) { this.totalStorage = totalStorage; return this; }
        public Builder setAvailableStorage(String availableStorage) { this.availableStorage = availableStorage; return this; }
        public Builder setMaxMemory(String maxMemory) { this.maxMemory = maxMemory; return this; }
        public Builder setTotalMemory(String totalMemory) { this.totalMemory = totalMemory; return this; }
        public Builder setFreeMemory(String freeMemory) { this.freeMemory = freeMemory; return this; }
        public Builder setUsedMemory(String usedMemory) { this.usedMemory = usedMemory; return this; }
        public Builder setUserAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder setAppInstallId(String appInstallId) { this.appInstallId = appInstallId; return this; }

        public DeviceInfo build() {
            return new DeviceInfo(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ANDROID DEVICE INFORMATION ===\n\n");

        appendOptional(sb, "Manufacturer", manufacturer);
        appendOptional(sb, "Model", model);
        appendOptional(sb, "Product", product);
        appendOptional(sb, "Device", device);
        appendOptional(sb, "Board", board);
        appendOptional(sb, "Hardware", hardware);
        appendOptional(sb, "Brand", brand);
        appendOptional(sb, "Android Version", androidVersion);
        appendOptional(sb, "API Level", apiLevel);
        appendOptional(sb, "Build ID", buildId);
        appendOptional(sb, "Fingerprint", fingerprint);
        appendOptional(sb, "Screen Resolution", screenResolution);
        appendOptional(sb, "Density", density);
        appendOptional(sb, "Density DPI", densityDpi);
        appendOptional(sb, "Android ID", androidId);
        appendOptional(sb, "Serial Number", serialNumber);
        appendOptional(sb, "Network Operator", networkOperator);
        appendOptional(sb, "Network Country", networkCountry);
        appendOptional(sb, "SIM Operator", simOperator);
        appendOptional(sb, "SIM Country", simCountry);
        appendOptional(sb, "SIM State", simState);
        appendOptional(sb, "SSID", ssid);
        appendOptional(sb, "BSSID", bssid);
        appendOptional(sb, "MAC Address", macAddress);
        appendOptional(sb, "Link Speed", linkSpeed);
        appendOptional(sb, "IP Address", ipAddress);
        appendOptional(sb, "Network ID", networkId);
        appendOptional(sb, "RSSI", rssi);
        appendOptional(sb, "Language", language);
        appendOptional(sb, "Country", country);
        appendOptional(sb, "Timezone", timezone);
        appendOptional(sb, "Total Storage", totalStorage);
        appendOptional(sb, "Available Storage", availableStorage);
        appendOptional(sb, "Max Memory", maxMemory);
        appendOptional(sb, "Total Memory", totalMemory);
        appendOptional(sb, "Free Memory", freeMemory);
        appendOptional(sb, "Used Memory", usedMemory);
        appendOptional(sb, "User Agent", userAgent);
        appendOptional(sb, "AppInstallId", appInstallId);

        return sb.toString();
    }

    private <T> void appendOptional(StringBuilder sb, String label, Optional<T> value) {
        sb.append(label).append(": ");
        value.ifPresentOrElse(
                val -> sb.append(val),
                () -> sb.append("Not available")
        );
        sb.append("\n");
    }
}