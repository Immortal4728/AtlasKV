/**
 * Performance and operational metrics for an AtlasKV node.
 */
export interface Metrics {
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
  totalCasAttempts: number;
  successfulCasRequests: number;
  failedCasRequests: number;
  averageCasLatencyMs: number;
  prefixQueryCount: number;
  averagePrefixLatencyMs: number;
  averagePrefixResultSize: number;
  historyReads: number;
  historyWrites: number;
  rollbackCount: number;
  averageHistorySize: number;
}
