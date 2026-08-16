package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

/**
 * Checks whether a key exists in AtlasKV.
 */
@Command(name = "exists",
        description = "Check if a key exists.",
        mixinStandardHelpOptions = true)
public final class ExistsCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key to check")
    private String key;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        try (AtlasKVClient client = ClientFactory.create(config, conn)) {
            boolean exists = client.keyValue().exists(key);
            if (exists) {
                OutputFormatter.printSuccess("Key exists: " + key);
            } else {
                OutputFormatter.printInfo("Key does not exist: " + key);
            }
        }
    }
}
