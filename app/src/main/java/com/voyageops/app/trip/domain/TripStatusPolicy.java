package com.voyageops.app.trip.domain;

import com.voyageops.app.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Domain rule for allowed trip status transitions.
 * <p>
 * Lifecycle: PLANNING -> PLANNED -> ONGOING -> COMPLETED, and any active
 * state may be CANCELLED. Terminal states (COMPLETED, CANCELLED) are immutable.
 */
@Component
public class TripStatusPolicy {

    private static final Map<TripStatus, Set<TripStatus>> ALLOWED_TRANSITIONS = Map.of(
            TripStatus.PLANNING, Set.of(TripStatus.PLANNED, TripStatus.CANCELLED),
            TripStatus.PLANNED, Set.of(TripStatus.ONGOING, TripStatus.CANCELLED),
            TripStatus.ONGOING, Set.of(TripStatus.COMPLETED, TripStatus.CANCELLED),
            TripStatus.COMPLETED, Set.of(),
            TripStatus.CANCELLED, Set.of()
    );

    private static final Set<TripStatus> TERMINAL = Set.of(TripStatus.COMPLETED, TripStatus.CANCELLED);

    /**
     * Throws {@link BusinessException} when the transition is not allowed.
     * Staying in the same state is always permitted.
     */
    public void validateTransition(TripStatus from, TripStatus to) {
        if (from == to) {
            return;
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException(
                    "Invalid trip status transition from " + from + " to " + to);
        }
    }

    public boolean isTerminal(TripStatus status) {
        return status != null && TERMINAL.contains(status);
    }
}