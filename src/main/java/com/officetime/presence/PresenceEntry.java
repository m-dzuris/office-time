package com.officetime.presence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/**
 * One person's decision for one day. It belongs to the user alone: groups never copy or own it, they only
 * decide who is allowed to see it.
 */
@Entity
@Table(name = "presence_entry", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "entry_date"}))
public class PresenceEntry {

    public static final int NOTE_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PresenceStatus status;

    @Column(length = NOTE_MAX_LENGTH)
    private String note;

    protected PresenceEntry() {
        // required by JPA
    }

    public PresenceEntry(Long userId, LocalDate date, PresenceStatus status, String note) {
        this.userId = userId;
        this.date = date;
        this.status = status;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public PresenceStatus getStatus() {
        return status;
    }

    public void setStatus(PresenceStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
