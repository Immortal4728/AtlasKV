package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.ClusterStatus;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.List;

/**
 * Cluster management subcommand group for AtlasKV.
 */
@Command(name = "cluster",
        description = "Cluster status and membership.",
        mixinStandardHelpOptions = true,
        subcommands = {
                ClusterCommand.StatusCmd.class,
                ClusterCommand.LeaderCmd.class,
                ClusterCommand.MembersCmd.class
        })
public final class ClusterCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    /**
     * Shows the cluster node status.
     */
    @Command(name = "status",
            description = "Show cluster node status.",
            mixinStandardHelpOptions = true)
    public static final class StatusCmd implements Runnable {

        @Mixin
        private ConnectionMixin conn;

        @Override
        public void run() {
            CliConfig config = CliConfig.load();
            try (AtlasKVClient client = ClientFactory.create(config, conn)) {
                ClusterStatus s = client.cluster().status();
                OutputFormatter.printHeader("Cluster Status");
                OutputFormatter.printField("Node ID", s.nodeId());
                OutputFormatter.printField("Role", s.role());
                OutputFormatter.printField("Term", s.currentTerm());
                OutputFormatter.printField("Commit Index", s.commitIndex());
                OutputFormatter.printField("Last Applied", s.lastApplied());
                OutputFormatter.printField("Leader", s.currentLeader());
                OutputFormatter.printField("Healthy", s.healthy());
                OutputFormatter.printField("Uptime",
                        OutputFormatter.formatDuration(s.uptimeMs()));
                OutputFormatter.printField("State", s.nodeState());
                OutputFormatter.printField("gRPC Port", s.grpcPort());
                OutputFormatter.printField("Peer Count", s.peerCount());
            }
        }
    }

    /**
     * Shows the current cluster leader.
     */
    @Command(name = "leader",
            description = "Show the current cluster leader.",
            mixinStandardHelpOptions = true)
    public static final class LeaderCmd implements Runnable {

        @Mixin
        private ConnectionMixin conn;

        @Override
        public void run() {
            CliConfig config = CliConfig.load();
            try (AtlasKVClient client = ClientFactory.create(config, conn)) {
                String leader = client.cluster().leader();
                if (leader != null) {
                    OutputFormatter.printSuccess("Leader: " + leader);
                } else {
                    OutputFormatter.printWarning("No leader elected");
                }
            }
        }
    }

    /**
     * Lists the cluster members.
     */
    @Command(name = "members",
            description = "List cluster members.",
            mixinStandardHelpOptions = true)
    public static final class MembersCmd implements Runnable {

        @Mixin
        private ConnectionMixin conn;

        @Override
        public void run() {
            CliConfig config = CliConfig.load();
            try (AtlasKVClient client = ClientFactory.create(config, conn)) {
                List<String> members = client.cluster().members();
                if (members.isEmpty()) {
                    OutputFormatter.printInfo("No members found");
                    return;
                }
                OutputFormatter.printHeader("Cluster Members");
                for (int i = 0; i < members.size(); i++) {
                    System.out.printf("  %d. %s%n", i + 1, members.get(i));
                }
            }
        }
    }
}
