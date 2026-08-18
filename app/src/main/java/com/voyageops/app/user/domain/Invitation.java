package com.voyageops.app.user.domain;

import com.voyageops.app.trip.domain.Trip;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private InvitationStatus status;

    @Column(unique = true, length = 64)
    private String token;

    private Instant expiresAt;
    private Instant createdAt;
    private String invitedEmail;
}