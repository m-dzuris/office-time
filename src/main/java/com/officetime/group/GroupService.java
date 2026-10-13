package com.officetime.group;

import com.officetime.auth.AppUser;
import com.officetime.auth.AppUserPrincipal;
import com.officetime.auth.AppUserRepository;
import com.officetime.auth.Emails;
import com.officetime.group.GroupDtos.GroupDetail;
import com.officetime.group.GroupDtos.GroupSummary;
import com.officetime.group.GroupDtos.MemberResponse;
import com.officetime.group.GroupDtos.MyInvitationResponse;
import com.officetime.group.GroupDtos.PendingInvitationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Groups only control who may see whose days. They never hold presence data themselves.
 *
 * <p>Rules: any signed-in user can create a group (and becomes its owner), join with its code, or accept an
 * invitation. Any member can invite. Only the owner can remove other members, change the join code or delete
 * the group. A group that you are not in looks exactly like a group that does not exist (404).
 */
@Service
@Transactional
public class GroupService {

    static final int MAX_PENDING_INVITATIONS = 50;

    // No 0/O or 1/I, so a code read aloud or copied by hand is hard to get wrong.
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 12; // 32^12 is about 2^60 possibilities

    private final SecureRandom random = new SecureRandom();
    private final OfficeGroupRepository groups;
    private final MembershipRepository memberships;
    private final InvitationRepository invitations;
    private final AppUserRepository users;

    public GroupService(OfficeGroupRepository groups, MembershipRepository memberships,
                        InvitationRepository invitations, AppUserRepository users) {
        this.groups = groups;
        this.memberships = memberships;
        this.invitations = invitations;
        this.users = users;
    }

    // ---- groups -------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<GroupSummary> myGroups(Long userId) {
        List<Membership> mine = memberships.findByUserId(userId);
        Map<Long, OfficeGroup> byId = groups.findAllById(mine.stream().map(Membership::getGroupId).toList())
                .stream().collect(Collectors.toMap(OfficeGroup::getId, group -> group));
        return mine.stream()
                .filter(membership -> byId.containsKey(membership.getGroupId()))
                .map(membership -> summary(byId.get(membership.getGroupId()), membership.getRole()))
                .sorted(Comparator.comparing(GroupSummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public GroupDetail create(AppUserPrincipal me, String name) {
        OfficeGroup group = groups.save(new OfficeGroup(name.strip(), newJoinCode()));
        Membership mine = memberships.save(new Membership(group.getId(), me.getId(), GroupRole.OWNER));
        return detail(group, mine);
    }

    public GroupDetail join(AppUserPrincipal me, String rawCode) {
        String code = rawCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        OfficeGroup group = groups.findByJoinCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No group has that code."));
        Membership mine = memberships.findByGroupIdAndUserId(group.getId(), me.getId())
                .orElseGet(() -> memberships.save(new Membership(group.getId(), me.getId(), GroupRole.MEMBER)));
        invitations.deleteByGroupIdAndInvitedEmail(group.getId(), me.getUsername()); // joined, so no need to answer it
        return detail(group, mine);
    }

    @Transactional(readOnly = true)
    public GroupDetail get(AppUserPrincipal me, Long groupId) {
        Membership mine = requireMembership(groupId, me.getId());
        return detail(loadGroup(groupId), mine);
    }

    public GroupDetail rotateJoinCode(AppUserPrincipal me, Long groupId) {
        Membership mine = requireMembership(groupId, me.getId());
        requireOwner(mine);
        OfficeGroup group = loadGroup(groupId);
        group.setJoinCode(newJoinCode());
        return detail(groups.save(group), mine);
    }

    public void delete(AppUserPrincipal me, Long groupId) {
        requireOwner(requireMembership(groupId, me.getId()));
        deleteEverything(groupId);
    }

    /** The ids of everyone in the group, for reading their days. Fails with 404 unless the caller is a member. */
    @Transactional(readOnly = true)
    public List<Long> memberIds(AppUserPrincipal me, Long groupId) {
        requireMembership(groupId, me.getId());
        return memberships.findByGroupIdOrderByJoinedAtAscIdAsc(groupId).stream().map(Membership::getUserId).toList();
    }

    // ---- members ------------------------------------------------------------------------------------------

    /** Leaves the group (when {@code userId} is the caller) or, for the owner, removes another member. */
    public void removeMember(AppUserPrincipal me, Long groupId, Long userId) {
        Membership mine = requireMembership(groupId, me.getId());
        if (userId.equals(me.getId())) {
            leave(groupId, mine);
            return;
        }
        requireOwner(mine);
        Membership target = memberships.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "That person isn't in this group."));
        memberships.delete(target);
    }

    private void leave(Long groupId, Membership mine) {
        List<Membership> others = memberships.findByGroupIdOrderByJoinedAtAscIdAsc(groupId).stream()
                .filter(membership -> !membership.getUserId().equals(mine.getUserId()))
                .toList();
        if (others.isEmpty()) {
            deleteEverything(groupId); // the last person leaving closes the group
            return;
        }
        if (mine.getRole() == GroupRole.OWNER) {
            Membership next = others.get(0); // ownership passes to whoever has been in the group longest
            next.setRole(GroupRole.OWNER);
            memberships.save(next);
        }
        memberships.delete(mine);
    }

