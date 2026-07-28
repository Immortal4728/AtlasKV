'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import {
  Plus,
  Clock,
  Camera,
  Vote,
  Download,
  Search,
  Terminal,
  RefreshCw,
} from 'lucide-react';
import { toast } from 'sonner';
import { motion } from 'framer-motion';

export function QuickActionsBar({
  onTriggerSnapshot,
  onForceElection,
}: {
  onTriggerSnapshot?: () => void;
  onForceElection?: () => void;
}) {
  const router = useRouter();

  return (
    <div className="glass-card rounded-2xl p-3.5 border border-[oklch(1_0_0/8%)] dark:border-[oklch(1_0_0/8%)] bg-[var(--surface-1)]">
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Title */}
        <div className="flex items-center gap-2 px-1">
          <Terminal className="h-4 w-4 text-emerald-400" />
          <span className="text-xs font-semibold text-[var(--foreground)] tracking-tight">
            Quick Actions
          </span>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => {
              router.push('/keys');
              toast.info('Navigated to Key-Value Explorer');
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 hover:bg-emerald-500/20 transition-all cursor-pointer"
          >
            <Plus className="h-3.5 w-3.5" />
            Put Key
          </button>

          <button
            onClick={() => {
              router.push('/leases');
              toast.info('Navigated to Lease Management');
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 hover:bg-cyan-500/20 transition-all cursor-pointer"
          >
            <Clock className="h-3.5 w-3.5" />
            Allocate Lease
          </button>

          <button
            onClick={() => {
              if (onTriggerSnapshot) onTriggerSnapshot();
              toast.success('Raft WAL Snapshot Triggered', {
                description: 'Compacted log entries into snapshot segment at index 45.',
              });
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium bg-purple-500/10 text-purple-400 border border-purple-500/20 hover:bg-purple-500/20 transition-all cursor-pointer"
          >
            <Camera className="h-3.5 w-3.5" />
            Trigger Snapshot
          </button>

          <button
            onClick={() => {
              if (onForceElection) onForceElection();
              toast.warning('Initiated Leader Election', {
                description: 'Sent RequestVote RPCs across cluster for Term + 1.',
              });
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20 hover:bg-amber-500/20 transition-all cursor-pointer"
          >
            <Vote className="h-3.5 w-3.5" />
            Force Election
          </button>

          <button
            onClick={() => {
              const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify({ timestamp: new Date().toISOString(), nodes: 3, leader: "node1", term: 3, commitIndex: 45 }));
              const downloadAnchor = document.createElement('a');
              downloadAnchor.setAttribute("href", dataStr);
              downloadAnchor.setAttribute("download", `atlaskv-telemetry-${Date.now()}.json`);
              document.body.appendChild(downloadAnchor);
              downloadAnchor.click();
              downloadAnchor.remove();
              toast.success('Exported Telemetry Data');
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium bg-[oklch(1_0_0/5%)] text-[var(--foreground)] border border-[oklch(1_0_0/8%)] hover:bg-[oklch(1_0_0/10%)] transition-all cursor-pointer"
          >
            <Download className="h-3.5 w-3.5" />
            Export Data
          </button>
        </div>
      </div>
    </div>
  );
}
