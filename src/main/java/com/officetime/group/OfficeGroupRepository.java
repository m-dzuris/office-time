package com.officetime.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficeGroupRepository extends JpaRepository<OfficeGroup, Long> {

    Optional<OfficeGroup> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
