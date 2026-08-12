package com.gemstore.backend.config;

import com.gemstore.backend.security.CustomUserDetails;
import com.gemstore.backend.services.auth.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JWTService jwtService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip filter for auth endpoints
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/auth/password/") ||
                path.startsWith("/oauth2") ||
                path.startsWith("/login/oauth2") ||
                // Skip filter for Swagger UI
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String correlationId =
                request.getHeader("X-Correlation-ID");

        log.info(
                "Request: {} {} | Correlation ID: {}",
                request.getMethod(),
                request.getRequestURI(),
                correlationId
        );

        String requestURI = request.getRequestURI();
        log.debug("[JWT Filter] Processing: {} {}", request.getMethod(), requestURI);

        // Extract token from Authorization header
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JWT Filter] No Bearer token found");
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Ignore literal "null", "undefined", or empty strings sent by frontend JS
        if (token.isBlank() || token.equals("null") || token.equals("undefined")) {
            log.debug("[JWT Filter] Token is null/undefined string from client. Skipping.");
            chain.doFilter(request, response);
            return;
        }

        log.debug("[JWT Filter] Token found: {}...", token.substring(0, Math.min(20, token.length())));

        try {
            // Extract username from token
            String username = jwtService.extractUsername(token);

            if (username == null) {
                log.warn("[JWT Filter] Username is null in token");
                chain.doFilter(request, response);
                return;
            }

            log.debug("[JWT Filter] Extracted username: {}", username);

            // Only set authentication if not already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                // Validate token
                if (!jwtService.isValid(token, username)) {
                    log.warn("[JWT Filter] Token validation failed for user: {}", username);
                    chain.doFilter(request, response);
                    return;
                }

                log.debug("[JWT Filter] Token is valid");

                // Extract user ID and role from JWT claims
                Long userId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);

                if (userId == null) {
                    log.error("[JWT Filter] USER ID IS NULL! Token doesn't contain 'uid' claim");
                    chain.doFilter(request, response);
                    return;
                }

                // Create CustomUserDetails directly from JWT claims
                CustomUserDetails userDetails = new CustomUserDetails(
                        userId,
                        username,
                        role != null ? role : "USER"
                );

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            //  Change log.error(..., e) to log.warn(...) so it doesn't print a 100-line stack trace!
            log.warn("[JWT Filter] Error during JWT authentication: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}