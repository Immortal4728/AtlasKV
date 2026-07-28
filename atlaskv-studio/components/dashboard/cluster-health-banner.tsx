'use client';

import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { Shield, ShieldAlert, Crown, Heart, HeartOff, Activity } from 'lucide-react';

interface ClusterHealthBannerProps {
  healthy: boolean;
  role: string;
  leader: string | null;
  nodeId: string;
  nodeState: string;
  loading?: boolean;
}

export function ClusterHealthBanner({
  healthy,
  role,
  leader,
  nodeId,
  nodeState,
  loading = false,
}: ClusterHealthBannerProps) {
  if (loading) {
    return (
      <div className="glass-card rounded-xl p-6">
        <div className="flex items-center gap-4">
          <div className="skeleton h-12 w-12 rounded-xl" />
          <div className="space-y-2">
            <div className="skeleton h-5 w-48 rounded" />
            <div className="skeleton h-3 w-72 rounded" />
          </div>
        </div>
      </div>
    );
  }

  const isLeader = role === 'LEADER';

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: [0.25, 0.46, 0.45, 0.94] as const }}
      className={cn(
        'relative overflow-hidden rounded-2xl border p-5',
        'backdrop-blur-xl bg-[var(--surface-1)]',
        healthy
          ? 'border-emerald-500/20 shadow-sm'
          : 'border-rose-500/20 shadow-sm'
      )}
    >
      {/* Animated gradient glow */}
      <div
        className={cn(
          'absolute -top-20 -right-20 h-40 w-40 rounded-full blur-3xl',
          healthy ? 'bg-emerald-500/10 animate-pulse-glow' : 'bg-rose-500/10 animate-pulse-glow'
        )}
      />

      <div className="relative flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div
            className={cn(
              'relative flex h-11 w-11 items-center justify-center rounded-xl',
              healthy
                ? 'bg-emerald-500/10 shadow-lg shadow-emerald-500/10'
                : 'bg-rose-500/10 shadow-lg shadow-rose-500/10'
            )}
          >
            {healthy ? (
              <Heart className="h-5 w-5 text-emerald-400 animate-heartbeat" strokeWidth={1.8} />
            ) : (
              <HeartOff className="h-5 w-5 text-rose-400" strokeWidth={1.8} />
            )}
          </div>
          <div>
            <div className="flex items-center gap-2.5">
              <h2 className="text-base font-semibold text-[var(--foreground)]">
                {healthy ? 'Cluster Healthy' : 'Cluster Unreachable'}
              </h2>
              <span
                className={cn(
                  'inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-semibold uppercase tracking-wider border',
                  healthy
                    ? 'bg-emerald-500/10 text-emerald-500 dark:text-emerald-400 border-emerald-500/20'
                    : 'bg-rose-500/10 text-rose-500 dark:text-rose-400 border-rose-500/20'
                )}
              >
                {nodeState}
              </span>
            </div>
            <p className="mt-0.5 text-[12px] text-neutral-400 font-mono">
              Node <span className="font-mono text-[var(--foreground)] font-bold">{nodeId}</span>
              {leader && (
                <>
                  {' · Leader '}
                  <span className="font-mono text-emerald-500 dark:text-emerald-400 font-bold">{leader}</span>
                </>
              )}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {isLeader ? (
            <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[11px] font-medium">
              <Crown className="h-3 w-3" />
              Leader
            </span>
          ) : (
            <span className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20 text-[11px] font-medium">
              {role === 'FOLLOWER' ? (
                <Shield className="h-3 w-3" />
              ) : (
                <ShieldAlert className="h-3 w-3" />
              )}
              {role}
            </span>
          )}
        </div>
      </div>
    </motion.div>
  );
}
