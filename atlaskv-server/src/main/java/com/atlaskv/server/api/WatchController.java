package com.atlaskv.server.api;

import com.atlaskv.server.watch.WatchManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST Controller exposing SSE watch endpoints for real-time key-value mutations.
 */
@RestController
@RequestMapping("/api/v1/watch")
@Tag(name = "Watch API", description = "Real-time key and prefix watch subscriptions using Server-Sent Events (SSE)")
public class WatchController {

    private final WatchManager watchManager;

    /**
     * Constructs a WatchController.
     *
     * @param watchManager the watch manager service
     */
    public WatchController(WatchManager watchManager) {
        this.watchManager = watchManager;
    }

    /**
     * Subscribes to real-time events for a single key.
     *
     * @param key the key to watch
     * @return Server-Sent Events emitter
     */
    @GetMapping(value = "/{key}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Watch a single key",
            description = "Subscribes to mutations (PUT, DELETE) on the specified key using Server-Sent Events (SSE)")
    public SseEmitter watchKey(@PathVariable @NotBlank String key) {
        return watchManager.register(key, false);
    }

    /**
     * Subscribes to real-time events for keys matching a prefix.
     *
     * @param prefix the key prefix to watch
     * @return Server-Sent Events emitter
     */
    @GetMapping(value = "/prefix/{prefix}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Watch a key prefix",
            description = "Subscribes to mutations (PUT, DELETE) on any keys starting with the specified prefix using Server-Sent Events (SSE)")
    public SseEmitter watchPrefix(@PathVariable @NotBlank String prefix) {
        return watchManager.register(prefix, true);
    }
}
