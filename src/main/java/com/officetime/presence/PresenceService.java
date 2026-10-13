package com.officetime.presence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PresenceService {

    private final PresenceRepository repository;

    public PresenceService(PresenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PresenceResponse> findBetween(LocalDate from, LocalDate to) {
        return repository.findByDateBetweenOrderByDate(from, to).stream()
                .map(PresenceResponse::from)
                .toList();
    }

    /** Creates the entry for the day, or updates it if one already exists. */
    @Transactional
    public PresenceResponse save(LocalDate date, PresenceRequest request) {
        String note = normalizeNote(request.note());
        PresenceEntry entry = repository.findByDate(date)
                .map(existing -> {
                    existing.setStatus(request.status());
                    existing.setNote(note);
                    return existing;
                })
                .orElseGet(() -> new PresenceEntry(date, request.status(), note));
        return PresenceResponse.from(repository.save(entry));
    }

    /** Clears the day back to "not set". Deleting a day that has no entry is not an error. */
    @Transactional
    public void clear(LocalDate date) {
        repository.deleteByDate(date);
    }

    private static String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
