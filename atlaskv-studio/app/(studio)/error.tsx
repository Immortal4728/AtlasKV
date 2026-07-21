'use client';

import { useEffect } from 'react';
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
    <div className="min-h-[400px] flex flex-col items-center justify-center text-center p-6 bg-zinc-900/40 border border-rose-500/20 rounded-2xl backdrop-blur-md space-y-4">
      <div className="h-12 w-12 rounded-full bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
        <AlertTriangle className="h-6 w-6" />
      </div>
      <div>
        <h2 className="text-lg font-bold text-white">Something went wrong</h2>
        <p className="text-xs text-zinc-400 mt-1 max-w-md">
          {error.message || 'An unexpected error occurred while communicating with the AtlasKV cluster.'}
        </p>
      </div>

      <Button
        onClick={() => reset()}
        className="bg-emerald-500 hover:bg-emerald-600 text-zinc-950 font-semibold text-xs px-4 py-2 gap-2"
      >
        <RefreshCw className="h-4 w-4" />
        Retry Connection
      </Button>
    </div>
  );
}
