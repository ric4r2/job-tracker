package io.github.ric4r2.jobtracker.jobapplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway migrations on an embedded database, so it also proves the
 * schema matches the entities (Hibernate is set to {@code validate}).
 */
@DataJpaTest
class JobApplicationRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-03-10T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    @Autowired
    JobApplicationRepository applications;
    @Autowired
    StatusChangeRepository history;

    private JobApplication save(String company, String role, ApplicationStatus status, LocalDate followUp) {
        var details = new JobApplication.Details(company, role, null, null, null, null, null, followUp);
        var application = applications.save(new JobApplication(details, status, NOW, TODAY));
        history.save(StatusChange.created(application, NOW));
        return application;
    }

    @Test
    void searchMatchesCompanyOrRoleIgnoringCase() {
        save("Acme", "Backend Engineer", ApplicationStatus.APPLIED, null);
        save("Globex", "Java Developer", ApplicationStatus.WISHLIST, null);
        save("Initech", "Frontend Engineer", ApplicationStatus.APPLIED, null);

        var result = applications.findAll(JobApplicationRepository.Filters.matches("java"),
                PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(JobApplication::getCompany).containsExactly("Globex");
    }

    @Test
    void searchTreatsWildcardsLiterally() {
        save("100% Remote Co", "Engineer", ApplicationStatus.APPLIED, null);
        save("Acme", "Engineer", ApplicationStatus.APPLIED, null);

        var result = applications.findAll(JobApplicationRepository.Filters.matches("%"), PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(JobApplication::getCompany).containsExactly("100% Remote Co");
    }

    @Test
    void dueFollowUpsIgnoreFinishedApplications() {
        save("Due", "Dev", ApplicationStatus.APPLIED, TODAY.minusDays(1));
        save("Later", "Dev", ApplicationStatus.APPLIED, TODAY.plusDays(5));
        var finished = save("Done", "Dev", ApplicationStatus.APPLIED, null);
        finished.changeStatus(ApplicationStatus.REJECTED, null, null, NOW, TODAY);

        var due = applications.findByNextFollowUpOnLessThanEqualAndStatusIn(TODAY, ApplicationStatus.active(),
                Sort.by("nextFollowUpOn"));

        assertThat(due).extracting(JobApplication::getCompany).containsExactly("Due");
    }

    @Test
    void countsApplicationsThatReachedAStatusEvenAfterMovingOn() {
        var application = save("Acme", "Dev", ApplicationStatus.APPLIED, null);
        history.save(application.changeStatus(ApplicationStatus.INTERVIEWING, null, null, NOW, TODAY));
        history.save(application.changeStatus(ApplicationStatus.REJECTED, null, null, NOW, TODAY));
        save("Globex", "Dev", ApplicationStatus.APPLIED, null);

        assertThat(history.countApplicationsThatReached(ApplicationStatus.INTERVIEWING)).isEqualTo(1);
        assertThat(applications.countByStatus())
                .extracting(JobApplicationRepository.StatusCount::getStatus)
                .containsExactlyInAnyOrder(ApplicationStatus.REJECTED, ApplicationStatus.APPLIED);
    }

    @Test
    void historyIsReturnedInOrder() {
        var application = save("Acme", "Dev", ApplicationStatus.WISHLIST, null);
        history.save(application.changeStatus(ApplicationStatus.APPLIED, "sent", null, NOW.plusSeconds(1), TODAY));

        var entries = history.findByJobApplicationIdOrderByChangedAtAscIdAsc(application.getId());

        assertThat(entries).extracting(StatusChange::getToStatus)
                .containsExactly(ApplicationStatus.WISHLIST, ApplicationStatus.APPLIED);
    }
}
