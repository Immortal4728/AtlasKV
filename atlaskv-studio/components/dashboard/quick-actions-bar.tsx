'use client';

import React from 'react';
import { useRouter } from 'next/navigation';
import {
  Plus,
  Clock,
  Camera,
  Vote,
  Download,
  Terminal,
  RefreshCw,
} from 'lucide-react';
import { toast } from 'sonner';
import { useTakeSnapshot } from '@/hooks/use-cluster';
import type { NodeDetail } from '@/types/api';

export function QuickActionsBar({
  nodes,
  leaderId,
  commitIndex,
  term,
  onForceElection,
}: {
  nodes?: NodeDetail[];
  leaderId?: string;
  commitIndex?: number;
  term?: number;
  onForceElection?: () => void;
}) {
  const router = useRouter();
  const snapshotMutation = useTakeSnapshot();

  const handleTriggerSnapshot = async () => {
    try {
      const res = await snapshotMutation.mutateAsync();
      toast.success('Raft WAL Snapshot Created', {
        description: `Compacted log entries into snapshot at index ${res.lastIncludedIndex} (Term ${res.lastIncludedTerm}).`,
      });
    } catch (err: any) {
      toast.error('Failed to trigger snapshot', {
        description: err?.response?.data?.message || err?.message || 'Unknown error',
      });
    }
  };

  const handleExportData = () => {
    const payload = {
      timestamp: new Date().toISOString(),
      cluster: {
        leaderId: leaderId || 'unknown',
        term: term || 0,
        commitIndex: commitIndex || 0,
        nodes: nodes || [],
      },
    };
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(payload, null, 2));
    const downloadAnchor = document.createElement('a');
    downloadAnchor.setAttribute('href', dataStr);
    downloadAnchor.setAttribute('download', `atlaskv-telemetry-${Date.now()}.json`);
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
    toast.success('Exported Telemetry Data');
  };

  return (
    <div className="glass-card rounded-2xl p-3.5 border border-[oklch(1_0_0/8%)] dark:border-[oklch(1_0_0/8%)] bg-[var(--surface-1)]">
      <div className="flex flex-wrap items-center justify-between gap-3">
        {/* Title */}
        <div className="flex items-center gap-2 px-1">
          <Terminal className="h-4 w-4 text-emerald-400" />
          <span className="text-xs font-semibold text-[var(--foreground)] tracking-tight font-mono">
            Quick Actions
          </span>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            id="quick-put-key-btn"
            onClick={() => {
              router.push('/keys');
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-emerald-500/12 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30 hover:bg-emerald-500/20 transition-all cursor-pointer shadow-sm"
          >
            <Plus className="h-3.5 w-3.5" />
            Put Key
          </button>

          <button
            id="quick-allocate-lease-btn"
            onClick={() => {
              router.push('/leases');
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-cyan-500/12 text-cyan-600 dark:text-cyan-400 border border-cyan-500/30 hover:bg-cyan-500/20 transition-all cursor-pointer shadow-sm"
          >
            <Clock className="h-3.5 w-3.5" />
            Allocate Lease
          </button>

          <button
            id="quick-trigger-snapshot-btn"
            onClick={handleTriggerSnapshot}
            disabled={snapshotMutation.isPending}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-purple-500/12 text-purple-600 dark:text-purple-400 border border-purple-500/30 hover:bg-purple-500/20 transition-all cursor-pointer shadow-sm disabled:opacity-50"
          >
            {snapshotMutation.isPending ? (
              <RefreshCw className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <Camera className="h-3.5 w-3.5" />
            )}
            Trigger Snapshot
          </button>

          <button
            id="quick-view-cluster-btn"
            onClick={() => {
              router.push('/cluster');
            }}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-amber-500/12 text-amber-600 dark:text-amber-400 border border-amber-500/30 hover:bg-amber-500/20 transition-all cursor-pointer shadow-sm"
          >
            <Vote className="h-3.5 w-3.5" />
            Topology & Nodes
          </button>

          <button
            id="quick-export-data-btn"
            onClick={handleExportData}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-neutral-100 dark:bg-[oklch(1_0_0/5%)] text-[var(--foreground)] border border-border dark:border-[oklch(1_0_0/8%)] hover:bg-neutral-200 dark:hover:bg-[oklch(1_0_0/10%)] transition-all cursor-pointer shadow-sm"
          >
            <Download className="h-3.5 w-3.5" />
            Export Data
          </button>
        </div>
      </div>
    </div>
  );
}
