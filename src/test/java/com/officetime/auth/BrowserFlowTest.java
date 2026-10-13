package com.officetime.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Talks to a real running server the way the page does: a cookie jar, the XSRF-TOKEN cookie copied into the
 * X-XSRF-TOKEN header on every changing request, and a fresh token fetched when the cookie is missing.
 *
 * <p>The other API tests use MockMvc, which skips the real cookie handling. This one covers the part they
 * can't: signing up, signing out and signing back in with the real CSRF cookie round trip.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:office-time-browser-flow;DB_CLOSE_DELAY=-1")
class BrowserFlowTest {

    private static final String EMAIL = "flow@example.com";
    private static final String PASSWORD = "correct horse battery";

    @LocalServerPort
    private int port;

    @Autowired
    private AppUserRepository users;

    private final CookieManager cookies = new CookieManager();
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .cookieHandler(cookies)
            .build();

    @BeforeEach
    void cleanDatabase() {
        users.deleteAll();
    }

    @Test
    void signUpSignOutAndSignBackInWorkWithTheRealCsrfCookie() throws Exception {
        // Opening the page: a plain read, which is also what hands the page its token cookie.
        assertStatus(204, send("GET", "/api/auth/me", null));
        assertNotNull(csrfCookie(), "the first read should hand out an XSRF-TOKEN cookie");

        assertStatus(201, send("POST", "/api/auth/register", registerJson()));
        assertStatus(200, send("GET", "/api/auth/me", null));

        assertStatus(204, send("POST", "/api/auth/logout", null));
        assertStatus(204, send("GET", "/api/auth/me", null));

        // The page asks for a fresh token when its cookie is missing (signing out can delete it).
        if (csrfCookie() == null) {
            send("GET", "/api/auth/me", null);
        }
        assertNotNull(csrfCookie(), "a read after signing out should leave the page with a token cookie");

        assertStatus(200, send("POST", "/api/auth/login", loginJson()));
        assertStatus(200, send("GET", "/api/auth/me", null));
    }

    @Test
    void aChangingRequestWithAWrongTokenIsRefused() throws Exception {
        send("GET", "/api/auth/me", null); // gets the real cookie into the jar

        assertStatus(403, sendWithToken("POST", "/api/auth/register", registerJson(), "not-the-real-token"));
        assertStatus(403, sendWithToken("POST", "/api/auth/register", registerJson(), null));
    }

    // ---- helpers ----------------------------------------------------------------------------------------

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        return sendWithToken(method, path, json, "GET".equals(method) ? null : csrfCookie());
    }

    private HttpResponse<String> sendWithToken(String method, String path, String json, String token)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) {
            request.header("X-XSRF-TOKEN", token);
        }
        BodyPublisher body = BodyPublishers.noBody();
        if (json != null) {
            request.header("Content-Type", "application/json");
            body = BodyPublishers.ofString(json);
        }
        return client.send(request.method(method, body).build(), BodyHandlers.ofString());
    }

    /** The token cookie in the jar, or null when there isn't one. */
    private String csrfCookie() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private static void assertStatus(int expected, HttpResponse<String> response) {
        assertEquals(expected, response.statusCode(),
                response.request().method() + " " + response.request().uri().getPath() + " answered: " + response.body());
    }

    private static String registerJson() {
        return "{\"email\":\"" + EMAIL + "\",\"displayName\":\"Flow\",\"password\":\"" + PASSWORD + "\"}";
    }

    private static String loginJson() {
        return "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}";
    }
}
