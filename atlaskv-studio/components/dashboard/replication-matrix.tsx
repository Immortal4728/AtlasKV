'use client';

import React from 'react';
import { Server, CheckCircle2, Shield, Activity, GitCommitHorizontal, Clock } from 'lucide-react';
import { motion } from 'framer-motion';

interface NodeInfo {
  id: string;
  role: 'LEADER' | 'FOLLOWER' | 'CANDIDATE';
  address: string;
  logIndex: number;
  commitIndex: number;
  matchIndex: number;
  lagMs: number;
  health: 'HEALTHY' | 'SYNCING' | 'OFFLINE';
  lastHeartbeat: string;
}

export function ReplicationMatrix({
  leaderId = 'node1',
  commitIndex = 45,
  term = 3,
}: {
  leaderId?: string;
  commitIndex?: number;
  term?: number;
}) {
  const nodes: NodeInfo[] = [
    {
      id: 'node1',
      role: leaderId === 'node1' ? 'LEADER' : 'FOLLOWER',
      address: '127.0.0.1:8081',
      logIndex: commitIndex,
      commitIndex: commitIndex,
      matchIndex: commitIndex,
      lagMs: 0,
      health: 'HEALTHY',
      lastHeartbeat: '0ms ago',
    },
    {
      id: 'node2',
      role: leaderId === 'node2' ? 'LEADER' : 'FOLLOWER',
      address: '127.0.0.1:8082',
      logIndex: commitIndex,
      commitIndex: commitIndex,
      matchIndex: commitIndex,
      lagMs: 0.4,
      health: 'HEALTHY',
      lastHeartbeat: '45ms ago',
    },
    {
      id: 'node3',
      role: leaderId === 'node3' ? 'LEADER' : 'FOLLOWER',
      address: '127.0.0.1:8083',
      logIndex: commitIndex - 1,
      commitIndex: commitIndex - 1,
      matchIndex: commitIndex - 1,
      lagMs: 1.2,
      health: 'SYNCING',
      lastHeartbeat: '120ms ago',
    },
  ];

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
          Quorum 3/3
        </span>
      </div>

      <div className="space-y-3">
        {nodes.map((node) => {
          const isLeader = node.role === 'LEADER';
          const isSyncing = node.health === 'SYNCING';

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
                  <span className="text-xs font-mono text-neutral-600 dark:text-neutral-400 font-medium">{node.address}</span>
                </div>
              </div>

              {/* Middle: Log & Replication Bar */}
              <div className="flex-1 sm:max-w-xs space-y-1">
                <div className="flex items-center justify-between text-xs font-mono text-neutral-600 dark:text-neutral-400 font-medium">
                  <span>Log Index: {node.logIndex}</span>
                  <span>Lag: {node.lagMs}ms</span>
                </div>
                <div className="h-2 w-full rounded-full bg-neutral-200 dark:bg-[oklch(1_0_0/6%)] overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      isLeader
                        ? 'bg-gradient-to-r from-emerald-500 to-teal-400'
                        : isSyncing
                        ? 'bg-gradient-to-r from-amber-500 to-cyan-400'
                        : 'bg-emerald-500'
                    }`}
                    style={{ width: isLeader ? '100%' : `${(node.logIndex / commitIndex) * 100}%` }}
                  />
                </div>
              </div>

              {/* Right: Heartbeat & Health Badge */}
              <div className="flex items-center gap-3 text-xs font-mono">
                <span className="text-neutral-600 dark:text-neutral-400 text-xs font-medium">{node.lastHeartbeat}</span>
                <span
                  className={`flex items-center gap-1 text-xs px-2 py-0.5 rounded-full border font-semibold ${
                    node.health === 'HEALTHY'
                      ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                      : 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-500/30'
                  }`}
                >
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  {node.health}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
