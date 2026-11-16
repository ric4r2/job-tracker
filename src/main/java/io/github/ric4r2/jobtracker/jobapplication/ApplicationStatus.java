package io.github.ric4r2.jobtracker.jobapplication;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a job application.
 *
 * <p>Transitions are deliberately restricted so the history stays meaningful:
 * you cannot, for example, jump from {@link #WISHLIST} straight to {@link #OFFER},
 * or reopen an application that already reached a final state.
 */
public enum ApplicationStatus {
    WISHLIST,
    APPLIED,
    INTERVIEWING,
    OFFER,
    ACCEPTED,
    DECLINED,
    REJECTED,
    WITHDRAWN;

    /** Statuses this one may move to next. Final statuses return an empty set. */
    public Set<ApplicationStatus> allowedNext() {
        return switch (this) {
            case WISHLIST -> EnumSet.of(APPLIED, WITHDRAWN);
            case APPLIED -> EnumSet.of(INTERVIEWING, OFFER, REJECTED, WITHDRAWN);
            case INTERVIEWING -> EnumSet.of(OFFER, REJECTED, WITHDRAWN);
            case OFFER -> EnumSet.of(ACCEPTED, DECLINED, REJECTED);
            case ACCEPTED, DECLINED, REJECTED, WITHDRAWN -> EnumSet.noneOf(ApplicationStatus.class);
        };
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        return allowedNext().contains(target);
    }

    /** A final status ends the application; no follow-up is needed any more. */
    public boolean isFinal() {
        return allowedNext().isEmpty();
    }

    /** Statuses that are still in play and may need a follow-up. */
    public static Set<ApplicationStatus> active() {
        return EnumSet.of(WISHLIST, APPLIED, INTERVIEWING, OFFER);
    }

    /** Statuses that mean the company answered in some way. */
    public static Set<ApplicationStatus> responded() {
        return EnumSet.of(INTERVIEWING, OFFER, ACCEPTED, DECLINED, REJECTED);
    }
}
