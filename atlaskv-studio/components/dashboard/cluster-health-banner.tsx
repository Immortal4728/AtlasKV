'use client';

import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { Shield, ShieldAlert, Crown, Heart, HeartOff } from 'lucide-react';

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
      <div className="rounded-xl border border-white/[0.06] bg-[#111113] p-6 animate-pulse">
        <div className="flex items-center gap-4">
          <div className="h-12 w-12 rounded-xl bg-white/[0.04]" />
          <div className="space-y-2">
            <div className="h-5 w-48 rounded bg-white/[0.06]" />
            <div className="h-3 w-72 rounded bg-white/[0.04]" />
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
      transition={{ duration: 0.4, ease: 'easeOut' }}
      className={cn(
        'relative overflow-hidden rounded-xl border p-6',
        healthy
          ? 'border-emerald-500/20 bg-gradient-to-br from-emerald-500/[0.04] to-transparent'
          : 'border-rose-500/20 bg-gradient-to-br from-rose-500/[0.04] to-transparent'
      )}
    >
      {/* Subtle glow */}
      <div
        className={cn(
          'absolute -top-24 -right-24 h-48 w-48 rounded-full blur-3xl opacity-20',
          healthy ? 'bg-emerald-500' : 'bg-rose-500'
        )}
      />

      <div className="relative flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div
            className={cn(
              'flex h-12 w-12 items-center justify-center rounded-xl',
              healthy
                ? 'bg-emerald-500/10 shadow-lg shadow-emerald-500/10'
                : 'bg-rose-500/10 shadow-lg shadow-rose-500/10'
            )}
          >
            {healthy ? (
              <Heart className="h-5.5 w-5.5 text-emerald-400" strokeWidth={1.8} />
            ) : (
              <HeartOff className="h-5.5 w-5.5 text-rose-400" strokeWidth={1.8} />
            )}
          </div>
          <div>
            <div className="flex items-center gap-2.5">
              <h2 className="text-lg font-semibold text-white/90">
                {healthy ? 'Cluster Healthy' : 'Cluster Unreachable'}
              </h2>
              <Badge
                className={cn(
                  'text-[10px] font-semibold uppercase tracking-wider',
                  healthy
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                    : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                )}
              >
                {nodeState}
              </Badge>
            </div>
            <p className="mt-0.5 text-sm text-white/35">
              Node <span className="font-mono text-white/50">{nodeId}</span>
              {leader && (
                <>
                  {' · Leader '}
                  <span className="font-mono text-white/50">{leader}</span>
                </>
              )}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {isLeader ? (
            <Badge className="gap-1.5 bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[11px] font-medium">
              <Crown className="h-3 w-3" />
              Leader
            </Badge>
          ) : (
            <Badge className="gap-1.5 bg-blue-500/10 text-blue-400 border border-blue-500/20 text-[11px] font-medium">
              {role === 'FOLLOWER' ? (
                <Shield className="h-3 w-3" />
              ) : (
                <ShieldAlert className="h-3 w-3" />
              )}
              {role}
            </Badge>
          )}
        </div>
      </div>
    </motion.div>
  );
}
