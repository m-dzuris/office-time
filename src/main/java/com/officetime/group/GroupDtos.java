package com.officetime.group;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class GroupDtos {

    private GroupDtos() {
    }

    public record CreateGroupRequest(@NotBlank @Size(max = 80) String name) {
    }

    public record JoinGroupRequest(@NotBlank @Size(max = 64) String code) {
    }

    public record InviteRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    /** One row in "my groups". */
    public record GroupSummary(Long id, String name, GroupRole role, int memberCount) {
    }

    public record MemberResponse(Long userId, String displayName, GroupRole role) {
    }

    /** An invitation still waiting for an answer, as the group sees it. */
    public record PendingInvitationResponse(Long id, String email, String invitedBy) {
    }

    /** Everything a member can see about a group. Only members ever receive this, join code included. */
    public record GroupDetail(Long id, String name, GroupRole role, String joinCode,
                              List<MemberResponse> members, List<PendingInvitationResponse> invitations) {
    }

    /** An invitation as the invited person sees it. */
    public record MyInvitationResponse(Long id, Long groupId, String groupName, String invitedBy, Instant createdAt) {
    }
}
