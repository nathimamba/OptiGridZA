package com.optigridza.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final String AUTH_COOKIE = "OPTIGRID_AUTH";
    private final javax.crypto.SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isLogin(path)) {
            return chain.filter(exchange);
        }

        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String token;
        boolean fromCookie = false;
        if (authHeader != null) {
            if (!authHeader.startsWith("Bearer ") || authHeader.length() <= "Bearer ".length()) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            token = authHeader.substring("Bearer ".length());
        } else if (exchange.getRequest().getCookies().getFirst(AUTH_COOKIE) != null) {
            token = exchange.getRequest().getCookies().getFirst(AUTH_COOKIE).getValue();
            fromCookie = true;
        } else {
            if (isRegister(path)) {
                return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        if (fromCookie) {
            exchange = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set("Authorization", "Bearer " + token)))
                .build();
        }
        return chain.filter(exchange);
    }

    private boolean isLogin(String path) {
        return "/api/v1/auth/login".equals(path);
    }

    private boolean isRegister(String path) {
        return "/api/v1/auth/register".equals(path);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
