package com.optigridza.notificationservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String email     = jwtService.extractEmail(jwt);
            final String role      = jwtService.extractRole(jwt);
            final String companyId = jwtService.extractCompanyId(jwt);
            log.info("Extracted JWT Data - Email: {}, Role: {}, CompanyId: {}", email, role, companyId);
            if (email != null && !jwtService.isTokenExpired(jwt) &&
                    SecurityContextHolder.getContext()
                            .getAuthentication() == null) {

                // ROLE_ prefix is required for hasRole() to work
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + role);

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                companyId,      // stored as credentials
                                List.of(authority)
                        );

                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);

                log.debug("Authenticated: {} role: {} company: {}",
                        email, role, companyId);
            }else{
                log.warn("Context not set. Email null? {} Expired? {} Auth existing? {}",
                        email == null, jwtService.isTokenExpired(jwt), SecurityContextHolder.getContext().getAuthentication() != null);
            }

        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
