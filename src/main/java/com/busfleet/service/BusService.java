package com.busfleet.service;

import com.busfleet.model.Bus;
import com.busfleet.model.BusModel;
import com.busfleet.repository.BusRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Сервис управления парком автобусов.
 */
@Service
public class BusService {

    private final BusRepository busRepository;

    public BusService(BusRepository busRepository) {
        this.busRepository = busRepository;
    }

    public boolean addBus(String stateNumber, BusModel model, long initialMileageKm) {
        String normalized = normalize(stateNumber);
        if (normalized.isEmpty()) return false;
        if (busRepository.existsByStateNumber(normalized)) return false;
        busRepository.save(new Bus(normalized, model, Math.max(0, initialMileageKm)));
        return true;
    }

    public boolean removeBus(String stateNumber) {
        return busRepository.deleteByStateNumber(stateNumber);
    }

    public Optional<Bus> findBus(String stateNumber) {
        return busRepository.findByStateNumber(stateNumber);
    }

    public Map<BusModel, List<Bus>> getAllGroupedByModel() {
        return busRepository.findAll().stream()
                .collect(Collectors.groupingBy(Bus::getModel, TreeMap::new, Collectors.toList()));
    }

    public List<Bus> getAllBuses() {
        return busRepository.findAll();
    }

    public boolean updateMileageSet(String stateNumber, long newMileageKm) {
        Optional<Bus> opt = busRepository.findByStateNumber(stateNumber);
        if (opt.isEmpty()) return false;
        Bus bus = opt.get();
        bus.setMileageKm(Math.max(0, newMileageKm));
        busRepository.save(bus);
        return true;
    }

    public boolean updateMileageAdd(String stateNumber, long kmToAdd) {
        Optional<Bus> opt = busRepository.findByStateNumber(stateNumber);
        if (opt.isEmpty()) return false;
        Bus bus = opt.get();
        bus.addMileageKm(kmToAdd);
        busRepository.save(bus);
        return true;
    }

    public Map<BusModel, Long> getStatsByModel() {
        return busRepository.findAll().stream()
                .collect(Collectors.groupingBy(Bus::getModel, TreeMap::new, Collectors.counting()));
    }

    private static String normalize(String stateNumber) {
        return stateNumber != null ? stateNumber.trim().toUpperCase() : "";
    }
}
