package com.officetime.presence;

import java.time.LocalDate;

public record PresenceResponse(LocalDate date, PresenceStatus status, String note) {

    static PresenceResponse from(PresenceEntry entry) {
        return new PresenceResponse(entry.getDate(), entry.getStatus(), entry.getNote());
    }
}
