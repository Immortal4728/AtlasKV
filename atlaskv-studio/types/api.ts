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
}

export interface LeaseRequest {
  ttl: string;
  leaseId?: string;
}

export interface CasConflictResponse {
  expectedVersion: number;
  currentVersion: number;
  message?: string;
}

export interface RevisionItem {
  version: number;
  value: string | null;
  timestamp: number;
  nodeLeader?: string;
}

export interface RevisionResponse {
  key: string;
  revisions: RevisionItem[];
  currentVersion: number;
}

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

