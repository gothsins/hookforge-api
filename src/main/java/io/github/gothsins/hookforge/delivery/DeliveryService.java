package io.github.gothsins.hookforge.delivery;

import io.github.gothsins.hookforge.delivery.dto.DeliveryResponse;
import io.github.gothsins.hookforge.delivery.dto.DeliveryAttemptResponse;
import io.github.gothsins.hookforge.endpoint.WebhookEndpointRepository;
import io.github.gothsins.hookforge.event.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    private final DeliveryRepository deliveries;
    private final WebhookEndpointRepository endpoints;
    private final DeliveryAttemptRepository attempts;

    @Transactional(propagation = Propagation.MANDATORY)
    public void createPendingFor(Event event) {
        Instant now = Instant.now();
        List<Delivery> pending = endpoints.findAllByOwnerIdAndActiveTrue(event.getOwnerId())
                .stream()
                .map(endpoint -> Delivery.pending(event, endpoint, now))
                .toList();
        deliveries.saveAll(pending);
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> findAll(UUID ownerId, UUID eventId) {
        List<Delivery> found = eventId == null
                ? deliveries.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)
                : deliveries.findAllByOwnerIdAndEventIdOrderByCreatedAtDesc(ownerId, eventId);
        return found.stream().map(DeliveryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryResponse findById(UUID ownerId, UUID id) {
        Delivery delivery = deliveries.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
        return DeliveryResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAttemptResponse> findAttempts(UUID ownerId, UUID id) {
        if (deliveries.findByIdAndOwnerId(id, ownerId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found");
        }
        return attempts.findAllByDeliveryIdOrderByAttemptNumberDesc(id).stream()
                .map(DeliveryAttemptResponse::from).toList();
    }
}
