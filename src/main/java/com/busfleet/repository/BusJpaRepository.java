package com.busfleet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA репозиторий для таблицы автобусов.
 */
public interface BusJpaRepository extends JpaRepository<BusEntity, String> {
}
