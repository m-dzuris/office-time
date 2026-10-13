package com.officetime.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByGroupIdOrderByCreatedAtAscIdAsc(Long groupId);

    /** Newest first. Expects a normalized email address. */
    List<Invitation> findByInvitedEmailOrderByCreatedAtDescIdDesc(String invitedEmail);

    Optional<Invitation> findByGroupIdAndInvitedEmail(Long groupId, String invitedEmail);

    long countByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);

    void deleteByGroupIdAndInvitedEmail(Long groupId, String invitedEmail);
}
