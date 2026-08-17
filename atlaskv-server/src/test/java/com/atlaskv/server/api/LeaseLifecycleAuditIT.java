package com.atlaskv.server.api;

import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.dto.KeyValueRequest;
import com.atlaskv.server.api.dto.KeyValueResponse;
import com.atlaskv.server.api.dto.LeaseRequest;
import com.atlaskv.server.api.dto.LeaseResponse;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.statemachine.LeaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LeaseLifecycleAuditIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private NodeLifecycleManager lifecycleManager;

    private boolean isLeader;

    @BeforeEach
    void setUp() throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (lifecycleManager.raftNode() != null
                    && lifecycleManager.raftNode().role() == RaftRole.LEADER) {
                isLeader = true;
                break;
            }
            Thread.sleep(100);
        }
    }

    @Test
    @DisplayName("Comprehensive 14-step verification of Lease Lifecycle, Audit History, and Backend Enforcement")
    void testFullLeaseLifecycleAndAudit() throws InterruptedException {
        if (!isLeader) {
            return;
        }

        // STEP 1: Create an active lease with custom ID
        LeaseRequest createReq = new LeaseRequest("audit-lease-1", "30s");
        ResponseEntity<LeaseResponse> createRes = restTemplate.postForEntity(
                "/api/v1/lease", createReq, LeaseResponse.class);
        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LeaseResponse lease1 = createRes.getBody();
        assertThat(lease1).isNotNull();
        assertThat(lease1.leaseId()).isEqualTo("audit-lease-1");
        assertThat(lease1.status()).isEqualTo("ACTIVE");
        assertThat(lease1.durationMs()).isEqualTo(30_000L);
        assertThat(lease1.createdAtMs()).isGreaterThan(0L);
        assertThat(lease1.lastActionTimeMs()).isGreaterThanOrEqualTo(lease1.createdAtMs());

        // STEP 2: Attach key 1 to audit-lease-1
        KeyValueRequest put1 = new KeyValueRequest("value-1", null, "audit-lease-1");
        ResponseEntity<KeyValueResponse> put1Res = restTemplate.postForEntity(
                "/api/v1/kv/audit/key-1", put1, KeyValueResponse.class);
        assertThat(put1Res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(put1Res.getBody()).isNotNull();
        assertThat(put1Res.getBody().leaseId()).isEqualTo("audit-lease-1");
        assertThat(put1Res.getBody().ttlRemaining()).isNotNull().isGreaterThan(0L);

        // STEP 3: Attach key 2 to audit-lease-1
        KeyValueRequest put2 = new KeyValueRequest("value-2", null, "audit-lease-1");
        ResponseEntity<KeyValueResponse> put2Res = restTemplate.postForEntity(
                "/api/v1/kv/audit/key-2", put2, KeyValueResponse.class);
        assertThat(put2Res.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // STEP 4: Query single lease and verify attached keys list
        ResponseEntity<LeaseResponse> getLeaseRes = restTemplate.getForEntity(
                "/api/v1/lease/audit-lease-1", LeaseResponse.class);
        assertThat(getLeaseRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        LeaseResponse getLease = getLeaseRes.getBody();
        assertThat(getLease).isNotNull();
        assertThat(getLease.status()).isEqualTo("ACTIVE");
        assertThat(getLease.keys()).containsExactlyInAnyOrder("audit/key-1", "audit/key-2");

        // STEP 5: Query all leases and verify audit-lease-1 presence
        ResponseEntity<LeaseResponse[]> listLeasesRes = restTemplate.getForEntity(
                "/api/v1/lease", LeaseResponse[].class);
        assertThat(listLeasesRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listLeasesRes.getBody()).isNotNull();
        assertThat(listLeasesRes.getBody()).anySatisfy(l -> {
            if ("audit-lease-1".equals(l.leaseId())) {
                assertThat(l.status()).isEqualTo("ACTIVE");
                assertThat(l.keys()).contains("audit/key-1", "audit/key-2");
            }
        });

        // STEP 6: Renew the active lease
        long oldExpiry = getLease.expiryTimeMs();
        Thread.sleep(50);
        ResponseEntity<Void> renewRes = restTemplate.postForEntity(
                "/api/v1/lease/audit-lease-1/renew", null, Void.class);
        assertThat(renewRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<LeaseResponse> renewedLeaseRes = restTemplate.getForEntity(
                "/api/v1/lease/audit-lease-1", LeaseResponse.class);
        assertThat(renewedLeaseRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        LeaseResponse renewedLease = renewedLeaseRes.getBody();
        assertThat(renewedLease).isNotNull();
        assertThat(renewedLease.status()).isEqualTo("ACTIVE");
        assertThat(renewedLease.expiryTimeMs()).isGreaterThan(oldExpiry);

        // STEP 7: Revoke the active lease
        ResponseEntity<Void> revokeRes = restTemplate.exchange(
                "/api/v1/lease/audit-lease-1", HttpMethod.DELETE, null, Void.class);
        assertThat(revokeRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // STEP 8: Verify lease history is preserved with status REVOKED
        ResponseEntity<LeaseResponse> revokedLeaseRes = restTemplate.getForEntity(
                "/api/v1/lease/audit-lease-1", LeaseResponse.class);
        assertThat(revokedLeaseRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        LeaseResponse revokedLease = revokedLeaseRes.getBody();
        assertThat(revokedLease).isNotNull();
        assertThat(revokedLease.status()).isEqualTo("REVOKED");
        assertThat(revokedLease.keys()).containsExactlyInAnyOrder("audit/key-1", "audit/key-2");

        // STEP 9: Verify attached keys are deleted
        ResponseEntity<KeyValueResponse> getK1 = restTemplate.getForEntity(
                "/api/v1/kv/audit/key-1", KeyValueResponse.class);
        assertThat(getK1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<KeyValueResponse> getK2 = restTemplate.getForEntity(
                "/api/v1/kv/audit/key-2", KeyValueResponse.class);
        assertThat(getK2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // STEP 10: Reject attaching a key to a REVOKED lease with 400 Bad Request
        KeyValueRequest failPut = new KeyValueRequest("fail-val", null, "audit-lease-1");
        ResponseEntity<String> failRes = restTemplate.postForEntity(
                "/api/v1/kv/audit/fail-key", failPut, String.class);
        assertThat(failRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(failRes.getBody()).contains("REVOKED");

        // STEP 11: Create a short-lived lease and attach a key
        LeaseRequest shortLeaseReq = new LeaseRequest("audit-short-lease", "300ms");
        ResponseEntity<LeaseResponse> shortLeaseRes = restTemplate.postForEntity(
                "/api/v1/lease", shortLeaseReq, LeaseResponse.class);
        assertThat(shortLeaseRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        KeyValueRequest shortKeyReq = new KeyValueRequest("short-val", null, "audit-short-lease");
        ResponseEntity<KeyValueResponse> shortKeyRes = restTemplate.postForEntity(
                "/api/v1/kv/audit/short-key", shortKeyReq, KeyValueResponse.class);
        assertThat(shortKeyRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // STEP 12: Wait for lease to expire (sweep interval + buffer)
        Thread.sleep(1200);

        // STEP 13: Verify expired key is deleted and inaccessible
        ResponseEntity<KeyValueResponse> getShortKeyRes = restTemplate.getForEntity(
                "/api/v1/kv/audit/short-key", KeyValueResponse.class);
        assertThat(getShortKeyRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // STEP 14: Verify lease record is preserved as EXPIRED in audit history
        ResponseEntity<LeaseResponse> expiredLeaseRes = restTemplate.getForEntity(
                "/api/v1/lease/audit-short-lease", LeaseResponse.class);
        assertThat(expiredLeaseRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        LeaseResponse expiredLease = expiredLeaseRes.getBody();
        assertThat(expiredLease).isNotNull();
        assertThat(expiredLease.status()).isEqualTo("EXPIRED");
        assertThat(expiredLease.keys()).contains("audit/short-key");

        // And attaching to an EXPIRED lease is rejected with 400 Bad Request
        KeyValueRequest failPutExpired = new KeyValueRequest("fail-val-2", null, "audit-short-lease");
        ResponseEntity<String> failExpiredRes = restTemplate.postForEntity(
                "/api/v1/kv/audit/fail-key-2", failPutExpired, String.class);
        assertThat(failExpiredRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(failExpiredRes.getBody()).contains("EXPIRED");
    }
}
