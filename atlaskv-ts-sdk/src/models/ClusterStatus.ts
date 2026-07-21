/**
 * Represents the current status and consensus details of an AtlasKV cluster node.
 */
export interface ClusterStatus {
  /** The unique ID of the queried node. */
  nodeId: string;
  /** The current Raft role of the node (e.g., LEADER, FOLLOWER, CANDIDATE). */
  role: string;
  /** The current consensus term number. */
  currentTerm: number;
  /** The highest committed log index. */
  commitIndex: number;
  /** The highest applied log index in the state machine. */
  lastApplied: number;
  /** The node ID of the current leader, or null if unknown. */
  currentLeader: string | null;
  /** Indicates whether the node is healthy and operational. */
  healthy: boolean;
  /** The node's uptime in milliseconds. */
  uptimeMs: number;
  /** The lifecycle state of the node (e.g., STARTED, STOPPING). */
  nodeState: string;
  /** The port on which the node's internal gRPC engine is listening. */
  grpcPort: number;
  /** The number of configured peers in the cluster. */
  peerCount: number;
}
