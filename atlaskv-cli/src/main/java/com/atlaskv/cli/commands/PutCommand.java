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
 * Stores a key-value pair in AtlasKV.
 */
@Command(name = "put",
        description = "Store a key-value pair.",
        mixinStandardHelpOptions = true)
public final class PutCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key to store")
    private String key;

    @Parameters(index = "1", description = "Value to store")
    private String value;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn.getHost(), conn.getPort())) {
            KeyValue result = client.keyValue().put(key, value);
            OutputFormatter.printSuccess("Key stored successfully");
            OutputFormatter.printField("Key", result.key());
            OutputFormatter.printField("Value", result.value());
            OutputFormatter.printField("Version", result.version());
            OutputFormatter.printField("Created", OutputFormatter.formatTimestamp(result.createdAt()));
            OutputFormatter.printField("Updated", OutputFormatter.formatTimestamp(result.updatedAt()));
        }
    }
}
