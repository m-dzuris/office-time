package com.officetime.auth;

import com.officetime.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    @Test
    void registeringSignsYouIn() throws Exception {
        MockHttpSession ada = register("ada@example.com", "Ada");

        getAs(ada, "/api/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.displayName").value("Ada"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void emailsAreStoredInLowerCase() throws Exception {
        MockHttpSession ada = register("Ada@Example.COM", "Ada");

        getAs(ada, "/api/auth/me").andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void meIsEmptyWhenNobodyIsSignedIn() throws Exception {
        getAs(null, "/api/auth/me").andExpect(status().isNoContent());
    }

    @Test
    void anEmailCanOnlyRegisterOnce_whateverItsCase() throws Exception {
        register("ada@example.com", "Ada");

        postAs(null, "/api/auth/register",
                "{\"email\":\"ADA@example.com\",\"displayName\":\"Other\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists."));
    }

    @Test
    void shortPasswordsAreRejected() throws Exception {
        postAs(null, "/api/auth/register",
                "{\"email\":\"ada@example.com\",\"displayName\":\"Ada\",\"password\":\"short\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordsLongerThanBcryptCanReadAreRejected() throws Exception {
        String tooLong = "é".repeat(40); // 40 characters but 80 bytes
        postAs(null, "/api/auth/register",
                "{\"email\":\"ada@example.com\",\"displayName\":\"Ada\",\"password\":\"" + tooLong + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidEmailOrMissingNameIsRejected() throws Exception {
        postAs(null, "/api/auth/register",
                "{\"email\":\"not-an-email\",\"displayName\":\"Ada\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isBadRequest());
        postAs(null, "/api/auth/register",
                "{\"email\":\"ada@example.com\",\"displayName\":\"  \",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void signingInWithTheRightPasswordWorks_andIgnoresEmailCase() throws Exception {
        register("ada@example.com", "Ada");

        var login = postAs(null, "/api/auth/login",
                "{\"email\":\"Ada@Example.com\",\"password\":\"" + PASSWORD + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ada"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        getAs(session, "/api/auth/me").andExpect(status().isOk());
    }

    @Test
    void aWrongPasswordAndAnUnknownEmailLookTheSame() throws Exception {
        register("ada@example.com", "Ada");

        postAs(null, "/api/auth/login", "{\"email\":\"ada@example.com\",\"password\":\"not the password\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Wrong email or password."));
        postAs(null, "/api/auth/login", "{\"email\":\"nobody@example.com\",\"password\":\"whatever it is\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Wrong email or password."));
    }

    @Test
    void signingOutEndsTheSession() throws Exception {
        MockHttpSession ada = register("ada@example.com", "Ada");

        postAs(ada, "/api/auth/logout", null).andExpect(status().isNoContent());

        getAs(ada, "/api/presence?from=2026-10-05&to=2026-10-11").andExpect(status().isUnauthorized());
    }

    /**
     * The page asks for this cookie with a plain GET (on load, and again after signing out, which deletes it)
     * and then sends its value back in the X-XSRF-TOKEN header.
     */
    @Test
    void anyReadOfTheApiHandsOutTheCsrfCookie() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/auth/me")).andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void theApiNeedsASignedInUser() throws Exception {
        getAs(null, "/api/presence?from=2026-10-05&to=2026-10-11").andExpect(status().isUnauthorized());
        getAs(null, "/api/groups").andExpect(status().isUnauthorized());
        getAs(null, "/api/invitations").andExpect(status().isUnauthorized());
    }

    @Test
    void changingRequestsWithoutACsrfTokenAreRefused() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@example.com\",\"displayName\":\"Ada\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden());

        MockHttpSession ada = register("ada@example.com", "Ada");
        mvc.perform(MockMvcRequestBuilders.put("/api/presence/2026-10-07")
                        .session(ada)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFFICE\"}"))
                .andExpect(status().isForbidden());

        // and the same request goes through once the token is attached
        mvc.perform(MockMvcRequestBuilders.put("/api/presence/2026-10-07")
                        .session(ada)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFFICE\"}"))
                .andExpect(status().isOk());
    }
}
