package io.github.ric4r2.jobtracker.stats;

import io.github.ric4r2.jobtracker.jobapplication.ApplicationStatus;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationRepository;
import io.github.ric4r2.jobtracker.jobapplication.StatusChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/** Numbers that answer "how is my job search going?". */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private final JobApplicationRepository applications;
    private final StatusChangeRepository history;
    private final Clock clock;

    public StatsService(JobApplicationRepository applications, StatusChangeRepository history, Clock clock) {
        this.applications = applications;
        this.history = history;
        this.clock = clock;
    }

    public StatsResponse summary() {
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, 0L);
        }
        long total = 0;
        for (var row : applications.countByStatus()) {
            byStatus.put(row.getStatus(), row.getTotal());
            total += row.getTotal();
        }

        long submitted = applications.countByAppliedOnIsNotNull();
        long responded = applications.countByAppliedOnIsNotNullAndStatusIn(ApplicationStatus.responded());
        long interviewed = history.countApplicationsThatReached(ApplicationStatus.INTERVIEWING);
        long offers = history.countApplicationsThatReached(ApplicationStatus.OFFER);
        long followUpsDue = applications.countByNextFollowUpOnLessThanEqualAndStatusIn(
                LocalDate.now(clock), ApplicationStatus.active());

        return new StatsResponse(total, submitted, byStatus,
                ratio(responded, submitted), ratio(interviewed, submitted), ratio(offers, submitted),
                followUpsDue);
    }

    /** Share of {@code part} in {@code whole}, rounded to three decimals; 0 when there is nothing yet. */
    static double ratio(long part, long whole) {
        if (whole == 0) {
            return 0.0;
        }
        return Math.round(part * 1000.0 / whole) / 1000.0;
    }

    public record StatsResponse(
            long total,
            long submitted,
            Map<ApplicationStatus, Long> byStatus,
            double responseRate,
            double interviewRate,
            double offerRate,
            long followUpsDue) {
    }
}
