package com.busfleet.model;

import java.time.LocalDateTime;

/**
 * Автобус — конкретный экземпляр модели с уникальным госномером.
 */
public class Bus {
    private final String stateNumber;
    private final BusModel model;
    private long mileageKm;
    private LocalDateTime lastMileageUpdateAt;

    public Bus(String stateNumber, BusModel model, long mileageKm) {
        this(stateNumber, model, mileageKm, LocalDateTime.now());
    }

    public Bus(String stateNumber, BusModel model, long mileageKm, LocalDateTime lastMileageUpdateAt) {
        this.stateNumber = stateNumber != null ? stateNumber.trim().toUpperCase() : "";
        this.model = model;
        this.mileageKm = Math.max(0, mileageKm);
        this.lastMileageUpdateAt = lastMileageUpdateAt != null ? lastMileageUpdateAt : LocalDateTime.now();
    }

    public String getStateNumber() {
        return stateNumber;
    }

    public BusModel getModel() {
        return model;
    }

    public long getMileageKm() {
        return mileageKm;
    }

    public LocalDateTime getLastMileageUpdateAt() {
        return lastMileageUpdateAt;
    }

    public void setMileageKm(long mileageKm) {
        this.mileageKm = Math.max(0, mileageKm);
        this.lastMileageUpdateAt = LocalDateTime.now();
    }

    public void addMileageKm(long km) {
        this.mileageKm = Math.max(0, this.mileageKm + km);
        this.lastMileageUpdateAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("%s (%s) — %d км", stateNumber, model.getDisplayName(), mileageKm);
    }
}
