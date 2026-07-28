'use client';

import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { Users, GitBranch, Crown, Server } from 'lucide-react';

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
      <div className="glass-card rounded-xl p-5">
        <div className="space-y-3">
          <div className="skeleton h-3 w-24 rounded" />
          <div className="flex gap-3">
            <div className="skeleton h-16 w-32 rounded-lg" />
            <div className="skeleton h-16 w-32 rounded-lg" />
            <div className="skeleton h-16 w-32 rounded-lg" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] as const, delay: 0.15 }}
      className="glass-card rounded-xl p-5"
    >
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Users className="h-4 w-4 text-[oklch(1_0_0/25%)]" strokeWidth={1.8} />
          <h3 className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[oklch(1_0_0/28%)]">
            Cluster Members
          </h3>
        </div>
        {isJoint && (
          <div className="flex items-center gap-1.5 rounded-lg bg-amber-500/10 px-2.5 py-1 border border-amber-500/15">
            <GitBranch className="h-3 w-3 text-amber-400" />
            <span className="text-[10px] font-semibold text-amber-400 uppercase tracking-wider">
              Joint Consensus
            </span>
          </div>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        {members.map((member, idx) => {
          const isLeader = member === leader;

          return (
            <motion.div
              key={member}
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.3, delay: idx * 0.08 }}
              whileHover={{ scale: 1.03, transition: { duration: 0.15 } }}
              className={cn(
                'relative flex items-center gap-3 rounded-xl border px-4 py-3 transition-all duration-200 cursor-default',
                isLeader
                  ? 'border-emerald-500/20 bg-emerald-500/[0.06] shadow-[0_0_16px_oklch(0.72_0.19_160/6%)]'
                  : 'border-[oklch(1_0_0/6%)] bg-[oklch(1_0_0/2%)] hover:border-[oklch(1_0_0/10%)] hover:bg-[oklch(1_0_0/3%)]'
              )}
            >
              <div
                className={cn(
                  'flex h-8 w-8 items-center justify-center rounded-lg border',
                  isLeader
                    ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
                    : 'bg-[oklch(1_0_0/4%)] border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/40%)]'
                )}
              >
                {isLeader ? (
                  <Crown className="h-3.5 w-3.5" />
                ) : (
                  <Server className="h-3.5 w-3.5" />
                )}
              </div>

              <div>
                <div className="flex items-center gap-2">
                  <span
                    className={cn(
                      'text-xs font-mono font-medium',
                      isLeader ? 'text-emerald-400/90' : 'text-[oklch(1_0_0/55%)]'
                    )}
                  >
                    {member}
                  </span>
                  {isLeader && (
                    <span className="text-[9px] uppercase tracking-wider text-emerald-400/50 font-semibold bg-emerald-500/8 px-1.5 py-0.5 rounded">
                      Leader
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-1.5 mt-0.5">
                  <span
                    className={cn(
                      'h-1.5 w-1.5 rounded-full',
                      isLeader
                        ? 'bg-emerald-400 shadow-[0_0_6px_oklch(0.72_0.19_160/60%)]'
                        : 'bg-[oklch(1_0_0/20%)]'
                    )}
                  />
                  <span className="text-[10px] text-[oklch(1_0_0/25%)] font-mono">
                    {isLeader ? 'Active' : 'Following'}
                  </span>
                </div>
              </div>
            </motion.div>
          );
        })}
      </div>
    </motion.div>
  );
}
