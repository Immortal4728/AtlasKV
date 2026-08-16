package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.api.WatchListener;
import com.atlaskv.sdk.api.WatchApi;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.WatchEvent;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.CountDownLatch;

/**
 * Watches a key or prefix for real-time changes.
 */
@Command(name = "watch",
        description = "Watch a key or prefix for live changes.",
        mixinStandardHelpOptions = true)
public final class WatchCommand implements Runnable {

    @Mixin
    private ConnectionMixin conn;

    @Parameters(index = "0", description = "Key or prefix to watch")
    private String target;

    @Option(names = {"--prefix"},
            description = "Watch all keys with this prefix")
    private boolean prefixMode;

    @Override
    public void run() {
        CliConfig config = CliConfig.load();
        AtlasKVClient client = ClientFactory.create(config, conn);
        CountDownLatch latch = new CountDownLatch(1);

        WatchListener listener = new WatchListener() {
            @Override
            public void onEvent(WatchEvent event) {
                String label = switch (event.type()) {
                    case "PUT" -> "\033[32m PUT \033[0m";
                    case "DELETE" -> "\033[31m DEL \033[0m";
                    case "EXPIRE" -> "\033[33m EXP \033[0m";
                    default -> " " + event.type() + " ";
                };
                System.out.printf("[%s] %s = %s%n",
                        label, event.key(),
                        event.value() != null ? event.value() : "(deleted)");
            }

            @Override
            public void onError(Throwable throwable) {
                OutputFormatter.printError(
                        "Watch error: " + throwable.getMessage());
            }

            @Override
            public void onConnected() {
                OutputFormatter.printSuccess(
                        "Watching " + (prefixMode ? "prefix" : "key")
                                + ": " + target);
                OutputFormatter.printInfo(
                        "Press Ctrl+C to stop");
            }

            @Override
            public void onDisconnected() {
                OutputFormatter.printWarning(
                        "Disconnected, reconnecting...");
            }
        };

        WatchApi.WatchSession session;
        if (prefixMode) {
            session = client.watch().watchPrefix(target, listener);
        } else {
            session = client.watch().watch(target, listener);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            session.close();
            client.close();
            System.out.println();
            OutputFormatter.printInfo("Watch stopped");
            latch.countDown();
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            session.close();
            client.close();
        }
    }
}
