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
 * Retrieves the value and metadata for a key from AtlasKV.
 */
@Command(name = "get",
        description = "Retrieve a value by key.",
        mixinStandardHelpOptions = true)
public final class GetCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key to retrieve")
    private String key;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn.getHost(), conn.getPort())) {
            KeyValue result = client.keyValue().get(key);
            if (!result.exists()) {
                OutputFormatter.printWarning("Key not found: " + key);
                return;
            }
            OutputFormatter.printHeader("Key-Value");
            OutputFormatter.printField("Key", result.key());
            OutputFormatter.printField("Value", result.value());
            OutputFormatter.printField("Version", result.version());
            OutputFormatter.printField("Created", OutputFormatter.formatTimestamp(result.createdAt()));
            OutputFormatter.printField("Updated", OutputFormatter.formatTimestamp(result.updatedAt()));
        }
    }
}
