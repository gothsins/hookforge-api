package io.github.gothsins.hookforge;

import io.github.gothsins.hookforge.delivery.DeliveryRepository;
import io.github.gothsins.hookforge.endpoint.WebhookEndpointRepository;
import io.github.gothsins.hookforge.event.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@Import(AbstractIntegrationTest.Containers.class)
public abstract class AbstractIntegrationTest {

    @Autowired private DeliveryRepository deliveries;
    @Autowired private EventRepository events;
    @Autowired private WebhookEndpointRepository endpoints;

    @BeforeEach
    void clearResources() {
        deliveries.deleteAll();
        events.deleteAll();
        endpoints.deleteAll();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }
}
