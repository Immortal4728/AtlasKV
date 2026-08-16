package com.atlaskv.server.api;

import com.atlaskv.server.security.NamespaceResolver;
import com.atlaskv.server.watch.WatchManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST Controller exposing SSE watch endpoints for real-time key-value mutations.
 * Watch streams are isolated to the caller's logical namespace.
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
     * Subscribes to real-time events for a single key within the caller's namespace.
     *
     * @param key the key to watch
     * @param request HTTP request
     * @return Server-Sent Events emitter
     */
    @GetMapping(value = "/{key}/**", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Watch a single key",
            description = "Subscribes to mutations (PUT, DELETE) on the specified key using Server-Sent Events (SSE)")
    public SseEmitter watchKey(
            @PathVariable @NotBlank String key,
            HttpServletRequest request) {
        String clientKey = extractKey(request);
        String namespace = NamespaceResolver.resolveNamespace(request);
        String storageKey = NamespaceResolver.toStorageKey(clientKey, namespace);
        return watchManager.register(storageKey, false, namespace);
    }

    /**
     * Subscribes to real-time events for keys matching a prefix within the caller's namespace.
     *
     * @param prefix the key prefix to watch
     * @param request HTTP request
     * @return Server-Sent Events emitter
     */
    @GetMapping(value = {"/prefix", "/prefix/", "/prefix/{prefix}", "/prefix/{prefix}/**"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Watch a key prefix",
            description = "Subscribes to mutations (PUT, DELETE) on any keys starting with the specified prefix using Server-Sent Events (SSE)")
    public SseEmitter watchPrefix(
            @PathVariable(name = "prefix", required = false)
            @Parameter(description = "Key prefix to watch")
            String prefix,
            HttpServletRequest request) {
        String clientPrefix = extractFullPrefix(request);
        String namespace = NamespaceResolver.resolveNamespace(request);
        String storagePrefix = NamespaceResolver.toStoragePrefix(clientPrefix, namespace);
        return watchManager.register(storagePrefix, true, namespace);
    }

    private String extractKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String marker = "/api/v1/watch/";
        int idx = uri.indexOf(marker);
        if (idx != -1) {
            String sub = uri.substring(idx + marker.length());
            int queryIdx = sub.indexOf('?');
            if (queryIdx != -1) {
                sub = sub.substring(0, queryIdx);
            }
            return sub;
        }
        return "";
    }

    private String extractFullPrefix(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String marker = "/api/v1/watch/prefix";
        int idx = uri.indexOf(marker);
        if (idx != -1) {
            String sub = uri.substring(idx + marker.length());
            if (sub.startsWith("/")) {
                sub = sub.substring(1);
            }
            int queryIdx = sub.indexOf('?');
            if (queryIdx != -1) {
                sub = sub.substring(0, queryIdx);
            }
            return sub;
        }
        return "";
    }
}
