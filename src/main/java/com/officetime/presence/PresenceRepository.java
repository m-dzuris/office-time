package com.officetime.presence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PresenceRepository extends JpaRepository<PresenceEntry, Long> {

    List<PresenceEntry> findByDateBetweenOrderByDate(LocalDate from, LocalDate to);

    Optional<PresenceEntry> findByDate(LocalDate date);

    void deleteByDate(LocalDate date);
}
