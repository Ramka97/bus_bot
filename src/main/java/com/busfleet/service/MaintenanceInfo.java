package com.busfleet.service;

/**
 * Информация о техническом обслуживании автобуса.
 * ТО-1 каждые 15 000 км, ТО-2 каждые 30 000 км.
 */
public class MaintenanceInfo {
    private static final long TO1_INTERVAL_KM = 15_000;
    private static final long TO2_INTERVAL_KM = 30_000;
    private static final long WARNING_THRESHOLD_KM = 1_000;

    private final long mileageSinceLastTO1;
    private final long kmUntilNextTO1;
    private final long mileageSinceLastTO2;
    private final long kmUntilNextTO2;
    private final boolean warningTO1;
    private final boolean warningTO2;

    public MaintenanceInfo(long currentMileageKm) {
        this.mileageSinceLastTO1 = currentMileageKm % TO1_INTERVAL_KM;
        this.kmUntilNextTO1 = TO1_INTERVAL_KM - mileageSinceLastTO1;
        this.mileageSinceLastTO2 = currentMileageKm % TO2_INTERVAL_KM;
        this.kmUntilNextTO2 = TO2_INTERVAL_KM - mileageSinceLastTO2;
        this.warningTO1 = kmUntilNextTO1 <= WARNING_THRESHOLD_KM && kmUntilNextTO1 > 0;
        this.warningTO2 = kmUntilNextTO2 <= WARNING_THRESHOLD_KM && kmUntilNextTO2 > 0;
    }

    public long getMileageSinceLastTO1() {
        return mileageSinceLastTO1;
    }

    public long getKmUntilNextTO1() {
        return kmUntilNextTO1;
    }

    public long getMileageSinceLastTO2() {
        return mileageSinceLastTO2;
    }

    public long getKmUntilNextTO2() {
        return kmUntilNextTO2;
    }

    public boolean isWarningTO1() {
        return warningTO1;
    }

    public boolean isWarningTO2() {
        return warningTO2;
    }

    public boolean needsTO1() {
        return mileageSinceLastTO1 == 0 && kmUntilNextTO1 == TO1_INTERVAL_KM;
    }

    public boolean needsTO2() {
        return mileageSinceLastTO2 == 0 && kmUntilNextTO2 == TO2_INTERVAL_KM;
    }
}
