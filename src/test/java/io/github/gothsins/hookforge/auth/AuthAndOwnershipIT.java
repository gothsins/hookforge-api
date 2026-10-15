package io.github.gothsins.hookforge.auth;

import io.github.gothsins.hookforge.AbstractIntegrationTest;
import io.github.gothsins.hookforge.auth.dto.AuthResponse;
import io.github.gothsins.hookforge.auth.dto.RegisterRequest;
import io.github.gothsins.hookforge.endpoint.WebhookEndpoint;
import io.github.gothsins.hookforge.endpoint.WebhookEndpointRepository;
import io.github.gothsins.hookforge.event.Event;
import io.github.gothsins.hookforge.event.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthAndOwnershipIT extends AbstractIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired AuthService authService;
    @Autowired AccessTokenRepository tokens;
    @Autowired WebhookEndpointRepository endpoints;
    @Autowired EventRepository events;

    @Test
    void registerAndLoginReturnUsableTokensWithoutStoringRawValues() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String body = "{\"email\":\"" + email + "\",\"password\":\"long-test-password\"}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accountId").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresAt").exists());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        assertThat(tokens.findAll()).allSatisfy(token ->
                assertThat(token.getTokenHash()).hasSize(64));
    }

    @Test
    void invalidCredentialsAndUnauthenticatedRequestsAreRejected() throws Exception {
        AuthResponse account = register();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + account.email()
                                + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"test\",\"payload\":{}}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/endpoints")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesOnlyCurrentToken() throws Exception {
        AuthResponse account = register();
        String authorization = "Bearer " + account.token();
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.accountId().toString()));
        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", authorization))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", authorization))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anotherAccountCannotListOrReadResources() throws Exception {
        AuthResponse owner = register();
        AuthResponse other = register();
        UUID endpointId = endpoints.save(WebhookEndpoint.create(
                "Private endpoint", "https://example.com/hook", owner.accountId())).getId();
        UUID eventId = events.save(Event.create(
                "private.event", Map.of("private", true), owner.accountId())).getId();
        String otherAuthorization = "Bearer " + other.token();

        mockMvc.perform(get("/api/v1/endpoints/{id}", endpointId)
                        .header("Authorization", otherAuthorization))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/events/{id}", eventId)
                        .header("Authorization", otherAuthorization))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/endpoints")
                        .header("Authorization", otherAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/events")
                        .header("Authorization", otherAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/endpoints/{id}", endpointId)
                        .header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/events/{id}", eventId)
                        .header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk());
    }

    private AuthResponse register() {
        return authService.register(new RegisterRequest(
                UUID.randomUUID() + "@example.com", "long-test-password"));
    }
}
