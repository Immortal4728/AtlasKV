package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.Metrics;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Displays internal performance metrics from the AtlasKV cluster.
 */
@Command(name = "metrics",
        description = "Show cluster performance metrics.",
        mixinStandardHelpOptions = true)
public final class MetricsCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn)) {
            Metrics m = client.cluster().metrics();

            OutputFormatter.printHeader("Node: " + m.nodeId());

            OutputFormatter.printHeader("Raft State");
            OutputFormatter.printField("Term", m.currentTerm());
            OutputFormatter.printField("Commit Index", m.commitIndex());
            OutputFormatter.printField("Last Applied", m.lastApplied());
            OutputFormatter.printField("Log Length", m.logLength());
            OutputFormatter.printField("Snapshot Idx", m.snapshotLastIndex());
            OutputFormatter.printField("Snapshot Term", m.snapshotLastTerm());

            OutputFormatter.printHeader("Key-Value Store");
            OutputFormatter.printField("Store Size", m.kvStoreSize());
            OutputFormatter.printField("Uptime",
                    OutputFormatter.formatDuration(m.uptimeMs()));

            OutputFormatter.printHeader("Read Metrics");
            OutputFormatter.printField("Total Reads", m.totalReadRequests());
            OutputFormatter.printField("Successful", m.successfulReadRequests());
            OutputFormatter.printField("Avg Latency",
                    String.format("%.2fms", m.averageReadLatencyMs()));

            OutputFormatter.printHeader("CAS Metrics");
            OutputFormatter.printField("Total CAS", m.totalCasAttempts());
            OutputFormatter.printField("Successful", m.successfulCasRequests());
            OutputFormatter.printField("Failed", m.failedCasRequests());
            OutputFormatter.printField("Avg Latency",
                    String.format("%.2fms", m.averageCasLatencyMs()));

            OutputFormatter.printHeader("Prefix Query Metrics");
            OutputFormatter.printField("Queries", m.prefixQueryCount());
            OutputFormatter.printField("Avg Latency",
                    String.format("%.2fms", m.averagePrefixLatencyMs()));
            OutputFormatter.printField("Avg Results",
                    String.format("%.1f", m.averagePrefixResultSize()));

            OutputFormatter.printHeader("History Metrics");
            OutputFormatter.printField("Reads", m.historyReads());
            OutputFormatter.printField("Writes", m.historyWrites());
            OutputFormatter.printField("Rollbacks", m.rollbackCount());
            OutputFormatter.printField("Avg History",
                    String.format("%.1f", m.averageHistorySize()));

            OutputFormatter.printHeader("Membership");
            OutputFormatter.printField("Changes", m.membershipChangeCount());
            OutputFormatter.printField("Avg Latency",
                    String.format("%.2fms",
                            m.averageMembershipChangeLatencyMs()));
        }
    }
}
