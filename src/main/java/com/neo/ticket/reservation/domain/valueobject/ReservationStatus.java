package com.neo.ticket.reservation.domain.valueobject;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {

    PENDING {
        @Override
        public Set<ReservationStatus> allowedTransitions() {
            return EnumSet.of(CONFIRMED, CANCELLED);
        }
    },

    CONFIRMED {
        @Override
        public Set<ReservationStatus> allowedTransitions() {
            return EnumSet.of(CANCELLED);
        }
    },

    CANCELLED {
        @Override
        public Set<ReservationStatus> allowedTransitions() {
            return EnumSet.noneOf(ReservationStatus.class);
        }
    };

    public abstract Set<ReservationStatus> allowedTransitions();

    public boolean canTransitionTo(ReservationStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }

    public boolean holdsSeats() {
        return this != CANCELLED;
    }
}
