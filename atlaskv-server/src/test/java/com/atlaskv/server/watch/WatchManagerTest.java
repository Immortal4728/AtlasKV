package com.atlaskv.server.watch;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.NotLeaderException;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.WatchMetrics;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class WatchManagerTest {

    private KeyValueStateMachine stateMachine;
    private NodeLifecycleManager lifecycleManager;
    private RaftNode raftNode;
    private WatchMetrics metrics;
    private WatchManager watchManager;

    @BeforeEach
    void setUp() {
        stateMachine = Mockito.mock(KeyValueStateMachine.class);
        lifecycleManager = Mockito.mock(NodeLifecycleManager.class);
        raftNode = Mockito.mock(RaftNode.class);
        metrics = Mockito.mock(WatchMetrics.class);

        when(lifecycleManager.raftNode()).thenReturn(raftNode);
        when(raftNode.role()).thenReturn(RaftRole.LEADER);

        watchManager = new WatchManager(stateMachine, lifecycleManager, metrics);
    }

    @AfterEach
    void tearDown() {
        watchManager.close();
    }

    @Test
    @DisplayName("Successfully registers key watcher when node is leader")
    void registersKeyWatcherWhenLeader() {
        SseEmitter emitter = watchManager.register("test_key", false);
        assertThat(emitter).isNotNull();

        List<WatchSubscription> subs = watchManager.subscriptions();
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).target()).isEqualTo("test_key");
        assertThat(subs.get(0).isPrefix()).isFalse();
    }

    @Test
    @DisplayName("Throws NotLeaderException when registering watcher on a follower node")
    void throwsWhenNotLeader() {
        when(raftNode.role()).thenReturn(RaftRole.FOLLOWER);

        assertThatThrownBy(() -> watchManager.register("test_key", false))
                .isInstanceOf(NotLeaderException.class)
                .hasMessageContaining("Cannot establish watch connection");
    }

    @Test
    @DisplayName("Matches key and prefix watch subscriptions correctly")
    void subscriptionMatchesCorrectly() {
        WatchSubscription keySub = new WatchSubscription("users:123", false, null);
        WatchSubscription prefixSub = new WatchSubscription("users:", true, null);

        assertThat(keySub.matches("users:123")).isTrue();
        assertThat(keySub.matches("users:1234")).isFalse();
        assertThat(keySub.matches("other_key")).isFalse();

        assertThat(prefixSub.matches("users:123")).isTrue();
        assertThat(prefixSub.matches("users:456")).isTrue();
        assertThat(prefixSub.matches("other_users:")).isFalse();
    }

    @Test
    @DisplayName("Closes all active watchers when leadership check fails")
    void closesWatchersOnLeadershipLoss() throws Exception {
        watchManager.register("test_key", false);
        assertThat(watchManager.subscriptions()).hasSize(1);

        // Simulate leadership loss
        when(raftNode.role()).thenReturn(RaftRole.FOLLOWER);

        // Manually trigger checkLeadership
        java.lang.reflect.Method checkMethod = WatchManager.class.getDeclaredMethod("checkLeadership");
        checkMethod.setAccessible(true);
        checkMethod.invoke(watchManager);

        assertThat(watchManager.subscriptions()).isEmpty();
    }

    @Test
    @DisplayName("Closes all active watchers when server stops")
    void closesWatchersOnClose() {
        watchManager.register("test_key", false);
        assertThat(watchManager.subscriptions()).hasSize(1);

        watchManager.close();

        assertThat(watchManager.subscriptions()).isEmpty();
    }
}
