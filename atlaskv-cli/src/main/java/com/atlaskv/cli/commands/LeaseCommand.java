package com.atlaskv.cli.commands;

import com.atlaskv.cli.CliConfig;
import com.atlaskv.cli.ClientFactory;
import com.atlaskv.cli.OutputFormatter;
import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.Lease;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lease management subcommand group for AtlasKV.
 */
@Command(name = "lease",
        description = "Manage distributed leases.",
        mixinStandardHelpOptions = true,
        subcommands = {
                LeaseCommand.CreateLease.class,
                LeaseCommand.RenewLease.class,
                LeaseCommand.RevokeLease.class,
                LeaseCommand.ListLeases.class
        })
public final class LeaseCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    /**
     * Creates a new distributed lease.
     */
    @Command(name = "create",
            description = "Create a new lease.",
            mixinStandardHelpOptions = true)
    public static final class CreateLease implements Runnable {

        @Mixin
        private ConnectionMixin conn;

        @Option(names = {"--ttl"}, description = "Lease TTL (e.g. 30s, 5m)",
                required = true)
        private String ttl;

        @Option(names = {"--id"}, description = "Custom lease ID (optional)")
        private String leaseId;

        @Override
        public void run() {
            CliConfig config = CliConfig.load();
            try (AtlasKVClient client = ClientFactory.create(
                    config, conn.getHost(), conn.getPort())) {
                Lease lease;
                if (leaseId != null && !leaseId.isBlank()) {
                    lease = client.lease().createLease(leaseId, ttl);
                } else {
                    lease = client.lease().createLease(ttl);
                }
                OutputFormatter.printSuccess("Lease created");
                OutputFormatter.printField("Lease ID", lease.leaseId());
                OutputFormatter.printField("Duration",
                        OutputFormatter.formatDuration(lease.durationMs()));
                OutputFormatter.printField("Expires",
                        OutputFormatter.formatTimestamp(lease.expiryTimeMs()));
            }
        }
    }

    /**
     * Renews an active lease.
     */
    @Command(name = "renew",
            description = "Renew an active lease.",
            mixinStandardHelpOptions = true)
    public static final class RenewLease implements Runnable {

        @Mixin
        private ConnectionMixin conn;

        @Parameters(index = "0", description = "Lease ID to renew")
        private String leaseId;

        @Override
        public void run() {
            CliConfig config = CliConfig.load();
            try (AtlasKVClient client = ClientFactory.create(
                    config, conn.getHost(), conn.getPort())) {
                client.lease().renewLease(leaseId);
                OutputFormatter.printSuccess("Lease renewed: " + leaseId);
            }
        }
    }

    /**
     * Revokes an active lease.
     */
    @Command(name = "revoke",
            description = "Revoke an active lease.",
            mixinStandardHelpOptions = true)
    public static final class RevokeLease implements Runnable {

        @Mixin
        private ConnectionMixin conn;

        @Parameters(index = "0", description = "Lease ID to revoke")
        private String leaseId;

        @Override
        public void run() {
            CliConfig config = CliConfig.load();
            try (AtlasKVClient client = ClientFactory.create(
                    config, conn.getHost(), conn.getPort())) {
                client.lease().revokeLease(leaseId);
                OutputFormatter.printSuccess("Lease revoked: " + leaseId);
            }
        }
    }

    /**
     * Lists all active leases.
     */
    @Command(name = "list",
            description = "List all active leases.",
            mixinStandardHelpOptions = true)
    public static final class ListLeases implements Runnable {

        @Mixin
        private ConnectionMixin conn;

        @Override
        public void run() {
            CliConfig config = CliConfig.load();
            try (AtlasKVClient client = ClientFactory.create(
                    config, conn.getHost(), conn.getPort())) {
                List<Lease> leases = client.lease().listLeases();
                if (leases.isEmpty()) {
                    OutputFormatter.printInfo("No active leases");
                    return;
                }

                OutputFormatter.printHeader("Active Leases");
                String[] headers = {"Lease ID", "Duration", "Expires", "Keys"};
                List<String[]> rows = new ArrayList<>();
                for (Lease lease : leases) {
                    Set<String> keys = lease.keys();
                    String keyStr = (keys != null && !keys.isEmpty())
                            ? String.join(", ", keys) : "—";
                    rows.add(new String[]{
                            lease.leaseId(),
                            OutputFormatter.formatDuration(lease.durationMs()),
                            OutputFormatter.formatTimestamp(lease.expiryTimeMs()),
                            keyStr
                    });
                }
                OutputFormatter.printTable(headers, rows);
            }
        }
    }
}
