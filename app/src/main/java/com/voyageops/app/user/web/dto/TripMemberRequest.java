package com.voyageops.app.user.web.dto;

import com.voyageops.app.user.domain.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripMemberRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "role is required")
    private MemberRole role;
}
