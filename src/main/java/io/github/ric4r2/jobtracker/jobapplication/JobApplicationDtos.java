package io.github.ric4r2.jobtracker.jobapplication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/** Request and response bodies for the job application API. */
public final class JobApplicationDtos {

    private JobApplicationDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String company,
            @NotBlank @Size(max = 200) String role,
            @Size(max = 200) String location,
            @URL @Size(max = 2000) String jobUrl,
            @Size(max = 100) String source,
            @Size(max = 4000) String notes,
            ApplicationStatus status,
            LocalDate appliedOn,
            LocalDate nextFollowUpOn) {

        JobApplication.Details details() {
            return new JobApplication.Details(company.strip(), role.strip(), location, jobUrl, source, notes,
                    appliedOn, nextFollowUpOn);
        }

        ApplicationStatus initialStatus() {
            return status == null ? ApplicationStatus.WISHLIST : status;
        }
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 200) String company,
            @NotBlank @Size(max = 200) String role,
            @Size(max = 200) String location,
            @URL @Size(max = 2000) String jobUrl,
            @Size(max = 100) String source,
            @Size(max = 4000) String notes,
            LocalDate appliedOn,
            LocalDate nextFollowUpOn) {

        JobApplication.Details details() {
            return new JobApplication.Details(company.strip(), role.strip(), location, jobUrl, source, notes,
                    appliedOn, nextFollowUpOn);
        }
    }

    public record ChangeStatusRequest(
            @NotNull ApplicationStatus status,
            @Size(max = 1000) String note,
            LocalDate nextFollowUpOn) {
    }

    public record JobApplicationResponse(
            Long id,
            String company,
            String role,
            String location,
            String jobUrl,
            String source,
            String notes,
            ApplicationStatus status,
            List<ApplicationStatus> allowedNextStatuses,
            LocalDate appliedOn,
            LocalDate nextFollowUpOn,
            Instant createdAt,
            Instant updatedAt) {

        static JobApplicationResponse from(JobApplication a) {
            return new JobApplicationResponse(a.getId(), a.getCompany(), a.getRole(), a.getLocation(),
                    a.getJobUrl(), a.getSource(), a.getNotes(), a.getStatus(),
                    List.copyOf(a.getStatus().allowedNext()), a.getAppliedOn(), a.getNextFollowUpOn(),
                    a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    public record StatusChangeResponse(
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus,
            String note,
            Instant changedAt) {

        static StatusChangeResponse from(StatusChange c) {
            return new StatusChangeResponse(c.getFromStatus(), c.getToStatus(), c.getNote(), c.getChangedAt());
        }
    }

    /** A stable page shape, so the JSON does not depend on Spring Data internals. */
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
