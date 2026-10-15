package io.github.gothsins.hookforge.delivery;

import io.github.gothsins.hookforge.delivery.dto.DeliveryResponse;
import io.github.gothsins.hookforge.event.Event;
import io.github.gothsins.hookforge.event.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DeliveryDispatcher {
    private final DeliveryRepository deliveries;
    private final DeliveryAttemptRepository attempts;
    private final EventRepository events;
    private final WebhookHttpClient http;
    private final ObjectMapper json;

    @Transactional
    public DeliveryResponse dispatch(UUID ownerId, UUID id) {
        Delivery delivery = deliveries.findLocked(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
        if (delivery.getStatus() != DeliveryStatus.PENDING && delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Delivery cannot be dispatched");
        }
        Event event = events.findByIdAndOwnerId(delivery.getEventId(), ownerId).orElseThrow();
        byte[] body = json.writeValueAsBytes(Map.of(
                "id", event.getId(), "type", event.getEventType(), "payload", event.getPayload()));
        long started = System.nanoTime();
        Integer httpStatus = null;
        DeliveryErrorCategory error = null;
        DeliveryStatus result;
        try {
            httpStatus = http.post(delivery.getEndpointUrl(), body);
            if (httpStatus >= 200 && httpStatus < 300) {
                result = DeliveryStatus.SUCCEEDED;
            } else {
                error = DeliveryErrorCategory.HTTP_RESPONSE;
                result = httpStatus == 408 || httpStatus == 429 || httpStatus >= 500
                        ? DeliveryStatus.FAILED : DeliveryStatus.DEAD;
            }
        } catch (UnsafeWebhookTarget exception) {
            error = exception.category();
            result = DeliveryStatus.DEAD;
        } catch (SocketTimeoutException exception) {
            error = DeliveryErrorCategory.TIMEOUT;
            result = DeliveryStatus.FAILED;
        } catch (BlockedWebhookAddress exception) {
            error = DeliveryErrorCategory.BLOCKED_ADDRESS;
            result = DeliveryStatus.DEAD;
        } catch (UnknownHostException exception) {
            error = DeliveryErrorCategory.NETWORK;
            result = DeliveryStatus.FAILED;
        } catch (IOException exception) {
            error = DeliveryErrorCategory.NETWORK;
            result = DeliveryStatus.FAILED;
        }
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        Instant now = Instant.now();
        if (delivery.getAttemptCount() >= 4 && result == DeliveryStatus.FAILED) {
            result = DeliveryStatus.DEAD;
        }
        delivery.completeAttempt(result, httpStatus, now);
        attempts.save(DeliveryAttempt.record(id, delivery.getAttemptCount(), httpStatus, duration, error, now));
        return DeliveryResponse.from(delivery);
    }
}
