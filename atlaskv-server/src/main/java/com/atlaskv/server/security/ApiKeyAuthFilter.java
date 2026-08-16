package com.atlaskv.server.security;

import com.atlaskv.server.config.AtlasKvProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * Filter that enforces API authentication when enabled.
 * Supports {@code Authorization: Bearer <token>}, {@code Authorization: ApiKey <token>},
 * and {@code X-API-Key: <token>} headers.
 *
 * <p>When authentication succeeds, the resolved {@link AuthenticatedPrincipal} is placed
 * into the {@link SecurityContext} for downstream access. When authentication is disabled,
 * a default local-development principal with ADMIN role is injected.</p>
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    /** Default principal used when authentication is disabled (local development). */
    private static final AuthenticatedPrincipal LOCAL_DEV_PRINCIPAL =
            new AuthenticatedPrincipal("local-dev", "Local Developer", UserRole.ADMIN);

    private final AtlasKvProperties properties;
    private final AuthenticationService authenticationService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the auth filter.
     *
     * @param properties            server configuration properties
     * @param authenticationService the authentication service
     * @param objectMapper          Jackson JSON object mapper
     */
    public ApiKeyAuthFilter(AtlasKvProperties properties,
                            AuthenticationService authenticationService,
                            ObjectMapper objectMapper) {
        this.properties = properties;
        this.authenticationService = authenticationService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            AtlasKvProperties.SecurityProperties sec = properties.getSecurity();
            if (sec == null || !sec.isAuthEnabled()) {
                SecurityContext.setPrincipal(request, LOCAL_DEV_PRINCIPAL);
                filterChain.doFilter(request, response);
                return;
            }

            String path = request.getRequestURI();

            // Allow CORS pre-flight requests without credentials
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            // Exempt endpoints: only health checking and internal error dispatch
            if (isExemptPath(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!authenticationService.hasConfiguredKeys()) {
                writeProblemResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Authentication is enabled but no API keys are configured on the server",
                        "https://atlaskv.dev/errors/config", "Configuration Error", path);
                return;
            }

            String providedToken = extractToken(request);
            if (providedToken == null || providedToken.isBlank()) {
                writeProblemResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Authentication required. Provide a valid Bearer token or X-API-Key header.",
                        "https://atlaskv.dev/errors/unauthorized", "Unauthorized", path);
                return;
            }

            Optional<AuthenticatedPrincipal> principal = authenticationService.authenticate(providedToken);
            if (principal.isEmpty()) {
                writeProblemResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        "Invalid authentication credentials.",
                        "https://atlaskv.dev/errors/forbidden", "Forbidden", path);
                return;
            }

            SecurityContext.setPrincipal(request, principal.get());
            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }

    private boolean isExemptPath(String path) {
        return path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.equals("/error");
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            if (authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return authHeader.substring(7).trim();
            }
            if (authHeader.regionMatches(true, 0, "ApiKey ", 0, 7)) {
                return authHeader.substring(7).trim();
            }
            return authHeader.trim();
        }
        String apiKeyHeader = request.getHeader("X-API-Key");
        if (apiKeyHeader != null && !apiKeyHeader.isBlank()) {
            return apiKeyHeader.trim();
        }
        String queryApiKey = request.getParameter("apiKey");
        if (queryApiKey != null && !queryApiKey.isBlank()) {
            return queryApiKey.trim();
        }
        String queryToken = request.getParameter("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken.trim();
        }
        return null;
    }

    private void writeProblemResponse(HttpServletResponse response, int status, String detail,
                                      String typeUri, String title, String instance) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(status), detail);
        problem.setType(URI.create(typeUri));
        problem.setTitle(title);
        problem.setInstance(URI.create(instance));
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
