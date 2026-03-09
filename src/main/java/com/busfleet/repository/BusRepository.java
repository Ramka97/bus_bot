package com.busfleet.repository;

import com.busfleet.model.Bus;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с автобусами.
 */
public interface BusRepository {

    List<Bus> findAll();

    Optional<Bus> findByStateNumber(String stateNumber);

    Bus save(Bus bus);

    boolean deleteByStateNumber(String stateNumber);

    boolean existsByStateNumber(String stateNumber);
}
