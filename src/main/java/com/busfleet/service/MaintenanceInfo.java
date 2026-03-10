package com.busfleet.service;

import com.busfleet.model.BusModel;

/**
 * Информация о техническом обслуживании автобуса.
 * МАЗ: ТО-1 каждые 15 000 км, ТО-2 каждые 30 000 км.
 * ZHONG TONG: только ТО-2 каждые 50 000 км.
 */
public class MaintenanceInfo {
    private static final long TO1_INTERVAL_KM = 15_000;
    private static final long TO2_INTERVAL_KM = 30_000;
    private static final long ZHONG_TONG_TO2_INTERVAL_KM = 50_000;
    private static final long WARNING_THRESHOLD_KM = 1_000;

    private final boolean zhongTong;
    private final long mileageSinceLastTO1;
    private final long kmUntilNextTO1;
    private final long mileageSinceLastTO2;
    private final long kmUntilNextTO2;
    private final boolean warningTO1;
    private final boolean warningTO2;

    public MaintenanceInfo(long currentMileageKm) {
        this(currentMileageKm, null);
    }

    /** С учётом модели: для ZHONG_TONG только ТО-2 каждые 50 000 км. */
    public MaintenanceInfo(long currentMileageKm, BusModel model) {
        this.zhongTong = model == BusModel.ZHONG_TONG;
        if (zhongTong) {
            this.mileageSinceLastTO1 = 0;
            this.kmUntilNextTO1 = -1; // не применимо
            this.mileageSinceLastTO2 = currentMileageKm % ZHONG_TONG_TO2_INTERVAL_KM;
            this.kmUntilNextTO2 = ZHONG_TONG_TO2_INTERVAL_KM - mileageSinceLastTO2;
        } else {
            this.mileageSinceLastTO1 = currentMileageKm % TO1_INTERVAL_KM;
            this.kmUntilNextTO1 = TO1_INTERVAL_KM - mileageSinceLastTO1;
            this.mileageSinceLastTO2 = currentMileageKm % TO2_INTERVAL_KM;
            this.kmUntilNextTO2 = TO2_INTERVAL_KM - mileageSinceLastTO2;
        }
        this.warningTO1 = !zhongTong && kmUntilNextTO1 <= WARNING_THRESHOLD_KM && kmUntilNextTO1 > 0;
        this.warningTO2 = kmUntilNextTO2 <= WARNING_THRESHOLD_KM && kmUntilNextTO2 > 0;
    }

    public boolean isZhongTong() {
        return zhongTong;
    }

    /** ТО-1 применимо только для не-Zhong Tong. */
    public boolean isTO1Applicable() {
        return !zhongTong && kmUntilNextTO1 >= 0;
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
        return !zhongTong && mileageSinceLastTO1 == 0 && kmUntilNextTO1 == TO1_INTERVAL_KM;
    }

    public boolean needsTO2() {
        return zhongTong
                ? (mileageSinceLastTO2 == 0 && kmUntilNextTO2 == ZHONG_TONG_TO2_INTERVAL_KM)
                : (mileageSinceLastTO2 == 0 && kmUntilNextTO2 == TO2_INTERVAL_KM);
    }
}
