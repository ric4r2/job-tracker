package io.github.ric4r2.jobtracker.jobapplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StatusChangeRepository extends JpaRepository<StatusChange, Long> {

    List<StatusChange> findByJobApplicationIdOrderByChangedAtAscIdAsc(Long jobApplicationId);

    /** How many applications reached {@code status} at some point, even if they moved on since. */
    @Query("select count(distinct s.jobApplication.id) from StatusChange s where s.toStatus = :status")
    long countApplicationsThatReached(ApplicationStatus status);

    @Modifying
    @Query("delete from StatusChange s where s.jobApplication.id = :jobApplicationId")
    void deleteByJobApplicationId(Long jobApplicationId);
}
