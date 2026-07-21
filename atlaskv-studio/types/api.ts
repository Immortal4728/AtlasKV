// ─── AtlasKV Backend API Types ───────────────────────────────────────────────
// These types mirror the Java record DTOs in atlaskv-server

export type RaftRole = 'LEADER' | 'FOLLOWER' | 'CANDIDATE';

export interface ClusterStatusResponse {
  nodeId: string;
  role: RaftRole;
  currentTerm: number;
  commitIndex: number;
  lastApplied: number;
  currentLeader: string | null;
  healthy: boolean;
  uptimeMs: number;
  nodeState: string;
  grpcPort: number;
  peerCount: number;
}

export interface LeaderResponse {
  leaderId: string | null;
  isThisNodeLeader: boolean;
  currentTerm: number;
}

export interface MetricsResponse {
  nodeId: string;
  currentTerm: number;
  commitIndex: number;
  lastApplied: number;
  logLength: number;
  snapshotLastIndex: number;
  snapshotLastTerm: number;
  kvStoreSize: number;
  uptimeMs: number;
  totalReadRequests: number;
  successfulReadRequests: number;
  averageReadLatencyMs: number;
  membershipChangeCount: number;
  averageMembershipChangeLatencyMs: number;
}

export interface ClusterMembersResponse {
  members: string[];
  jointConsensusActive: boolean;
  oldMembers: string[];
  newMembers: string[];
  leaderId: string | null;
}

export interface KeyValueResponse {
  key: string;
  value: string | null;
  found: boolean;
}

export interface AddMemberRequest {
  nodeId: string;
  address?: string;
}

export interface HealthResponse {
  status: string;
  components?: Record<string, { status: string; details?: Record<string, unknown> }>;
}
