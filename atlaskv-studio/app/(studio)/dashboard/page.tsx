'use client';

import { useClusterStatus, useMetrics, useMembers } from '@/hooks/use-cluster';
import { StatCard } from '@/components/dashboard/stat-card';
import { ClusterHealthBanner } from '@/components/dashboard/cluster-health-banner';
import { MembersBanner } from '@/components/dashboard/members-banner';
import { ConnectionError } from '@/components/dashboard/connection-error';
import { useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Hash,
  GitCommitHorizontal,
  CheckCheck,
  ScrollText,
  Database,
  BookOpen,
  BookCheck,
  Timer,
  Users,
  Activity,
  Clock,
  GitBranch,
} from 'lucide-react';

function formatUptime(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}d ${hours % 24}h`;
  if (hours > 0) return `${hours}h ${minutes % 60}m`;
  if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
  return `${seconds}s`;
}

function formatLatency(ms: number): string {
  if (ms === 0) return '—';
  if (ms < 1) return `${(ms * 1000).toFixed(0)}µs`;
  return `${ms.toFixed(1)}ms`;
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

  const isLoading = statusLoading || metricsLoading || membersLoading;

  if (statusError && !status) {
    return (
      <ConnectionError
        onRetry={() => {
          queryClient.invalidateQueries({ queryKey: ['cluster'] });
        }}
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
      >
        <h1 className="text-xl font-semibold tracking-tight text-white/90">
          Dashboard
        </h1>
        <p className="text-sm text-white/30 mt-0.5">
          Real-time overview of your AtlasKV cluster
        </p>
      </motion.div>

      {/* Cluster Health Banner */}
      <ClusterHealthBanner
        healthy={status?.healthy ?? false}
        role={status?.role ?? 'UNKNOWN'}
        leader={status?.currentLeader ?? null}
        nodeId={status?.nodeId ?? '—'}
        nodeState={status?.nodeState ?? 'UNKNOWN'}
        loading={isLoading}
      />

      {/* Primary Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Current Term"
          value={status?.currentTerm ?? 0}
          icon={Hash}
          accentColor="blue"
          subtitle="Raft election term"
          loading={isLoading}
        />
        <StatCard
          title="Commit Index"
          value={metrics?.commitIndex ?? 0}
          icon={GitCommitHorizontal}
          accentColor="emerald"
          subtitle="Highest committed entry"
          loading={isLoading}
        />
        <StatCard
          title="Last Applied"
          value={metrics?.lastApplied ?? 0}
          icon={CheckCheck}
          accentColor="purple"
          subtitle="Last applied to state machine"
          loading={isLoading}
        />
        <StatCard
          title="Log Length"
          value={metrics?.logLength ?? 0}
          icon={ScrollText}
          accentColor="cyan"
          subtitle="Total log entries"
          loading={isLoading}
        />
      </div>

      {/* Secondary Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="KV Store Size"
          value={metrics?.kvStoreSize ?? 0}
          icon={Database}
          accentColor="amber"
          subtitle="Keys stored"
          loading={isLoading}
        />
        <StatCard
          title="Read Requests"
          value={metrics?.totalReadRequests ?? 0}
          icon={BookOpen}
          accentColor="blue"
          subtitle={`${metrics?.successfulReadRequests ?? 0} successful`}
          loading={isLoading}
        />
        <StatCard
          title="Avg Read Latency"
          value={formatLatency(metrics?.averageReadLatencyMs ?? 0)}
          icon={Timer}
          accentColor="purple"
          subtitle="ReadIndex latency"
          loading={isLoading}
        />
        <StatCard
          title="Uptime"
          value={formatUptime(status?.uptimeMs ?? 0)}
          icon={Clock}
          accentColor="emerald"
          subtitle="Since last start"
          loading={isLoading}
        />
      </div>

      {/* Bottom Row: Members + Additional metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Members Banner (2 cols) */}
        <div className="lg:col-span-2">
          <MembersBanner
            members={members?.members ?? []}
            isJoint={members?.jointConsensusActive ?? false}
            leader={members?.leaderId ?? null}
            loading={isLoading}
          />
        </div>

        {/* Additional Stats (1 col) */}
        <div className="space-y-4">
          <StatCard
            title="Node Count"
            value={members?.members.length ?? 0}
            icon={Users}
            accentColor="cyan"
            subtitle={`${status?.peerCount ?? 0} peers`}
            loading={isLoading}
          />
          <StatCard
            title="Membership Changes"
            value={metrics?.membershipChangeCount ?? 0}
            icon={GitBranch}
            accentColor="amber"
            subtitle={
              metrics?.averageMembershipChangeLatencyMs
                ? `Avg ${formatLatency(metrics.averageMembershipChangeLatencyMs)}`
                : 'Joint consensus ops'
            }
            loading={isLoading}
          />
        </div>
      </div>
    </div>
  );
}
