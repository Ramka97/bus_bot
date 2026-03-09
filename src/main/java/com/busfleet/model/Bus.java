package com.busfleet.model;

/**
 * Автобус — конкретный экземпляр модели с уникальным госномером.
 */
public class Bus {
    private final String stateNumber;
    private final BusModel model;
    private long mileageKm;

    public Bus(String stateNumber, BusModel model, long mileageKm) {
        this.stateNumber = stateNumber != null ? stateNumber.trim().toUpperCase() : "";
        this.model = model;
        this.mileageKm = Math.max(0, mileageKm);
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

    public void setMileageKm(long mileageKm) {
        this.mileageKm = Math.max(0, mileageKm);
    }

    public void addMileageKm(long km) {
        this.mileageKm = Math.max(0, this.mileageKm + km);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) — %d км", stateNumber, model.getDisplayName(), mileageKm);
    }
}
