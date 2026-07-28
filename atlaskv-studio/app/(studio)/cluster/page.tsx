'use client';

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

      {/* Cluster Topology Diagram */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="glass-card rounded-xl p-6 overflow-hidden relative"
      >
        {/* Ambient glow */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[300px] h-[200px] bg-emerald-500/5 blur-[80px] pointer-events-none" />

        <div className="relative flex items-center justify-center py-8">
          <svg width="480" height="180" viewBox="0 0 480 180" className="opacity-80">
            {/* Connection lines with animated data flow */}
            <line x1="240" y1="30" x2="80" y2="150" stroke="oklch(1 0 0 / 8%)" strokeWidth="2" />
            <line x1="240" y1="30" x2="400" y2="150" stroke="oklch(1 0 0 / 8%)" strokeWidth="2" />
            <line x1="80" y1="150" x2="400" y2="150" stroke="oklch(1 0 0 / 6%)" strokeWidth="1.5" strokeDasharray="4 4" />

            {/* Animated data packets on lines */}
            <circle r="3" fill="#10b981" opacity="0.8">
              <animateMotion dur="2s" repeatCount="indefinite" path="M240,30 L80,150" />
            </circle>
            <circle r="3" fill="#10b981" opacity="0.8">
              <animateMotion dur="2.5s" repeatCount="indefinite" path="M240,30 L400,150" />
            </circle>
            <circle r="2" fill="#06b6d4" opacity="0.6">
              <animateMotion dur="3s" repeatCount="indefinite" path="M80,150 L400,150" />
            </circle>

            {/* Leader node (top center) */}
            <g>
              <circle cx="240" cy="30" r="16" fill="oklch(0.72 0.19 160 / 15%)" stroke="oklch(0.72 0.19 160 / 40%)" strokeWidth="2">
                <animate attributeName="r" values="16;18;16" dur="2s" repeatCount="indefinite" />
              </circle>
              <circle cx="240" cy="30" r="6" fill="#10b981">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="1.5s" repeatCount="indefinite" />
              </circle>
              <text x="240" y="56" fill="oklch(1 0 0 / 50%)" fontSize="10" textAnchor="middle" fontFamily="monospace">node3 (Leader)</text>
            </g>

            {/* Follower 1 (bottom left) */}
            <g>
              <circle cx="80" cy="150" r="12" fill="oklch(0.6 0.2 250 / 10%)" stroke="oklch(0.6 0.2 250 / 30%)" strokeWidth="1.5" />
              <circle cx="80" cy="150" r="4" fill="#3b82f6" />
              <text x="80" y="174" fill="oklch(1 0 0 / 40%)" fontSize="10" textAnchor="middle" fontFamily="monospace">node1</text>
            </g>

            {/* Follower 2 (bottom right) */}
            <g>
              <circle cx="400" cy="150" r="12" fill="oklch(0.6 0.2 250 / 10%)" stroke="oklch(0.6 0.2 250 / 30%)" strokeWidth="1.5" />
              <circle cx="400" cy="150" r="4" fill="#3b82f6" />
              <text x="400" y="174" fill="oklch(1 0 0 / 40%)" fontSize="10" textAnchor="middle" fontFamily="monospace">node2</text>
            </g>
          </svg>
        </div>

        <div className="text-center text-[10px] text-[oklch(1_0_0/25%)] font-mono">
          Raft Consensus Topology · 3-Node Quorum · Term {status?.currentTerm ?? 47}
        </div>
      </motion.div>

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
                  ? 'bg-emerald-500/[0.04] border-emerald-500/20 shadow-[0_0_30px_oklch(0.72_0.19_160/6%)]'
                  : 'bg-[oklch(0.12_0.008_280/50%)] border-[oklch(1_0_0/6%)] hover:border-[oklch(1_0_0/10%)]'
              )}
            >
              {/* Leader glow */}
              {isLeader && (
                <div className="absolute -top-10 -right-10 w-24 h-24 bg-emerald-500/10 rounded-full blur-2xl" />
              )}

              {/* Node Card Header */}
              <div className="relative flex items-center justify-between border-b border-[oklch(1_0_0/6%)] pb-4">
                <div className="flex items-center gap-2.5">
                  <div
                    className={cn(
                      'relative h-10 w-10 rounded-xl flex items-center justify-center border',
                      isLeader
                        ? 'bg-emerald-500/10 border-emerald-500/25 text-emerald-400'
                        : 'bg-[oklch(1_0_0/4%)] border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/40%)]'
                    )}
                  >
                    {isLeader ? <Crown className="h-4.5 w-4.5" /> : <Server className="h-4.5 w-4.5" />}
                    {/* Pulse ring for leader */}
                    {isLeader && (
                      <span className="absolute inset-0 rounded-xl animate-glow-ring" />
                    )}
                  </div>
                  <div>
                    <h2 className="text-sm font-bold text-white font-mono">{node.id}</h2>
                    <p className="text-[10px] text-[oklch(1_0_0/30%)] font-mono">
                      {node.host}:{node.port}
                    </p>
                  </div>
                </div>

                <span
                  className={cn(
                    'px-2 py-0.5 rounded-md text-[10px] font-mono font-semibold border',
                    isLeader
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                      : 'bg-[oklch(1_0_0/4%)] text-[oklch(1_0_0/45%)] border-[oklch(1_0_0/8%)]'
                  )}
                >
                  {node.role}
                </span>
              </div>

              {/* Node Card Body */}
              <div className="relative py-4 space-y-2.5 font-mono text-xs">
                <div className="flex items-center justify-between text-[oklch(1_0_0/35%)]">
                  <span>Health Status</span>
                  <span className="text-emerald-400 flex items-center gap-1.5 font-semibold">
                    <Heart className="h-3 w-3 animate-heartbeat" />
                    Healthy
                  </span>
                </div>

                <div className="flex items-center justify-between text-[oklch(1_0_0/35%)]">
                  <span>Current Term</span>
                  <span className="text-[oklch(1_0_0/70%)] font-semibold">{node.term}</span>
                </div>

                <div className="flex items-center justify-between text-[oklch(1_0_0/35%)]">
                  <span>Commit Index</span>
                  <span className="text-emerald-400 font-semibold">{node.commitIndex.toLocaleString()}</span>
                </div>

                <div className="flex items-center justify-between text-[oklch(1_0_0/35%)]">
                  <span>Applied Index</span>
                  <span className="text-purple-400 font-semibold">{node.appliedIndex.toLocaleString()}</span>
                </div>

                <div className="flex items-center justify-between text-[oklch(1_0_0/35%)]">
                  <span>gRPC Port</span>
                  <span className="text-[oklch(1_0_0/50%)]">{node.grpcPort}</span>
                </div>

                <div className="flex items-center justify-between text-[oklch(1_0_0/35%)]">
                  <span>Replication Latency</span>
                  <span className="text-cyan-400">{node.latencyMs}ms</span>
                </div>
              </div>

              {/* Replication progress bar */}
              <div className="mb-3">
                <div className="flex items-center justify-between text-[10px] text-[oklch(1_0_0/25%)] mb-1">
                  <span>Sync Progress</span>
                  <span>100%</span>
                </div>
                <div className="h-1 rounded-full bg-[oklch(1_0_0/6%)] overflow-hidden">
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
              <div className="pt-3 border-t border-[oklch(1_0_0/6%)] flex items-center justify-between text-[10px] text-[oklch(1_0_0/25%)] font-mono">
                <span>Peers: {node.peers}</span>
                <span className="flex items-center gap-1">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
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
