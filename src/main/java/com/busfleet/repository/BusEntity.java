package com.busfleet.repository;

import com.busfleet.model.BusModel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Сущность JPA для хранения автобуса в БД.
 */
@Entity
@Table(name = "bus")
public class BusEntity {

    @Id
    @Column(name = "state_number", length = 20)
    private String stateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "model", nullable = false, length = 20)
    private BusModel model;

    @Column(name = "mileage_km", nullable = false)
    private long mileageKm;

    @Column(name = "last_mileage_update_at")
    private LocalDateTime lastMileageUpdateAt;

    public BusEntity() {
    }

    public BusEntity(String stateNumber, BusModel model, long mileageKm, LocalDateTime lastMileageUpdateAt) {
        this.stateNumber = stateNumber;
        this.model = model;
        this.mileageKm = mileageKm;
        this.lastMileageUpdateAt = lastMileageUpdateAt;
    }

    public String getStateNumber() {
        return stateNumber;
    }

    public void setStateNumber(String stateNumber) {
        this.stateNumber = stateNumber;
    }

    public BusModel getModel() {
        return model;
    }

    public void setModel(BusModel model) {
        this.model = model;
    }

    public long getMileageKm() {
        return mileageKm;
    }

    public void setMileageKm(long mileageKm) {
        this.mileageKm = mileageKm;
    }

    public LocalDateTime getLastMileageUpdateAt() {
        return lastMileageUpdateAt;
    }

    public void setLastMileageUpdateAt(LocalDateTime lastMileageUpdateAt) {
        this.lastMileageUpdateAt = lastMileageUpdateAt;
    }
}
