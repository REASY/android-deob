package com.example.device;

import java.util.Arrays;
import java.util.Collections;
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
    private final String[] supportedAbis;

    // Display Information
    private final Optional<String> screenResolution;
    private final Optional<Float> density;
    private final Optional<Integer> densityDpi;

    // Device Identifiers
    private final Optional<String> androidId;
    private final Optional<String> serialNumber;

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
        this.supportedAbis = builder.supportedAbis;
        this.screenResolution = Optional.ofNullable(builder.screenResolution);
        this.density = Optional.ofNullable(builder.density);
        this.densityDpi = Optional.ofNullable(builder.densityDpi);
        this.androidId = Optional.ofNullable(builder.androidId);
        this.serialNumber = Optional.ofNullable(builder.serialNumber);
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
        private String[] supportedAbis;
        private String screenResolution;
        private Float density;
        private Integer densityDpi;
        private String androidId;
        private String serialNumber;
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

        public Builder setSupportedAbis(String[] supportedAbis) { this.supportedAbis = supportedAbis; return  this; }

        public DeviceInfo build() {
            return new DeviceInfo(this);
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
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
        appendOptional(sb, "Supported ABIs", Optional.of(Arrays.toString(supportedAbis)));
        appendOptional(sb, "Screen Resolution", screenResolution);
        appendOptional(sb, "Density", density);
        appendOptional(sb, "Density DPI", densityDpi);
        appendOptional(sb, "Android ID", androidId);
        appendOptional(sb, "Serial Number", serialNumber);
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
        if (value.isPresent()) {
            sb.append(value.get()).append("\n");
        }
        else {
            sb.append("Not available").append("\n");
        }
    }
}