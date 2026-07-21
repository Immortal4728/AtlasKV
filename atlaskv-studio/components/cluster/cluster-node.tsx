'use client';

import { Handle, Position } from '@xyflow/react';
import { cn } from '@/lib/utils';
import { Shield, ShieldAlert, Crown, Heart, HeartOff } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

interface NodeData {
  nodeId: string;
  role: 'LEADER' | 'FOLLOWER' | 'CANDIDATE';
  healthy: boolean;
  term: number;
  commitIndex: number;
  selected?: boolean;
}

export function ClusterNode({ data }: { data: NodeData }) {
  const isLeader = data.role === 'LEADER';

  return (
    <div
      className={cn(
        'relative w-[220px] rounded-xl border bg-[#111113] p-4 transition-all duration-200 select-none',
        data.selected
          ? 'border-emerald-500 shadow-[0_0_15px_rgba(16,185,129,0.15)] ring-1 ring-emerald-500/30'
          : 'border-white/[0.06] hover:border-white/[0.12] hover:bg-[#141416]'
      )}
    >
      {/* Node status / Handles */}
      <Handle
        type="target"
        position={Position.Top}
        className="!bg-emerald-500/60 !w-2 !h-2"
      />
      <Handle
        type="source"
        position={Position.Bottom}
        className="!bg-emerald-500/60 !w-2 !h-2"
      />

      <div className="space-y-3">
        {/* Node Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 min-w-0">
            <span className="text-xs font-mono font-medium truncate text-white/80">
              {data.nodeId}
            </span>
          </div>
          {data.healthy ? (
            <Heart className="h-3.5 w-3.5 text-emerald-400 shrink-0" />
          ) : (
            <HeartOff className="h-3.5 w-3.5 text-rose-400 shrink-0" />
          )}
        </div>

        {/* Node Role Badge */}
        <div className="flex items-center">
          {isLeader ? (
            <Badge className="gap-1 bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[10px] font-medium py-0.5 px-2">
              <Crown className="h-2.5 w-2.5" />
              Leader
            </Badge>
          ) : (
            <Badge className="gap-1 bg-blue-500/10 text-blue-400 border border-blue-500/20 text-[10px] font-medium py-0.5 px-2">
              {data.role === 'FOLLOWER' ? (
                <Shield className="h-2.5 w-2.5" />
              ) : (
                <ShieldAlert className="h-2.5 w-2.5" />
              )}
              {data.role}
            </Badge>
          )}
        </div>

        {/* Term / Commit info */}
        <div className="grid grid-cols-2 gap-2 pt-2 border-t border-white/[0.04] text-[11px]">
          <div>
            <div className="text-white/20 uppercase tracking-wider">Term</div>
            <div className="font-mono text-white/70 mt-0.5">{data.term}</div>
          </div>
          <div>
            <div className="text-white/20 uppercase tracking-wider">Commit</div>
            <div className="font-mono text-white/70 mt-0.5">{data.commitIndex}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
