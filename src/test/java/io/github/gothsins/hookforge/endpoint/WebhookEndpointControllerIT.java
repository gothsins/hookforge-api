package io.github.gothsins.hookforge.endpoint;

import io.github.gothsins.hookforge.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class WebhookEndpointControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookEndpointRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldCreateWebhookEndpoint() throws Exception {

        String request = """
                {
                  "name": "Local Test Endpoint",
                  "url": "https://example.com/webhooks"
                }
                """;

        mockMvc.perform(post("/api/v1/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name")
                        .value("Local Test Endpoint"))
                .andExpect(jsonPath("$.url")
                        .value("https://example.com/webhooks"))
                .andExpect(jsonPath("$.active")
                        .value(true))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {

        String request = """
            {
              "name": "",
              "url": "https://example.com/webhooks"
            }
            """;

        mockMvc.perform(post("/api/v1/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldReturnBadRequestWhenUrlIsInvalid() throws Exception {

        String request = """
            {
              "name": "Invalid Endpoint",
              "url": "batata"
            }
            """;

        mockMvc.perform(post("/api/v1/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldReturnNotFoundWhenWebhookEndpointDoesNotExist() throws Exception {

        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/endpoints/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFindWebhookEndpointById() throws Exception {

        WebhookEndpoint endpoint =
                WebhookEndpoint.create(
                        "Test Endpoint",
                        "https://example.com/webhooks"
                );

        endpoint = repository.save(endpoint);

        mockMvc.perform(get("/api/v1/endpoints/{id}", endpoint.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(endpoint.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Endpoint"))
                .andExpect(jsonPath("$.url").value("https://example.com/webhooks"))
                .andExpect(jsonPath("$.active").value(true));
    }
}