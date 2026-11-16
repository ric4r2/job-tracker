package io.github.ric4r2.jobtracker.jobapplication;

public class JobApplicationNotFoundException extends RuntimeException {

    public JobApplicationNotFoundException(Long id) {
        super("Job application %d was not found".formatted(id));
    }
}
