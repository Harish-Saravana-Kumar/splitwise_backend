package com.splitwise.repositories;

import com.splitwise.models.GroupMembershipRequest;
import com.splitwise.models.enums.GroupMembershipRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMembershipRequestRepository extends JpaRepository<GroupMembershipRequest, Long> {

    boolean existsByGroup_IdAndInvitedUser_IdAndStatus(Long groupId, Long invitedUserId, GroupMembershipRequestStatus status);

    List<GroupMembershipRequest> findByInvitedUser_IdAndStatusOrderByCreatedAtDesc(Long invitedUserId, GroupMembershipRequestStatus status);

    Optional<GroupMembershipRequest> findByIdAndInvitedUser_Id(Long requestId, Long invitedUserId);
}
