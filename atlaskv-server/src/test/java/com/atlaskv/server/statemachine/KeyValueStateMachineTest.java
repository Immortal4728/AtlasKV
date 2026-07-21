package com.atlaskv.server.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KeyValueStateMachine}.
 */
class KeyValueStateMachineTest {

    private KeyValueStateMachine sm;

    @BeforeEach
    void setUp() {
        sm = new KeyValueStateMachine();
    }

    @Nested
    @DisplayName("PUT operations")
    class PutOperations {

        @Test
        @DisplayName("PUT stores key-value pair")
        void putStoresValue() {
            byte[] result = sm.apply(cmd("PUT name Alice"));
            assertThat(str(result)).isEqualTo("OK:name");
            assertThat(sm.get("name")).hasValue("Alice");
        }

        @Test
        @DisplayName("PUT overwrites existing value")
        void putOverwrites() {
            sm.apply(cmd("PUT name Alice"));
            sm.apply(cmd("PUT name Bob"));
            assertThat(sm.get("name")).hasValue("Bob");
        }

        @Test
        @DisplayName("PUT with spaces in value")
        void putWithSpaces() {
            sm.apply(cmd("PUT msg hello world"));
            assertThat(sm.get("msg")).hasValue("hello world");
        }

        @Test
        @DisplayName("PUT without value returns error")
        void putWithoutValue() {
            byte[] result = sm.apply(cmd("PUT key"));
            assertThat(str(result)).startsWith("ERROR");
        }
    }

    @Nested
    @DisplayName("GET operations")
    class GetOperations {

        @Test
        @DisplayName("GET existing key returns value")
        void getExisting() {
            sm.apply(cmd("PUT color red"));
            byte[] result = sm.apply(cmd("GET color"));
            assertThat(str(result)).isEqualTo("VALUE:red");
        }

        @Test
        @DisplayName("GET missing key returns NOT_FOUND")
        void getMissing() {
            byte[] result = sm.apply(cmd("GET missing"));
            assertThat(str(result)).isEqualTo("NOT_FOUND:missing");
        }
    }

    @Nested
    @DisplayName("DELETE operations")
    class DeleteOperations {

        @Test
        @DisplayName("DELETE existing key returns DELETED")
        void deleteExisting() {
            sm.apply(cmd("PUT temp value"));
            byte[] result = sm.apply(cmd("DELETE temp"));
            assertThat(str(result)).isEqualTo("DELETED:temp");
            assertThat(sm.get("temp")).isEmpty();
        }

        @Test
        @DisplayName("DELETE missing key returns NOT_FOUND")
        void deleteMissing() {
            byte[] result = sm.apply(cmd("DELETE ghost"));
            assertThat(str(result)).isEqualTo("NOT_FOUND:ghost");
        }
    }

    @Nested
    @DisplayName("Snapshot operations")
    class SnapshotOperations {

