package com.atlaskv.cli.commands;

import picocli.CommandLine.Option;

/**
 * Shared connection options mixin for all CLI commands.
 */
public final class ConnectionMixin {

    @Option(names = {"-H", "--host"},
            description = "AtlasKV server host (default: from config or localhost)")
    private String host;

    @Option(names = {"-p", "--port"},
            description = "AtlasKV server port (default: from config or 8080)")
    private Integer port;

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
