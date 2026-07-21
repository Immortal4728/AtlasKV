package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.OutputFormatter;

import picocli.CommandLine;
import picocli.CommandLine.Command;

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
            OutputFormatter.printField("Host", config.getHost());
            OutputFormatter.printField("Port", config.getPort());
            OutputFormatter.printField("Timeout",
                    config.getTimeoutSeconds() + "s");
            OutputFormatter.printField("Auth Type",
                    config.getAuthType());
            OutputFormatter.printField("Config File",
                    CliConfig.configPath());
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
                OutputFormatter.printWarning(
                        "Config already exists: " + configPath);
                return;
            }

            String defaultConfig = """
                    # AtlasKV CLI Configuration
                    host: localhost
                    port: 8080
                    timeout: 5

                    # Authentication (optional)
                    # authentication:
                    #   type: bearer
                    #   token: your-token-here
                    #
                    # Or use basic auth:
                    # authentication:
                    #   type: basic
                    #   username: admin
                    #   password: secret
                    """;

            try {
                Path parentDir = configPath.getParent();
                if (parentDir != null) {
                    Files.createDirectories(parentDir);
                }
                Files.writeString(configPath, defaultConfig);
                OutputFormatter.printSuccess(
                        "Config created: " + configPath);
            } catch (IOException e) {
                OutputFormatter.printError(
                        "Failed to create config: " + e.getMessage());
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
