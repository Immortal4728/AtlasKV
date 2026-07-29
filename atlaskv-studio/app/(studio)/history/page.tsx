'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { History, Search, GitBranch, Clock, RotateCcw } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { PageHeader } from '@/components/ui/page-header';
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
      <PageHeader
        title="Revision History"
        description="View key version timeline."
        icon={History}
        iconColor="text-indigo-400"
      />

      {/* Search Bar */}
      <motion.form
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        onSubmit={(e) => {
          e.preventDefault();
          setActiveKey(searchKey);
        }}
        className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 glass-card rounded-xl p-3 border border-border dark:border-[oklch(1_0_0/8%)]"
      >
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-neutral-400" />
          <Input
            placeholder="Enter key to view version timeline..."
            value={searchKey}
            onChange={(e) => setSearchKey(e.target.value)}
            className="pl-9 bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] placeholder:text-neutral-400 focus-visible:ring-indigo-500/30 rounded-lg"
          />
        </div>

        <div className="flex items-center gap-2">
          <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
            <Button
              type="submit"
              className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs px-4 py-2 shadow-sm gap-1.5 rounded-lg border-0"
            >
              <Search className="h-4 w-4" />
              View History
            </Button>
          </motion.div>
        </div>
      </motion.form>

      {rollbackSuccess && (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs font-mono font-bold"
        >
          {rollbackSuccess}
        </motion.div>
      )}

      {/* Timeline View */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, delay: 0.15 }}
        className="glass-card rounded-xl p-6 space-y-6 border border-border dark:border-[oklch(1_0_0/6%)]"
      >
        <div className="flex items-center justify-between border-b border-border dark:border-[oklch(1_0_0/6%)] pb-4">
          <div className="flex items-center gap-2">
            <GitBranch className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
            <h2 className="text-sm font-bold text-[var(--foreground)] font-mono">{activeKey}</h2>
          </div>
          <span className="text-xs text-neutral-600 dark:text-neutral-400 font-mono font-semibold">Total Revisions: {revisions.length}</span>
        </div>

        {isLoading ? (
          <div className="space-y-4 font-mono">
            {[1, 2].map((i) => (
              <div key={i} className="p-4 rounded-xl border border-border dark:border-[oklch(1_0_0/5%)] bg-[var(--surface-2)] space-y-2">
                <div className="skeleton h-4 w-32 rounded" />
                <div className="skeleton h-8 w-full rounded" />
              </div>
            ))}
          </div>
        ) : revisions.length === 0 ? (
          <div className="py-16 text-center">
            <div className="flex flex-col items-center gap-3">
              <div className="h-14 w-14 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center shadow-xs">
                <History className="h-7 w-7 text-indigo-600 dark:text-indigo-400" />
              </div>
              <div className="space-y-1">
                <h4 className="text-sm font-bold text-[var(--foreground)]">No Revision History Found</h4>
                <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-sm">
                  No previous version timeline exists for key "{activeKey}". Search for another key to inspect atomic state rollbacks.
                </p>
              </div>
            </div>
          </div>
        ) : (
          <div className="relative border-l-2 border-indigo-500/30 ml-4 pl-6 space-y-6">
            {revisions.map((rev, idx) => (
              <motion.div
                key={rev.version}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.3, delay: idx * 0.05 }}
                className="relative group"
              >
                <div
                  className={`absolute -left-[31px] top-1 h-3.5 w-3.5 rounded-full border-2 bg-[var(--surface-1)] transition-colors ${
                    idx === 0
                      ? 'border-indigo-500 bg-indigo-500 shadow-sm'
                      : 'border-border dark:border-[oklch(1_0_0/20%)] group-hover:border-indigo-500'
                  }`}
                />

                <div className="p-4 rounded-xl border border-border dark:border-[oklch(1_0_0/6%)] bg-[var(--surface-2)] space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span
                        className={`px-2 py-0.5 rounded-md font-mono text-[10px] border ${
                          idx === 0
                            ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
                            : 'bg-[oklch(1_0_0/4%)] text-[oklch(1_0_0/40%)] border-[oklch(1_0_0/6%)]'
                        }`}
                      >
                        Revision v{rev.version} {idx === 0 && '(Current)'}
                      </span>
                      <span className="text-xs text-[oklch(1_0_0/35%)] font-mono flex items-center gap-1">
                        <Clock className="h-3 w-3 text-[oklch(1_0_0/20%)]" />
                        {new Date(rev.timestamp || Date.now()).toLocaleString()}
                      </span>
                    </div>

                    {idx > 0 && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleRollback(rev.version)}
                        disabled={rollbackMutation.isPending}
                        className="h-7 px-2.5 border-indigo-500/20 text-indigo-300 hover:bg-indigo-500/10 text-[11px] gap-1 rounded-lg"
                      >
                        <RotateCcw className="h-3 w-3" />
                        Rollback to v{rev.version}
                      </Button>
                    )}
                  </div>

                  <div className="p-3 rounded-lg bg-[var(--surface-1)] border border-[oklch(1_0_0/5%)] font-mono text-xs text-emerald-400 overflow-x-auto">
                    {rev.value ?? '<null>'}
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </motion.div>
    </div>
  );
}
