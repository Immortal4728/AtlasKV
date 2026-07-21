package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.Revision;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Retrieves version history for a key, or a specific revision.
 */
@Command(name = "history",
        description = "Show version history for a key.",
        mixinStandardHelpOptions = true)
public final class HistoryCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key to query")
    private String key;

    @Parameters(index = "1", description = "Specific revision number (optional)",
            arity = "0..1", defaultValue = "-1")
    private long revision;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn.getHost(), conn.getPort())) {
            if (revision >= 0) {
                showRevision(client);
            } else {
                showHistory(client);
            }
        }
    }

    private void showHistory(AtlasKVClient client) {
        List<Revision> history = client.history().history(key);
        if (history.isEmpty()) {
            OutputFormatter.printInfo("No history for key: " + key);
            return;
        }

        OutputFormatter.printHeader("History: " + key);
        String[] headers = {"Rev", "Operation", "Value", "Timestamp", "Node"};
        List<String[]> rows = new ArrayList<>();
        for (Revision rev : history) {
            rows.add(new String[]{
                    String.valueOf(rev.revisionNumber()),
                    rev.operation(),
                    truncate(rev.value(), 30),
                    OutputFormatter.formatTimestamp(rev.timestamp()),
                    rev.nodeId() != null ? rev.nodeId() : "—"
            });
        }
        OutputFormatter.printTable(headers, rows);
    }

    private void showRevision(AtlasKVClient client) {
        Optional<Revision> rev = client.history().revision(key, revision);
        if (rev.isEmpty()) {
            OutputFormatter.printWarning("Revision " + revision + " not found for key: " + key);
            return;
        }
        Revision r = rev.get();
        OutputFormatter.printHeader("Revision " + r.revisionNumber());
        OutputFormatter.printField("Key", key);
        OutputFormatter.printField("Value", r.value());
        OutputFormatter.printField("Operation", r.operation());
        OutputFormatter.printField("Timestamp", OutputFormatter.formatTimestamp(r.timestamp()));
        OutputFormatter.printField("Node", r.nodeId());
        OutputFormatter.printField("Lease", r.leaseId());
        OutputFormatter.printField("TTL", r.ttl());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "—";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 3) + "...";
    }
}
