package com.officetime.group;

import com.officetime.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupApiTest extends ApiTestSupport {

    private static final String WEEK = "from=2026-10-05&to=2026-10-11";

    private MockHttpSession ada;
    private MockHttpSession bob;

    @BeforeEach
    void signIn() throws Exception {
        ada = register("ada@example.com", "Ada");
        bob = register("bob@example.com", "Bob");
    }

    // ---- creating and joining ---------------------------------------------------------------------------------

    @Test
    void creatingAGroupMakesYouItsOwner() throws Exception {
        postAs(ada, "/api/groups", "{\"name\":\"  Design team \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Design team"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.joinCode").value(matchesPattern("[A-HJ-NP-Z2-9]{12}")))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.members[0].displayName").value("Ada"));

        getAs(ada, "/api/groups")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Design team"))
                .andExpect(jsonPath("$[0].memberCount").value(1));
    }

    @Test
    void aGroupNeedsAName() throws Exception {
        postAs(ada, "/api/groups", "{\"name\":\"   \"}").andExpect(status().isBadRequest());
    }

    @Test
    void joiningWithTheCodeAddsYouAsAMember_evenIfTypedLooselyWithDashes() throws Exception {
        var created = postAs(ada, "/api/groups", "{\"name\":\"Design\"}");
        String code = stringAt(created, "$.joinCode");
        String typed = (code.substring(0, 4) + "-" + code.substring(4, 8) + " " + code.substring(8)).toLowerCase();

        postAs(bob, "/api/groups/join", "{\"code\":\"" + typed + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.members.length()").value(2));

        getAs(bob, "/api/groups").andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void aWrongCodeFindsNothing() throws Exception {
        postAs(bob, "/api/groups/join", "{\"code\":\"AAAAAAAAAAAA\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No group has that code."));
    }

    // ---- who can see a group ----------------------------------------------------------------------------------

    @Test
    void aGroupYouAreNotInLooksLikeItDoesNotExist() throws Exception {
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Design\"}"), "$.id");

        getAs(bob, "/api/groups/" + id).andExpect(status().isNotFound());
        getAs(bob, "/api/groups/" + id + "/presence?" + WEEK).andExpect(status().isNotFound());
        postAs(bob, "/api/groups/" + id + "/invitations", "{\"email\":\"x@example.com\"}")
                .andExpect(status().isNotFound());
        deleteAs(bob, "/api/groups/" + id).andExpect(status().isNotFound());
        getAs(bob, "/api/groups").andExpect(jsonPath("$.length()").value(0));
    }

    // ---- invitations ------------------------------------------------------------------------------------------

    @Test
    void anInvitedPersonSeesItAndCanAccept() throws Exception {
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Design\"}"), "$.id");

        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"Bob@Example.com\"}")
                .andExpect(status().isNoContent());
        getAs(ada, "/api/groups/" + id)
                .andExpect(jsonPath("$.invitations.length()").value(1))
                .andExpect(jsonPath("$.invitations[0].email").value("bob@example.com"))
                .andExpect(jsonPath("$.invitations[0].invitedBy").value("Ada"));

        var mine = getAs(bob, "/api/invitations")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].groupName").value("Design"))
                .andExpect(jsonPath("$[0].invitedBy").value("Ada"));
        long invitationId = longAt(mine, "$[0].id");

        postAs(bob, "/api/invitations/" + invitationId + "/accept", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Design"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.memberCount").value(2));

        getAs(bob, "/api/invitations").andExpect(jsonPath("$.length()").value(0));
        getAs(ada, "/api/groups/" + id)
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.invitations.length()").value(0));
    }

    @Test
    void anInvitationWaitsForSomeoneWhoRegistersLater() throws Exception {
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Design\"}"), "$.id");
        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"late@example.com\"}")
                .andExpect(status().isNoContent());

        MockHttpSession late = register("late@example.com", "Late");

        getAs(late, "/api/invitations")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].groupName").value("Design"));
    }

    @Test
    void anInvitationIsOnlyVisibleToAndUsableByItsAddressee() throws Exception {
        MockHttpSession cy = register("cy@example.com", "Cy");
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Design\"}"), "$.id");
        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"bob@example.com\"}")
                .andExpect(status().isNoContent());
        long invitationId = longAt(getAs(bob, "/api/invitations"), "$[0].id");

        getAs(cy, "/api/invitations").andExpect(jsonPath("$.length()").value(0));
        postAs(cy, "/api/invitations/" + invitationId + "/accept", null).andExpect(status().isNotFound());
        deleteAs(cy, "/api/invitations/" + invitationId).andExpect(status().isNotFound());
    }

    @Test
    void anInvitationCanBeDeclined() throws Exception {
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Design\"}"), "$.id");
        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"bob@example.com\"}");
        long invitationId = longAt(getAs(bob, "/api/invitations"), "$[0].id");

        deleteAs(bob, "/api/invitations/" + invitationId).andExpect(status().isNoContent());

        getAs(bob, "/api/invitations").andExpect(jsonPath("$.length()").value(0));
        getAs(bob, "/api/groups").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aMemberCanRevokeAPendingInvitation() throws Exception {
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Design\"}"), "$.id");
        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"bob@example.com\"}");
        long invitationId = longAt(getAs(ada, "/api/groups/" + id), "$.invitations[0].id");

        deleteAs(ada, "/api/groups/" + id + "/invitations/" + invitationId).andExpect(status().isNoContent());

        getAs(bob, "/api/invitations").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invitingSomeoneAlreadyInTheGroupChangesNothingAndSaysNothing() throws Exception {
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Design\"}"), "$.id");
        postAs(bob, "/api/groups/join", "{\"code\":\"" + stringAt(getAs(ada, "/api/groups/" + id), "$.joinCode") + "\"}");

        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"bob@example.com\"}")
                .andExpect(status().isNoContent());
        // inviting the same address twice is also a quiet no-op
        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"new@example.com\"}")
                .andExpect(status().isNoContent());
        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"new@example.com\"}")
                .andExpect(status().isNoContent());

        getAs(ada, "/api/groups/" + id)
                .andExpect(jsonPath("$.invitations.length()").value(1))
                .andExpect(jsonPath("$.invitations[0].email").value("new@example.com"));
    }

    @Test
    void joiningWithTheCodeClearsYourPendingInvitation() throws Exception {
        var created = postAs(ada, "/api/groups", "{\"name\":\"Design\"}");
        long id = longAt(created, "$.id");
        String code = stringAt(created, "$.joinCode");
        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"bob@example.com\"}");

        postAs(bob, "/api/groups/join", "{\"code\":\"" + code + "\"}").andExpect(status().isOk());

        getAs(bob, "/api/invitations").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void somethingThatIsNotAnEmailCannotBeInvited() throws Exception {
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Design\"}"), "$.id");

        postAs(ada, "/api/groups/" + id + "/invitations", "{\"email\":\"nope\"}").andExpect(status().isBadRequest());
    }

    // ---- what a group lets you see ----------------------------------------------------------------------------

    @Test
    void membersSeeEachOthersStatusButNeverTheirNotes() throws Exception {
        long id = joinTogether("Design");
        long bobId = longAt(getAs(bob, "/api/auth/me"), "$.id");
        putAs(bob, "/api/presence/2026-10-07", "{\"status\":\"OFFICE\",\"note\":\"Dentist at 3\"}")
                .andExpect(status().isOk());

        getAs(ada, "/api/groups/" + id + "/presence?" + WEEK)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(bobId))
                .andExpect(jsonPath("$[0].date").value("2026-10-07"))
                .andExpect(jsonPath("$[0].status").value("OFFICE"))
                .andExpect(jsonPath("$[0].note").doesNotExist());
    }

    @Test
    void oneDecisionIsSharedWithEveryGroupYouAreIn() throws Exception {
        MockHttpSession cy = register("cy@example.com", "Cy");
        long withAda = joinTogether("Design");
        var second = postAs(cy, "/api/groups", "{\"name\":\"Lunch club\"}");
        long withCy = longAt(second, "$.id");
        postAs(bob, "/api/groups/join", "{\"code\":\"" + stringAt(second, "$.joinCode") + "\"}")
                .andExpect(status().isOk());

        // Bob decides once...
        putAs(bob, "/api/presence/2026-10-07", "{\"status\":\"REMOTE\"}").andExpect(status().isOk());

        // ...and both groups see that same decision.
        getAs(ada, "/api/groups/" + withAda + "/presence?" + WEEK)
                .andExpect(jsonPath("$[0].status").value("REMOTE"));
        getAs(cy, "/api/groups/" + withCy + "/presence?" + WEEK)
                .andExpect(jsonPath("$[0].status").value("REMOTE"));

        // Cy is not in Ada's group, so Bob's days there are not visible to Cy.
        getAs(cy, "/api/groups/" + withAda + "/presence?" + WEEK).andExpect(status().isNotFound());
    }

    @Test
    void leavingAGroupStopsItSeeingYouButKeepsYourDays() throws Exception {
        long id = joinTogether("Design");
        long bobId = longAt(getAs(bob, "/api/auth/me"), "$.id");
        putAs(bob, "/api/presence/2026-10-07", "{\"status\":\"OFFICE\"}").andExpect(status().isOk());

        deleteAs(bob, "/api/groups/" + id + "/members/" + bobId).andExpect(status().isNoContent());

        getAs(ada, "/api/groups/" + id + "/presence?" + WEEK).andExpect(jsonPath("$.length()").value(0));
        getAs(bob, "/api/groups/" + id).andExpect(status().isNotFound());
        getAs(bob, "/api/presence?" + WEEK)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("OFFICE"));
    }

    // ---- owner powers -----------------------------------------------------------------------------------------

    @Test
    void onlyTheOwnerCanRemoveMembersChangeTheCodeOrDeleteTheGroup() throws Exception {
        long id = joinTogether("Design");
        long adaId = longAt(getAs(ada, "/api/auth/me"), "$.id");
        long bobId = longAt(getAs(bob, "/api/auth/me"), "$.id");

        deleteAs(bob, "/api/groups/" + id + "/members/" + adaId).andExpect(status().isForbidden());
        postAs(bob, "/api/groups/" + id + "/join-code", null).andExpect(status().isForbidden());
        deleteAs(bob, "/api/groups/" + id).andExpect(status().isForbidden());

        deleteAs(ada, "/api/groups/" + id + "/members/" + bobId).andExpect(status().isNoContent());
        getAs(bob, "/api/groups/" + id).andExpect(status().isNotFound());
    }

    @Test
    void aNewJoinCodeInvalidatesTheOldOne() throws Exception {
        var created = postAs(ada, "/api/groups", "{\"name\":\"Design\"}");
        long id = longAt(created, "$.id");
        String oldCode = stringAt(created, "$.joinCode");

        var rotated = postAs(ada, "/api/groups/" + id + "/join-code", null).andExpect(status().isOk());
        String newCode = stringAt(rotated, "$.joinCode");

        postAs(bob, "/api/groups/join", "{\"code\":\"" + oldCode + "\"}").andExpect(status().isNotFound());
        postAs(bob, "/api/groups/join", "{\"code\":\"" + newCode + "\"}").andExpect(status().isOk());
    }

    @Test
    void whenTheOwnerLeavesTheLongestStandingMemberTakesOver() throws Exception {
        long id = joinTogether("Design");
        long adaId = longAt(getAs(ada, "/api/auth/me"), "$.id");

        deleteAs(ada, "/api/groups/" + id + "/members/" + adaId).andExpect(status().isNoContent());

        getAs(bob, "/api/groups/" + id)
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    void theLastPersonLeavingClosesTheGroup() throws Exception {
        long id = longAt(postAs(ada, "/api/groups", "{\"name\":\"Solo\"}"), "$.id");
        long adaId = longAt(getAs(ada, "/api/auth/me"), "$.id");

        deleteAs(ada, "/api/groups/" + id + "/members/" + adaId).andExpect(status().isNoContent());

        getAs(ada, "/api/groups").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theOwnerCanDeleteTheGroupWithoutTouchingAnyonesDays() throws Exception {
        long id = joinTogether("Design");
        putAs(bob, "/api/presence/2026-10-07", "{\"status\":\"OFFICE\"}").andExpect(status().isOk());

        deleteAs(ada, "/api/groups/" + id).andExpect(status().isNoContent());

        getAs(ada, "/api/groups").andExpect(jsonPath("$.length()").value(0));
        getAs(bob, "/api/groups").andExpect(jsonPath("$.length()").value(0));
        getAs(bob, "/api/presence?" + WEEK).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void groupsAreListedAlphabetically() throws Exception {
        postAs(ada, "/api/groups", "{\"name\":\"zebra\"}");
        postAs(ada, "/api/groups", "{\"name\":\"Apple\"}");

        getAs(ada, "/api/groups")
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[1].name").value("zebra"))
                .andExpect(jsonPath("$[*].role", hasItem("OWNER")));
    }

    /** Ada creates a group and Bob joins it with the code. Returns the group id. */
    private long joinTogether(String name) throws Exception {
        var created = postAs(ada, "/api/groups", "{\"name\":\"" + name + "\"}").andExpect(status().isCreated());
        postAs(bob, "/api/groups/join", "{\"code\":\"" + stringAt(created, "$.joinCode") + "\"}")
                .andExpect(status().isOk());
        return longAt(created, "$.id");
    }
}
