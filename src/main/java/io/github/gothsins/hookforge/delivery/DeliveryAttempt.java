package io.github.gothsins.hookforge.delivery;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "delivery_attempts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", length = 32)
    private DeliveryErrorCategory errorCategory;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    private DeliveryAttempt(UUID deliveryId, int attemptNumber, Integer httpStatus,
                            long durationMs, DeliveryErrorCategory errorCategory, Instant attemptedAt) {
        this.deliveryId = deliveryId;
        this.attemptNumber = attemptNumber;
        this.httpStatus = httpStatus;
        this.durationMs = durationMs;
        this.errorCategory = errorCategory;
        this.attemptedAt = attemptedAt;
    }

    public static DeliveryAttempt record(UUID deliveryId, int attemptNumber, Integer httpStatus,
                                         long durationMs, DeliveryErrorCategory errorCategory, Instant attemptedAt) {
        return new DeliveryAttempt(deliveryId, attemptNumber, httpStatus, durationMs, errorCategory, attemptedAt);
    }
}
