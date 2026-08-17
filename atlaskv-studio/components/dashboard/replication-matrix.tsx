'use client';

import React from 'react';
import { Server, CheckCircle2, AlertTriangle, XCircle, Shield } from 'lucide-react';
import type { NodeDetail } from '@/types/api';

interface ReplicationMatrixProps {
  nodes?: NodeDetail[];
  leaderId?: string;
  commitIndex?: number;
  term?: number;
}

export function ReplicationMatrix({
  nodes: propNodes,
  leaderId = 'node1',
  commitIndex = 0,
  term = 0,
}: ReplicationMatrixProps) {
  // Default fallback if nodes query is still loading or unavailable
  const fallbackNodes: NodeDetail[] = [
    {
      id: 'node1',
      host: '127.0.0.1',
      port: 8081,
      grpcPort: 50051,
      role: leaderId === 'node1' ? 'LEADER' : 'FOLLOWER',
      healthy: true,
      term: term || 1,
      commitIndex: commitIndex || 1,
      appliedIndex: commitIndex || 1,
      matchIndex: commitIndex || 1,
      nextIndex: (commitIndex || 1) + 1,
      isLeader: leaderId === 'node1',
      isLocal: true,
      latencyMs: 0,
      peers: 2,
    },
    {
      id: 'node2',
      host: '127.0.0.1',
      port: 8082,
      grpcPort: 50052,
      role: leaderId === 'node2' ? 'LEADER' : 'FOLLOWER',
      healthy: true,
      term: term || 1,
      commitIndex: commitIndex || 1,
      appliedIndex: commitIndex || 1,
      matchIndex: commitIndex || 1,
      nextIndex: (commitIndex || 1) + 1,
      isLeader: leaderId === 'node2',
      isLocal: false,
      latencyMs: 0.45,
      peers: 2,
    },
    {
      id: 'node3',
      host: '127.0.0.1',
      port: 8083,
      grpcPort: 50053,
      role: leaderId === 'node3' ? 'LEADER' : 'FOLLOWER',
      healthy: true,
      term: term || 1,
      commitIndex: commitIndex || 1,
      appliedIndex: commitIndex || 1,
      matchIndex: commitIndex || 1,
      nextIndex: (commitIndex || 1) + 1,
      isLeader: leaderId === 'node3',
      isLocal: false,
      latencyMs: 0.45,
      peers: 2,
    },
  ];

  const nodes = propNodes && propNodes.length > 0 ? propNodes : fallbackNodes;
  const healthyCount = nodes.filter((n) => n.healthy).length;
  const totalCount = nodes.length;
  const effectiveCommit = Math.max(commitIndex, ...nodes.map((n) => n.commitIndex), 1);

  return (
    <div className="glass-card rounded-2xl p-5 border border-[oklch(1_0_0/8%)] bg-[var(--surface-1)]">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Server className="h-4 w-4 text-emerald-400" />
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] font-mono">
            Node Status & Replication Matrix
          </h3>
        </div>
        <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
          Quorum {healthyCount}/{totalCount}
        </span>
      </div>

      <div className="space-y-3">
        {nodes.map((node) => {
          const isLeader = node.role === 'LEADER' || node.isLeader;
          const matchIdx = node.matchIndex ?? node.commitIndex;
          const progressPercent = Math.min(100, Math.max(10, Math.round((matchIdx / effectiveCommit) * 100)));

          return (
            <div
              key={node.id}
              className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-3 rounded-xl border border-[oklch(1_0_0/6%)] bg-[var(--surface-0)] hover:border-emerald-500/20 transition-all"
            >
              {/* Left: Node Info */}
              <div className="flex items-center gap-3">
                <div
                  className={`h-8 w-8 rounded-lg flex items-center justify-center font-mono text-xs font-bold ${
                    isLeader
                      ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                      : 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                  }`}
                >
                  {node.id.slice(-1)}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-[var(--foreground)]">{node.id}</span>
                    {node.isLocal && (
                      <span className="text-[9px] font-mono text-cyan-400 bg-cyan-500/10 px-1.5 py-0.2 rounded border border-cyan-500/20">
                        LOCAL
                      </span>
                    )}
                    <span
                      className={`text-[9px] font-mono font-bold px-1.5 py-0.2 rounded ${
                        isLeader
                          ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                          : 'bg-neutral-500/15 text-neutral-400 border border-neutral-500/20'
                      }`}
                    >
                      {node.role}
                    </span>
                  </div>
                  <span className="text-xs font-mono text-neutral-600 dark:text-neutral-400 font-medium">
                    {node.host}:{node.port} <span className="text-[10px] text-neutral-500">(gRPC :{node.grpcPort})</span>
                  </span>
                </div>
              </div>

              {/* Middle: Log & Replication Bar */}
              <div className="flex-1 sm:max-w-xs space-y-1">
                <div className="flex items-center justify-between text-xs font-mono text-neutral-600 dark:text-neutral-400 font-medium">
                  <span>Match: #{matchIdx}</span>
                  <span>Lag: {node.latencyMs.toFixed(1)}ms</span>
                </div>
                <div className="h-2 w-full rounded-full bg-neutral-200 dark:bg-[oklch(1_0_0/6%)] overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      isLeader
                        ? 'bg-gradient-to-r from-emerald-500 to-teal-400'
                        : 'bg-gradient-to-r from-teal-500 to-cyan-400'
                    }`}
                    style={{ width: `${progressPercent}%` }}
                  />
                </div>
              </div>

              {/* Right: Heartbeat & Health Badge */}
              <div className="flex items-center gap-3 text-xs font-mono">
                <span className="text-neutral-600 dark:text-neutral-400 text-xs font-medium">
                  Term {node.term}
                </span>
                <span
                  className={`flex items-center gap-1 text-xs px-2 py-0.5 rounded-full border font-semibold ${
                    node.healthy
                      ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                      : 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-500/30'
                  }`}
                >
                  {node.healthy ? <CheckCircle2 className="h-3.5 w-3.5" /> : <XCircle className="h-3.5 w-3.5" />}
                  {node.healthy ? 'HEALTHY' : 'OFFLINE'}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
