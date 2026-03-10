package com.busfleet.repository;

import com.busfleet.model.Bus;
import com.busfleet.model.BusModel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Реализация репозитория с сохранением в БД (H2).
 * Данные хранятся в файле и не теряются при перезапуске.
 */
@Repository
public class JpaBusRepository implements BusRepository {

    private final BusJpaRepository jpaRepository;

    public JpaBusRepository(BusJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Bus> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toBus)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Bus> findByStateNumber(String stateNumber) {
        return jpaRepository.findById(normalize(stateNumber))
                .map(this::toBus);
    }

    @Override
    public Bus save(Bus bus) {
        BusEntity entity = toEntity(bus);
        jpaRepository.save(entity);
        return bus;
    }

    @Override
    public boolean deleteByStateNumber(String stateNumber) {
        String key = normalize(stateNumber);
        if (!jpaRepository.existsById(key)) return false;
        jpaRepository.deleteById(key);
        return true;
    }

    @Override
    public boolean existsByStateNumber(String stateNumber) {
        return jpaRepository.existsById(normalize(stateNumber));
    }

    private Bus toBus(BusEntity e) {
        return new Bus(
                e.getStateNumber(),
                e.getModel(),
                e.getMileageKm(),
                e.getLastMileageUpdateAt()
        );
    }

    private BusEntity toEntity(Bus bus) {
        BusEntity e = new BusEntity(
                bus.getStateNumber(),
                bus.getModel(),
                bus.getMileageKm(),
                bus.getLastMileageUpdateAt()
        );
        return e;
    }

    private static String normalize(String stateNumber) {
        return stateNumber != null ? stateNumber.trim().toUpperCase() : "";
    }
}
