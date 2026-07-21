package com.atlaskv.cli;

import com.atlaskv.cli.commands.CasCommand;
import com.atlaskv.cli.commands.ClusterCommand;
import com.atlaskv.cli.commands.ConfigCommand;
import com.atlaskv.cli.commands.DeleteCommand;
import com.atlaskv.cli.commands.ExistsCommand;
import com.atlaskv.cli.commands.GetCommand;
import com.atlaskv.cli.commands.HistoryCommand;
import com.atlaskv.cli.commands.LeaseCommand;
import com.atlaskv.cli.commands.MetricsCommand;
import com.atlaskv.cli.commands.PrefixCommand;
import com.atlaskv.cli.commands.PutCommand;
import com.atlaskv.cli.commands.RollbackCommand;
import com.atlaskv.cli.commands.WatchCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * AtlasKV CLI — Official command-line interface for AtlasKV distributed key-value store.
 */
@Command(
        name = "atlaskv",
        description = "Official CLI for the AtlasKV distributed key-value store.",
        version = "atlaskv-cli 0.1.0",
        mixinStandardHelpOptions = true,
        subcommands = {
                PutCommand.class,
                GetCommand.class,
                DeleteCommand.class,
                ExistsCommand.class,
                CasCommand.class,
                PrefixCommand.class,
                HistoryCommand.class,
                RollbackCommand.class,
                LeaseCommand.class,
                WatchCommand.class,
                ClusterCommand.class,
                MetricsCommand.class,
                ConfigCommand.class,
                CommandLine.HelpCommand.class
        },
        footer = "%nUse 'atlaskv <command> --help' for more information on a command."
)
public final class AtlasKVCli implements Runnable {

    /**
     * Main entry point for the AtlasKV CLI.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = createCommandLine().execute(args);
        System.exit(exitCode);
    }

    /**
     * Creates a configured CommandLine instance.
     *
     * @return configured command line
     */
    public static CommandLine createCommandLine() {
        CommandLine cmd = new CommandLine(new AtlasKVCli());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            OutputFormatter.printError(ex.getMessage());
            return 1;
        });
        return cmd;
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
