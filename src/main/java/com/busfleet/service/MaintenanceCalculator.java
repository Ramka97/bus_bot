package com.busfleet.service;

import com.busfleet.model.Bus;

/**
 * Расчёт информации о техническом обслуживании автобусов.
 */
public class MaintenanceCalculator {

    public static MaintenanceInfo calculate(Bus bus) {
        return new MaintenanceInfo(bus.getMileageKm());
    }
}
