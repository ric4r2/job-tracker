package io.github.ric4r2.jobtracker.jobapplication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A single job you are tracking. Status changes go through {@link #changeStatus}
 * so that the transition rules are enforced and every change is recorded.
 */
@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(nullable = false, length = 200)
    private String company;

    @Column(nullable = false, length = 200)
    private String role;

    @Column(length = 200)
    private String location;

    @Column(name = "job_url", length = 2000)
    private String jobUrl;

    @Column(length = 100)
    private String source;

    @Column(length = 4000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "applied_on")
    private LocalDate appliedOn;

    @Column(name = "next_follow_up_on")
    private LocalDate nextFollowUpOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobApplication() {
        // for JPA
    }

    public JobApplication(Details details, ApplicationStatus initialStatus, Instant now, LocalDate today) {
        this.status = Objects.requireNonNull(initialStatus, "initialStatus");
        this.createdAt = now;
        apply(details, now);
        if (appliedOn == null && initialStatus != ApplicationStatus.WISHLIST) {
            appliedOn = today;
        }
    }

    /** Replaces the editable fields. The status is not editable here on purpose. */
    public void updateDetails(Details details, Instant now) {
        apply(details, now);
    }

    /**
     * Moves the application to {@code target} and returns the history entry to store.
     *
     * @throws IllegalStatusTransitionException if the lifecycle does not allow it
     */
    public StatusChange changeStatus(ApplicationStatus target, String note, LocalDate followUpOn,
                                     Instant now, LocalDate today) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStatusTransitionException(id, status, target);
        }
        ApplicationStatus previous = status;
        status = target;
        if (target == ApplicationStatus.APPLIED && appliedOn == null) {
            appliedOn = today;
        }
        nextFollowUpOn = target.isFinal() ? null : followUpOn;
        updatedAt = now;
        return new StatusChange(this, previous, target, note, now);
    }

    private void apply(Details details, Instant now) {
        this.company = details.company();
        this.role = details.role();
        this.location = details.location();
        this.jobUrl = details.jobUrl();
        this.source = details.source();
        this.notes = details.notes();
        this.appliedOn = details.appliedOn();
        this.nextFollowUpOn = details.nextFollowUpOn();
        this.updatedAt = now;
    }

    /** The user-editable fields of an application. */
    public record Details(String company, String role, String location, String jobUrl, String source,
                          String notes, LocalDate appliedOn, LocalDate nextFollowUpOn) {
    }

    public Long getId() {
        return id;
    }

    public String getCompany() {
        return company;
    }

    public String getRole() {
        return role;
    }

    public String getLocation() {
        return location;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public String getSource() {
        return source;
    }

    public String getNotes() {
        return notes;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDate getAppliedOn() {
        return appliedOn;
    }

    public LocalDate getNextFollowUpOn() {
        return nextFollowUpOn;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
