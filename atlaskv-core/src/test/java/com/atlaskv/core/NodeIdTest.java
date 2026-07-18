package com.atlaskv.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeIdTest {

    @Test
    @DisplayName("Valid NodeId creation succeeds")
    void testValidNodeId() {
        NodeId nodeId = NodeId.of("node-1");
        assertThat(nodeId.value()).isEqualTo("node-1");
    }

    @Test
    @DisplayName("Null value throws NullPointerException")
    void testNullValue() {
        assertThatThrownBy(() -> new NodeId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("NodeId value must not be null");
    }

    @Test
    @DisplayName("Blank value throws IllegalArgumentException")
    void testBlankValue() {
        assertThatThrownBy(() -> new NodeId("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NodeId value must not be blank");
    }

    @Test
    @DisplayName("Equality and hashCode behave correctly")
    void testEquality() {
        NodeId nodeA1 = NodeId.of("node-A");
        NodeId nodeA2 = NodeId.of("node-A");
        NodeId nodeB = NodeId.of("node-B");

        assertThat(nodeA1).isEqualTo(nodeA2);
        assertThat(nodeA1.hashCode()).isEqualTo(nodeA2.hashCode());
        assertThat(nodeA1).isNotEqualTo(nodeB);
    }
}
