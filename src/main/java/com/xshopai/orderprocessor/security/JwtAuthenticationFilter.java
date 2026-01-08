package com.xshopai.orderprocessor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT Authentication Filter
 * Validates JWT tokens from Authorization header and sets Spring Security context
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Skip JWT validation for Dapr endpoints
        if (request.getRequestURI().startsWith("/dapr/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Skip if no Authorization header or doesn't start with Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request to {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token
        jwt = authHeader.substring(7);

        try {
            // Extract user email from token
            userEmail = jwtService.extractUsername(jwt);

            // If user is not already authenticated and token is valid
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Validate token
                if (jwtService.validateToken(jwt)) {
                    // Extract authorities/roles
                    List<GrantedAuthority> authorities = jwtService.extractAuthorities(jwt);
                    String userId = jwtService.extractUserId(jwt);
                    
                    log.debug("JWT validated for user: {} (ID: {}), roles: {}", userEmail, userId, authorities);

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            authorities
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set authentication in Security Context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.info("Authentication successful for user: {} with roles: {}", userEmail, authorities);
                } else {
                    log.warn("Invalid or expired JWT token");
                }
            }
        } catch (Exception e) {
            log.error("JWT authentication failed", e);
            // Continue the filter chain even if authentication fails
            // The security config will handle unauthorized access
        }

        filterChain.doFilter(request, response);
    }
}
