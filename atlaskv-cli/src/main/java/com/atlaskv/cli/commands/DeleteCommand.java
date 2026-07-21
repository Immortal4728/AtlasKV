package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

/**
 * Deletes a key-value pair from AtlasKV.
 */
@Command(name = "delete",
        description = "Delete a key-value pair.",
        mixinStandardHelpOptions = true)
public final class DeleteCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key to delete")
    private String key;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn.getHost(), conn.getPort())) {
            boolean deleted = client.keyValue().delete(key);
            if (deleted) {
                OutputFormatter.printSuccess("Deleted key: " + key);
            } else {
                OutputFormatter.printWarning("Key not found: " + key);
            }
        }
    }
}
