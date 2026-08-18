package com.voyageops.app.trip.domain;

import com.voyageops.app.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TripStatusPolicyTest {

    private final TripStatusPolicy policy = new TripStatusPolicy();

    @Test
    void allowsLegalForwardTransitions() {
        assertDoesNotThrow(() -> policy.validateTransition(TripStatus.PLANNING, TripStatus.PLANNED));
        assertDoesNotThrow(() -> policy.validateTransition(TripStatus.PLANNED, TripStatus.ONGOING));
        assertDoesNotThrow(() -> policy.validateTransition(TripStatus.ONGOING, TripStatus.COMPLETED));
    }

    @Test
    void allowsCancellationFromAnyActiveState() {
        assertDoesNotThrow(() -> policy.validateTransition(TripStatus.PLANNING, TripStatus.CANCELLED));
        assertDoesNotThrow(() -> policy.validateTransition(TripStatus.PLANNED, TripStatus.CANCELLED));
        assertDoesNotThrow(() -> policy.validateTransition(TripStatus.ONGOING, TripStatus.CANCELLED));
    }

    @Test
    void rejectsSkippedStates() {
        assertThatThrownBy(() -> policy.validateTransition(TripStatus.PLANNING, TripStatus.ONGOING))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid trip status transition");
        assertThatThrownBy(() -> policy.validateTransition(TripStatus.PLANNING, TripStatus.COMPLETED))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validateTransition(TripStatus.PLANNED, TripStatus.COMPLETED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void allowsStayingInSameState() {
        assertDoesNotThrow(() -> policy.validateTransition(TripStatus.ONGOING, TripStatus.ONGOING));
    }

    @Test
    void rejectsAnyTransitionFromTerminalStates() {
        assertThatThrownBy(() -> policy.validateTransition(TripStatus.COMPLETED, TripStatus.PLANNING))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> policy.validateTransition(TripStatus.CANCELLED, TripStatus.PLANNING))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void identifiesTerminalStates() {
        assertThat(policy.isTerminal(TripStatus.COMPLETED)).isTrue();
        assertThat(policy.isTerminal(TripStatus.CANCELLED)).isTrue();
        assertThat(policy.isTerminal(TripStatus.PLANNING)).isFalse();
        assertThat(policy.isTerminal(null)).isFalse();
    }
}