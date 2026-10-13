package com.officetime;

import com.jayway.jsonpath.JsonPath;
import com.officetime.auth.AppUserRepository;
import com.officetime.group.InvitationRepository;
import com.officetime.group.MembershipRepository;
import com.officetime.group.OfficeGroupRepository;
import com.officetime.presence.PresenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for the API tests. A "person" is a MockHttpSession obtained by really registering through the
 * API, so the tests exercise the real security configuration. Pass null as the session to act signed out.
 *
 * <p>The helpers are called getAs/postAs/... because a method named get() here would hide the static imports
 * of MockMvcRequestBuilders in subclasses.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:office-time-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "correct horse battery";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    private PresenceRepository presence;
    @Autowired
    private MembershipRepository memberships;
    @Autowired
    private InvitationRepository invitations;
    @Autowired
    private OfficeGroupRepository groups;
    @Autowired
    private AppUserRepository users;

    @BeforeEach
    protected void cleanDatabase() {
        presence.deleteAll();
        memberships.deleteAll();
        invitations.deleteAll();
        groups.deleteAll();
        users.deleteAll();
    }

    /** Registers a new account and returns its signed-in session. */
    protected MockHttpSession register(String email, String displayName) throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"displayName\":\"" + displayName
                                + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected ResultActions getAs(MockHttpSession session, String url) throws Exception {
        return mvc.perform(withSession(MockMvcRequestBuilders.get(url), session));
    }

    protected ResultActions postAs(MockHttpSession session, String url, String json) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(url).with(csrf());
        return mvc.perform(withBody(withSession(request, session), json));
    }

    protected ResultActions putAs(MockHttpSession session, String url, String json) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.put(url).with(csrf());
        return mvc.perform(withBody(withSession(request, session), json));
    }

    protected ResultActions deleteAs(MockHttpSession session, String url) throws Exception {
        return mvc.perform(withSession(MockMvcRequestBuilders.delete(url).with(csrf()), session));
    }

    /** Reads a number such as "$.id" from a response body. */
    protected static long longAt(ResultActions result, String path) throws Exception {
        Number value = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
        return value.longValue();
    }

    protected static String stringAt(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    private static MockHttpServletRequestBuilder withSession(MockHttpServletRequestBuilder request, MockHttpSession session) {
        return session == null ? request : request.session(session);
    }

    private static MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder request, String json) {
        return json == null ? request : request.contentType(MediaType.APPLICATION_JSON).content(json);
    }
}
