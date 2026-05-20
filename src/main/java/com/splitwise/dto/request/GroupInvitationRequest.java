package com.splitwise.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupInvitationRequest {

    @NotBlank(message = "Invitee email is required")
    @Email(message = "Invitee email must be valid")
    private String invitedEmail;
}
