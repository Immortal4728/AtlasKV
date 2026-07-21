package com.atlaskv.server.api;

import com.atlaskv.server.api.dto.LeaseRequest;
import com.atlaskv.server.api.dto.LeaseResponse;
import com.atlaskv.server.lease.LeaseManager;
import com.atlaskv.server.statemachine.LeaseInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for cluster lease management.
 */
@RestController
@RequestMapping("/api/v1/lease")
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
     * @return response details of the created lease
     */
    @PostMapping
    @Operation(summary = "Create a lease", description = "Creates and replicates a new lease across the cluster")
    public ResponseEntity<LeaseResponse> createLease(@RequestBody @Valid LeaseRequest request) {
        String leaseId = request.leaseId();
        if (leaseId == null || leaseId.isBlank()) {
            leaseId = UUID.randomUUID().toString().substring(0, 8);
        }
        leaseManager.createLease(leaseId, request.ttl());

        // Read back lease details
        Collection<LeaseInfo> active = leaseManager.listLeases();
        for (LeaseInfo info : active) {
            if (info.leaseId().equals(leaseId)) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new LeaseResponse(info.leaseId(), info.durationMs(), info.expiryTimeMs(), info.keys()));
            }
        }
        throw new IllegalStateException("Failed to retrieve created lease info");
    }

    /**
     * Renews an active lease.
     *
     * @param leaseId lease ID to renew
     * @return OK response status
     */
    @PostMapping("/{leaseId}/renew")
    @Operation(summary = "Renew a lease", description = "Renews the lease and extends its expiration deadline")
    public ResponseEntity<Void> renewLease(@PathVariable @NotBlank String leaseId) {
        leaseManager.renewLease(leaseId);
        return ResponseEntity.ok().build();
    }

    /**
     * Revokes an active lease and expires associated keys.
     *
     * @param leaseId lease ID to revoke
     * @return OK response status
     */
    @DeleteMapping("/{leaseId}")
    @Operation(summary = "Revoke a lease", description = "Revokes the lease and deletes all associated keys")
    public ResponseEntity<Void> revokeLease(@PathVariable @NotBlank String leaseId) {
        leaseManager.revokeLease(leaseId);
        return ResponseEntity.ok().build();
    }

    /**
     * Lists all active leases in the cluster.
     *
     * @return list of leases
     */
    @GetMapping
    @Operation(summary = "List all active leases", description = "Returns a list of all active leases in the state machine")
    public ResponseEntity<Collection<LeaseResponse>> listLeases() {
        Collection<LeaseResponse> list = leaseManager.listLeases().stream()
                .map(info -> new LeaseResponse(info.leaseId(), info.durationMs(), info.expiryTimeMs(), info.keys()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}
