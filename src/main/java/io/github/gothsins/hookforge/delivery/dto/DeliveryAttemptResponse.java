package io.github.gothsins.hookforge.delivery.dto;

import io.github.gothsins.hookforge.delivery.DeliveryAttempt;
import io.github.gothsins.hookforge.delivery.DeliveryErrorCategory;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAttemptResponse(UUID id, int attemptNumber, Integer httpStatus,
                                      long durationMs, DeliveryErrorCategory errorCategory, Instant attemptedAt) {
    public static DeliveryAttemptResponse from(DeliveryAttempt attempt) {
        return new DeliveryAttemptResponse(attempt.getId(), attempt.getAttemptNumber(), attempt.getHttpStatus(),
                attempt.getDurationMs(), attempt.getErrorCategory(), attempt.getAttemptedAt());
    }
}
