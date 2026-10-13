package com.officetime.presence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:office-time-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PresenceApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PresenceRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void savedDayShowsUpInRangeQuery() throws Exception {
        mvc.perform(put("/api/presence/2026-10-07")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFFICE\",\"note\":\"Team lunch\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-10-07"))
                .andExpect(jsonPath("$.status").value("OFFICE"))
                .andExpect(jsonPath("$.note").value("Team lunch"));

        mvc.perform(get("/api/presence").param("from", "2026-10-05").param("to", "2026-10-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].date").value("2026-10-07"));
    }

    @Test
    void rangeQueryExcludesDaysOutsideTheRange() throws Exception {
        save("2026-10-04", "OFFICE");
        save("2026-10-05", "REMOTE");
        save("2026-10-12", "AWAY");

        mvc.perform(get("/api/presence").param("from", "2026-10-05").param("to", "2026-10-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("REMOTE"));
    }

    @Test
    void savingTheSameDayAgainUpdatesInsteadOfDuplicating() throws Exception {
        save("2026-10-08", "OFFICE");
        mvc.perform(put("/api/presence/2026-10-08")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REMOTE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMOTE"));

        mvc.perform(get("/api/presence").param("from", "2026-10-08").param("to", "2026-10-08"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("REMOTE"));
    }

    @Test
    void blankNoteIsStoredAsNull() throws Exception {
        mvc.perform(put("/api/presence/2026-10-08")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFFICE\",\"note\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.note").isEmpty());
    }

    @Test
    void clearingADayRemovesItAndIsIdempotent() throws Exception {
        save("2026-10-09", "OFFICE");

        mvc.perform(delete("/api/presence/2026-10-09")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/presence/2026-10-09")).andExpect(status().isNoContent());

        mvc.perform(get("/api/presence").param("from", "2026-10-09").param("to", "2026-10-09"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void unknownStatusIsRejected() throws Exception {
        mvc.perform(put("/api/presence/2026-10-07")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BEACH\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingStatusIsRejected() throws Exception {
        mvc.perform(put("/api/presence/2026-10-07")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"hi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlongNoteIsRejected() throws Exception {
        String note = "x".repeat(PresenceEntry.NOTE_MAX_LENGTH + 1);
        mvc.perform(put("/api/presence/2026-10-07")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFFICE\",\"note\":\"" + note + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedDateIsRejected() throws Exception {
        mvc.perform(put("/api/presence/07-10-2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFFICE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reversedRangeIsRejected() throws Exception {
        mvc.perform(get("/api/presence").param("from", "2026-10-11").param("to", "2026-10-05"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedRangeIsRejected() throws Exception {
        mvc.perform(get("/api/presence").param("from", "2026-01-01").param("to", "2026-12-31"))
                .andExpect(status().isBadRequest());
    }

    private void save(String date, String status) throws Exception {
        mvc.perform(put("/api/presence/" + date)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }
}
