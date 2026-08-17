package com.atlaskv.cli.commands;

import picocli.CommandLine.Option;

/**
 * Shared connection options mixin for all CLI commands.
 */
public final class ConnectionMixin {

    @Option(names = {"-e", "--endpoint"},
            description = "AtlasKV server endpoint URL (e.g. https://atlaskv.example.com)")
    private String endpoint;

    @Option(names = {"-H", "--host"},
            description = "AtlasKV server host (default: from config or localhost)")
    private String host;

    @Option(names = {"-p", "--port"},
            description = "AtlasKV server port (default: from config or 8080)")
    private Integer port;

    @Option(names = {"-k", "--api-key"},
            description = "AtlasKV API key secret")
    private String apiKey;

    @Option(names = {"-n", "--namespace"},
            description = "Target namespace scope for multi-tenant isolation")
    private String namespace;

    public String getEndpoint() {
        return endpoint;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getNamespace() {
        return namespace;
    }
}
