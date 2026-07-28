'use client';

import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';

interface GlassCardProps {
  children: React.ReactNode;
  className?: string;
  glowColor?: 'emerald' | 'cyan' | 'purple' | 'amber' | 'rose' | 'blue' | 'none';
  hover?: boolean;
  padding?: 'sm' | 'md' | 'lg';
}

const glowMap = {
  emerald: 'hover:shadow-[0_0_30px_oklch(0.72_0.19_160/8%)]',
  cyan: 'hover:shadow-[0_0_30px_oklch(0.72_0.15_200/8%)]',
  purple: 'hover:shadow-[0_0_30px_oklch(0.65_0.2_280/8%)]',
  amber: 'hover:shadow-[0_0_30px_oklch(0.7_0.15_70/8%)]',
  rose: 'hover:shadow-[0_0_30px_oklch(0.7_0.18_20/8%)]',
  blue: 'hover:shadow-[0_0_30px_oklch(0.6_0.2_250/8%)]',
  none: '',
};

const paddingMap = {
  sm: 'p-4',
  md: 'p-5',
  lg: 'p-6',
};

export function GlassCard({
  children,
  className,
  glowColor = 'emerald',
  hover = true,
  padding = 'md',
}: GlassCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: [0.25, 0.46, 0.45, 0.94] as const }}
      whileHover={hover ? { y: -2, transition: { duration: 0.2 } } : undefined}
      className={cn(
        'relative rounded-xl',
        'bg-[oklch(0.12_0.008_280/50%)]',
        'border border-[oklch(1_0_0/6%)]',
        'backdrop-blur-xl',
        'shadow-[0_4px_24px_oklch(0_0_0/25%),inset_0_1px_0_oklch(1_0_0/4%)]',
        'transition-all duration-300',
        hover && 'hover:border-[oklch(1_0_0/10%)] hover:bg-[oklch(0.14_0.008_280/55%)]',
        hover && glowMap[glowColor],
        paddingMap[padding],
        className
      )}
    >
      {children}
    </motion.div>
  );
}
