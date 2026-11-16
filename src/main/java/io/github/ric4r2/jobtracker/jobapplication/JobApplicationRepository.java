package io.github.ric4r2.jobtracker.jobapplication;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {

    List<JobApplication> findByNextFollowUpOnLessThanEqualAndStatusIn(
            LocalDate date, Collection<ApplicationStatus> statuses, Sort sort);

    long countByNextFollowUpOnLessThanEqualAndStatusIn(LocalDate date, Collection<ApplicationStatus> statuses);

    long countByAppliedOnIsNotNull();

    long countByAppliedOnIsNotNullAndStatusIn(Collection<ApplicationStatus> statuses);

    @Query("select a.status as status, count(a) as total from JobApplication a group by a.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        ApplicationStatus getStatus();

        long getTotal();
    }

    /** Reusable filters for {@link #findAll(Specification, org.springframework.data.domain.Pageable)}. */
    final class Filters {

        private Filters() {
        }

        static Specification<JobApplication> hasStatus(ApplicationStatus status) {
            return (root, query, cb) -> cb.equal(root.get("status"), status);
        }

        /** Case-insensitive match on company or role. */
        static Specification<JobApplication> matches(String text) {
            String pattern = "%" + text.toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
            return (root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("company")), pattern, '\\'),
                    cb.like(cb.lower(root.get("role")), pattern, '\\'));
        }
    }
}
