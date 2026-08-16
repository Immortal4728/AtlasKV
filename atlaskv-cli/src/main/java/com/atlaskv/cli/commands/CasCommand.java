package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.exceptions.ConflictException;
import com.atlaskv.sdk.models.KeyValue;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

/**
 * Performs a Compare-And-Swap (CAS) update on a key in AtlasKV.
 */
@Command(name = "cas",
        description = "Compare-and-swap update a key.",
        mixinStandardHelpOptions = true)
public final class CasCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key to update")
    private String key;

    @Parameters(index = "1", description = "New value")
    private String value;

    @Parameters(index = "2", description = "Expected version number")
    private long version;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn)) {
            KeyValue result = client.keyValue().casPut(key, value, version);
            OutputFormatter.printSuccess("CAS update succeeded");
            OutputFormatter.printField("Key", result.key());
            OutputFormatter.printField("Value", result.value());
            OutputFormatter.printField("Version", result.version());
            OutputFormatter.printField("Updated", OutputFormatter.formatTimestamp(result.updatedAt()));
        } catch (ConflictException e) {
            OutputFormatter.printError("CAS conflict: " + e.getMessage());
            OutputFormatter.printField("Expected", e.getExpectedVersion());
            OutputFormatter.printField("Current", e.getCurrentVersion());
        }
    }
}
