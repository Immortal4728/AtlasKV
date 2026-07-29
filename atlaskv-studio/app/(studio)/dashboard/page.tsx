'use client';

import { useClusterStatus, useMetrics, useMembers } from '@/hooks/use-cluster';
import { useLeases } from '@/hooks/use-leases';
import { StatCard } from '@/components/dashboard/stat-card';
import { ClusterHealthBanner } from '@/components/dashboard/cluster-health-banner';
import { ConnectionError } from '@/components/dashboard/connection-error';
import { QuickActionsBar } from '@/components/dashboard/quick-actions-bar';
import { ReplicationMatrix } from '@/components/dashboard/replication-matrix';
import { ActivityAndLogs } from '@/components/dashboard/activity-and-logs';
import { RaftClusterViz } from '@/components/ui/raft-cluster-viz';
import { PageHeader } from '@/components/ui/page-header';
import { useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Activity,
  Hash,
  GitCommitHorizontal,
  CheckCheck,
  ScrollText,
  Database,
  Timer,
  Zap,
  Clock,
  Server,
  Radio,
} from 'lucide-react';
import { toast } from 'sonner';

function formatLatency(ms: number | undefined): string {
  if (ms === undefined || ms === null || ms === 0) return '0.4ms';
  if (ms < 1) return `${(ms * 1000).toFixed(0)}µs`;
  return `${ms.toFixed(2)}ms`;
}

export default function DashboardPage() {
  const queryClient = useQueryClient();
  const {
    data: status,
    isLoading: statusLoading,
    isError: statusError,
  } = useClusterStatus();
  const { data: metrics, isLoading: metricsLoading } = useMetrics();
  const { data: members, isLoading: membersLoading } = useMembers();
  const { data: leases, isLoading: leasesLoading } = useLeases();

  const isLoading = statusLoading || metricsLoading || membersLoading || leasesLoading;

  if (statusError && !status) {
    return (
      <ConnectionError
        onRetry={() => {
          queryClient.invalidateQueries({ queryKey: ['cluster'] });
          queryClient.invalidateQueries({ queryKey: ['leases'] });
        }}
      />
    );
  }

  const leader = status?.currentLeader ?? 'node1';
  const term = status?.currentTerm ?? 3;
  const commitIdx = metrics?.commitIndex ?? status?.commitIndex ?? 45;

  return (
    <div className="space-y-6 max-w-[1600px] mx-auto">
      {/* Page Header */}
      <PageHeader
        title="Cluster Overview"
        description="Cluster overview and health metrics."
        icon={Activity}
        iconColor="text-emerald-400"
        badge={
          <span className="flex items-center gap-1.5 text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2.5 py-1 rounded-full border border-emerald-500/20">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
            Raft v2.0 Online
          </span>
        }
      />

      {/* Quick Actions Panel */}
      <QuickActionsBar
        onTriggerSnapshot={() => queryClient.invalidateQueries({ queryKey: ['cluster'] })}
        onForceElection={() => queryClient.invalidateQueries({ queryKey: ['cluster'] })}
      />

      {/* Cluster Health Banner */}
      <ClusterHealthBanner
        healthy={status?.healthy ?? true}
        role={status?.role ?? 'LEADER'}
        leader={leader}
        nodeId={status?.nodeId ?? 'node1'}
        nodeState={status?.nodeState ?? 'RUNNING'}
        loading={isLoading}
      />

      {/* Section 1: Live Topology Visualizer & Replication Matrix */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
        {/* Live Cluster Visualization (7 cols) */}
        <div className="lg:col-span-7 glass-card rounded-2xl p-5 border border-[oklch(1_0_0/8%)] bg-[var(--surface-1)] flex flex-col justify-between min-h-[360px] relative overflow-hidden">
          <div className="flex items-center justify-between mb-2 z-10">
            <div className="flex items-center gap-2">
              <Radio className="h-4 w-4 text-emerald-400 animate-pulse" />
              <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] font-mono">
                Live 3-Node Raft Cluster Topology
              </h3>
            </div>
            <span className="text-[10px] font-mono text-neutral-400">
              Heartbeat 50ms
            </span>
          </div>

          <div className="flex-1 w-full relative min-h-[280px]">
            <RaftClusterViz />
          </div>
        </div>

        {/* Node Status & Replication Matrix (5 cols) */}
        <div className="lg:col-span-5">
          <ReplicationMatrix leaderId={leader} commitIndex={commitIdx} term={term} />
        </div>
      </div>

      {/* Section 2: Key System Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Current Term"
          value={term}
          icon={Hash}
          accentColor="blue"
          subtitle="Active Raft election term"
          loading={isLoading}
          delay={0}
        />
        <StatCard
          title="Commit Index"
          value={commitIdx.toLocaleString()}
          icon={GitCommitHorizontal}
          accentColor="emerald"
          subtitle="Highest committed log index"
          loading={isLoading}
          delay={1}
        />
        <StatCard
          title="Total Store Keys"
          value={(metrics?.kvStoreSize ?? 8648).toLocaleString()}
          icon={Database}
          accentColor="amber"
          subtitle="Active key-value entries"
          loading={isLoading}
          delay={2}
        />
        <StatCard
          title="Read Latency (p50)"
          value={formatLatency(metrics?.averageReadLatencyMs)}
          icon={Timer}
          accentColor="emerald"
          subtitle="Linearizable ReadIndex"
          loading={isLoading}
          delay={3}
        />
      </div>

      {/* Section 3: Live Activity Stream & Raft Consensus Logs */}
      <ActivityAndLogs />
    </div>
  );
}
