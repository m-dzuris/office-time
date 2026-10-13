package com.officetime.presence;

import com.officetime.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PresenceApiTest extends ApiTestSupport {

    private MockHttpSession ada;

    @BeforeEach
    void signIn() throws Exception {
        ada = register("ada@example.com", "Ada");
    }

    @Test
    void savedDayShowsUpInRangeQuery() throws Exception {
        putAs(ada, "/api/presence/2026-10-07", "{\"status\":\"OFFICE\",\"note\":\"Team lunch\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-07"))
                .andExpect(jsonPath("$.status").value("OFFICE"))
                .andExpect(jsonPath("$.note").value("Team lunch"));

        getAs(ada, "/api/presence?from=2026-10-05&to=2026-10-11")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].date").value("2026-10-07"));
    }

    @Test
    void rangeQueryExcludesDaysOutsideTheRange() throws Exception {
        save("2026-10-04", "OFFICE");
        save("2026-10-05", "REMOTE");
        save("2026-10-12", "AWAY");

        getAs(ada, "/api/presence?from=2026-10-05&to=2026-10-11")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("REMOTE"));
    }

    @Test
    void savingTheSameDayAgainUpdatesInsteadOfDuplicating() throws Exception {
        save("2026-10-08", "OFFICE");
        putAs(ada, "/api/presence/2026-10-08", "{\"status\":\"REMOTE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOTE"));

        getAs(ada, "/api/presence?from=2026-10-08&to=2026-10-08")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("REMOTE"));
    }

    @Test
    void blankNoteIsStoredAsNoNote() throws Exception {
        putAs(ada, "/api/presence/2026-10-08", "{\"status\":\"OFFICE\",\"note\":\"   \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").isEmpty());
    }

    @Test
    void clearingADayRemovesItAndIsIdempotent() throws Exception {
        save("2026-10-09", "OFFICE");

        deleteAs(ada, "/api/presence/2026-10-09").andExpect(status().isNoContent());
        deleteAs(ada, "/api/presence/2026-10-09").andExpect(status().isNoContent());

        getAs(ada, "/api/presence?from=2026-10-09&to=2026-10-09").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void everyPersonHasTheirOwnCalendar() throws Exception {
        MockHttpSession bob = register("bob@example.com", "Bob");
        save("2026-10-07", "OFFICE");

        getAs(bob, "/api/presence?from=2026-10-05&to=2026-10-11").andExpect(jsonPath("$.length()").value(0));

        putAs(bob, "/api/presence/2026-10-07", "{\"status\":\"AWAY\"}").andExpect(status().isOk());

        getAs(ada, "/api/presence?from=2026-10-07&to=2026-10-07")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("OFFICE"));
        getAs(bob, "/api/presence?from=2026-10-07&to=2026-10-07")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("AWAY"));

        deleteAs(bob, "/api/presence/2026-10-07").andExpect(status().isNoContent());
        getAs(ada, "/api/presence?from=2026-10-07&to=2026-10-07").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unknownStatusIsRejected() throws Exception {
        putAs(ada, "/api/presence/2026-10-07", "{\"status\":\"BEACH\"}").andExpect(status().isBadRequest());
    }

    @Test
    void missingStatusIsRejected() throws Exception {
        putAs(ada, "/api/presence/2026-10-07", "{\"note\":\"hi\"}").andExpect(status().isBadRequest());
    }

    @Test
    void overlongNoteIsRejected() throws Exception {
        String note = "x".repeat(PresenceEntry.NOTE_MAX_LENGTH + 1);
        putAs(ada, "/api/presence/2026-10-07", "{\"status\":\"OFFICE\",\"note\":\"" + note + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedDateIsRejected() throws Exception {
        putAs(ada, "/api/presence/07-10-2026", "{\"status\":\"OFFICE\"}").andExpect(status().isBadRequest());
    }

    @Test
    void reversedRangeIsRejected() throws Exception {
        getAs(ada, "/api/presence?from=2026-10-11&to=2026-10-05").andExpect(status().isBadRequest());
    }

    @Test
    void oversizedRangeIsRejected() throws Exception {
        getAs(ada, "/api/presence?from=2026-01-01&to=2026-12-31").andExpect(status().isBadRequest());
    }

    private void save(String date, String status) throws Exception {
        putAs(ada, "/api/presence/" + date, "{\"status\":\"" + status + "\"}").andExpect(status().isOk());
    }
}
