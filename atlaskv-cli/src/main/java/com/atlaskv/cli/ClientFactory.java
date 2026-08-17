package com.atlaskv.cli;

import com.atlaskv.cli.commands.ConnectionMixin;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.client.AtlasKVClientBuilder;
import com.atlaskv.sdk.connection.Authentication;

import java.time.Duration;
import java.util.Locale;

/**
 * Factory for creating AtlasKV SDK client instances from CLI configuration and connection options.
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
        return create(config, null, null, null, null);
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
        return create(config, null, host, port, null);
    }

    /**
     * Creates an AtlasKVClient using a ConnectionMixin for overrides.
     *
     * @param config CLI configuration
     * @param conn   connection mixin options
     * @return configured client
     */
    public static AtlasKVClient create(CliConfig config, ConnectionMixin conn) {
        if (conn == null) {
            return create(config);
        }
        return create(config, conn.getEndpoint(), conn.getHost(), conn.getPort(), conn.getApiKey(), conn.getNamespace());
    }

    /**
     * Creates an AtlasKVClient using full configuration overrides.
     *
     * @param config   CLI configuration
     * @param endpoint overridden endpoint URL
     * @param host     overridden host
     * @param port     overridden port
     * @param apiKey   overridden API key
     * @return configured client
     */
    public static AtlasKVClient create(CliConfig config, String endpoint, String host, Integer port, String apiKey) {
        return create(config, endpoint, host, port, apiKey, null);
    }

    /**
     * Creates an AtlasKVClient using full configuration overrides including namespace.
     *
     * @param config    CLI configuration
     * @param endpoint  overridden endpoint URL
     * @param host      overridden host
     * @param port      overridden port
     * @param apiKey    overridden API key
     * @param namespace overridden namespace
     * @return configured client
     */
    public static AtlasKVClient create(CliConfig config, String endpoint, String host, Integer port, String apiKey, String namespace) {
        AtlasKVClientBuilder builder = AtlasKVClient.builder()
                .timeout(Duration.ofSeconds(config != null ? config.getTimeoutSeconds() : 5));

        String effectiveEndpoint = (endpoint != null && !endpoint.isBlank())
                ? endpoint
                : (config != null ? config.getEndpoint() : null);

        if (effectiveEndpoint != null && !effectiveEndpoint.isBlank()) {
            builder.endpoint(effectiveEndpoint);
        } else {
            String effectiveHost = (host != null && !host.isBlank())
                    ? host
                    : (config != null ? config.getHost() : "localhost");
            int effectivePort = (port != null)
                    ? port
                    : (config != null ? config.getPort() : 8080);
            builder.host(effectiveHost).port(effectivePort);
        }

        String effectiveApiKey = (apiKey != null && !apiKey.isBlank())
                ? apiKey
                : (config != null ? config.getApiKey() : null);

        if (effectiveApiKey != null && !effectiveApiKey.isBlank()) {
            builder.apiKey(effectiveApiKey);
        } else if (config != null) {
            Authentication auth = resolveAuth(config);
            if (auth != null) {
                builder.authentication(auth);
            }
        }

        String effectiveNamespace = (namespace != null && !namespace.isBlank())
                ? namespace
                : (config != null ? config.getNamespace() : null);

        if (effectiveNamespace != null && !effectiveNamespace.isBlank()) {
            builder.namespace(effectiveNamespace);
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
