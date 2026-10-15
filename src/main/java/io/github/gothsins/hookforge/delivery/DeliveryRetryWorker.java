package io.github.gothsins.hookforge.delivery;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeliveryRetryWorker {
    private static final Logger log = LoggerFactory.getLogger(DeliveryRetryWorker.class);
    private final DeliveryRepository deliveries;
    private final DeliveryDispatcher dispatcher;

    @Value("${hookforge.dispatch.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${hookforge.dispatch.poll-delay-ms:5000}")
    public void poll() {
        if (enabled) {
            processDue();
        }
    }

    public int processDue() {
        var due = deliveries.findAllByStatusInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                List.of(DeliveryStatus.PENDING, DeliveryStatus.FAILED), Instant.now(), PageRequest.of(0, 25));
        int processed = 0;
        for (Delivery delivery : due) {
            try {
                if (dispatcher.dispatchDue(delivery.getOwnerId(), delivery.getId())) {
                    processed++;
                }
            } catch (RuntimeException exception) {
                // A later poll can retry; never log the URL, body, response or exception message.
                log.warn("Delivery {} could not be processed ({})", delivery.getId(),
                        exception.getClass().getSimpleName());
            }
        }
        return processed;
    }
}
