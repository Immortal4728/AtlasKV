'use client';

import { useEffect } from 'react';
import { motion } from 'framer-motion';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('Studio Error Boundary caught error:', error);
  }, [error]);

  return (
    <div className="min-h-[440px] flex flex-col items-center justify-center text-center p-8">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.4 }}
        className="glass-card rounded-2xl p-8 max-w-md space-y-5 border border-rose-500/20"
      >
        <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-400 mx-auto">
          <AlertTriangle className="h-7 w-7" strokeWidth={1.8} />
        </div>

        <div className="space-y-1.5">
          <h2 className="text-lg font-bold text-white">Something went wrong</h2>
          <p className="text-xs text-[oklch(1_0_0/35%)] max-w-sm mx-auto leading-relaxed">
            {error.message || 'An unexpected error occurred while communicating with the AtlasKV cluster.'}
          </p>
        </div>

        <motion.div whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.97 }}>
          <Button
            onClick={() => reset()}
            className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-semibold text-xs px-5 py-2.5 shadow-lg shadow-emerald-500/20 gap-2 rounded-lg border-0"
          >
            <RefreshCw className="h-4 w-4" />
            Retry Connection
          </Button>
        </motion.div>
      </motion.div>
    </div>
  );
}
