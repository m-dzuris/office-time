package com.officetime.presence;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PresenceRequest(
        @NotNull PresenceStatus status,
        @Size(max = PresenceEntry.NOTE_MAX_LENGTH) String note) {
}
