'use client';

import { cn } from '@/lib/utils';
import { motion } from 'framer-motion';
import { AlertCircle, RefreshCw } from 'lucide-react';

interface ConnectionErrorProps {
  message?: string;
  onRetry?: () => void;
}

export function ConnectionError({ message, onRetry }: ConnectionErrorProps) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      className="flex flex-col items-center justify-center py-20"
    >
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-500/10 mb-4">
        <AlertCircle className="h-7 w-7 text-rose-400" strokeWidth={1.5} />
      </div>
      <h3 className="text-lg font-semibold text-white/80 mb-1">
        Connection Failed
      </h3>
      <p className="text-sm text-white/30 max-w-md text-center mb-5">
        {message || 'Unable to connect to AtlasKV backend. Make sure the server is running on the configured address.'}
      </p>
      {onRetry && (
        <button
          onClick={onRetry}
          className={cn(
            'flex items-center gap-2 rounded-lg border border-white/[0.08] bg-white/[0.04] px-4 py-2',
            'text-xs font-medium text-white/50 hover:text-white/70 hover:bg-white/[0.06] transition-all'
          )}
        >
          <RefreshCw className="h-3.5 w-3.5" />
          Retry Connection
        </button>
      )}
    </motion.div>
  );
}
