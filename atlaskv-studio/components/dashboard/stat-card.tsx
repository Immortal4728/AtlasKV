'use client';

import { cn } from '@/lib/utils';
import { type LucideIcon } from 'lucide-react';
import { motion } from 'framer-motion';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: 'up' | 'down' | 'neutral';
  accentColor?: 'emerald' | 'blue' | 'amber' | 'purple' | 'rose' | 'cyan';
  loading?: boolean;
}

const accentStyles = {
  emerald: {
    iconBg: 'bg-emerald-500/10',
    iconText: 'text-emerald-400',
    glow: 'shadow-emerald-500/5',
  },
  blue: {
    iconBg: 'bg-blue-500/10',
    iconText: 'text-blue-400',
    glow: 'shadow-blue-500/5',
  },
  amber: {
    iconBg: 'bg-amber-500/10',
    iconText: 'text-amber-400',
    glow: 'shadow-amber-500/5',
  },
  purple: {
    iconBg: 'bg-purple-500/10',
    iconText: 'text-purple-400',
    glow: 'shadow-purple-500/5',
  },
  rose: {
    iconBg: 'bg-rose-500/10',
    iconText: 'text-rose-400',
    glow: 'shadow-rose-500/5',
  },
  cyan: {
    iconBg: 'bg-cyan-500/10',
    iconText: 'text-cyan-400',
    glow: 'shadow-cyan-500/5',
  },
};

export function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  accentColor = 'emerald',
  loading = false,
}: StatCardProps) {
  const accent = accentStyles[accentColor];

  if (loading) {
    return (
      <div className="rounded-xl border border-white/[0.06] bg-[#111113] p-5 animate-pulse">
        <div className="flex items-start justify-between">
          <div className="space-y-3">
            <div className="h-3 w-20 rounded bg-white/[0.06]" />
            <div className="h-7 w-16 rounded bg-white/[0.06]" />
          </div>
          <div className="h-9 w-9 rounded-lg bg-white/[0.04]" />
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
      whileHover={{ y: -1, transition: { duration: 0.15 } }}
      className={cn(
        'group relative rounded-xl border border-white/[0.06] bg-[#111113] p-5 transition-colors duration-200',
        'hover:border-white/[0.1] hover:bg-[#141416]'
      )}
    >
      <div className="flex items-start justify-between">
        <div className="space-y-1.5">
          <p className="text-[12px] font-medium uppercase tracking-wider text-white/30">
            {title}
          </p>
          <p className="text-2xl font-semibold tracking-tight text-white/90">
            {value}
          </p>
          {subtitle && (
            <p className="text-[11px] text-white/25">{subtitle}</p>
          )}
        </div>
        <div
          className={cn(
            'flex h-9 w-9 items-center justify-center rounded-lg transition-all duration-200',
            accent.iconBg,
            accent.glow
          )}
        >
          <Icon className={cn('h-4.5 w-4.5', accent.iconText)} strokeWidth={1.8} />
        </div>
      </div>
    </motion.div>
  );
}
