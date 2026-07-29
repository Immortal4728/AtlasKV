'use client';

import { InteractiveClusterViz } from '@/components/cluster/interactive-cluster-viz';

import { useClusterStatus, useMembers } from '@/hooks/use-cluster';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { PageHeader } from '@/components/ui/page-header';
import { Network, Server, Crown, Shield, Activity, RefreshCw, Cpu, Heart } from 'lucide-react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';

interface NodeDetail {
  id: string;
  host: string;
  port: number;
  grpcPort: number;
  role: 'LEADER' | 'FOLLOWER';
  healthy: boolean;
  term: number;
  commitIndex: number;
  appliedIndex: number;
  latencyMs: number;
  peers: number;
}

const NODES_DATA: NodeDetail[] = [
  { id: 'node1', host: 'localhost', port: 8081, grpcPort: 50051, role: 'FOLLOWER', healthy: true, term: 47, commitIndex: 34601, appliedIndex: 34601, latencyMs: 0.45, peers: 2 },
  { id: 'node2', host: 'localhost', port: 8082, grpcPort: 50052, role: 'FOLLOWER', healthy: true, term: 47, commitIndex: 34601, appliedIndex: 34601, latencyMs: 0.52, peers: 2 },
  { id: 'node3', host: 'localhost', port: 8083, grpcPort: 50053, role: 'LEADER', healthy: true, term: 47, commitIndex: 34601, appliedIndex: 34601, latencyMs: 0.38, peers: 2 },
];

const stagger = {
  animate: {
    transition: { staggerChildren: 0.1 },
  },
};

const cardVariant = {
  initial: { opacity: 0, y: 20, scale: 0.97 },
  animate: {
    opacity: 1, y: 0, scale: 1,
    transition: { duration: 0.45, ease: [0.25, 0.46, 0.45, 0.94] as const },
  },
};

