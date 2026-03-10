package com.busfleet.repository;

import com.busfleet.model.Bus;
import com.busfleet.model.BusModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация репозитория с сохранением в файл buspark.dat.
 */
@Repository
public class FileBusRepository implements BusRepository {

    private static final String SEPARATOR = ";";
    private final Path filePath;
    private final Map<String, Bus> buses = new ConcurrentHashMap<>();

    public FileBusRepository(@Value("${bus.storage.file:buspark.dat}") String fileName) {
        this.filePath = Path.of(fileName);
    }

    @PostConstruct
    public void load() {
        try {
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
                buses.clear();
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split(SEPARATOR, -1);
                    if (parts.length >= 3) {
                        try {
                            BusModel model = BusModel.valueOf(parts[1].trim());
                            long mileage = Long.parseLong(parts[2].trim());
                            LocalDateTime lastUpdate = null;
                            if (parts.length >= 4 && !parts[3].trim().isEmpty()) {
                                try {
                                    lastUpdate = LocalDateTime.parse(parts[3].trim());
                                } catch (Exception ignored) {}
                            }
                            Bus bus = new Bus(parts[0].trim(), model, mileage, lastUpdate);
                            buses.put(bus.getStateNumber(), bus);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        } catch (IOException e) {
            // при ошибке парк остаётся пустым
        }
    }

    private void persist() {
        try {
            List<String> lines = new ArrayList<>();
            for (Bus bus : buses.values()) {
                String lastUpdate = bus.getLastMileageUpdateAt() != null
                        ? bus.getLastMileageUpdateAt().toString()
                        : "";
                lines.add(bus.getStateNumber() + SEPARATOR
                        + bus.getModel().name() + SEPARATOR
                        + bus.getMileageKm() + SEPARATOR
                        + lastUpdate);
            }
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения: " + e.getMessage());
        }
    }

    @Override
    public List<Bus> findAll() {
        return new ArrayList<>(buses.values());
    }

    @Override
    public Optional<Bus> findByStateNumber(String stateNumber) {
        return Optional.ofNullable(buses.get(normalize(stateNumber)));
    }

    @Override
    public Bus save(Bus bus) {
        String key = bus.getStateNumber();
        buses.put(key, bus);
        persist();
        return bus;
    }

    @Override
    public boolean deleteByStateNumber(String stateNumber) {
        boolean removed = buses.remove(normalize(stateNumber)) != null;
        if (removed) persist();
        return removed;
    }

    @Override
    public boolean existsByStateNumber(String stateNumber) {
        return buses.containsKey(normalize(stateNumber));
    }

    private static String normalize(String stateNumber) {
        return stateNumber != null ? stateNumber.trim().toUpperCase() : "";
    }
}
