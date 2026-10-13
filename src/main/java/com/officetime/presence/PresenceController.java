package com.officetime.presence;

import com.officetime.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/** The signed-in user's own calendar. Nobody can read or change anyone else's days through these endpoints. */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService service;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    @GetMapping
    public List<PresenceResponse> list(@AuthenticationPrincipal AppUserPrincipal me,
                                       @RequestParam @DateTimeFormat(iso = DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DATE) LocalDate to) {
        DateRange.check(from, to);
        return service.findBetween(me.getId(), from, to);
    }

    @PutMapping("/{date}")
    public PresenceResponse save(@AuthenticationPrincipal AppUserPrincipal me,
                                 @PathVariable @DateTimeFormat(iso = DATE) LocalDate date,
                                 @Valid @RequestBody PresenceRequest request) {
        return service.save(me.getId(), date, request);
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal AppUserPrincipal me,
                      @PathVariable @DateTimeFormat(iso = DATE) LocalDate date) {
        service.clear(me.getId(), date);
    }
}
