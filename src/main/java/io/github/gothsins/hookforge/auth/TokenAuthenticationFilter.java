package io.github.gothsins.hookforge.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final AccessTokenRepository tokens;

    public TokenAuthenticationFilter(AccessTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String value = authorization.substring(7);
            if (value.length() == 43) {
                tokens.findByTokenHashAndExpiresAtAfter(TokenDigest.sha256(value), Instant.now())
                        .ifPresent(token -> SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(token.getAccountId(), null, List.of())));
            }
        }
        chain.doFilter(request, response);
    }
}
