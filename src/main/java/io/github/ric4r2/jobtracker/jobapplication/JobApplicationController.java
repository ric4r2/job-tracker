package io.github.ric4r2.jobtracker.jobapplication;

import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.ChangeStatusRequest;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.CreateRequest;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.JobApplicationResponse;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.PageResponse;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.StatusChangeResponse;
import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.UpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody CreateRequest request) {
        JobApplicationResponse created = service.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<JobApplicationResponse> search(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(name = "q", required = false) String text,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(status, text, pageable);
    }

    @GetMapping("/follow-ups")
    public List<JobApplicationResponse> dueFollowUps() {
        return service.dueFollowUps();
    }

    @GetMapping("/{id}")
    public JobApplicationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public JobApplicationResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/status")
    public JobApplicationResponse changeStatus(@PathVariable Long id,
                                               @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(id, request);
    }

    @GetMapping("/{id}/history")
    public List<StatusChangeResponse> history(@PathVariable Long id) {
        return service.history(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
