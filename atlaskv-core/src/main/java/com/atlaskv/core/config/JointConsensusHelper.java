package com.atlaskv.core.config;

import com.atlaskv.core.NodeId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Helper utility for Joint Consensus cluster membership transition validations and transformations.
 */
public final class JointConsensusHelper {

    private JointConsensusHelper() {
        // Utility class
    }

    /**
     * Validates a member change request against the current cluster membership and creates a joint membership.
     *
     * @param current current cluster membership
     * @param isAdd true for ADD, false for REMOVE
     * @param targetNode target node ID to add or remove
     * @return joint consensus membership instance (C_old,new)
     */
    public static ClusterMembership createJointMembership(ClusterMembership current, boolean isAdd, NodeId targetNode) {
        Objects.requireNonNull(current, "Current membership must not be null");
        Objects.requireNonNull(targetNode, "TargetNode must not be null");

        if (current.isJoint()) {
            throw new IllegalStateException("Configuration change already in progress");
        }

        Set<NodeId> oldSet = current.oldMembers();
        if (isAdd) {
            if (oldSet.contains(targetNode)) {
                throw new IllegalArgumentException("Node " + targetNode + " is already a cluster member");
            }
        } else {
            if (!oldSet.contains(targetNode)) {
                throw new IllegalArgumentException("Node " + targetNode + " is not a cluster member");
            }
            if (oldSet.size() <= 1) {
                throw new IllegalArgumentException("Cannot remove node: cluster must have at least one member");
            }
        }

        Set<NodeId> newSet = new HashSet<>(oldSet);
        if (isAdd) {
            newSet.add(targetNode);
        } else {
            newSet.remove(targetNode);
        }

        return ClusterMembership.ofJoint(oldSet, newSet);
    }
}