        @Test
        @DisplayName("Snapshot and restore preserves state")
        void snapshotRestore() {
            sm.apply(cmd("PUT a 1"));
            sm.apply(cmd("PUT b 2"));
            sm.apply(cmd("PUT c 3"));

            byte[] snapshot = sm.takeSnapshot();
            assertThat(snapshot).isNotEmpty();

            // Restore into fresh state machine
            KeyValueStateMachine restored = new KeyValueStateMachine();
            restored.restoreSnapshot(snapshot);

            assertThat(restored.get("a")).hasValue("1");
            assertThat(restored.get("b")).hasValue("2");
            assertThat(restored.get("c")).hasValue("3");
            assertThat(restored.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("Restore from empty snapshot clears state")
        void restoreEmpty() {
            sm.apply(cmd("PUT key val"));
            sm.restoreSnapshot(new byte[0]);
            assertThat(sm.size()).isZero();
        }

        @Test
        @DisplayName("Restore from null snapshot clears state")
        void restoreNull() {
            sm.apply(cmd("PUT key val"));
            sm.restoreSnapshot(null);
            assertThat(sm.size()).isZero();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Invalid command returns error")
        void invalidCommand() {
            byte[] result = sm.apply(cmd("INVALID"));
            assertThat(str(result)).startsWith("ERROR");
        }

        @Test
        @DisplayName("Unknown operation returns error")
        void unknownOperation() {
            byte[] result = sm.apply(cmd("PATCH key value"));
            assertThat(str(result)).startsWith("ERROR");
        }

        @Test
        @DisplayName("Size returns correct count")
        void sizeCorrect() {
            assertThat(sm.size()).isZero();
            sm.apply(cmd("PUT a 1"));
            sm.apply(cmd("PUT b 2"));
            assertThat(sm.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("Snapshot returns unmodifiable map")
        void snapshotMap() {
            sm.apply(cmd("PUT x y"));
            assertThat(sm.snapshot()).containsEntry("x", "y");
        }
    }

    @Nested
    @DisplayName("CAS and Metadata operations")
    class CasAndMetadataOperations {

        @Test
        @DisplayName("PUT initializes version to 1 and increments on updates")
        void putMetadataVersion() {
            sm.apply(cmd("PUT user Alice"));
            var meta = sm.metadata().get("user");
            assertThat(meta).isNotNull();
            assertThat(meta.version()).isEqualTo(1);
            assertThat(meta.createdAt()).isPositive();
            assertThat(meta.updatedAt()).isEqualTo(meta.createdAt());

            sm.apply(cmd("PUT user Bob"));
            meta = sm.metadata().get("user");
            assertThat(meta.version()).isEqualTo(2);
            assertThat(meta.updatedAt()).isGreaterThanOrEqualTo(meta.createdAt());
        }

        @Test
        @DisplayName("CAS PUT succeeds if expected version matches")
        void casPutSucceeds() {
            sm.apply(cmd("PUT counter 10")); // Initial version is 1
            byte[] result = sm.apply(cmd("CAS_PUT counter 1 20"));
            assertThat(str(result)).isEqualTo("OK:counter");
            assertThat(sm.get("counter")).hasValue("20");

            var meta = sm.metadata().get("counter");
            assertThat(meta.version()).isEqualTo(2);
        }

        @Test
        @DisplayName("CAS PUT fails with conflict if expected version mismatches")
        void casPutConflict() {
            sm.apply(cmd("PUT counter 10")); // Initial version is 1
            byte[] result = sm.apply(cmd("CAS_PUT counter 5 20")); // Expected version 5, current is 1
            assertThat(str(result)).isEqualTo("CONFLICT:expected=5,current=1");
            assertThat(sm.get("counter")).hasValue("10"); // No update

            var meta = sm.metadata().get("counter");
            assertThat(meta.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("CAS PUT succeeds on non-existent key if expected version is 0")
        void casPutNewKey() {
            byte[] result = sm.apply(cmd("CAS_PUT counter 0 100"));
            assertThat(str(result)).isEqualTo("OK:counter");
            assertThat(sm.get("counter")).hasValue("100");

            var meta = sm.metadata().get("counter");
            assertThat(meta.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("DELETE cleans up metadata")
        void deleteCleansMetadata() {
            sm.apply(cmd("PUT temp val"));
            assertThat(sm.metadata()).containsKey("temp");

            sm.apply(cmd("DELETE temp"));
            assertThat(sm.metadata()).doesNotContainKey("temp");
        }

        @Test
        @DisplayName("Snapshot preserves KeyMetadata version and timestamps")
        void snapshotPreservesMetadata() throws Exception {
            sm.apply(cmd("PUT k1 v1")); // version 1
            sm.apply(cmd("PUT k1 v2")); // version 2

            byte[] snapshot = sm.takeSnapshot();
            assertThat(snapshot).isNotEmpty();

            KeyValueStateMachine restored = new KeyValueStateMachine();
            restored.restoreSnapshot(snapshot);

            var meta = restored.metadata().get("k1");
            assertThat(meta).isNotNull();
            assertThat(meta.version()).isEqualTo(2);
            assertThat(meta.createdAt()).isEqualTo(sm.metadata().get("k1").createdAt());
            assertThat(meta.updatedAt()).isEqualTo(sm.metadata().get("k1").updatedAt());
        }
    }

    @Nested
    @DisplayName("Prefix Query operations")
    class PrefixQueryOperations {

        @Test
        @DisplayName("getByPrefix returns matching keys sorted")
        void prefixReturnsMatchingKeysSorted() {
            sm.apply(cmd("PUT config/db/url jdbc:mysql"));
            sm.apply(cmd("PUT config/db/user root"));
            sm.apply(cmd("PUT config/cache/enabled true"));
            sm.apply(cmd("PUT users/100/profile John"));

            var results = sm.getByPrefix("config/");
            assertThat(results).hasSize(3);
            assertThat(results.get(0).getKey()).isEqualTo("config/cache/enabled");
            assertThat(results.get(1).getKey()).isEqualTo("config/db/url");
            assertThat(results.get(2).getKey()).isEqualTo("config/db/user");
        }

        @Test
        @DisplayName("getByPrefix with deeper prefix narrows results")
        void prefixNarrows() {
            sm.apply(cmd("PUT config/db/url jdbc:mysql"));
            sm.apply(cmd("PUT config/db/user root"));
            sm.apply(cmd("PUT config/cache/enabled true"));

            var results = sm.getByPrefix("config/db/");
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getKey()).isEqualTo("config/db/url");
            assertThat(results.get(1).getKey()).isEqualTo("config/db/user");
        }

        @Test
        @DisplayName("getByPrefix returns empty list for no matches")
        void prefixNoMatches() {
            sm.apply(cmd("PUT config/x y"));
            var results = sm.getByPrefix("users/");
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("getByPrefix on empty store returns empty list")
        void prefixEmptyStore() {
            var results = sm.getByPrefix("any/");
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("getByPrefix excludes deleted keys")
        void prefixExcludesDeleted() {
            sm.apply(cmd("PUT ns/a 1"));
            sm.apply(cmd("PUT ns/b 2"));
            sm.apply(cmd("DELETE ns/a"));

            var results = sm.getByPrefix("ns/");
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getKey()).isEqualTo("ns/b");
        }

        @Test
        @DisplayName("getByPrefix works with TTL keys")
        void prefixWithTtlKeys() {
            sm.apply(cmd("PUT_TTL ns/ttl 30s NULL val1"));
            sm.apply(cmd("PUT ns/plain val2"));

            var results = sm.getByPrefix("ns/");
            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("getByPrefix survives snapshot and restore")
        void prefixSurvivesSnapshot() {
            sm.apply(cmd("PUT data/a 1"));
            sm.apply(cmd("PUT data/b 2"));
            sm.apply(cmd("PUT other/c 3"));

            byte[] snapshot = sm.takeSnapshot();
            KeyValueStateMachine restored = new KeyValueStateMachine();
            restored.restoreSnapshot(snapshot);

            var results = restored.getByPrefix("data/");
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getKey()).isEqualTo("data/a");
            assertThat(results.get(1).getKey()).isEqualTo("data/b");
        }

        @Test
        @DisplayName("getByPrefix returns metadata for matching keys")
        void prefixMetadata() {
            sm.apply(cmd("PUT ns/x val1"));
            sm.apply(cmd("PUT ns/x val2")); // version 2

            var results = sm.getByPrefix("ns/");
            assertThat(results).hasSize(1);
            var meta = sm.metadata().get("ns/x");
            assertThat(meta.version()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Version History operations")
    class VersionHistoryOperations {

        @Test
        @DisplayName("PUT_HIST logs revision and nodeId")
        void putHistLogsRevision() {
            byte[] result = sm.apply(cmd("PUT_HIST nodeA mykey valueA"));
            assertThat(str(result)).isEqualTo("OK:mykey");

            var historyList = sm.history().get("mykey");
            assertThat(historyList).isNotNull().hasSize(1);
            var rev = historyList.get(0);
            assertThat(rev.revisionNumber()).isEqualTo(1);
            assertThat(rev.value()).isEqualTo("valueA");
            assertThat(rev.operation()).isEqualTo("PUT");
            assertThat(rev.nodeId()).isEqualTo("nodeA");
        }

        @Test
        @DisplayName("PUT_TTL_HIST logs TTL revision")
        void putTtlHistLogsRevision() {
            sm.apply(cmd("LEASE_CREATE lease123 5000"));
            byte[] result = sm.apply(cmd("PUT_TTL_HIST nodeB keyTtl 10s lease123 valueTtl"));
            assertThat(str(result)).isEqualTo("OK:keyTtl");

            var historyList = sm.history().get("keyTtl");
            assertThat(historyList).isNotNull().hasSize(1);
            var rev = historyList.get(0);
            assertThat(rev.value()).isEqualTo("valueTtl");
            assertThat(rev.operation()).isEqualTo("PUT");
            assertThat(rev.nodeId()).isEqualTo("nodeB");
            assertThat(rev.leaseId()).isEqualTo("lease123");
            assertThat(rev.ttl()).isEqualTo("10s");
        }

        @Test
        @DisplayName("DELETE_HIST logs DELETE revision")
        void deleteHistLogsRevision() {
            sm.apply(cmd("PUT_HIST nodeA keyDel val1"));
            byte[] result = sm.apply(cmd("DELETE_HIST nodeB keyDel"));
            assertThat(str(result)).isEqualTo("DELETED:keyDel");

            var historyList = sm.history().get("keyDel");
            assertThat(historyList).isNotNull().hasSize(2);
            var first = historyList.get(0);
            var second = historyList.get(1);

            assertThat(first.value()).isEqualTo("val1");
            assertThat(first.operation()).isEqualTo("PUT");

            assertThat(second.value()).isNull();
            assertThat(second.operation()).isEqualTo("DELETE");
            assertThat(second.nodeId()).isEqualTo("nodeB");
        }

        @Test
        @DisplayName("CAS_PUT_HIST logs CAS revision")
        void casPutHistLogsRevision() {
            sm.apply(cmd("PUT_HIST nodeA keyCas val1")); // version 1
            byte[] result = sm.apply(cmd("CAS_PUT_HIST nodeB keyCas 1 val2")); // expected version 1
            assertThat(str(result)).isEqualTo("OK:keyCas");

            var historyList = sm.history().get("keyCas");
            assertThat(historyList).isNotNull().hasSize(2);
            assertThat(historyList.get(1).value()).isEqualTo("val2");
            assertThat(historyList.get(1).operation()).isEqualTo("PUT");
            assertThat(historyList.get(1).nodeId()).isEqualTo("nodeB");
        }

        @Test
        @DisplayName("EXPIRE_HIST logs EXPIRE revision")
        void expireHistLogsRevision() {
            sm.apply(cmd("PUT_HIST nodeA keyExp val1"));
            byte[] result = sm.apply(cmd("EXPIRE_HIST nodeB keyExp"));
            assertThat(str(result)).isEqualTo("EXPIRED:keyExp");

            var historyList = sm.history().get("keyExp");
            assertThat(historyList).isNotNull().hasSize(2);
            assertThat(historyList.get(1).value()).isNull();
            assertThat(historyList.get(1).operation()).isEqualTo("EXPIRE");
            assertThat(historyList.get(1).nodeId()).isEqualTo("nodeB");
        }

        @Test
        @DisplayName("ROLLBACK reverts state and logs a rollback revision")
        void rollbackRevertsState() {
            sm.apply(cmd("PUT_HIST nodeA keyRb val1")); // revision 1
            sm.apply(cmd("PUT_HIST nodeA keyRb val2")); // revision 2
            sm.apply(cmd("PUT_HIST nodeA keyRb val3")); // revision 3

            // Rollback to revision 2
            byte[] result = sm.apply(cmd("ROLLBACK nodeB keyRb 2"));
            assertThat(str(result)).isEqualTo("OK:keyRb:4");

            assertThat(sm.get("keyRb")).hasValue("val2");

            var historyList = sm.history().get("keyRb");
            assertThat(historyList).isNotNull().hasSize(4);

            var rollbackRev = historyList.get(3);
            assertThat(rollbackRev.revisionNumber()).isEqualTo(4);
            assertThat(rollbackRev.value()).isEqualTo("val2");
            assertThat(rollbackRev.operation()).isEqualTo("ROLLBACK");
            assertThat(rollbackRev.nodeId()).isEqualTo("nodeB");
        }

        @Test
        @DisplayName("ROLLBACK to nonexistent revision returns error")
        void rollbackNonexistentReturnsError() {
            sm.apply(cmd("PUT_HIST nodeA keyRb val1"));
            byte[] result = sm.apply(cmd("ROLLBACK nodeB keyRb 99"));
            assertThat(str(result)).startsWith("ERROR");
        }

        @Test
        @DisplayName("History survives snapshot and restore")
        void historySurvivesSnapshot() {
            sm.apply(cmd("PUT_HIST nodeA keySnap val1"));
            sm.apply(cmd("DELETE_HIST nodeB keySnap"));

            byte[] snapshot = sm.takeSnapshot();
            KeyValueStateMachine restored = new KeyValueStateMachine();
            restored.restoreSnapshot(snapshot);

            var historyList = restored.history().get("keySnap");
            assertThat(historyList).isNotNull().hasSize(2);
            assertThat(historyList.get(0).value()).isEqualTo("val1");
            assertThat(historyList.get(1).operation()).isEqualTo("DELETE");
            assertThat(historyList.get(1).nodeId()).isEqualTo("nodeB");
        }
    }

    private static byte[] cmd(String command) {
        return command.getBytes(StandardCharsets.UTF_8);
    }

    private static String str(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
