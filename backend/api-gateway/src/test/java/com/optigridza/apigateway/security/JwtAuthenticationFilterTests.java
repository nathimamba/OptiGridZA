package com.optigridza.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTests {
    private static final String TEST_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(TEST_SECRET);

    @Test
    void rejectsInvalidBearerToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/companies")
                        .header("Authorization", "Bearer not-a-token")
        );

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void doesNotTreatPublicPathPrefixAsPublic() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/auth/register/extra")
        );
        AtomicReference<Boolean> forwarded = new AtomicReference<>(false);
        GatewayFilterChain chain = ignored -> {
            forwarded.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertFalse(forwarded.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void allowsRegistrationWithoutAnExistingSession() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/register")
        );
        AtomicReference<Boolean> forwarded = new AtomicReference<>(false);
        GatewayFilterChain chain = ignored -> {
            forwarded.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertTrue(forwarded.get());
    }

    @Test
    void validatesAndForwardsCookieTokenAsBearerHeader() {
        String token = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/companies")
                    .cookie(new HttpCookie("OPTIGRID_AUTH", token))
        );
        AtomicReference<String> forwardedAuthorization = new AtomicReference<>();
        GatewayFilterChain chain = forwarded -> {
            forwardedAuthorization.set(forwarded.getRequest().getHeaders().getFirst("Authorization"));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertTrue(forwardedAuthorization.get().startsWith("Bearer "));
    }
}