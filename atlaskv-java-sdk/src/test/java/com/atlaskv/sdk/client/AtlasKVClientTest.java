package com.atlaskv.sdk.client;

import com.atlaskv.sdk.exceptions.ConflictException;
import com.atlaskv.sdk.exceptions.NotLeaderException;
import com.atlaskv.sdk.exceptions.TimeoutException;
import com.atlaskv.sdk.models.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtlasKVClientTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private AtlasKVClient client;

    @BeforeEach
    void setUp() {
        client = AtlasKVClient.builder()
                .host("localhost")
                .port(8080)
                .timeout(Duration.ofSeconds(1))
                .build();
        
        // Inject mock HttpClient into the client's connection pool
        // Using reflection to avoid exposing public setters
        try {
            java.lang.reflect.Field poolField = AtlasKVClient.class.getDeclaredField("connectionPool");
            poolField.setAccessible(true);
            Object pool = poolField.get(client);
            
            java.lang.reflect.Field clientField = pool.getClass().getDeclaredField("httpClient");
            clientField.setAccessible(true);
            clientField.set(pool, mockHttpClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testClientBuilderDefaults() {
        AtlasKVClient defaultClient = AtlasKVClient.builder().build();
        assertThat(defaultClient.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(defaultClient.activeBaseUri()).isEqualTo(URI.create("http://localhost:8080"));
    }

    @Test
    void testSuccessfulGet() throws Exception {
        String json = "{\"key\":\"myKey\",\"value\":\"myValue\",\"exists\":true,\"version\":1,\"createdAt\":1000,\"updatedAt\":2000}";
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(json);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        KeyValue result = client.keyValue().get("myKey");

        assertThat(result.key()).isEqualTo("myKey");
        assertThat(result.value()).isEqualTo("myValue");
        assertThat(result.exists()).isTrue();
        assertThat(result.version()).isEqualTo(1);
    }

    @Test
    void testGetNotFound() throws Exception {
        when(mockResponse.statusCode()).thenReturn(404);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        KeyValue result = client.keyValue().get("missingKey");

        assertThat(result.key()).isEqualTo("missingKey");
        assertThat(result.value()).isNull();
        assertThat(result.exists()).isFalse();
    }

    @Test
    void testCasConflictThrowsConflictException() throws Exception {
        String conflictJson = "{\"expectedVersion\":1,\"currentVersion\":2,\"message\":\"Version mismatch: expected 1 but current is 2\"}";
        when(mockResponse.statusCode()).thenReturn(409);
        when(mockResponse.body()).thenReturn(conflictJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        assertThatThrownBy(() -> client.keyValue().casPut("key", "val", 1))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Version mismatch")
                .satisfies(e -> {
                    ConflictException ex = (ConflictException) e;
                    assertThat(ex.getExpectedVersion()).isEqualTo(1);
                    assertThat(ex.getCurrentVersion()).isEqualTo(2);
                });
    }

    @Test
    void testNotLeaderThrowsNotLeaderException() throws Exception {
        String notLeaderJson = "{\"detail\":\"This node is not the leader\",\"leaderId\":\"node-2\",\"leaderAddress\":\"localhost:8081\"}";
        when(mockResponse.statusCode()).thenReturn(503);
        when(mockResponse.body()).thenReturn(notLeaderJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        // When a NotLeaderException is thrown, the execute method will catch it, try to redirect, and retry.
        // In this test, we make the mock send() throw on all retries.
        assertThatThrownBy(() -> client.keyValue().put("key", "val"))
                .isInstanceOf(NotLeaderException.class)
                .satisfies(e -> {
                    NotLeaderException ex = (NotLeaderException) e;
                    assertThat(ex.getLeaderId()).isEqualTo("node-2");
                    assertThat(ex.getLeaderAddress()).isEqualTo("localhost:8081");
                });
    }

    @Test
    void testNetworkTimeoutThrowsTimeoutException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("connect timed out"));

        assertThatThrownBy(() -> client.keyValue().get("key"))
                .isInstanceOf(TimeoutException.class)
                .hasMessageContaining("Request timed out");
    }
}
