'use client';

import { motion } from 'framer-motion';
import { X, Crown, Shield, ShieldAlert, Heart, HeartOff, Clock, Terminal } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

interface NodeDetail {
  nodeId: string;
  role: 'LEADER' | 'FOLLOWER' | 'CANDIDATE';
  healthy: boolean;
  term: number;
  commitIndex: number;
  lastApplied: number;
  logLength: number;
  kvStoreSize: number;
  uptimeMs: number;
  nodeState: string;
}

interface NodePanelProps {
  node: NodeDetail | null;
  onClose: () => void;
}

function formatUptime(ms: number): string {
  if (ms <= 0) return '—';
  const seconds = Math.floor(ms / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}d ${hours % 24}h ${minutes % 60}m`;
  if (hours > 0) return `${hours}h ${minutes % 60}m`;
  if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
  return `${seconds}s`;
}

export function NodePanel({ node, onClose }: NodePanelProps) {
  if (!node) return null;

  const isLeader = node.role === 'LEADER';

  return (
    <div className="w-[340px] border-l border-white/[0.06] bg-[#0a0a0b] flex flex-col h-full shrink-0">
      {/* Header */}
      <div className="flex h-14 items-center justify-between px-5 border-b border-white/[0.06]">
        <div className="flex flex-col">
          <span className="text-[13px] font-semibold text-white/90">Node Inspector</span>
          <span className="text-[10px] font-mono text-white/30 truncate max-w-[200px]">
            {node.nodeId}
          </span>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={onClose}
          className="h-8 w-8 text-white/40 hover:text-white hover:bg-white/[0.04]"
        >
          <X className="h-4 w-4" />
        </Button>
      </div>

      {/* Body Content */}
      <div className="flex-1 overflow-y-auto p-5 space-y-6">
        {/* Status Indicator Hero */}
        <div className="rounded-xl border border-white/[0.06] bg-[#111113] p-4 flex items-center justify-between">
          <div className="space-y-1">
            <span className="text-[10px] font-semibold uppercase tracking-wider text-white/20">
              Connection
            </span>
            <div className="flex items-center gap-1.5">
              {node.healthy ? (
                <>
                  <Heart className="h-3.5 w-3.5 text-emerald-400" />
                  <span className="text-xs font-semibold text-emerald-400">ONLINE</span>
                </>
              ) : (
                <>
                  <HeartOff className="h-3.5 w-3.5 text-rose-400" />
                  <span className="text-xs font-semibold text-rose-400">OFFLINE</span>
                </>
              )}
            </div>
          </div>
          <div className="flex items-center">
            {isLeader ? (
              <Badge className="gap-1 bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[10px] font-medium py-1 px-2.5">
                <Crown className="h-3 w-3" />
                Leader
              </Badge>
            ) : (
              <Badge className="gap-1 bg-blue-500/10 text-blue-400 border border-blue-500/20 text-[10px] font-medium py-1 px-2.5">
                {node.role === 'FOLLOWER' ? (
                  <Shield className="h-3 w-3" />
                ) : (
                  <ShieldAlert className="h-3 w-3" />
                )}
                {node.role}
              </Badge>
            )}
          </div>
        </div>

        {/* Detailed Stats Block */}
        <div className="space-y-4">
          <h4 className="text-[11px] font-semibold uppercase tracking-wider text-white/35">
            Parameters
          </h4>
          <div className="rounded-xl border border-white/[0.06] bg-[#111113] divide-y divide-white/[0.04] text-xs">
            {/* Term */}
            <div className="flex items-center justify-between p-3.5">
              <span className="text-white/40">Current Term</span>
              <span className="font-mono text-white/70 font-medium">{node.term}</span>
            </div>

            {/* Commit Index */}
            <div className="flex items-center justify-between p-3.5">
              <span className="text-white/40">Commit Index</span>
              <span className="font-mono text-white/70 font-medium">{node.commitIndex}</span>
            </div>

            {/* Last Applied */}
            <div className="flex items-center justify-between p-3.5">
              <span className="text-white/40">Last Applied</span>
              <span className="font-mono text-white/70 font-medium">{node.lastApplied}</span>
            </div>

            {/* Log Length */}
            <div className="flex items-center justify-between p-3.5">
              <span className="text-white/40">Log Length</span>
              <span className="font-mono text-white/70 font-medium">{node.logLength}</span>
            </div>

            {/* KV Store Size */}
            <div className="flex items-center justify-between p-3.5">
              <span className="text-white/40">KV Store Size</span>
              <span className="font-mono text-white/70 font-medium">{node.kvStoreSize} keys</span>
            </div>

            {/* Uptime */}
            <div className="flex items-center justify-between p-3.5">
              <span className="text-white/40">Uptime</span>
              <div className="flex items-center gap-1.5 text-white/70">
                <Clock className="h-3.5 w-3.5 text-white/30" />
                <span>{formatUptime(node.uptimeMs)}</span>
              </div>
            </div>

            {/* Lifecycle State */}
            <div className="flex items-center justify-between p-3.5">
              <span className="text-white/40">Lifecycle State</span>
              <Badge className="bg-white/[0.04] text-white/50 border border-white/[0.08] hover:bg-white/[0.04] text-[9px] uppercase tracking-wider py-0.5 px-1.5">
                {node.nodeState}
              </Badge>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
