package com.atlaskv.cli;

import picocli.CommandLine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AtlasKVCli entry point and command registration.
 */
final class AtlasKVCliTest {

    @Test
    void helpExitCodeIsZero() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        int exitCode = cmd.execute("--help");
        assertThat(exitCode).isZero();
    }

    @Test
    void versionExitCodeIsZero() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        int exitCode = cmd.execute("--version");
        assertThat(exitCode).isZero();
    }

    @Test
    void unknownCommandExitsNonZero() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        int exitCode = cmd.execute("nonexistent-command");
        assertThat(exitCode).isNotZero();
    }

    @Test
    void allSubcommandsRegistered() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        assertThat(cmd.getSubcommands()).containsKey("put");
        assertThat(cmd.getSubcommands()).containsKey("get");
        assertThat(cmd.getSubcommands()).containsKey("delete");
        assertThat(cmd.getSubcommands()).containsKey("exists");
        assertThat(cmd.getSubcommands()).containsKey("cas");
        assertThat(cmd.getSubcommands()).containsKey("prefix");
        assertThat(cmd.getSubcommands()).containsKey("history");
        assertThat(cmd.getSubcommands()).containsKey("rollback");
        assertThat(cmd.getSubcommands()).containsKey("lease");
        assertThat(cmd.getSubcommands()).containsKey("watch");
        assertThat(cmd.getSubcommands()).containsKey("cluster");
        assertThat(cmd.getSubcommands()).containsKey("metrics");
        assertThat(cmd.getSubcommands()).containsKey("config");
    }

    @Test
    void leaseSubcommandsRegistered() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        CommandLine lease = cmd.getSubcommands().get("lease");
        assertThat(lease.getSubcommands()).containsKey("create");
        assertThat(lease.getSubcommands()).containsKey("renew");
        assertThat(lease.getSubcommands()).containsKey("revoke");
        assertThat(lease.getSubcommands()).containsKey("list");
    }

    @Test
    void clusterSubcommandsRegistered() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        CommandLine cluster = cmd.getSubcommands().get("cluster");
        assertThat(cluster.getSubcommands()).containsKey("status");
        assertThat(cluster.getSubcommands()).containsKey("leader");
        assertThat(cluster.getSubcommands()).containsKey("members");
    }

    @Test
    void configSubcommandsRegistered() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        CommandLine config = cmd.getSubcommands().get("config");
        assertThat(config.getSubcommands()).containsKey("show");
        assertThat(config.getSubcommands()).containsKey("set");
        assertThat(config.getSubcommands()).containsKey("init");
        assertThat(config.getSubcommands()).containsKey("path");
    }

    @Test
    void subcommandHelpExitsZero() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        assertThat(cmd.execute("put", "--help")).isZero();
        assertThat(cmd.execute("get", "--help")).isZero();
        assertThat(cmd.execute("delete", "--help")).isZero();
        assertThat(cmd.execute("exists", "--help")).isZero();
        assertThat(cmd.execute("cas", "--help")).isZero();
        assertThat(cmd.execute("prefix", "--help")).isZero();
        assertThat(cmd.execute("history", "--help")).isZero();
        assertThat(cmd.execute("rollback", "--help")).isZero();
        assertThat(cmd.execute("lease", "--help")).isZero();
        assertThat(cmd.execute("watch", "--help")).isZero();
        assertThat(cmd.execute("cluster", "--help")).isZero();
        assertThat(cmd.execute("metrics", "--help")).isZero();
        assertThat(cmd.execute("config", "--help")).isZero();
    }

    @Test
    void namespaceOptionParsedOnSubcommands() {
        CommandLine cmd = AtlasKVCli.createCommandLine();
        CommandLine.ParseResult result = cmd.parseArgs("put", "-n", "tenant-test", "mykey", "myval");
        CommandLine.ParseResult subResult = result.subcommand();
        assertThat(subResult).isNotNull();
        assertThat(subResult.matchedOption("-n")).isNotNull();
        assertThat((String) subResult.matchedOption("-n").getValue()).isEqualTo("tenant-test");
    }
}
