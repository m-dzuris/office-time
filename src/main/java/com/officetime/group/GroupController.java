package com.officetime.group;

import com.officetime.auth.AppUserPrincipal;
import com.officetime.group.GroupDtos.CreateGroupRequest;
import com.officetime.group.GroupDtos.GroupDetail;
import com.officetime.group.GroupDtos.GroupSummary;
import com.officetime.group.GroupDtos.InviteRequest;
import com.officetime.group.GroupDtos.JoinGroupRequest;
import com.officetime.presence.DateRange;
import com.officetime.presence.MemberPresenceResponse;
import com.officetime.presence.PresenceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groups;
    private final PresenceService presence;

    public GroupController(GroupService groups, PresenceService presence) {
        this.groups = groups;
        this.presence = presence;
    }

    @GetMapping
    public List<GroupSummary> mine(@AuthenticationPrincipal AppUserPrincipal me) {
        return groups.myGroups(me.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDetail create(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody CreateGroupRequest body) {
        return groups.create(me, body.name());
    }

    @PostMapping("/join")
    public GroupDetail join(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody JoinGroupRequest body) {
        return groups.join(me, body.code());
    }

    @GetMapping("/{id}")
    public GroupDetail get(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return groups.get(me, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        groups.delete(me, id);
    }

    @PostMapping("/{id}/join-code")
    public GroupDetail newJoinCode(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id) {
        return groups.rotateJoinCode(me, id);
    }

    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invite(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
                       @Valid @RequestBody InviteRequest body) {
        groups.invite(me, id, body.email());
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
                                 @PathVariable Long invitationId) {
        groups.revokeInvitation(me, id, invitationId);
    }

    /** Leave the group (your own id) or, as the owner, remove someone else. */
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
                             @PathVariable Long userId) {
        groups.removeMember(me, id, userId);
    }

    /** Every member's statuses in the range. Members only; notes are never included. */
    @GetMapping("/{id}/presence")
    public List<MemberPresenceResponse> presence(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable Long id,
                                                 @RequestParam @DateTimeFormat(iso = DATE) LocalDate from,
                                                 @RequestParam @DateTimeFormat(iso = DATE) LocalDate to) {
        DateRange.check(from, to);
        return presence.findForUsers(groups.memberIds(me, id), from, to);
    }
}
