package io.github.ric4r2.jobtracker.jobapplication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static io.github.ric4r2.jobtracker.jobapplication.ApplicationStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "WISHLIST, APPLIED",
            "WISHLIST, WITHDRAWN",
            "APPLIED, INTERVIEWING",
            "APPLIED, OFFER",
            "APPLIED, REJECTED",
            "INTERVIEWING, OFFER",
            "INTERVIEWING, REJECTED",
            "OFFER, ACCEPTED",
            "OFFER, DECLINED",
    })
    void allowsForwardTransitions(ApplicationStatus from, ApplicationStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "WISHLIST, OFFER",
            "WISHLIST, INTERVIEWING",
            "APPLIED, WISHLIST",
            "INTERVIEWING, APPLIED",
            "APPLIED, APPLIED",
            "OFFER, WITHDRAWN",
    })
    void rejectsBackwardsOrSkippingTransitions(ApplicationStatus from, ApplicationStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(names = {"ACCEPTED", "DECLINED", "REJECTED", "WITHDRAWN"})
    void finalStatusesCannotMoveAnywhere(ApplicationStatus status) {
        assertThat(status.isFinal()).isTrue();
        assertThat(status.allowedNext()).isEmpty();
    }

    @Test
    void activeStatusesAreExactlyTheNonFinalOnes() {
        for (ApplicationStatus status : values()) {
            assertThat(active().contains(status)).as(status.name()).isEqualTo(!status.isFinal());
        }
    }
}
