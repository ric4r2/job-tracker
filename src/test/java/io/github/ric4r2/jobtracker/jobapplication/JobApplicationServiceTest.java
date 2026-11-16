package io.github.ric4r2.jobtracker.jobapplication;

import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.ChangeStatusRequest;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.CreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    JobApplicationRepository applications;
    @Mock
    StatusChangeRepository history;

    JobApplicationService service;

    @BeforeEach
    void setUp() {
        service = new JobApplicationService(applications, history, CLOCK);
    }

    @Test
    void createDefaultsToWishlistAndWritesTheFirstHistoryEntry() {
        var request = new CreateRequest("  Acme  ", "Java Dev", null, null, null, null, null, null, null);

        var response = service.create(request);

        assertThat(response.company()).isEqualTo("Acme");
        assertThat(response.status()).isEqualTo(ApplicationStatus.WISHLIST);
        assertThat(response.allowedNextStatuses())
                .containsExactlyInAnyOrder(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN);
        var entry = ArgumentCaptor.forClass(StatusChange.class);
        verify(history).save(entry.capture());
        assertThat(entry.getValue().getFromStatus()).isNull();
        assertThat(entry.getValue().getToStatus()).isEqualTo(ApplicationStatus.WISHLIST);
    }

    @Test
    void changeStatusUsesTheClockForDates() {
        var existing = new JobApplication(JobApplicationTest.details(), ApplicationStatus.WISHLIST,
                CLOCK.instant(), LocalDate.now(CLOCK));
        when(applications.findById(1L)).thenReturn(Optional.of(existing));

        var response = service.changeStatus(1L, new ChangeStatusRequest(ApplicationStatus.APPLIED, null, null));

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(response.appliedOn()).isEqualTo(LocalDate.of(2026, 3, 10));
        verify(history).save(any(StatusChange.class));
    }

    @Test
    void illegalChangeIsNotRecorded() {
        var existing = new JobApplication(JobApplicationTest.details(), ApplicationStatus.WISHLIST,
                CLOCK.instant(), LocalDate.now(CLOCK));
        when(applications.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.changeStatus(1L,
                new ChangeStatusRequest(ApplicationStatus.ACCEPTED, null, null)))
                .isInstanceOf(IllegalStatusTransitionException.class);
        verify(history, never()).save(any());
    }

    @Test
    void missingApplicationThrowsNotFound() {
        when(applications.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(JobApplicationNotFoundException.class);
    }

    @Test
    void historyOfMissingApplicationThrowsNotFound() {
        when(applications.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.history(99L)).isInstanceOf(JobApplicationNotFoundException.class);
    }
}
