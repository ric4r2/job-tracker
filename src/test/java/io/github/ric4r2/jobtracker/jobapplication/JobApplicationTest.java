package io.github.ric4r2.jobtracker.jobapplication;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobApplicationTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    static JobApplication.Details details() {
        return new JobApplication.Details("Acme", "Junior Java Developer", "Remote", null, "LinkedIn", null,
                null, null);
    }

    @Test
    void wishlistItemHasNoApplicationDate() {
        var application = new JobApplication(details(), ApplicationStatus.WISHLIST, NOW, TODAY);

        assertThat(application.getAppliedOn()).isNull();
        assertThat(application.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void creatingAsAppliedDefaultsTheApplicationDateToToday() {
        var application = new JobApplication(details(), ApplicationStatus.APPLIED, NOW, TODAY);

        assertThat(application.getAppliedOn()).isEqualTo(TODAY);
    }

    @Test
    void movingToAppliedSetsTheDateAndRecordsTheChange() {
        var application = new JobApplication(details(), ApplicationStatus.WISHLIST, NOW, TODAY);
        LocalDate followUp = TODAY.plusDays(7);

        StatusChange change = application.changeStatus(ApplicationStatus.APPLIED, "Sent CV", followUp,
                NOW.plusSeconds(60), TODAY);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(application.getAppliedOn()).isEqualTo(TODAY);
        assertThat(application.getNextFollowUpOn()).isEqualTo(followUp);
        assertThat(change.getFromStatus()).isEqualTo(ApplicationStatus.WISHLIST);
        assertThat(change.getToStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(change.getNote()).isEqualTo("Sent CV");
    }

    @Test
    void reachingAFinalStatusClearsTheFollowUp() {
        var application = new JobApplication(details(), ApplicationStatus.APPLIED, NOW, TODAY);

        application.changeStatus(ApplicationStatus.REJECTED, null, TODAY.plusDays(3), NOW, TODAY);

        assertThat(application.getNextFollowUpOn()).isNull();
    }

    @Test
    void illegalTransitionLeavesTheApplicationUnchanged() {
        var application = new JobApplication(details(), ApplicationStatus.WISHLIST, NOW, TODAY);

        assertThatThrownBy(() -> application.changeStatus(ApplicationStatus.OFFER, null, null, NOW, TODAY))
                .isInstanceOf(IllegalStatusTransitionException.class)
                .hasMessageContaining("WISHLIST to OFFER");
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WISHLIST);
    }
}
