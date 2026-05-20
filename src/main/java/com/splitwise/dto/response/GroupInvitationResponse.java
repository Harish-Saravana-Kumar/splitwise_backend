package com.splitwise.dto.response;

import com.splitwise.models.enums.GroupMembershipRequestStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupInvitationResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private UserResponse invitedUser;
    private UserResponse invitedBy;
    private GroupMembershipRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
