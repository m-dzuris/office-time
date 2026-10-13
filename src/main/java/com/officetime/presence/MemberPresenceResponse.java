package com.officetime.presence;

import java.time.LocalDate;

/** What a group sees of a member's day: the status only. Notes stay private to their author. */
public record MemberPresenceResponse(Long userId, LocalDate date, PresenceStatus status) {

    static MemberPresenceResponse from(PresenceEntry entry) {
        return new MemberPresenceResponse(entry.getUserId(), entry.getDate(), entry.getStatus());
    }
}
