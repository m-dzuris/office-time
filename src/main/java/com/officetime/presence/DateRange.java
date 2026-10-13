package com.officetime.presence;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateRange {

    /** A week view needs 7 days; this leaves room for a month view later without allowing unbounded reads. */
    public static final int MAX_DAYS = 92;

    private DateRange() {
    }

    /** Rejects a reversed range, or one longer than {@link #MAX_DAYS} days (both ends inclusive). */
    public static void check(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date range must not exceed " + MAX_DAYS + " days");
        }
    }
}
