'use client';

import { useState } from 'react';
import { History, Search, GitBranch, Clock, RefreshCw, RotateCcw, AlertTriangle } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useHistory, useRollbackKey } from '@/hooks/use-history';

export default function HistoryPage() {
  const [searchKey, setSearchKey] = useState('app/config/theme');
  const [activeKey, setActiveKey] = useState('app/config/theme');

  const { data: historyData, isLoading, refetch } = useHistory(activeKey);
  const rollbackMutation = useRollbackKey();

  const [rollbackSuccess, setRollbackSuccess] = useState<string | null>(null);

  const revisions = historyData?.revisions ?? [];

  const handleRollback = async (version: number) => {
    try {
      await rollbackMutation.mutateAsync({ key: activeKey, revision: version });
      setRollbackSuccess(`Successfully rolled back key '${activeKey}' to version v${version}`);
      refetch();
      setTimeout(() => setRollbackSuccess(null), 4000);
    } catch (err: any) {
      console.error('Rollback error', err);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
          <History className="h-5 w-5 text-indigo-400" />
          Key Revision History & Rollback
        </h1>
        <p className="text-xs text-zinc-400 mt-1">
          Inspect append-only version timelines and execute atomic state rollbacks
        </p>
      </div>

      {/* Search Bar */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          setActiveKey(searchKey);
        }}
        className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 bg-zinc-900/60 p-3 rounded-xl border border-white/[0.08] backdrop-blur-md"
      >
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
          <Input
            placeholder="Enter key to view version timeline..."
            value={searchKey}
            onChange={(e) => setSearchKey(e.target.value)}
            className="pl-9 bg-zinc-950/50 border-white/10 text-xs font-mono text-zinc-200 placeholder:text-zinc-500 focus-visible:ring-indigo-500/50"
          />
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="submit"
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs px-4 py-2 shadow-lg shadow-indigo-600/20 gap-1.5"
          >
            <Search className="h-4 w-4" />
            View History
          </Button>
        </div>
      </form>

      {rollbackSuccess && (
        <div className="p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-mono">
          {rollbackSuccess}
        </div>
      )}

      {/* Timeline View */}
      <div className="rounded-xl border border-white/[0.08] bg-zinc-900/40 backdrop-blur-md p-6 space-y-6">
        <div className="flex items-center justify-between border-b border-white/[0.08] pb-4">
          <div className="flex items-center gap-2">
            <GitBranch className="h-4 w-4 text-indigo-400" />
            <h2 className="text-sm font-semibold text-white font-mono">{activeKey}</h2>
          </div>
          <span className="text-xs text-zinc-400 font-mono">Total Revisions: {revisions.length}</span>
        </div>

        {isLoading ? (
          <div className="space-y-4 font-mono">
            {[1, 2].map((i) => (
              <div key={i} className="animate-pulse p-4 rounded-xl border border-white/5 bg-zinc-950/40 space-y-2">
                <div className="h-4 w-32 bg-zinc-800 rounded" />
                <div className="h-8 w-full bg-zinc-800 rounded" />
              </div>
            ))}
          </div>
        ) : revisions.length === 0 ? (
          <div className="py-12 text-center text-zinc-500 text-xs font-mono">
            No revision history found for key "{activeKey}"
          </div>
        ) : (
          <div className="relative border-l-2 border-indigo-500/30 ml-4 pl-6 space-y-6">
            {revisions.map((rev, idx) => (
              <div key={rev.version} className="relative group">
                <div
                  className={`absolute -left-[31px] top-1 h-3.5 w-3.5 rounded-full border-2 bg-zinc-950 transition-colors ${
                    idx === 0
                      ? 'border-indigo-400 bg-indigo-500'
                      : 'border-zinc-700 group-hover:border-indigo-400'
                  }`}
                />

                <div className="p-4 rounded-xl border border-white/[0.08] bg-zinc-950/60 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Badge
                        variant="outline"
                        className={
                          idx === 0
                            ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20 font-mono text-[10px]'
                            : 'bg-zinc-800 text-zinc-400 border-zinc-700 font-mono text-[10px]'
                        }
                      >
                        Revision v{rev.version} {idx === 0 && '(Current)'}
                      </Badge>
                      <span className="text-xs text-zinc-400 font-mono flex items-center gap-1">
                        <Clock className="h-3 w-3 text-zinc-500" />
                        {new Date(rev.timestamp || Date.now()).toLocaleString()}
                      </span>
                    </div>

                    {idx > 0 && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleRollback(rev.version)}
                        disabled={rollbackMutation.isPending}
                        className="h-7 px-2 border-indigo-500/20 text-indigo-300 hover:bg-indigo-500/10 text-[11px] gap-1"
                      >
                        <RotateCcw className="h-3 w-3" />
                        Rollback to v{rev.version}
                      </Button>
                    )}
                  </div>

                  <div className="p-3 rounded-lg bg-zinc-900 border border-white/5 font-mono text-xs text-emerald-400 overflow-x-auto">
                    {rev.value ?? '<null>'}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