export default function ClusterPage() {
  const { data: status, refetch } = useClusterStatus();
  const { data: members } = useMembers();

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Cluster Topology"
        description="Distributed consensus node status, quorum health, terms, and replication progress"
        icon={Network}
        iconColor="text-emerald-400"
        actions={
          <Button
            onClick={() => refetch()}
            variant="outline"
            className="border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/50%)] hover:bg-[oklch(1_0_0/4%)] hover:text-white text-xs gap-1.5 rounded-lg"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh Nodes
          </Button>
        }
      />

      {/* Interactive Cluster Topology Visualization */}
      <InteractiveClusterViz />

      {/* Node Grid */}
      <motion.div
        variants={stagger}
        initial="initial"
        animate="animate"
        className="grid grid-cols-1 md:grid-cols-3 gap-5"
      >
        {NODES_DATA.map((node) => {
          const isLeader = node.role === 'LEADER';

          return (
            <motion.div
              key={node.id}
              variants={cardVariant}
              whileHover={{ y: -4, transition: { duration: 0.2 } }}
              className={cn(
                'relative overflow-hidden rounded-xl border backdrop-blur-xl p-5 transition-all duration-300',
                isLeader
                  ? 'bg-emerald-500/[0.08] border-emerald-500/30 shadow-md'
                  : 'bg-[var(--surface-1)] border-border dark:border-[oklch(1_0_0/8%)] hover:border-emerald-500/30 shadow-sm'
              )}
            >
              {/* Leader glow */}
              {isLeader && (
                <div className="absolute -top-10 -right-10 w-24 h-24 bg-emerald-500/10 rounded-full blur-2xl" />
              )}

              {/* Node Card Header */}
              <div className="relative flex items-center justify-between border-b border-border dark:border-[oklch(1_0_0/6%)] pb-4">
                <div className="flex items-center gap-2.5">
                  <div
                    className={cn(
                      'relative h-10 w-10 rounded-xl flex items-center justify-center border',
                      isLeader
                        ? 'bg-emerald-500/15 border-emerald-500/30 text-emerald-600 dark:text-emerald-400'
                        : 'bg-neutral-100 dark:bg-[oklch(1_0_0/4%)] border-border dark:border-[oklch(1_0_0/8%)] text-neutral-600 dark:text-neutral-400'
                    )}
                  >
                    {isLeader ? <Crown className="h-4.5 w-4.5" /> : <Server className="h-4.5 w-4.5" />}
                    {/* Pulse ring for leader */}
                    {isLeader && (
                      <span className="absolute inset-0 rounded-xl animate-glow-ring" />
                    )}
                  </div>
                  <div>
                    <h2 className="text-sm font-bold text-[var(--foreground)] font-mono">{node.id}</h2>
                    <p className="text-xs text-neutral-600 dark:text-neutral-400 font-mono font-medium">
                      {node.host}:{node.port}
                    </p>
                  </div>
                </div>

                <span
                  className={cn(
                    'px-2 py-0.5 rounded-md text-xs font-mono font-semibold border',
                    isLeader
                      ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                      : 'bg-neutral-100 dark:bg-[oklch(1_0_0/4%)] text-neutral-600 dark:text-neutral-400 border-border dark:border-[oklch(1_0_0/8%)]'
                  )}
                >
                  {node.role}
                </span>
              </div>

              {/* Node Card Body */}
              <div className="relative py-4 space-y-2.5 font-mono text-xs">
                <div className="flex items-center justify-between text-neutral-600 dark:text-neutral-400 font-medium">
                  <span>Health Status</span>
                  <span className="text-emerald-600 dark:text-emerald-400 flex items-center gap-1.5 font-semibold">
                    <Heart className="h-3 w-3 animate-heartbeat" />
                    Healthy
                  </span>
                </div>

                <div className="flex items-center justify-between text-neutral-600 dark:text-neutral-400 font-medium">
                  <span>Current Term</span>
                  <span className="text-[var(--foreground)] font-bold">{node.term}</span>
                </div>

                <div className="flex items-center justify-between text-neutral-600 dark:text-neutral-400 font-medium">
                  <span>Commit Index</span>
                  <span className="text-emerald-600 dark:text-emerald-400 font-bold">{node.commitIndex.toLocaleString()}</span>
                </div>

                <div className="flex items-center justify-between text-neutral-600 dark:text-neutral-400 font-medium">
                  <span>Applied Index</span>
                  <span className="text-purple-600 dark:text-purple-400 font-bold">{node.appliedIndex.toLocaleString()}</span>
                </div>

                <div className="flex items-center justify-between text-neutral-600 dark:text-neutral-400 font-medium">
                  <span>gRPC Port</span>
                  <span className="text-[var(--foreground)] font-semibold">{node.grpcPort}</span>
                </div>

                <div className="flex items-center justify-between text-neutral-600 dark:text-neutral-400 font-medium">
                  <span>Replication Latency</span>
                  <span className="text-cyan-600 dark:text-cyan-400 font-semibold">{node.latencyMs}ms</span>
                </div>
              </div>

              {/* Replication progress bar */}
              <div className="mb-3">
                <div className="flex items-center justify-between text-xs text-neutral-600 dark:text-neutral-400 font-mono font-medium mb-1">
                  <span>Sync Progress</span>
                  <span>100%</span>
                </div>
                <div className="h-1.5 rounded-full bg-neutral-200 dark:bg-[oklch(1_0_0/6%)] overflow-hidden">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: '100%' }}
                    transition={{ duration: 1.5, ease: 'easeOut', delay: 0.5 }}
                    className={cn(
                      'h-full rounded-full',
                      isLeader
                        ? 'bg-gradient-to-r from-emerald-500 to-cyan-500'
                        : 'bg-gradient-to-r from-blue-500 to-cyan-500'
                    )}
                  />
                </div>
              </div>

              {/* Node Card Footer */}
              <div className="pt-3 border-t border-border dark:border-[oklch(1_0_0/6%)] flex items-center justify-between text-xs text-neutral-600 dark:text-neutral-400 font-mono font-medium">
                <span>Peers: {node.peers}</span>
                <span className="flex items-center gap-1 font-semibold text-emerald-600 dark:text-emerald-400">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                  RUNNING
                </span>
              </div>
            </motion.div>
          );
        })}
      </motion.div>
    </div>
  );
}
