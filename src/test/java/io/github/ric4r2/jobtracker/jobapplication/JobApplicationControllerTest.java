package io.github.ric4r2.jobtracker.jobapplication;

import io.github.ric4r2.jobtracker.jobapplication.JobApplicationDtos.JobApplicationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web layer only: routing, validation and error format. The service is mocked. */
@WebMvcTest(JobApplicationController.class)
class JobApplicationControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JobApplicationService service;

    private static JobApplicationResponse sample(long id, ApplicationStatus status) {
        Instant now = Instant.parse("2026-03-10T12:00:00Z");
        return new JobApplicationResponse(id, "Acme", "Java Dev", null, null, null, null, status,
                List.copyOf(status.allowedNext()), null, null, now, now);
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        when(service.create(any())).thenReturn(sample(7, ApplicationStatus.WISHLIST));

        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Acme", "role": "Java Dev"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/applications/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("WISHLIST"));
    }

    @Test
    void invalidBodyReturnsProblemWithFieldErrors() throws Exception {
        mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": " ", "role": "Java Dev", "jobUrl": "not a url"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.company").exists())
                .andExpect(jsonPath("$.errors.jobUrl").exists());
        verifyNoInteractions(service);
    }

    @Test
    void unknownIdReturns404Problem() throws Exception {
        when(service.get(42L)).thenThrow(new JobApplicationNotFoundException(42L));

        mvc.perform(get("/api/applications/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Job application 42 was not found"));
    }

    @Test
    void illegalStatusChangeReturns409WithAllowedOptions() throws Exception {
        when(service.changeStatus(eq(1L), any())).thenThrow(
                new IllegalStatusTransitionException(1L, ApplicationStatus.WISHLIST, ApplicationStatus.OFFER));

        mvc.perform(post("/api/applications/1/status").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "OFFER"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("WISHLIST"))
                .andExpect(jsonPath("$.allowedNextStatuses").isArray());
    }

    @Test
    void unknownStatusValueIsABadRequest() throws Exception {
        mvc.perform(get("/api/applications").param("status", "HIRED"))
                .andExpect(status().isBadRequest());
    }
}
