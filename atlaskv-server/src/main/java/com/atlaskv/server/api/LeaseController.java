package com.atlaskv.server.api;

import com.atlaskv.server.api.dto.LeaseRequest;
import com.atlaskv.server.api.dto.LeaseResponse;
import com.atlaskv.server.lease.LeaseManager;
import com.atlaskv.server.security.NamespaceResolver;
import com.atlaskv.server.statemachine.LeaseInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for cluster lease management.
 * Lease operations are scoped to the caller's logical namespace.
 */
@RestController
@RequestMapping({"/api/v1/lease", "/api/v1/leases"})
@Tag(name = "Lease Management", description = "Endpoints for creating, renewing, and revoking distributed leases")
public class LeaseController {

    private final LeaseManager leaseManager;

    /**
     * Constructs a LeaseController.
     *
     * @param leaseManager the lease manager service
     */
    public LeaseController(LeaseManager leaseManager) {
        this.leaseManager = leaseManager;
    }

    /**
     * Creates a new distributed lease.
     *
     * @param request lease creation request
     * @param httpRequest HTTP servlet request
     * @return response details of the created lease
     */
    @PostMapping
    @Operation(summary = "Create a lease", description = "Creates and replicates a new lease across the cluster")
    public ResponseEntity<LeaseResponse> createLease(
            @RequestBody @Valid LeaseRequest request,
            HttpServletRequest httpRequest) {
        String clientLeaseId = request.leaseId();
        if (clientLeaseId == null || clientLeaseId.isBlank()) {
            clientLeaseId = UUID.randomUUID().toString().substring(0, 8);
        }

        String namespace = NamespaceResolver.resolveNamespace(httpRequest);
        String storageLeaseId = NamespaceResolver.toStorageLeaseId(clientLeaseId, namespace);

        leaseManager.createLease(storageLeaseId, request.ttl());

        // Read back lease details
        Collection<LeaseInfo> active = leaseManager.listLeases();
        for (LeaseInfo info : active) {
            if (info.leaseId().equals(storageLeaseId)) {
                Set<String> clientKeys = info.keys().stream()
                        .map(k -> NamespaceResolver.toClientKey(k, namespace))
                        .collect(Collectors.toSet());
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new LeaseResponse(clientLeaseId, info.durationMs(), info.expiryTimeMs(), clientKeys,
                                info.status() != null ? info.status().name() : "ACTIVE",
                                info.createdAtMs(), info.lastActionTimeMs()));
            }
        }
        throw new IllegalStateException("Failed to retrieve created lease info");
    }

    /**
     * Renews an active lease.
     *
     * @param leaseId lease ID to renew
     * @param httpRequest HTTP servlet request
     * @return OK response status
     */
    @PostMapping("/{leaseId}/renew")
    @Operation(summary = "Renew a lease", description = "Renews the lease and extends its expiration deadline")
    public ResponseEntity<Void> renewLease(
            @PathVariable @NotBlank String leaseId,
            HttpServletRequest httpRequest) {
        String namespace = NamespaceResolver.resolveNamespace(httpRequest);
        String storageLeaseId = NamespaceResolver.toStorageLeaseId(leaseId, namespace);
        leaseManager.renewLease(storageLeaseId);
        return ResponseEntity.ok().build();
    }

    /**
     * Revokes an active lease and expires associated keys.
     *
     * @param leaseId lease ID to revoke
     * @param httpRequest HTTP servlet request
     * @return OK response status
     */
    @DeleteMapping("/{leaseId}")
    @Operation(summary = "Revoke a lease", description = "Revokes the lease and deletes all associated keys")
    public ResponseEntity<Void> revokeLease(
            @PathVariable @NotBlank String leaseId,
            HttpServletRequest httpRequest) {
        String namespace = NamespaceResolver.resolveNamespace(httpRequest);
        String storageLeaseId = NamespaceResolver.toStorageLeaseId(leaseId, namespace);
        leaseManager.revokeLease(storageLeaseId);
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves a single lease by ID.
     *
     * @param leaseId lease ID to retrieve
     * @param httpRequest HTTP servlet request
     * @return lease details or 404 NOT_FOUND
     */
    @GetMapping("/{leaseId}")
    @Operation(summary = "Get a lease by ID", description = "Returns details and status of a specific lease")
    public ResponseEntity<LeaseResponse> getLease(
            @PathVariable @NotBlank String leaseId,
            HttpServletRequest httpRequest) {
        String namespace = NamespaceResolver.resolveNamespace(httpRequest);
        String storageLeaseId = NamespaceResolver.toStorageLeaseId(leaseId, namespace);
        LeaseInfo info = leaseManager.getLease(storageLeaseId);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        Set<String> clientKeys = info.keys().stream()
                .map(k -> NamespaceResolver.toClientKey(k, namespace))
                .collect(Collectors.toSet());
        return ResponseEntity.ok(new LeaseResponse(leaseId, info.durationMs(), info.expiryTimeMs(), clientKeys,
                info.status() != null ? info.status().name() : "ACTIVE",
                info.createdAtMs(), info.lastActionTimeMs()));
    }

    /**
     * Lists active leases within the caller's namespace.
     *
     * @param httpRequest HTTP servlet request
     * @return list of leases
     */
    @GetMapping
    @Operation(summary = "List all active leases", description = "Returns a list of active leases in the state machine")
    public ResponseEntity<Collection<LeaseResponse>> listLeases(HttpServletRequest httpRequest) {
        String namespace = NamespaceResolver.resolveNamespace(httpRequest);
        Collection<LeaseInfo> allLeases = leaseManager.listLeases();

        List<LeaseResponse> list = new ArrayList<>();
        if (namespace != null && !namespace.isBlank()) {
            String nsPrefix = "ns:" + namespace + ":";
            for (LeaseInfo info : allLeases) {
                if (info.leaseId().startsWith(nsPrefix)) {
                    String clientLeaseId = NamespaceResolver.toClientLeaseId(info.leaseId(), namespace);
                    Set<String> clientKeys = info.keys().stream()
                            .map(k -> NamespaceResolver.toClientKey(k, namespace))
                            .collect(Collectors.toSet());
                    list.add(new LeaseResponse(clientLeaseId, info.durationMs(), info.expiryTimeMs(), clientKeys,
                            info.status() != null ? info.status().name() : "ACTIVE",
                            info.createdAtMs(), info.lastActionTimeMs()));
                }
            }
        } else {
            for (LeaseInfo info : allLeases) {
                list.add(new LeaseResponse(info.leaseId(), info.durationMs(), info.expiryTimeMs(), info.keys(),
                        info.status() != null ? info.status().name() : "ACTIVE",
                        info.createdAtMs(), info.lastActionTimeMs()));
            }
        }

        return ResponseEntity.ok(list);
    }
}
