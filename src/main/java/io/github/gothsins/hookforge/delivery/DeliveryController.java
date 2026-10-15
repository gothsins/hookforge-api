package io.github.gothsins.hookforge.delivery;

import io.github.gothsins.hookforge.delivery.dto.DeliveryResponse;
import io.github.gothsins.hookforge.delivery.dto.DeliveryAttemptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {
    private final DeliveryService service;
    private final DeliveryDispatcher dispatcher;

    @GetMapping
    public List<DeliveryResponse> findAll(@AuthenticationPrincipal UUID ownerId,
                                          @RequestParam(required = false) UUID eventId) {
        return service.findAll(ownerId, eventId);
    }

    @GetMapping("/{id}")
    public DeliveryResponse findById(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID id) {
        return service.findById(ownerId, id);
    }

    @GetMapping("/{id}/attempts")
    public List<DeliveryAttemptResponse> findAttempts(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID id) {
        return service.findAttempts(ownerId, id);
    }

    @PostMapping("/{id}/dispatch")
    public DeliveryResponse dispatch(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID id) {
        return dispatcher.dispatch(ownerId, id);
    }
}
