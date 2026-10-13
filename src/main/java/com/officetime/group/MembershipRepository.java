package com.officetime.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByUserId(Long userId);

    /** Longest-standing member first. */
    List<Membership> findByGroupIdOrderByJoinedAtAscIdAsc(Long groupId);

    Optional<Membership> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    long countByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);
}
