package com.voyageops.app.user.infrastructure;

import com.voyageops.app.trip.domain.Trip;
import com.voyageops.app.user.domain.TripMember;
import com.voyageops.app.user.domain.User;
import com.voyageops.app.user.domain.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TripMemberRepository extends JpaRepository<TripMember, UUID> {
    boolean existsByTripAndUser(Trip trip, User user);
    Optional<TripMember> findByTripAndUser(Trip trip, User user);
    List<TripMember> findByTrip(Trip trip);
    long countByTripAndRole(Trip trip, MemberRole role);
}
