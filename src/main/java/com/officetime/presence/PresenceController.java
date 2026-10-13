package com.officetime.presence;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    /** A week view needs 7 days; this leaves room for a month view later without allowing unbounded reads. */
    static final int MAX_RANGE_DAYS = 92;

    private final PresenceService service;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    @GetMapping
    public List<PresenceResponse> list(@RequestParam @DateTimeFormat(iso = DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DATE) LocalDate to) {
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        return service.findBetween(from, to);
    }

    @PutMapping("/{date}")
    public PresenceResponse save(@PathVariable @DateTimeFormat(iso = DATE) LocalDate date,
                                 @Valid @RequestBody PresenceRequest request) {
        return service.save(date, request);
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable @DateTimeFormat(iso = DATE) LocalDate date) {
        service.clear(date);
    }
}