    // ---- invitations --------------------------------------------------------------------------------------

    /**
     * Invites an email address. The result is the same whether or not that address has an account or is
     * already in the group, so inviting can't be used to find out who uses the app.
     */
    public void invite(AppUserPrincipal me, Long groupId, String rawEmail) {
        requireMembership(groupId, me.getId());
        String email = Emails.normalize(rawEmail);

        boolean alreadyMember = users.findByEmail(email)
                .map(user -> memberships.existsByGroupIdAndUserId(groupId, user.getId()))
                .orElse(false);
        if (alreadyMember || invitations.findByGroupIdAndInvitedEmail(groupId, email).isPresent()) {
            return;
        }
        if (invitations.countByGroupId(groupId) >= MAX_PENDING_INVITATIONS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This group has too many pending invitations. Remove some before inviting more.");
        }
        invitations.save(new Invitation(groupId, email, me.getId()));
    }

    public void revokeInvitation(AppUserPrincipal me, Long groupId, Long invitationId) {
        requireMembership(groupId, me.getId());
        Invitation invitation = invitations.findById(invitationId)
                .filter(found -> found.getGroupId().equals(groupId))
                .orElseThrow(this::invitationNotFound);
        invitations.delete(invitation);
    }

    @Transactional(readOnly = true)
    public List<MyInvitationResponse> myInvitations(AppUserPrincipal me) {
        List<Invitation> mine = invitations.findByInvitedEmailOrderByCreatedAtDescIdDesc(me.getUsername());
        Map<Long, OfficeGroup> byId = groups.findAllById(mine.stream().map(Invitation::getGroupId).toList())
                .stream().collect(Collectors.toMap(OfficeGroup::getId, group -> group));
        Map<Long, String> names = names(mine.stream().map(Invitation::getInvitedByUserId).toList());
        return mine.stream()
                .filter(invitation -> byId.containsKey(invitation.getGroupId()))
                .map(invitation -> new MyInvitationResponse(
                        invitation.getId(),
                        invitation.getGroupId(),
                        byId.get(invitation.getGroupId()).getName(),
                        names.getOrDefault(invitation.getInvitedByUserId(), "Someone"),
                        invitation.getCreatedAt()))
                .toList();
    }

    public GroupSummary acceptInvitation(AppUserPrincipal me, Long invitationId) {
        Invitation invitation = findMyInvitation(me, invitationId);
        OfficeGroup group = loadGroup(invitation.getGroupId());
        Membership mine = memberships.findByGroupIdAndUserId(group.getId(), me.getId())
                .orElseGet(() -> memberships.save(new Membership(group.getId(), me.getId(), GroupRole.MEMBER)));
        invitations.delete(invitation);
        return summary(group, mine.getRole());
    }

    public void declineInvitation(AppUserPrincipal me, Long invitationId) {
        invitations.delete(findMyInvitation(me, invitationId));
    }

    private Invitation findMyInvitation(AppUserPrincipal me, Long invitationId) {
        return invitations.findById(invitationId)
                .filter(found -> found.getInvitedEmail().equals(me.getUsername()))
                .orElseThrow(this::invitationNotFound);
    }

    // ---- helpers ------------------------------------------------------------------------------------------

    private Membership requireMembership(Long groupId, Long userId) {
        return memberships.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));
    }

    private static void requireOwner(Membership membership) {
        if (membership.getRole() != GroupRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group's owner can do that.");
        }
    }

    private OfficeGroup loadGroup(Long groupId) {
        return groups.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));
    }

    private ResponseStatusException invitationNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found.");
    }

    private void deleteEverything(Long groupId) {
        memberships.deleteByGroupId(groupId);
        invitations.deleteByGroupId(groupId);
        groups.deleteById(groupId);
    }

    private String newJoinCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (groups.existsByJoinCode(code));
        return code;
    }

    private GroupSummary summary(OfficeGroup group, GroupRole role) {
        return new GroupSummary(group.getId(), group.getName(), role, (int) memberships.countByGroupId(group.getId()));
    }

    private GroupDetail detail(OfficeGroup group, Membership mine) {
        List<Membership> members = memberships.findByGroupIdOrderByJoinedAtAscIdAsc(group.getId());
        List<Invitation> pending = invitations.findByGroupIdOrderByCreatedAtAscIdAsc(group.getId());

        Set<Long> userIds = new HashSet<>();
        members.forEach(member -> userIds.add(member.getUserId()));
        pending.forEach(invitation -> userIds.add(invitation.getInvitedByUserId()));
        Map<Long, String> names = names(userIds);

        return new GroupDetail(
                group.getId(),
                group.getName(),
                mine.getRole(),
                group.getJoinCode(),
                members.stream()
                        .map(member -> new MemberResponse(
                                member.getUserId(), names.getOrDefault(member.getUserId(), "Former member"), member.getRole()))
                        .toList(),
                pending.stream()
                        .map(invitation -> new PendingInvitationResponse(
                                invitation.getId(),
                                invitation.getInvitedEmail(),
                                names.getOrDefault(invitation.getInvitedByUserId(), "Someone")))
                        .toList());
    }

    private Map<Long, String> names(Collection<Long> userIds) {
        return users.findAllById(userIds).stream()
                .collect(Collectors.toMap(AppUser::getId, AppUser::getDisplayName));
    }
}
