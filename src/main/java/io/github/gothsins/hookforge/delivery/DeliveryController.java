package io.github.gothsins.hookforge.delivery;

import io.github.gothsins.hookforge.delivery.dto.DeliveryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping
    public List<DeliveryResponse> findAll(@AuthenticationPrincipal UUID ownerId,
                                          @RequestParam(required = false) UUID eventId) {
        return service.findAll(ownerId, eventId);
    }

    @GetMapping("/{id}")
    public DeliveryResponse findById(@AuthenticationPrincipal UUID ownerId, @PathVariable UUID id) {
        return service.findById(ownerId, id);
    }
}
