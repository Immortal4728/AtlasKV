package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.PrefixResult;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Queries keys matching a prefix from AtlasKV.
 */
@Command(name = "prefix",
        description = "List keys matching a prefix.",
        mixinStandardHelpOptions = true)
public final class PrefixCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key prefix to scan")
    private String prefix;

    @Option(names = {"--offset"}, description = "Pagination offset (default: 0)", defaultValue = "0")
    private int offset;

    @Option(names = {"--limit"}, description = "Maximum results (default: 100)", defaultValue = "100")
    private int limit;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn.getHost(), conn.getPort())) {
            PrefixResult result = client.keyValue().prefix(prefix, offset, limit);

            OutputFormatter.printHeader("Prefix Scan: " + prefix);
            OutputFormatter.printField("Total", result.totalCount());
            OutputFormatter.printField("Offset", result.offset());
            OutputFormatter.printField("Limit", result.limit());
            System.out.println();

            if (result.entries() == null || result.entries().isEmpty()) {
                OutputFormatter.printInfo("No keys found matching prefix: " + prefix);
                return;
            }

            String[] headers = {"Key", "Value", "Version", "Updated"};
            List<String[]> rows = new ArrayList<>();
            for (PrefixResult.PrefixEntry entry : result.entries()) {
                rows.add(new String[]{
                        entry.key(),
                        truncate(entry.value(), 40),
                        entry.version() != null ? String.valueOf(entry.version()) : "—",
                        OutputFormatter.formatTimestamp(entry.updatedAt())
                });
            }
            OutputFormatter.printTable(headers, rows);
        }
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
