package com.officetime.presence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Service
public class PresenceService {

    private final PresenceRepository repository;

    public PresenceService(PresenceRepository repository) {
        this.repository = repository;
    }

    /** The user's own days, notes included. */
    @Transactional(readOnly = true)
    public List<PresenceResponse> findBetween(Long userId, LocalDate from, LocalDate to) {
        return repository.findByUserIdAndDateBetweenOrderByDate(userId, from, to).stream()
                .map(PresenceResponse::from)
                .toList();
    }

    /**
     * Several people's days, statuses only. The caller must already have checked that the viewer is allowed
     * to see every one of these users.
     */
    @Transactional(readOnly = true)
    public List<MemberPresenceResponse> findForUsers(Collection<Long> userIds, LocalDate from, LocalDate to) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return repository.findByUserIdInAndDateBetweenOrderByDate(userIds, from, to).stream()
                .map(MemberPresenceResponse::from)
                .toList();
    }

    /** Creates the user's entry for the day, or updates it if one already exists. */
    @Transactional
    public PresenceResponse save(Long userId, LocalDate date, PresenceRequest request) {
        String note = normalizeNote(request.note());
        PresenceEntry entry = repository.findByUserIdAndDate(userId, date)
                .map(existing -> {
                    existing.setStatus(request.status());
                    existing.setNote(note);
                    return existing;
                })
                .orElseGet(() -> new PresenceEntry(userId, date, request.status(), note));
        return PresenceResponse.from(repository.save(entry));
    }

    /** Clears the day back to "not set". Deleting a day that has no entry is not an error. */
    @Transactional
    public void clear(Long userId, LocalDate date) {
        repository.deleteByUserIdAndDate(userId, date);
    }

    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
