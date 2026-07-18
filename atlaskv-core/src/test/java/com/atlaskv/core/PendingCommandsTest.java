package com.atlaskv.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PendingCommandsTest {

    private PendingCommands pendingCommands;

    @BeforeEach
    void setUp() {
        pendingCommands = new PendingCommands();
    }

    @Test
    @DisplayName("Register and remove returns the correct future")
    void registerAndRemove() {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        pendingCommands.register(1L, future);

        CompletableFuture<byte[]> removed = pendingCommands.remove(1L);
        assertThat(removed).isSameAs(future);
    }

    @Test
    @DisplayName("Remove returns null for unknown index")
    void removeUnknown() {
        assertThat(pendingCommands.remove(99L)).isNull();
    }

    @Test
    @DisplayName("failAll completes all pending futures exceptionally")
    void failAll() {
        CompletableFuture<byte[]> f1 = new CompletableFuture<>();
        CompletableFuture<byte[]> f2 = new CompletableFuture<>();
        pendingCommands.register(1L, f1);
        pendingCommands.register(2L, f2);

        pendingCommands.failAll(new IllegalStateException("lost leadership"));

        assertThat(f1.isCompletedExceptionally()).isTrue();
        assertThat(f2.isCompletedExceptionally()).isTrue();

        assertThatThrownBy(f1::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("failAll clears the pending map")
    void failAllClears() {
        pendingCommands.register(1L, new CompletableFuture<>());
        pendingCommands.failAll(new RuntimeException("test"));

        assertThat(pendingCommands.remove(1L)).isNull();
    }
}
