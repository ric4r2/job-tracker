package io.github.ric4r2.jobtracker.jobapplication;

import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.ChangeStatusRequest;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.CreateRequest;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.JobApplicationResponse;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.PageResponse;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.StatusChangeResponse;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.UpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class JobApplicationService {

    private final JobApplicationRepository applications;
    private final StatusChangeRepository history;
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository applications, StatusChangeRepository history,
                                 Clock clock) {
        this.applications = applications;
        this.history = history;
        this.clock = clock;
    }

    public JobApplicationResponse create(CreateRequest request) {
        Instant now = clock.instant();
        JobApplication application = new JobApplication(request.details(), request.initialStatus(), now,
                LocalDate.now(clock));
        applications.save(application);
        history.save(StatusChange.created(application, now));
        return JobApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse get(Long id) {
        return JobApplicationResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobApplicationResponse> search(ApplicationStatus status, String text, Pageable pageable) {
        List<Specification<JobApplication>> filters = new ArrayList<>();
        if (status != null) {
            filters.add(JobApplicationRepository.Filters.hasStatus(status));
        }
        if (StringUtils.hasText(text)) {
            filters.add(JobApplicationRepository.Filters.matches(text.strip()));
        }
        var page = applications.findAll(Specification.allOf(filters), pageable);
        return PageResponse.from(page, JobApplicationResponse::from);
    }

    /** Active applications whose follow-up date is today or earlier, oldest first. */
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> dueFollowUps() {
        return applications.findByNextFollowUpOnLessThanEqualAndStatusIn(LocalDate.now(clock),
                        ApplicationStatus.active(), Sort.by("nextFollowUpOn", "id"))
                .stream()
                .map(JobApplicationResponse::from)
                .toList();
    }

    public JobApplicationResponse update(Long id, UpdateRequest request) {
        JobApplication application = find(id);
        application.updateDetails(request.details(), clock.instant());
        return JobApplicationResponse.from(application);
    }

    public JobApplicationResponse changeStatus(Long id, ChangeStatusRequest request) {
        JobApplication application = find(id);
        StatusChange change = application.changeStatus(request.status(), request.note(),
                request.nextFollowUpOn(), clock.instant(), LocalDate.now(clock));
        history.save(change);
        return JobApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public List<StatusChangeResponse> history(Long id) {
        if (!applications.existsById(id)) {
            throw new JobApplicationNotFoundException(id);
        }
        return history.findByJobApplicationIdOrderByChangedAtAscIdAsc(id).stream()
                .map(StatusChangeResponse::from)
                .toList();
    }

    public void delete(Long id) {
        JobApplication application = find(id);
        history.deleteByJobApplicationId(id);
        applications.delete(application);
    }

    private JobApplication find(Long id) {
        return applications.findById(id).orElseThrow(() -> new JobApplicationNotFoundException(id));
    }
}
