package com.officetime.group;

import com.officetime.auth.AppUserPrincipal;
import com.officetime.group.GroupDtos.GroupSummary;
import com.officetime.group.GroupDtos.MyInvitationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Invitations addressed to the signed-in user's email address. */
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final GroupService groups;

    public InvitationController(GroupService groups) {
        this.groups = groups;
    }

    @GetMapping
    public List<MyInvitationResponse> mine(@AuthenticationPrincipal AppUserPrincipal me) {
        return groups.myInvitations(me);
    }

    @PostMapping("/{id}/accept")
    public GroupSummary accept(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return groups.acceptInvitation(me, id);
    }

    /** Declining removes the invitation. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        groups.declineInvitation(me, id);
    }
}
