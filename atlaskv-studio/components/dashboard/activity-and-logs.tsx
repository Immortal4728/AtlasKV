'use client';

import React, { useState } from 'react';
import { ScrollText, Activity, AlertCircle, ShieldCheck, Zap, Vote, Camera, UserPlus } from 'lucide-react';

interface EventItem {
  id: string;
  type: 'LEADER_ELECTED' | 'APPEND_ENTRIES' | 'SNAPSHOT_CREATED' | 'MEMBER_JOINED' | 'LEASE_GRANTED';
  timestamp: string;
  message: string;
  details: string;
}

const SAMPLE_EVENTS: EventItem[] = [
  {
    id: 'evt-1',
    type: 'LEADER_ELECTED',
    timestamp: '10:42:15 AM',
    message: 'Node 1 elected Leader for Term 3',
    details: 'Received 2/3 votes from node2 and node3.',
  },
  {
    id: 'evt-2',
    type: 'APPEND_ENTRIES',
    timestamp: '10:42:18 AM',
    message: 'Log Index #45 committed across quorum',
    details: 'Key "user:profile:102" written successfully in 0.42ms.',
  },
  {
    id: 'evt-3',
    type: 'SNAPSHOT_CREATED',
    timestamp: '10:38:00 AM',
    message: 'Periodic Raft snapshot created',
    details: 'Compacted 10,000 log entries down to 1.2 MB snapshot.',
  },
  {
    id: 'evt-4',
    type: 'LEASE_GRANTED',
    timestamp: '10:35:12 AM',
    message: 'Distributed lease granted to client-session-99',
    details: 'TTL 30,000ms attached to key "lock:leader-election".',
  },
  {
    id: 'evt-5',
    type: 'MEMBER_JOINED',
    timestamp: '10:20:00 AM',
    message: 'Node 3 joined cluster via Joint Consensus',
    details: 'Configuration changed cold -> C(old,new) -> Cnew.',
  },
];

export function ActivityAndLogs() {
  const [activeTab, setActiveTab] = useState<'events' | 'logs'>('events');

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      {/* Left: Activity & Audit Feed */}
      <div className="glass-card rounded-2xl p-5 border border-[oklch(1_0_0/8%)] bg-[var(--surface-1)]">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Activity className="h-4 w-4 text-emerald-400" />
            <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] font-mono">
              Live Activity & Cluster Audit Stream
            </h3>
          </div>
          <span className="flex items-center gap-1.5 text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
            Streaming
          </span>
        </div>

        <div className="space-y-2.5 max-h-[320px] overflow-y-auto pr-1">
          {SAMPLE_EVENTS.map((evt) => {
            let badgeStyle = 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
            let Icon = ShieldCheck;

            if (evt.type === 'LEADER_ELECTED') {
              badgeStyle = 'bg-purple-500/10 text-purple-400 border-purple-500/20';
              Icon = Vote;
            } else if (evt.type === 'SNAPSHOT_CREATED') {
              badgeStyle = 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20';
              Icon = Camera;
            } else if (evt.type === 'MEMBER_JOINED') {
              badgeStyle = 'bg-amber-500/10 text-amber-400 border-amber-500/20';
              Icon = UserPlus;
            }

            return (
              <div
                key={evt.id}
                className="flex items-start gap-3 p-3 rounded-xl border border-[oklch(1_0_0/5%)] bg-[var(--surface-0)] hover:border-emerald-500/20 transition-all text-xs"
              >
                <div className={`p-1.5 rounded-lg border ${badgeStyle} shrink-0 mt-0.5`}>
                  <Icon className="h-3.5 w-3.5" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-semibold text-[var(--foreground)] truncate">{evt.message}</span>
                    <span className="text-[10px] font-mono text-neutral-400 shrink-0">{evt.timestamp}</span>
                  </div>
                  <p className="text-[11px] text-neutral-400 mt-0.5 leading-relaxed">{evt.details}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Right: Raft Logs Terminal & Election History */}
      <div className="glass-card rounded-2xl p-5 border border-[oklch(1_0_0/8%)] bg-[var(--surface-1)]">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <ScrollText className="h-4 w-4 text-cyan-400" />
            <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--foreground)] font-mono">
              Raft Consensus Terminal Tail
            </h3>
          </div>
          <span className="text-[10px] font-mono text-neutral-400 bg-[oklch(1_0_0/4%)] px-2 py-0.5 rounded-md border border-[oklch(1_0_0/8%)]">
            Tail -f 100
          </span>
        </div>

        <div className="rounded-xl p-3.5 bg-[#050505] border border-white/10 font-mono text-[11px] leading-relaxed text-neutral-300 max-h-[320px] overflow-y-auto space-y-1.5 selection:bg-emerald-500/30">
          <div className="text-emerald-400 font-semibold">[INFO] 10:42:15.102 [RaftNode-1] Transitioned to LEADER for Term 3.</div>
          <div className="text-neutral-400">[DEBUG] 10:42:15.105 [RaftNode-1] Sent initial heartbeat AppendEntries to peers (node2, node3).</div>
          <div className="text-emerald-400/80">[INFO] 10:42:15.110 [RaftNode-2] Received AppendEntries from Leader node1 (Term 3, PrevIndex 44).</div>
          <div className="text-cyan-400">[INFO] 10:42:16.420 [gRPC-Server] Client PUT request received for key "user:profile:102".</div>
          <div className="text-neutral-300">[DEBUG] 10:42:16.422 [WAL] Appending Entry index 45 to segment file segment-003.wal (fsync ok).</div>
          <div className="text-purple-400">[INFO] 10:42:16.425 [RaftNode-1] Replicated index 45 to node2 and node3 (ACK count = 3/3).</div>
          <div className="text-emerald-400 font-semibold">[INFO] 10:42:16.426 [StateMachine] Applied index 45 to KV Memory Store.</div>
          <div className="text-neutral-400">[DEBUG] 10:42:17.000 [ReadIndex] Linearizable read index check passed via heartbeat quorum verification.</div>
        </div>
      </div>
    </div>
  );
}
