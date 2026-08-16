package com.atlaskv.server.api;

import com.atlaskv.server.api.dto.AuthInfoResponse;
import com.atlaskv.server.config.AtlasKvProperties;
import com.atlaskv.server.security.AuthenticatedPrincipal;
import com.atlaskv.server.security.NamespaceResolver;
import com.atlaskv.server.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing identity and authentication information for the current caller.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Current authenticated user and namespace information")
public class AuthController {

    private final AtlasKvProperties properties;

    public AuthController(AtlasKvProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns identity, role, and enforced namespace for the authenticated caller.
     *
     * @param request HTTP servlet request
     * @return current caller authentication and namespace details
     */
    @GetMapping("/me")
    @Operation(summary = "Get current authenticated caller",
            description = "Returns user identifier, display name, role, and enforced namespace for the active request")
    public ResponseEntity<AuthInfoResponse> getAuthInfo(HttpServletRequest request) {
        boolean authEnabled = properties.getSecurity() != null && properties.getSecurity().isAuthEnabled();
        AuthenticatedPrincipal principal = SecurityContext.getPrincipal(request)
                .or(SecurityContext::getPrincipal)
                .orElse(null);

        if (principal == null) {
            return ResponseEntity.ok(new AuthInfoResponse(
                    false,
                    "anonymous",
                    "Anonymous User",
                    "USER",
                    ""
            ));
        }

        String effectiveNamespace = NamespaceResolver.resolveNamespace(request);

        return ResponseEntity.ok(new AuthInfoResponse(
                authEnabled,
                principal.userId(),
                principal.username(),
                principal.role().name(),
                effectiveNamespace
        ));
    }
}
