package io.github.gothsins.hookforge.event;

import io.github.gothsins.hookforge.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class EventControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldCreateEvent() throws Exception {

        String request = """
                {
                  "type": "order.created",
                  "payload": {
                    "orderId": 42,
                    "customer": "Gui"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value("order.created"))
                .andExpect(jsonPath("$.payload.orderId").value(42))
                .andExpect(jsonPath("$.payload.customer").value("Gui"))
                .andExpect(jsonPath("$.receivedAt").exists());

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturnBadRequestWhenTypeIsBlank() throws Exception {

        String request = """
                {
                  "type": "",
                  "payload": {}
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {

        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/events/{id}", unknownId))
                .andExpect(status().isNotFound());
    }
}