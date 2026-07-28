'use client';

import { motion } from 'framer-motion';
import { type LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';

interface PageHeaderProps {
  title: string;
  description: string;
  icon?: LucideIcon;
  iconColor?: string;
  actions?: React.ReactNode;
  badge?: React.ReactNode;
}

export function PageHeader({
  title,
  description,
  icon: Icon,
  iconColor = 'text-emerald-400',
  actions,
  badge,
}: PageHeaderProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] as const }}
      className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
    >
      <div className="space-y-1">
        <div className="flex items-center gap-3">
          {Icon && (
            <div className={cn(
              'flex h-9 w-9 items-center justify-center rounded-lg',
              'bg-gradient-to-br from-[oklch(1_0_0/6%)] to-[oklch(1_0_0/2%)]',
              'border border-[oklch(1_0_0/8%)]',
              'shadow-sm'
            )}>
              <Icon className={cn('h-4.5 w-4.5', iconColor)} strokeWidth={1.8} />
            </div>
          )}
          <div>
            <h1 className="text-lg font-semibold tracking-tight gradient-text flex items-center gap-2.5">
              {title}
              {badge}
            </h1>
            <p className="text-xs text-[oklch(1_0_0/35%)] mt-0.5 max-w-lg">
              {description}
            </p>
          </div>
        </div>
      </div>
      {actions && (
        <motion.div
          initial={{ opacity: 0, x: 10 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
          className="flex items-center gap-2"
        >
          {actions}
        </motion.div>
      )}
    </motion.div>
  );
}
