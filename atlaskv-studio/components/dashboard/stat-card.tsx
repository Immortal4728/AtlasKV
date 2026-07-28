'use client';

import { cn } from '@/lib/utils';
import { type LucideIcon } from 'lucide-react';
import { motion } from 'framer-motion';
import { AnimatedCounter } from '@/components/ui/animated-counter';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: 'up' | 'down' | 'neutral';
  accentColor?: 'emerald' | 'blue' | 'amber' | 'purple' | 'rose' | 'cyan';
  loading?: boolean;
  delay?: number;
}

const accentStyles = {
  emerald: {
    iconBg: 'bg-emerald-500/10',
    iconText: 'text-emerald-400',
    glow: 'group-hover:shadow-[0_0_24px_oklch(0.72_0.19_160/10%)]',
    borderHover: 'group-hover:border-emerald-500/15',
    gradient: 'from-emerald-500/10 via-transparent to-transparent',
  },
  blue: {
    iconBg: 'bg-blue-500/10',
    iconText: 'text-blue-400',
    glow: 'group-hover:shadow-[0_0_24px_oklch(0.6_0.2_250/10%)]',
    borderHover: 'group-hover:border-blue-500/15',
    gradient: 'from-blue-500/10 via-transparent to-transparent',
  },
  amber: {
    iconBg: 'bg-amber-500/10',
    iconText: 'text-amber-400',
    glow: 'group-hover:shadow-[0_0_24px_oklch(0.7_0.15_70/10%)]',
    borderHover: 'group-hover:border-amber-500/15',
    gradient: 'from-amber-500/10 via-transparent to-transparent',
  },
  purple: {
    iconBg: 'bg-purple-500/10',
    iconText: 'text-purple-400',
    glow: 'group-hover:shadow-[0_0_24px_oklch(0.65_0.2_280/10%)]',
    borderHover: 'group-hover:border-purple-500/15',
    gradient: 'from-purple-500/10 via-transparent to-transparent',
  },
  rose: {
    iconBg: 'bg-rose-500/10',
    iconText: 'text-rose-400',
    glow: 'group-hover:shadow-[0_0_24px_oklch(0.7_0.18_20/10%)]',
    borderHover: 'group-hover:border-rose-500/15',
    gradient: 'from-rose-500/10 via-transparent to-transparent',
  },
  cyan: {
    iconBg: 'bg-cyan-500/10',
    iconText: 'text-cyan-400',
    glow: 'group-hover:shadow-[0_0_24px_oklch(0.72_0.15_200/10%)]',
    borderHover: 'group-hover:border-cyan-500/15',
    gradient: 'from-cyan-500/10 via-transparent to-transparent',
  },
};

export function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  accentColor = 'emerald',
  loading = false,
  delay = 0,
}: StatCardProps) {
  const accent = accentStyles[accentColor];

  if (loading) {
    return (
      <div className="glass-card rounded-xl p-5">
        <div className="flex items-start justify-between">
          <div className="space-y-3">
            <div className="skeleton h-3 w-20 rounded" />
            <div className="skeleton h-7 w-16 rounded" />
          </div>
          <div className="skeleton h-9 w-9 rounded-lg" />
        </div>
        <div className="skeleton h-2 w-24 rounded mt-3" />
      </div>
    );
  }

  const numericValue = typeof value === 'number' ? value : parseFloat(String(value).replace(/,/g, ''));
  const isNumeric = !isNaN(numericValue);

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] as const, delay: delay * 0.05 }}
      whileHover={{ y: -3, transition: { duration: 0.2 } }}
      className={cn(
        'group relative rounded-2xl overflow-hidden',
        'bg-[var(--surface-1)]',
        'border border-[oklch(1_0_0/8%)] dark:border-[oklch(1_0_0/8%)]',
        'backdrop-blur-xl shadow-sm',
        'p-5 transition-all duration-300',
        accent.borderHover,
        accent.glow
      )}
    >
      {/* Hover gradient overlay */}
      <div className={cn(
        'absolute inset-0 bg-gradient-to-br opacity-0 group-hover:opacity-100 transition-opacity duration-500',
        accent.gradient
      )} />

      <div className="relative flex items-start justify-between">
        <div className="space-y-1.5">
          <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-neutral-400 dark:text-neutral-500">
            {title}
          </p>
          <p className="text-2xl font-bold tracking-tight text-[var(--foreground)]">
            {isNumeric ? (
              <AnimatedCounter value={numericValue} />
            ) : (
              value
            )}
          </p>
          {subtitle && (
            <p className="text-[10px] text-neutral-400 font-medium">{subtitle}</p>
          )}
        </div>
        <div
          className={cn(
            'flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-300',
            accent.iconBg,
            'group-hover:scale-110'
          )}
        >
          <Icon className={cn('h-5 w-5', accent.iconText)} strokeWidth={1.8} />
        </div>
      </div>
    </motion.div>
  );
}
