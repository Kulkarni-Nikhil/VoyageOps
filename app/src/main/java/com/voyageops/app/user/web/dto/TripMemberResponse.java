package com.voyageops.app.user.web.dto;

import com.voyageops.app.user.domain.MemberRole;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMemberResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private MemberRole role;
    private Instant joinedAt;
}
