package com.atlaskv.core.config;

import com.atlaskv.core.NodeId;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Codec for serializing and deserializing {@link ClusterMembership} objects to/from log entry command byte arrays.
 */
public final class ClusterMembershipCodec {

    private static final String PREFIX = "MEMBERSHIP:";
    private static final String SINGLE_PREFIX = "MEMBERSHIP:SINGLE:";
    private static final String JOINT_PREFIX = "MEMBERSHIP:JOINT:";

    private ClusterMembershipCodec() {
        // Utility class
    }

    /**
     * Checks whether the given command byte array represents a membership configuration change entry.
     *
     * @param command raw command bytes
     * @return true if command is a membership entry
     */
    public static boolean isMembershipCommand(byte[] command) {
        if (command == null || command.length < PREFIX.length()) {
            return false;
        }
        String str = new String(command, StandardCharsets.UTF_8);
        return str.startsWith(PREFIX);
    }

    /**
     * Serializes a {@link ClusterMembership} instance into a UTF-8 command byte array.
     *
     * @param membership cluster membership configuration
     * @return byte array payload
     */
    public static byte[] encode(ClusterMembership membership) {
        if (!membership.isJoint()) {
            String membersStr = membership.oldMembers().stream()
                    .map(NodeId::value)
                    .sorted()
                    .collect(Collectors.joining(","));
            return (SINGLE_PREFIX + membersStr).getBytes(StandardCharsets.UTF_8);
        } else {
            String oldStr = membership.oldMembers().stream()
                    .map(NodeId::value)
                    .sorted()
                    .collect(Collectors.joining(","));
            String newStr = membership.newMembers().stream()
                    .map(NodeId::value)
                    .sorted()
                    .collect(Collectors.joining(","));
            return (JOINT_PREFIX + oldStr + "|" + newStr).getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Deserializes a command byte array into a {@link ClusterMembership} instance.
     *
     * @param command raw command bytes
     * @return optional containing the parsed membership, or empty if not a membership command
     */
    public static Optional<ClusterMembership> decode(byte[] command) {
        if (!isMembershipCommand(command)) {
            return Optional.empty();
        }

        String str = new String(command, StandardCharsets.UTF_8);
        if (str.startsWith(SINGLE_PREFIX)) {
            String list = str.substring(SINGLE_PREFIX.length());
            Set<NodeId> members = parseNodeIds(list);
            return Optional.of(ClusterMembership.ofSingle(members));
        } else if (str.startsWith(JOINT_PREFIX)) {
            String payload = str.substring(JOINT_PREFIX.length());
            String[] parts = payload.split("\\|", 2);
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid joint membership payload: " + str);
            }
            Set<NodeId> oldMembers = parseNodeIds(parts[0]);
            Set<NodeId> newMembers = parseNodeIds(parts[1]);
            return Optional.of(ClusterMembership.ofJoint(oldMembers, newMembers));
        }

        throw new IllegalArgumentException("Unknown membership command format: " + str);
    }

    private static Set<NodeId> parseNodeIds(String csv) {
        if (csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(NodeId::of)
                .collect(Collectors.toSet());
    }
}
