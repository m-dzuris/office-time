package com.officetime.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * An invitation is addressed to an email address, not to an account, so it also works for someone who
 * registers after being invited. It disappears when accepted, declined or revoked.
 */
@Entity
@Table(name = "group_invitation", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "invited_email"}))
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "invited_email", nullable = false, length = 254)
    private String invitedEmail;

    @Column(name = "invited_by_user_id", nullable = false)
    private Long invitedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Invitation() {
        // required by JPA
    }

    public Invitation(Long groupId, String invitedEmail, Long invitedByUserId) {
        this.groupId = groupId;
        this.invitedEmail = invitedEmail;
        this.invitedByUserId = invitedByUserId;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public Long getInvitedByUserId() {
        return invitedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
