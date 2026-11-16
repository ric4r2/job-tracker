package io.github.ric4r2.jobtracker;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end flow against a real PostgreSQL started by Testcontainers.
 * Skipped automatically when Docker is not available; always runs in CI.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JobApplicationApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void fullLifecycle() throws Exception {
        String created = mvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Acme", "role": "Junior Java Developer", "source": "LinkedIn"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");
        String base = "/api/applications/" + id;

        mvc.perform(put(base).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company": "Acme Corp", "role": "Junior Java Developer", "notes": "Referral"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("Acme Corp"));

        mvc.perform(post(base + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "APPLIED", "note": "Sent CV"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedOn").isNotEmpty());

        mvc.perform(post(base + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACCEPTED"}
                                """))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/applications").param("q", "acme").param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mvc.perform(get(base + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].toStatus").value("APPLIED"));

        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submitted").value(1));

        mvc.perform(delete(base)).andExpect(status().isNoContent());
        mvc.perform(get(base)).andExpect(status().isNotFound());
    }
}
