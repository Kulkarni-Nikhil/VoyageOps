package com.voyageops.app.user.infrastructure;

import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.user.domain.Invitation;
import com.voyageops.app.user.domain.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    boolean existsByTripAndInvitedEmailAndStatus(Trip trip, String email, InvitationStatus status);
    Optional<Invitation> findByToken(String token);
    List<Invitation> findByStatusAndExpiresAtBefore(InvitationStatus status, Instant now);
}
