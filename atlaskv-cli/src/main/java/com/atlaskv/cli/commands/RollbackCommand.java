package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.KeyValue;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

/**
 * Rolls back a key to a specific revision in AtlasKV.
 */
@Command(name = "rollback",
        description = "Rollback a key to a specific revision.",
        mixinStandardHelpOptions = true)
public final class RollbackCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key to rollback")
    private String key;

    @Parameters(index = "1", description = "Target revision number")
    private long revision;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn.getHost(), conn.getPort())) {
            KeyValue result = client.history().rollback(key, revision);
            OutputFormatter.printSuccess("Rolled back '" + key + "' to revision " + revision);
            OutputFormatter.printField("Key", result.key());
            OutputFormatter.printField("Value", result.value());
            OutputFormatter.printField("Version", result.version());
            OutputFormatter.printField("Updated", OutputFormatter.formatTimestamp(result.updatedAt()));
        }
    }
}
