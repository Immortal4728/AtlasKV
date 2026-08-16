package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.OutputFormatter;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages CLI configuration stored in ~/.atlaskv/config.yml.
 */
@Command(name = "config",
        description = "Manage CLI configuration.",
        mixinStandardHelpOptions = true,
        subcommands = {
                ConfigCommand.ShowConfig.class,
                ConfigCommand.SetConfig.class,
                ConfigCommand.InitConfig.class,
                ConfigCommand.PathConfig.class
        })
public final class ConfigCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    /**
     * Displays the current configuration.
     */
    @Command(name = "show",
            description = "Display current configuration.",
            mixinStandardHelpOptions = true)
    public static final class ShowConfig implements Runnable {

        @Override
        public void run() {
            CliConfig config = CliConfig.load();
            OutputFormatter.printHeader("Configuration");
            if (config.getEndpoint() != null) {
                OutputFormatter.printField("Endpoint", config.getEndpoint());
            }
            OutputFormatter.printField("Host", config.getHost());
            OutputFormatter.printField("Port", config.getPort());
            OutputFormatter.printField("Timeout", config.getTimeoutSeconds() + "s");
            OutputFormatter.printField("Auth Type", config.getAuthType() != null ? config.getAuthType() : "(none)");
            OutputFormatter.printField("API Key", maskApiKey(config.getApiKey()));
            OutputFormatter.printField("Config File", CliConfig.configPath());
        }

        private static String maskApiKey(String key) {
            if (key == null || key.isBlank()) {
                return "(not set)";
            }
            if (key.length() <= 8) {
                return "********";
            }
            return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
        }
    }

    /**
     * Sets a configuration property and persists it.
     */
    @Command(name = "set",
            description = "Set a configuration property (e.g. endpoint, api-key, host, port, timeout).",
            mixinStandardHelpOptions = true)
    public static final class SetConfig implements Runnable {

        @Parameters(index = "0", description = "Configuration key (endpoint, api-key, host, port, timeout)")
        private String key;

        @Parameters(index = "1", description = "Configuration value")
        private String value;

        @Override
        public void run() {
            try {
                CliConfig config = CliConfig.load();
                config.set(key, value);
                config.save();
                OutputFormatter.printSuccess("Updated " + key + " in " + CliConfig.configPath());
            } catch (IllegalArgumentException e) {
                OutputFormatter.printError(e.getMessage());
            } catch (IOException e) {
                OutputFormatter.printError("Failed to save config: " + e.getMessage());
            }
        }
    }

    /**
     * Creates a default configuration file.
     */
    @Command(name = "init",
            description = "Create default config file.",
            mixinStandardHelpOptions = true)
    public static final class InitConfig implements Runnable {

        @Override
        public void run() {
            Path configPath = CliConfig.configPath();
            if (Files.exists(configPath)) {
                OutputFormatter.printWarning("Config already exists: " + configPath);
                return;
            }

            String defaultConfig = """
                    # AtlasKV CLI Configuration
                    # Remote Cloud Endpoint (optional, overrides host/port if set):
                    # endpoint: https://atlaskv.example.com
                    host: localhost
                    port: 8080
                    timeout: 5

                    # API Key Authentication (optional)
                    # api-key: your-api-key-here
                    """;

            try {
                Path parentDir = configPath.getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                }
                Files.writeString(configPath, defaultConfig);
                OutputFormatter.printSuccess("Config created: " + configPath);
            } catch (IOException e) {
                OutputFormatter.printError("Failed to create config: " + e.getMessage());
            }
        }
    }

    /**
     * Shows the config file path.
     */
    @Command(name = "path",
            description = "Show config file location.",
            mixinStandardHelpOptions = true)
    public static final class PathConfig implements Runnable {

        @Override
        public void run() {
            System.out.println(CliConfig.configPath());
        }
    }
}
