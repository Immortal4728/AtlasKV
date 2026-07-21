'use client';

import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { Users, GitBranch } from 'lucide-react';

interface MembersBannerProps {
  members: string[];
  isJoint: boolean;
  leader: string | null;
  loading?: boolean;
}

export function MembersBanner({
  members,
  isJoint,
  leader,
  loading = false,
}: MembersBannerProps) {
  if (loading) {
    return (
      <div className="rounded-xl border border-white/[0.06] bg-[#111113] p-5 animate-pulse">
        <div className="space-y-3">
          <div className="h-3 w-24 rounded bg-white/[0.06]" />
          <div className="flex gap-2">
            <div className="h-8 w-20 rounded-lg bg-white/[0.04]" />
            <div className="h-8 w-20 rounded-lg bg-white/[0.04]" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: 'easeOut', delay: 0.15 }}
      className="rounded-xl border border-white/[0.06] bg-[#111113] p-5"
    >
      <div className="flex items-center justify-between mb-3.5">
        <div className="flex items-center gap-2">
          <Users className="h-4 w-4 text-white/30" strokeWidth={1.8} />
          <h3 className="text-[12px] font-medium uppercase tracking-wider text-white/30">
            Cluster Members
          </h3>
        </div>
        {isJoint && (
          <div className="flex items-center gap-1.5 rounded-md bg-amber-500/10 px-2 py-1 border border-amber-500/20">
            <GitBranch className="h-3 w-3 text-amber-400" />
            <span className="text-[10px] font-semibold text-amber-400 uppercase tracking-wider">
              Joint Consensus
            </span>
          </div>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        {members.map((member) => (
          <div
            key={member}
            className={cn(
              'flex items-center gap-2 rounded-lg border px-3 py-1.5 transition-colors',
              member === leader
                ? 'border-emerald-500/20 bg-emerald-500/[0.06]'
                : 'border-white/[0.06] bg-white/[0.02]'
            )}
          >
            <div
              className={cn(
                'h-2 w-2 rounded-full',
                member === leader
                  ? 'bg-emerald-500 shadow-sm shadow-emerald-500/50'
                  : 'bg-white/20'
              )}
            />
            <span
              className={cn(
                'text-xs font-mono',
                member === leader ? 'text-emerald-400/80' : 'text-white/50'
              )}
            >
              {member}
            </span>
            {member === leader && (
              <span className="text-[9px] uppercase tracking-wider text-emerald-400/50 font-semibold">
                Leader
              </span>
            )}
          </div>
        ))}
      </div>
    </motion.div>
  );
}
