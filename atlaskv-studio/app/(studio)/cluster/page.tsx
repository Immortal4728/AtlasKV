'use client';

import { useClusterStatus, useMembers, useMetrics } from '@/hooks/use-cluster';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Network, Server, Crown, Shield, Activity, RefreshCw, Cpu, Layers } from 'lucide-react';
import { motion } from 'framer-motion';

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

export default function ClusterPage() {
  const { data: status, refetch } = useClusterStatus();
  const { data: members } = useMembers();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Network className="h-5 w-5 text-emerald-400" />
            Cluster Topology & Raft Nodes
          </h1>
          <p className="text-xs text-zinc-400 mt-1">
            Distributed consensus node status, quorum health, terms, and replication progress
          </p>
        </div>

        <Button
          onClick={() => refetch()}
          variant="outline"
          className="border-white/10 text-zinc-300 hover:bg-white/5 text-xs gap-1.5"
        >
          <RefreshCw className="h-3.5 w-3.5" />
          Refresh Nodes
        </Button>
      </div>

      {/* Node Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {NODES_DATA.map((node) => {
          const isLeader = node.role === 'LEADER';

          return (
            <motion.div
              key={node.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={`p-5 rounded-xl border backdrop-blur-md transition-all ${
                isLeader
                  ? 'bg-emerald-500/[0.04] border-emerald-500/30 shadow-lg shadow-emerald-500/5'
                  : 'bg-zinc-900/50 border-white/[0.08]'
              }`}
            >
              {/* Node Card Header */}
              <div className="flex items-center justify-between border-b border-white/[0.08] pb-4">
                <div className="flex items-center gap-2.5">
                  <div
                    className={`h-9 w-9 rounded-xl flex items-center justify-center border ${
                      isLeader
                        ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                        : 'bg-zinc-800 border-zinc-700 text-zinc-300'
                    }`}
                  >
                    {isLeader ? <Crown className="h-4 w-4" /> : <Server className="h-4 w-4" />}
                  </div>
                  <div>
                    <h2 className="text-sm font-bold text-white font-mono">{node.id}</h2>
                    <p className="text-[11px] text-zinc-400 font-mono">
                      {node.host}:{node.port}
                    </p>
                  </div>
                </div>

                <Badge
                  variant="outline"
                  className={
                    isLeader
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30 text-[10px] font-mono'
                      : 'bg-zinc-800 text-zinc-300 border-zinc-700 text-[10px] font-mono'
                  }
                >
                  {node.role}
                </Badge>
              </div>

              {/* Node Card Body */}
              <div className="py-4 space-y-3 font-mono text-xs">
                <div className="flex items-center justify-between text-zinc-400">
                  <span>Health Status</span>
                  <span className="text-emerald-400 flex items-center gap-1 font-semibold">
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" /> Healthy
                  </span>
                </div>

                <div className="flex items-center justify-between text-zinc-400">
                  <span>Current Term</span>
                  <span className="text-zinc-200 font-semibold">{node.term}</span>
                </div>

                <div className="flex items-center justify-between text-zinc-400">
                  <span>Commit Index</span>
                  <span className="text-emerald-400 font-semibold">{node.commitIndex.toLocaleString()}</span>
                </div>

                <div className="flex items-center justify-between text-zinc-400">
                  <span>Applied Index</span>
                  <span className="text-purple-400 font-semibold">{node.appliedIndex.toLocaleString()}</span>
                </div>

                <div className="flex items-center justify-between text-zinc-400">
                  <span>gRPC Port</span>
                  <span className="text-zinc-300">{node.grpcPort}</span>
                </div>

                <div className="flex items-center justify-between text-zinc-400">
                  <span>Replication Latency</span>
                  <span className="text-cyan-400">{node.latencyMs}ms</span>
                </div>
              </div>

              {/* Node Card Footer */}
              <div className="pt-3 border-t border-white/[0.08] flex items-center justify-between text-[11px] text-zinc-500 font-mono">
                <span>Peers: {node.peers}</span>
                <span>Raft State: RUNNING</span>
              </div>
            </motion.div>
          );
        })}
      </div>
    </div>
  );
}
