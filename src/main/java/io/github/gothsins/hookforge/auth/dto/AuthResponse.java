package io.github.gothsins.hookforge.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(UUID accountId, String email, String token, Instant expiresAt) {}
