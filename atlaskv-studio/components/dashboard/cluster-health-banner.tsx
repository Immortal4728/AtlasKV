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
        'relative overflow-hidden rounded-xl border p-6',
        'backdrop-blur-xl',
        healthy
          ? 'border-emerald-500/15 bg-gradient-to-br from-emerald-500/[0.06] via-[oklch(0.12_0.008_280/50%)] to-cyan-500/[0.03]'
          : 'border-rose-500/15 bg-gradient-to-br from-rose-500/[0.06] via-[oklch(0.12_0.008_280/50%)] to-transparent'
      )}
    >
      {/* Animated gradient glow */}
      <div
        className={cn(
          'absolute -top-20 -right-20 h-40 w-40 rounded-full blur-3xl',
          healthy ? 'bg-emerald-500/15 animate-pulse-glow' : 'bg-rose-500/15 animate-pulse-glow'
        )}
      />
      <div
        className={cn(
          'absolute -bottom-16 -left-16 h-32 w-32 rounded-full blur-3xl',
          healthy ? 'bg-cyan-500/10' : 'bg-rose-400/8'
        )}
      />

      <div className="relative flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div
            className={cn(
              'relative flex h-12 w-12 items-center justify-center rounded-xl',
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
            {/* Pulse ring */}
            {healthy && (
              <span className="absolute inset-0 rounded-xl animate-glow-ring" />
            )}
          </div>
          <div>
            <div className="flex items-center gap-2.5">
              <h2 className="text-base font-semibold text-white/90">
                {healthy ? 'Cluster Healthy' : 'Cluster Unreachable'}
              </h2>
              <span
                className={cn(
                  'inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-semibold uppercase tracking-wider border',
                  healthy
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                    : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                )}
              >
                {nodeState}
              </span>
            </div>
            <p className="mt-0.5 text-[13px] text-[oklch(1_0_0/30%)]">
              Node <span className="font-mono text-[oklch(1_0_0/45%)]">{nodeId}</span>
              {leader && (
                <>
                  {' · Leader '}
                  <span className="font-mono text-[oklch(1_0_0/45%)]">{leader}</span>
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
