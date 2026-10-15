package io.github.gothsins.hookforge.delivery;

import com.jayway.jsonpath.JsonPath;
import io.github.gothsins.hookforge.AbstractIntegrationTest;
import io.github.gothsins.hookforge.auth.AuthService;
import io.github.gothsins.hookforge.auth.dto.AuthResponse;
import io.github.gothsins.hookforge.auth.dto.RegisterRequest;
import io.github.gothsins.hookforge.endpoint.WebhookEndpoint;
import io.github.gothsins.hookforge.endpoint.WebhookEndpointRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class DeliveryDispatchIT extends AbstractIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired WebhookEndpointRepository endpoints;
    @Autowired DeliveryRepository deliveries;
    @Autowired DeliveryAttemptRepository attempts;
    @Autowired DeliveryDispatcher dispatcher;
    @Autowired DeliveryRetryWorker worker;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean WebhookHttpClient http;

    @Test
    void successfulAttemptIsPersistedAndCannotRunTwice() throws Exception {
        AuthResponse owner = register();
        AuthResponse other = register();
        UUID id = createDelivery(owner, "https://receiver.example/hook");
        when(http.post(eq("https://receiver.example/hook"), any(byte[].class))).thenReturn(204);

        mvc.perform(post("/api/v1/deliveries/{id}/dispatch", id).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/deliveries/{id}/dispatch", id))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/deliveries/{id}/dispatch", id).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.lastHttpStatus").value(204));
        mvc.perform(post("/api/v1/deliveries/{id}/dispatch", id).header("Authorization", bearer(owner)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/deliveries/{id}/attempts", id).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].httpStatus").value(204));
        mvc.perform(get("/api/v1/deliveries/{id}/attempts", id).header("Authorization", bearer(other)))
                .andExpect(status().isNotFound());
        assertThat(attempts.count()).isEqualTo(1);
        verify(http).post(eq("https://receiver.example/hook"), any(byte[].class));
    }

    @Test
    void transientFailureCanBeRetriedAndPermanentFailureStops() throws Exception {
        AuthResponse owner = register();
        UUID retried = createDelivery(owner, "https://retry.example/hook");
        when(http.post(eq("https://retry.example/hook"), any(byte[].class)))
                .thenThrow(new IOException("private transport detail")).thenReturn(200);
        mvc.perform(post("/api/v1/deliveries/{id}/dispatch", retried).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"));
        mvc.perform(post("/api/v1/deliveries/{id}/dispatch", retried).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.attemptCount").value(2));

        UUID rejected = createDelivery(owner, "https://rejected.example/hook");
        when(http.post(eq("https://rejected.example/hook"), any(byte[].class))).thenReturn(400);
        mvc.perform(post("/api/v1/deliveries/{id}/dispatch", rejected).header("Authorization", bearer(owner)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEAD"));
        assertThat(attempts.count()).isEqualTo(3);
    }

    @Test
    void concurrentDispatchesSendOnlyOnce() throws Exception {
        AuthResponse owner = register();
        UUID id = createDelivery(owner, "https://concurrent.example/hook");
        CountDownLatch sending = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        when(http.post(eq("https://concurrent.example/hook"), any(byte[].class))).thenAnswer(invocation -> {
            sending.countDown();
            if (!finish.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Test send timed out");
            }
            return 204;
        });
        CompletableFuture<DeliveryStatus> first = CompletableFuture.supplyAsync(
                () -> dispatcher.dispatch(owner.accountId(), id).status());
        assertThat(sending.await(5, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<Throwable> second = CompletableFuture.supplyAsync(() -> {
            try {
                dispatcher.dispatch(owner.accountId(), id);
                return null;
            } catch (Throwable failure) {
                return failure;
            }
        });
        finish.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(DeliveryStatus.SUCCEEDED);
        assertThat(second.get(5, TimeUnit.SECONDS)).isInstanceOf(ResponseStatusException.class);
        verify(http, times(1)).post(eq("https://concurrent.example/hook"), any(byte[].class));
        assertThat(attempts.count()).isEqualTo(1);
    }

    @Test
    void workerRetriesOnlyWhenDueAndRecordsBackoff() throws Exception {
        AuthResponse owner = register();
        UUID id = createDelivery(owner, "https://scheduled.example/hook");
        when(http.post(eq("https://scheduled.example/hook"), any(byte[].class)))
                .thenReturn(503, 204);
        Instant before = Instant.now();

        assertThat(worker.processDue()).isEqualTo(1);
        Delivery failed = deliveries.findById(id).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(failed.getNextAttemptAt()).isBetween(before.plusSeconds(30), Instant.now().plusSeconds(41));
        assertThat(worker.processDue()).isZero();

        jdbc.update("UPDATE deliveries SET next_attempt_at = NOW() - INTERVAL '1 second' WHERE id = ?", id);
        assertThat(worker.processDue()).isEqualTo(1);
        Delivery succeeded = deliveries.findById(id).orElseThrow();
        assertThat(succeeded.getStatus()).isEqualTo(DeliveryStatus.SUCCEEDED);
        assertThat(succeeded.getAttemptCount()).isEqualTo(2);
        assertThat(succeeded.getNextAttemptAt()).isNull();
        assertThat(attempts.findAllByDeliveryIdOrderByAttemptNumberDesc(id)).hasSize(2);
    }

    @Test
    void uncommittedSendIsRecoveredAfterRollback() throws Exception {
        AuthResponse owner = register();
        UUID id = createDelivery(owner, "https://recovery.example/hook");
        when(http.post(eq("https://recovery.example/hook"), any(byte[].class))).thenReturn(204);

        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
            dispatcher.dispatch(owner.accountId(), id);
            throw new IllegalStateException("simulate process stop before commit");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(deliveries.findById(id).orElseThrow().getStatus()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(attempts.findAllByDeliveryIdOrderByAttemptNumberDesc(id)).isEmpty();

        assertThat(worker.processDue()).isEqualTo(1);
        assertThat(deliveries.findById(id).orElseThrow().getStatus()).isEqualTo(DeliveryStatus.SUCCEEDED);
        verify(http, times(2)).post(eq("https://recovery.example/hook"), any(byte[].class));
    }

    @Test
    void fifthTransientFailureEndsDelivery() throws Exception {
        AuthResponse owner = register();
        UUID id = createDelivery(owner, "https://always-failing.example/hook");
        when(http.post(eq("https://always-failing.example/hook"), any(byte[].class))).thenReturn(503);

        for (int number = 1; number <= 5; number++) {
            assertThat(worker.processDue()).isEqualTo(1);
            Delivery current = deliveries.findById(id).orElseThrow();
            assertThat(current.getAttemptCount()).isEqualTo(number);
            if (number < 5) {
                assertThat(current.getStatus()).isEqualTo(DeliveryStatus.FAILED);
                jdbc.update("UPDATE deliveries SET next_attempt_at = NOW() - INTERVAL '1 second' WHERE id = ?", id);
            } else {
                assertThat(current.getStatus()).isEqualTo(DeliveryStatus.DEAD);
                assertThat(current.getNextAttemptAt()).isNull();
            }
        }
        assertThat(worker.processDue()).isZero();
        assertThat(attempts.findAllByDeliveryIdOrderByAttemptNumberDesc(id)).hasSize(5);
    }

    private UUID createDelivery(AuthResponse owner, String url) throws Exception {
        WebhookEndpoint endpoint = endpoints.save(WebhookEndpoint.create("Receiver", url, owner.accountId()));
        String response = mvc.perform(post("/api/v1/events")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"test.sent\",\"payload\":{\"ok\":true}}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String eventId = JsonPath.read(response, "$.id");
        return deliveries.findAllByOwnerIdAndEventIdOrderByCreatedAtDesc(owner.accountId(), UUID.fromString(eventId))
                .stream().filter(delivery -> delivery.getEndpointId().equals(endpoint.getId()))
                .findFirst().orElseThrow().getId();
    }

    private AuthResponse register() {
        return auth.register(new RegisterRequest(UUID.randomUUID() + "@example.com", "test-password-123"));
    }

    private String bearer(AuthResponse account) {
        return "Bearer " + account.token();
    }
}
