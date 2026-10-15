package io.github.gothsins.hookforge.delivery;

import com.jayway.jsonpath.JsonPath;
import io.github.gothsins.hookforge.AbstractIntegrationTest;
import io.github.gothsins.hookforge.auth.AuthService;
import io.github.gothsins.hookforge.auth.dto.AuthResponse;
import io.github.gothsins.hookforge.auth.dto.RegisterRequest;
import io.github.gothsins.hookforge.endpoint.WebhookEndpoint;
import io.github.gothsins.hookforge.endpoint.WebhookEndpointRepository;
import io.github.gothsins.hookforge.event.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class DeliveryControllerIT extends AbstractIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired AuthService authService;
    @Autowired DeliveryRepository deliveries;
    @Autowired WebhookEndpointRepository endpoints;
    @Autowired EventRepository events;

    @Test
    void eventCreatesOnePendingDeliveryForEachActiveOwnedEndpoint() throws Exception {
        AuthResponse owner = register();
        AuthResponse other = register();
        WebhookEndpoint first = endpoints.save(WebhookEndpoint.create(
                "First", "https://first.example.com/hook", owner.accountId()));
        WebhookEndpoint second = endpoints.save(WebhookEndpoint.create(
                "Second", "https://second.example.com/hook", owner.accountId()));
        WebhookEndpoint inactive = WebhookEndpoint.create(
                "Inactive", "https://inactive.example.com/hook", owner.accountId());
        inactive.deactivate();
        endpoints.save(inactive);
        endpoints.save(WebhookEndpoint.create("Other", "https://other.example.com/hook", other.accountId()));

        String response = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"order.created\",\"payload\":{\"id\":42}}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID eventId = UUID.fromString(JsonPath.read(response, "$.id"));

        assertThat(events.count()).isEqualTo(1);
        List<Delivery> scheduled = deliveries.findAllByOwnerIdAndEventIdOrderByCreatedAtDesc(
                owner.accountId(), eventId);
        assertThat(scheduled).hasSize(2);
        assertThat(scheduled).extracting(Delivery::getEndpointId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(scheduled).allSatisfy(delivery -> {
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
            assertThat(delivery.getAttemptCount()).isZero();
            assertThat(delivery.getNextAttemptAt()).isNotNull();
            assertThat(delivery.getEndpointUrl()).isEqualTo(
                    delivery.getEndpointId().equals(first.getId()) ? first.getUrl() : second.getUrl());
        });

        mockMvc.perform(get("/api/v1/deliveries").param("eventId", eventId.toString())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/v1/deliveries").param("eventId", eventId.toString())
                        .header("Authorization", bearer(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/deliveries/{id}", scheduled.getFirst().getId())
                        .header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventWithoutDestinationsIsPersistedWithoutDeliveries() throws Exception {
        AuthResponse owner = register();
        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"empty.fanout\",\"payload\":{}}"))
                .andExpect(status().isCreated());
        assertThat(events.count()).isEqualTo(1);
        assertThat(deliveries.count()).isZero();
    }

    @Test
    void deliveryQueriesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privateDestinationIsRejectedAndRecordedWithoutConnecting() throws Exception {
        AuthResponse owner = register();
        endpoints.save(WebhookEndpoint.create("Private", "http://127.0.0.1/", owner.accountId()));
        String response = mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"private.target\",\"payload\":{}}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID eventId = UUID.fromString(JsonPath.read(response, "$.id"));
        UUID deliveryId = deliveries.findAllByOwnerIdAndEventIdOrderByCreatedAtDesc(owner.accountId(), eventId)
                .getFirst().getId();

        mockMvc.perform(post("/api/v1/deliveries/{id}/dispatch", deliveryId)
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEAD"));
        mockMvc.perform(get("/api/v1/deliveries/{id}/attempts", deliveryId)
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].errorCategory").value("BLOCKED_ADDRESS"));
    }

    private AuthResponse register() {
        return authService.register(new RegisterRequest(
                UUID.randomUUID() + "@example.com", "test-password-123"));
    }

    private String bearer(AuthResponse account) {
        return "Bearer " + account.token();
    }
}
