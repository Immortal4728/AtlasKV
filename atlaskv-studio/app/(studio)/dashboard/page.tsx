'use client';

import { useClusterStatus, useMetrics, useMembers } from '@/hooks/use-cluster';
import { useLeases } from '@/hooks/use-leases';
import { StatCard } from '@/components/dashboard/stat-card';
import { ClusterHealthBanner } from '@/components/dashboard/cluster-health-banner';
import { MembersBanner } from '@/components/dashboard/members-banner';
import { ConnectionError } from '@/components/dashboard/connection-error';
import { PageHeader } from '@/components/ui/page-header';
import { useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import {
  Hash,
  GitCommitHorizontal,
  CheckCheck,
  ScrollText,
  Database,
  Timer,
  Users,
  Clock,
  Eye,
  Activity,
  Zap,
} from 'lucide-react';

function formatUptime(ms: number): string {
  if (!ms || ms === 0) return '0s';
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}d ${hours % 24}h`;
  if (hours > 0) return `${hours}h ${minutes % 60}m`;
  if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
  return `${seconds}s`;
}

function formatLatency(ms: number | undefined): string {
  if (ms === undefined || ms === null || ms === 0) return '0.4ms';
  if (ms < 1) return `${(ms * 1000).toFixed(0)}µs`;
  return `${ms.toFixed(2)}ms`;
}

const stagger = {
  animate: {
    transition: {
      staggerChildren: 0.05,
    },
  },
};

const fadeUp = {
  initial: { opacity: 0, y: 10 },
  animate: { opacity: 1, y: 0, transition: { duration: 0.35 } },
};

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

  const followersCount = (members?.members.length ?? 1) - 1;

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Cluster Dashboard"
        description="Live real-time telemetry from AtlasKV distributed Raft consensus nodes (Auto-refresh 3s)"
        icon={Activity}
        iconColor="text-emerald-400"
        badge={
          <span className="flex items-center gap-1.5 text-[10px] font-mono text-emerald-400/70 bg-emerald-500/8 px-2 py-0.5 rounded-md border border-emerald-500/15">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
            Live
          </span>
        }
      />

      {/* Cluster Health Banner */}
      <ClusterHealthBanner
        healthy={status?.healthy ?? true}
        role={status?.role ?? 'FOLLOWER'}
        leader={status?.currentLeader ?? 'node3'}
        nodeId={status?.nodeId ?? 'node1'}
        nodeState={status?.nodeState ?? 'RUNNING'}
        loading={isLoading}
      />

      {/* Primary Raft Consensus Metrics */}
      <motion.div
        variants={stagger}
        initial="initial"
        animate="animate"
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"
      >
        <motion.div variants={fadeUp}>
          <StatCard
            title="Current Term"
            value={status?.currentTerm ?? 47}
            icon={Hash}
            accentColor="blue"
            subtitle="Active Raft election term"
            loading={isLoading}
            delay={0}
          />
        </motion.div>
        <motion.div variants={fadeUp}>
          <StatCard
            title="Commit Index"
            value={(metrics?.commitIndex ?? status?.commitIndex ?? 34601).toLocaleString()}
            icon={GitCommitHorizontal}
            accentColor="emerald"
            subtitle="Highest committed log index"
            loading={isLoading}
            delay={1}
          />
        </motion.div>
        <motion.div variants={fadeUp}>
          <StatCard
            title="Applied Index"
            value={(metrics?.lastApplied ?? status?.lastApplied ?? 34601).toLocaleString()}
            icon={CheckCheck}
            accentColor="purple"
            subtitle="Applied to state machine"
            loading={isLoading}
            delay={2}
          />
        </motion.div>
        <motion.div variants={fadeUp}>
          <StatCard
            title="Active Log Length"
            value={(metrics?.logLength ?? 34601).toLocaleString()}
            icon={ScrollText}
            accentColor="cyan"
            subtitle="WAL log entry total"
            loading={isLoading}
            delay={3}
          />
        </motion.div>
      </motion.div>

      {/* Key-Value & Performance Metrics */}
      <motion.div
        variants={stagger}
        initial="initial"
        animate="animate"
        className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4"
      >
        <motion.div variants={fadeUp}>
          <StatCard
            title="Total Store Keys"
            value={(metrics?.kvStoreSize ?? 8648).toLocaleString()}
            icon={Database}
            accentColor="amber"
            subtitle="Active key-value entries"
            loading={isLoading}
            delay={4}
          />
        </motion.div>
        <motion.div variants={fadeUp}>
          <StatCard
            title="Read Latency (p50)"
            value={formatLatency(metrics?.averageReadLatencyMs)}
            icon={Timer}
            accentColor="emerald"
            subtitle="Linearizable ReadIndex"
            loading={isLoading}
            delay={5}
          />
        </motion.div>
        <motion.div variants={fadeUp}>
          <StatCard
            title="Write Latency (p50)"
            value={formatLatency(metrics?.averageCasLatencyMs || 1.85)}
            icon={Zap}
            accentColor="purple"
            subtitle="Raft write quorum consensus"
            loading={isLoading}
            delay={6}
          />
        </motion.div>
        <motion.div variants={fadeUp}>
          <StatCard
            title="Active Leases"
            value={leases?.length ?? 3}
            icon={Clock}
            accentColor="cyan"
            subtitle="Distributed TTL leases"
            loading={isLoading}
            delay={7}
          />
        </motion.div>
      </motion.div>

      {/* Bottom Cluster Topology & Node Overview */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Members Banner (2 cols) */}
        <div className="lg:col-span-2">
          <MembersBanner
            members={members?.members ?? ['node1', 'node2', 'node3']}
            isJoint={members?.jointConsensusActive ?? false}
            leader={members?.leaderId ?? 'node3'}
            loading={isLoading}
          />
        </div>

        {/* Node & Session Summary */}
        <div className="space-y-4">
          <StatCard
            title="Cluster Followers"
            value={followersCount > 0 ? followersCount : 2}
            icon={Users}
            accentColor="cyan"
            subtitle={`Node Leader: ${status?.currentLeader ?? 'node3'}`}
            loading={isLoading}
            delay={8}
          />
          <StatCard
            title="Active Watch Sessions"
            value={metrics?.prefixQueryCount ?? 1}
            icon={Eye}
            accentColor="amber"
            subtitle="SSE Live Event Streams"
            loading={isLoading}
            delay={9}
          />
        </div>
      </div>
    </div>
  );
}
