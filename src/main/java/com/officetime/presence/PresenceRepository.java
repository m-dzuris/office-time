package com.officetime.presence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PresenceRepository extends JpaRepository<PresenceEntry, Long> {

    List<PresenceEntry> findByUserIdAndDateBetweenOrderByDate(Long userId, LocalDate from, LocalDate to);

    List<PresenceEntry> findByUserIdInAndDateBetweenOrderByDate(Collection<Long> userIds, LocalDate from, LocalDate to);

    Optional<PresenceEntry> findByUserIdAndDate(Long userId, LocalDate date);

    void deleteByUserIdAndDate(Long userId, LocalDate date);
}
