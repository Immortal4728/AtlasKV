package com.atlaskv.cli;

import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.client.AtlasKVClientBuilder;
import com.atlaskv.sdk.connection.Authentication;

import java.time.Duration;
import java.util.Locale;

/**
 * Factory for creating AtlasKV SDK client instances from CLI configuration.
 */
public final class ClientFactory {

    private ClientFactory() {
    }

    /**
     * Creates an AtlasKVClient from the provided CLI configuration.
     *
     * @param config CLI configuration
     * @return configured client
     */
    public static AtlasKVClient create(CliConfig config) {
        AtlasKVClientBuilder builder = AtlasKVClient.builder()
                .host(config.getHost())
                .port(config.getPort())
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        Authentication auth = resolveAuth(config);
        if (auth != null) {
            builder.authentication(auth);
        }

        return builder.build();
    }

    /**
     * Creates an AtlasKVClient using overridden host/port and fallback config.
     *
     * @param config CLI configuration
     * @param host   overridden host (null to use config)
     * @param port   overridden port (null to use config)
     * @return configured client
     */
    public static AtlasKVClient create(CliConfig config, String host, Integer port) {
        String effectiveHost = host != null ? host : config.getHost();
        int effectivePort = port != null ? port : config.getPort();

        AtlasKVClientBuilder builder = AtlasKVClient.builder()
                .host(effectiveHost)
                .port(effectivePort)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        Authentication auth = resolveAuth(config);
        if (auth != null) {
            builder.authentication(auth);
        }

        return builder.build();
    }

    private static Authentication resolveAuth(CliConfig config) {
        String authType = config.getAuthType();
        if (authType == null) {
            return null;
        }

        return switch (authType.toLowerCase(Locale.ROOT)) {
            case "bearer" -> Authentication.bearer(config.getAuthToken());
            case "basic" -> Authentication.basic(config.getAuthUsername(), config.getAuthPassword());
            default -> null;
        };
    }
}
