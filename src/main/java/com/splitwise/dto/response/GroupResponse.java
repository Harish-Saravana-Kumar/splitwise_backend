package com.splitwise.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {

    private Long id;

    private String name;

    private String description;

    private UserResponse createdBy;

    private LocalDateTime createdAt;
}
