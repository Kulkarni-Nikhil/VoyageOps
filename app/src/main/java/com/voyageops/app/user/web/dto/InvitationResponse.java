package com.voyageops.app.user.web.dto;

import com.voyageops.app.user.domain.InvitationStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationResponse {
    private UUID id;
    private Long tripId;
    private String tripName;
    private String invitedByName;
    private String invitedEmail;
    private InvitationStatus status;
    private String token;
    private Instant expiresAt;
    private Instant createdAt;
}
