package io.github.ric4r2.jobtracker.jobapplication;

/** Thrown when a status change is not allowed by {@link ApplicationStatus#allowedNext()}. */
public class IllegalStatusTransitionException extends RuntimeException {

    private final ApplicationStatus current;
    private final ApplicationStatus requested;

    public IllegalStatusTransitionException(Long applicationId, ApplicationStatus current,
                                            ApplicationStatus requested) {
        super("Application %s cannot move from %s to %s".formatted(applicationId, current, requested));
        this.current = current;
        this.requested = requested;
    }

    public ApplicationStatus getCurrent() {
        return current;
    }

    public ApplicationStatus getRequested() {
        return requested;
    }
}
