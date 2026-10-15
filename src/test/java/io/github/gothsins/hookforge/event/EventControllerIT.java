package io.github.gothsins.hookforge.event;

import com.jayway.jsonpath.JsonPath;
import io.github.gothsins.hookforge.AbstractIntegrationTest;
import io.github.gothsins.hookforge.auth.AuthService;
import io.github.gothsins.hookforge.auth.dto.AuthResponse;
import io.github.gothsins.hookforge.auth.dto.RegisterRequest;
import io.github.gothsins.hookforge.delivery.DeliveryRepository;
import io.github.gothsins.hookforge.endpoint.WebhookEndpoint;
import io.github.gothsins.hookforge.endpoint.WebhookEndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private AuthService authService;

    @Autowired
    private WebhookEndpointRepository endpoints;

    @Autowired
    private DeliveryRepository deliveries;

    private String authorization;
    private UUID ownerId;

    @BeforeEach
    void cleanDatabase() {
        AuthResponse auth = authService.register(new RegisterRequest(
                UUID.randomUUID() + "@example.com", "test-password-123"));
        authorization = "Bearer " + auth.token();
        ownerId = auth.accountId();
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
                        .header("Authorization", authorization)
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
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertThat(repository.count()).isZero();
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {

        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/events/{id}", unknownId)
                        .header("Authorization", authorization))
                .andExpect(status().isNotFound());
    }

    @Test
    void keyReplayReturnsExistingEventWithoutNewDeliveryAndRejectsChangedContent() throws Exception {
        endpoints.save(WebhookEndpoint.create("Destination", "https://receiver.example/hook", ownerId));
        String body = "{\"type\":\"order.created\",\"payload\":{\"id\":42,\"detail\":{\"a\":1,\"b\":2}}}";
        String reorderedBody = "{\"payload\":{\"detail\":{\"b\":2,\"a\":1},\"id\":42},\"type\":\"order.created\"}";
        MvcResult created = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "order-42")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        MvcResult replay = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "order-42")
                        .contentType(MediaType.APPLICATION_JSON).content(reorderedBody))
                .andExpect(status().isOk()).andReturn();
        assertThat(JsonPath.<String>read(created.getResponse().getContentAsString(), "$.id"))
                .isEqualTo(JsonPath.read(replay.getResponse().getContentAsString(), "$.id"));
        assertThat(repository.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "order-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{\"id\":43}}"))
                .andExpect(status().isConflict());
        assertThat(repository.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);

        AuthResponse other = authService.register(new RegisterRequest(
                UUID.randomUUID() + "@example.com", "test-password-123"));
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer " + other.token())
                        .header("Idempotency-Key", "order-42")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void invalidKeyIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "invalid key with spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{}}"))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void concurrentRequestsWithSameKeyCreateOnlyOneEvent() throws Exception {
        endpoints.save(WebhookEndpoint.create("Destination", "https://receiver.example/hook", ownerId));
        CountDownLatch start = new CountDownLatch(1);
        var requests = java.util.stream.IntStream.range(0, 2).mapToObj(ignored ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await(5, TimeUnit.SECONDS);
                        return mockMvc.perform(post("/api/v1/events")
                                        .header("Authorization", authorization)
                                        .header("Idempotency-Key", "concurrent-order")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"type\":\"order.created\",\"payload\":{\"id\":42}}"))
                                .andReturn().getResponse();
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                })).toList();
        start.countDown();
        var first = requests.get(0).get(10, TimeUnit.SECONDS);
        var second = requests.get(1).get(10, TimeUnit.SECONDS);
        assertThat(first.getStatus()).isIn(200, 201);
        assertThat(second.getStatus()).isIn(200, 201);
        assertThat(first.getStatus()).isNotEqualTo(second.getStatus());
        assertThat(JsonPath.<String>read(first.getContentAsString(), "$.id"))
                .isEqualTo(JsonPath.read(second.getContentAsString(), "$.id"));
        assertThat(repository.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);
    }
}
