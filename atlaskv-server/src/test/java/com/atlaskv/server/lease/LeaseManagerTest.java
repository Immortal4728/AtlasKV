package com.atlaskv.server.lease;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.LeaseMetrics;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LeaseManagerTest {

    private KeyValueStateMachine stateMachine;
    private NodeLifecycleManager lifecycleManager;
    private RaftNode raftNode;
    private LeaseMetrics metrics;
    private LeaseManager leaseManager;

    @BeforeEach
    void setUp() {
        stateMachine = mock(KeyValueStateMachine.class);
        lifecycleManager = mock(NodeLifecycleManager.class);
        raftNode = mock(RaftNode.class);
        metrics = mock(LeaseMetrics.class);

        when(lifecycleManager.raftNode()).thenReturn(raftNode);
        when(raftNode.role()).thenReturn(RaftRole.LEADER);

        // Mock node.handleEvent to immediately complete the future successfully
        doAnswer(invocation -> {
            com.atlaskv.core.event.RaftEvent.ClientCommandEvent event = invocation.getArgument(0);
            event.responseFuture().complete("OK".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(raftNode).handleEvent(any());

        // Construct LeaseManager
        leaseManager = new LeaseManager(stateMachine, lifecycleManager, metrics);
    }

    @Test
    @DisplayName("Create lease submits LEASE_CREATE command to Raft")
    void createLeaseSubmitsCommand() {
        leaseManager.createLease("lease123", "5s");

        ArgumentCaptor<com.atlaskv.core.event.RaftEvent.ClientCommandEvent> captor =
                ArgumentCaptor.forClass(com.atlaskv.core.event.RaftEvent.ClientCommandEvent.class);
        verify(raftNode).handleEvent(captor.capture());

        String cmd = new String(captor.getValue().command(), StandardCharsets.UTF_8);
        assertThat(cmd).isEqualTo("LEASE_CREATE lease123 5000");
        verify(metrics).recordLeaseCreated(5000);
    }

    @Test
    @DisplayName("Renew lease submits LEASE_RENEW command to Raft")
    void renewLeaseSubmitsCommand() {
        leaseManager.renewLease("lease123");

        ArgumentCaptor<com.atlaskv.core.event.RaftEvent.ClientCommandEvent> captor =
                ArgumentCaptor.forClass(com.atlaskv.core.event.RaftEvent.ClientCommandEvent.class);
        verify(raftNode).handleEvent(captor.capture());

        String cmd = new String(captor.getValue().command(), StandardCharsets.UTF_8);
        assertThat(cmd).isEqualTo("LEASE_RENEW lease123");
        verify(metrics).recordRenewal();
    }

    @Test
    @DisplayName("Revoke lease submits LEASE_REVOKE command to Raft")
    void revokeLeaseSubmitsCommand() {
        leaseManager.revokeLease("lease123");

        ArgumentCaptor<com.atlaskv.core.event.RaftEvent.ClientCommandEvent> captor =
                ArgumentCaptor.forClass(com.atlaskv.core.event.RaftEvent.ClientCommandEvent.class);
        verify(raftNode).handleEvent(captor.capture());

        String cmd = new String(captor.getValue().command(), StandardCharsets.UTF_8);
        assertThat(cmd).isEqualTo("LEASE_REVOKE lease123");
        verify(metrics).recordLeaseRevoked();
    }

    @Test
    @DisplayName("Expiration scan submits EXPIRE command for expired key TTLs when leader")
    void expirationScanSubmitsExpireForKeys() throws Exception {
        Map<String, Long> keyTtls = new HashMap<>();
        keyTtls.put("expiredKey", System.currentTimeMillis() - 1000L); // expired 1s ago
        keyTtls.put("validKey", System.currentTimeMillis() + 10000L); // valid

        when(stateMachine.keyTtls()).thenReturn(keyTtls);
        when(stateMachine.leases()).thenReturn(Collections.emptyMap());

        // Trigger manual checkExpirations (since we mocked scheduler, we can invoke private checkExpirations via reflection or call directly if we test it)
        // Let's use a reflection helper to invoke the private checkExpirations method
        java.lang.reflect.Method method = LeaseManager.class.getDeclaredMethod("checkExpirations");
        method.setAccessible(true);
        method.invoke(leaseManager);

        ArgumentCaptor<com.atlaskv.core.event.RaftEvent.ClientCommandEvent> captor =
                ArgumentCaptor.forClass(com.atlaskv.core.event.RaftEvent.ClientCommandEvent.class);
        verify(raftNode).handleEvent(captor.capture());

        String cmd = new String(captor.getValue().command(), StandardCharsets.UTF_8);
        assertThat(cmd).isEqualTo("EXPIRE expiredKey");
    }
}
