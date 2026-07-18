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

    private static byte[] cmd(String command) {
        return command.getBytes(StandardCharsets.UTF_8);
    }

    private static String str(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
