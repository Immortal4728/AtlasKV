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

export interface NodeDetail {
  id: string;
  host: string;
  port: number;
  grpcPort: number;
  role: RaftRole;
  healthy: boolean;
  term: number;
  commitIndex: number;
  appliedIndex: number;
  matchIndex?: number;
  nextIndex?: number;
  isLeader: boolean;
  isLocal: boolean;
  latencyMs: number;
  peers: number;
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
  totalCasAttempts?: number;
  successfulCasRequests?: number;
  failedCasRequests?: number;
  averageCasLatencyMs?: number;
  prefixQueryCount?: number;
  averagePrefixLatencyMs?: number;
  averagePrefixResultSize?: number;
  historyReads?: number;
  historyWrites?: number;
  rollbackCount?: number;
  averageHistorySize?: number;
  activeWatchers?: number;
  totalEventsDelivered?: number;
  totalWatchConnections?: number;
  activeLeases?: number;
  expiredLeases?: number;
  leaseRenewals?: number;
  averageLeaseDurationMs?: number;
}

export interface ClusterMembersResponse {
  members: string[];
  jointConsensusActive: boolean;
  oldMembers: string[];
  newMembers: string[];
  leaderId: string | null;
}

export interface SnapshotResponse {
  success: boolean;
  lastIncludedIndex: number;
  lastIncludedTerm: number;
}

export interface KeyValueResponse {
  key: string;
  value: string | null;
  found: boolean;
  version?: number | null;
  createdAt?: number | null;
  updatedAt?: number | null;
  ttlRemaining?: number | null;
  leaseId?: string | null;
}

export interface PrefixEntry {
  key: string;
  value: string | null;
  version: number;
  createdAt: number | null;
  updatedAt: number | null;
  ttlRemaining: number | null;
  leaseId: string | null;
}

export interface PrefixQueryResponse {
  prefix: string;
  entries: PrefixEntry[];
  totalCount: number;
  offset: number;
  limit: number;
}

export interface LeaseResponse {
  leaseId: string;
  durationMs: number;
  expiryTimeMs: number;
  keys: string[];
  status?: 'ACTIVE' | 'EXPIRED' | 'REVOKED';
  createdAtMs?: number | null;
  lastActionTimeMs?: number | null;
}

export interface LeaseRequest {
  ttl: string;
  leaseId?: string;
}

export interface CasConflictResponse {
  expectedVersion: number;
  currentVersion: number;
  reason?: string;
  message?: string;
}

export interface KeyRevisionResponse {
  revisionNumber: number;
  value: string | null;
  timestamp: number;
  operation: 'PUT' | 'DELETE' | 'EXPIRE' | 'ROLLBACK' | string;
  nodeId?: string;
  leaseId?: string | null;
  ttl?: string | null;
}

// Backward compatibility aliases
export type RevisionItem = KeyRevisionResponse;
export type RevisionResponse = KeyRevisionResponse[];

export interface WatchEvent {
  type: 'PUT' | 'DELETE' | 'EXPIRE' | 'STATUS';
  key: string;
  value?: string | null;
  version?: number;
  timestamp?: number;
}

export interface AddMemberRequest {
  nodeId: string;
  address?: string;
}

export interface HealthResponse {
  status: string;
  components?: Record<string, { status: string; details?: Record<string, unknown> }>;
}

export type UserRole = 'ADMIN' | 'USER';

export interface AuthInfoResponse {
  authenticated: boolean;
  userId: string;
  username: string;
  role: UserRole;
  namespace: string;
}
