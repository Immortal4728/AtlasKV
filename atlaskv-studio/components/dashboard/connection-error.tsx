'use client';

import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { AlertCircle, RefreshCw, WifiOff } from 'lucide-react';

interface ConnectionErrorProps {
  message?: string;
  onRetry?: () => void;
}

export function ConnectionError({ message, onRetry }: ConnectionErrorProps) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5, ease: [0.25, 0.46, 0.45, 0.94] as const }}
      className="flex flex-col items-center justify-center py-24"
    >
      {/* Illustration */}
      <div className="relative mb-6">
        <div className="flex h-20 w-20 items-center justify-center rounded-2xl bg-rose-500/8 border border-rose-500/15">
          <WifiOff className="h-9 w-9 text-rose-400/80" strokeWidth={1.5} />
        </div>
        {/* Animated rings */}
        <div className="absolute inset-0 rounded-2xl border border-rose-500/20 animate-ping opacity-20" />
        <div className="absolute -inset-3 rounded-3xl border border-rose-500/10 animate-ping opacity-10" style={{ animationDelay: '0.5s' }} />
        {/* Ambient glow */}
        <div className="absolute inset-0 rounded-2xl bg-rose-500/10 blur-xl opacity-30" />
      </div>

      <h3 className="text-lg font-semibold text-white/80 mb-2">
        Connection Failed
      </h3>
      <p className="text-sm text-[oklch(1_0_0/30%)] max-w-md text-center mb-6 leading-relaxed">
        {message || 'Unable to connect to AtlasKV backend. Make sure the server is running on the configured address.'}
      </p>
      {onRetry && (
        <motion.button
          whileHover={{ scale: 1.03 }}
          whileTap={{ scale: 0.97 }}
          onClick={onRetry}
          className={cn(
            'flex items-center gap-2 rounded-xl border border-[oklch(1_0_0/8%)]',
            'bg-[oklch(1_0_0/4%)] px-5 py-2.5',
            'text-xs font-medium text-[oklch(1_0_0/50%)]',
            'hover:text-[oklch(1_0_0/70%)] hover:bg-[oklch(1_0_0/6%)]',
            'hover:border-[oklch(1_0_0/12%)]',
            'transition-all duration-200'
          )}
        >
          <RefreshCw className="h-3.5 w-3.5" />
          Retry Connection
        </motion.button>
      )}
    </motion.div>
  );
}
