'use client';

import { useState, useEffect, useMemo, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import {
  History,
  Search,
  GitBranch,
  Clock,
  RotateCcw,
  KeyRound,
  Server,
  Timer,
  CheckCircle2,
  Trash2,
  AlertTriangle,
  ArrowRight,
  Copy,
  Check,
  RefreshCw,
  Layers,
} from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { PageHeader } from '@/components/ui/page-header';
import { NamespaceBadge } from '@/components/ui/namespace-badge';
import { useHistory, useRollbackKey } from '@/hooks/use-history';
import { usePrefix } from '@/hooks/use-kv';
import { KeyRevisionResponse } from '@/types/api';
import { cn } from '@/lib/utils';

function HistoryContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const initialKey = searchParams.get('key') || 'app/config/theme';

  const [inputKey, setInputKey] = useState(initialKey);
  const [activeKey, setActiveKey] = useState(initialKey);
  const [copiedRev, setCopiedRev] = useState<number | null>(null);
  const [rollbackSuccess, setRollbackSuccess] = useState<string | null>(null);
  const [rollbackError, setRollbackError] = useState<string | null>(null);

  // Sync with URL query parameter changes
  useEffect(() => {
    const paramKey = searchParams.get('key');
    if (paramKey && paramKey !== activeKey) {
      setInputKey(paramKey);
      setActiveKey(paramKey);
    }
  }, [searchParams]);

  const { data: rawRevisions = [], isLoading, isFetching, refetch } = useHistory(activeKey);
  const { data: prefixData } = usePrefix('');
  const rollbackMutation = useRollbackKey();

  // Known live keys from prefix query for quick suggestions
  const quickKeys = useMemo(() => {
    const live = prefixData?.entries?.map((e) => e.key) ?? [];
    const defaults = ['test/lease-key', 'app/config/theme'];
    const merged = Array.from(new Set([...live, ...defaults]));
    return merged.slice(0, 6);
  }, [prefixData]);

  // Sort revisions reverse-chronologically (newest first)
  const revisions = useMemo(() => {
    return [...rawRevisions].sort((a, b) => b.revisionNumber - a.revisionNumber);
  }, [rawRevisions]);

  const latestRevision = revisions[0];
  const isCurrentlyDeletedOrExpired =
    latestRevision && (latestRevision.operation === 'DELETE' || latestRevision.operation === 'EXPIRE');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const clean = inputKey.trim();
    if (!clean) return;
    setActiveKey(clean);
    router.replace(`/history?key=${encodeURIComponent(clean)}`);
  };

  const handleSelectQuickKey = (key: string) => {
    setInputKey(key);
    setActiveKey(key);
    router.replace(`/history?key=${encodeURIComponent(key)}`);
  };

  const handleCopyValue = (val: string, revNum: number) => {
    navigator.clipboard.writeText(val);
    setCopiedRev(revNum);
    setTimeout(() => setCopiedRev(null), 2000);
  };

  const handleRollback = async (revNum: number) => {
    setRollbackSuccess(null);
    setRollbackError(null);
    try {
      await rollbackMutation.mutateAsync({ key: activeKey, revision: revNum });
      setRollbackSuccess(`Key '${activeKey}' was successfully rolled back to revision v${revNum}`);
      refetch();
      setTimeout(() => setRollbackSuccess(null), 5000);
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Failed to rollback key';
      setRollbackError(`Rollback error: ${msg}`);
      setTimeout(() => setRollbackError(null), 5000);
    }
  };

  const getOperationBadge = (rev: KeyRevisionResponse, isFirstRevision: boolean) => {
    const op = rev.operation?.toUpperCase() || 'PUT';
    if (op === 'DELETE') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md font-mono text-[11px] font-bold bg-rose-500/15 text-rose-700 dark:text-rose-400 border border-rose-500/30 shadow-xs">
          <Trash2 className="h-3 w-3" />
          DELETE
        </span>
      );
    }
    if (op === 'EXPIRE') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md font-mono text-[11px] font-bold bg-amber-500/15 text-amber-700 dark:text-amber-400 border border-amber-500/30 shadow-xs">
          <AlertTriangle className="h-3 w-3" />
          EXPIRE (Lease/TTL)
        </span>
      );
    }
    if (op === 'ROLLBACK') {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md font-mono text-[11px] font-bold bg-purple-500/15 text-purple-700 dark:text-purple-400 border border-purple-500/30 shadow-xs">
          <RotateCcw className="h-3 w-3" />
          ROLLBACK
        </span>
      );
    }
    if (isFirstRevision) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md font-mono text-[11px] font-bold bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border border-emerald-500/30 shadow-xs">
          <CheckCircle2 className="h-3 w-3" />
          CREATE (PUT)
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md font-mono text-[11px] font-bold bg-sky-500/15 text-sky-700 dark:text-sky-400 border border-sky-500/30 shadow-xs">
        <ArrowRight className="h-3 w-3" />
        UPDATE (PUT)
      </span>
    );
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <PageHeader
        title="Key Version History"
        description="Inspect per-key revision timelines, operation audit logs (PUT, update, DELETE, EXPIRE), and atomic consensus rollbacks."
        icon={History}
        iconColor="text-indigo-600 dark:text-indigo-400"
        badge={<NamespaceBadge showSwitcher={false} />}
      />

      {/* Search Bar & Quick Chips */}
      <div className="space-y-3">
        <motion.form
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          onSubmit={handleSearch}
          className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 glass-card rounded-xl p-3 border border-border dark:border-[oklch(1_0_0/8%)]"
        >
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-neutral-400" />
            <Input
              placeholder="Enter key name to inspect revision timeline (e.g. test/lease-key)..."
              value={inputKey}
              onChange={(e) => setInputKey(e.target.value)}
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
                Inspect Key
              </Button>
            </motion.div>
            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={() => refetch()}
              className="h-9 w-9 border-border dark:border-[oklch(1_0_0/8%)] text-muted-foreground hover:text-foreground rounded-lg"
              title="Refresh History"
            >
              <RefreshCw className={cn('h-4 w-4', isFetching && 'animate-spin')} />
            </Button>
          </div>
        </motion.form>

        {/* Quick Suggestion Chips */}
        {quickKeys.length > 0 && (
          <div className="flex flex-wrap items-center gap-1.5 px-1 text-xs">
            <span className="text-muted-foreground font-mono text-[11px] mr-1">Quick Select:</span>
            {quickKeys.map((k) => (
              <button
                key={k}
                type="button"
                onClick={() => handleSelectQuickKey(k)}
                className={cn(
                  'px-2 py-0.5 rounded-md font-mono text-[11px] border transition-all flex items-center gap-1',
                  activeKey === k
                    ? 'bg-indigo-500/20 text-indigo-700 dark:text-indigo-300 border-indigo-500/40 font-bold'
                    : 'bg-neutral-100 dark:bg-[oklch(1_0_0/3%)] text-neutral-600 dark:text-neutral-400 border-border dark:border-[oklch(1_0_0/8%)] hover:border-indigo-500/30 hover:text-foreground'
                )}
              >
                <KeyRound className="h-2.5 w-2.5 text-indigo-500 shrink-0" />
                {k}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Notifications */}
      <AnimatePresence>
        {rollbackSuccess && (
          <motion.div
            initial={{ opacity: 0, y: -6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/25 text-emerald-700 dark:text-emerald-400 text-xs font-mono font-semibold flex items-center gap-2"
          >
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-500" />
            <span>{rollbackSuccess}</span>
          </motion.div>
        )}
        {rollbackError && (
          <motion.div
            initial={{ opacity: 0, y: -6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -6 }}
            className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/25 text-rose-700 dark:text-rose-400 text-xs font-mono font-semibold flex items-center gap-2"
          >
            <AlertTriangle className="h-4 w-4 shrink-0 text-rose-500" />
            <span>{rollbackError}</span>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main Timeline Card */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, delay: 0.15 }}
        className="glass-card rounded-xl p-6 space-y-6 border border-border dark:border-[oklch(1_0_0/6%)]"
      >
        {/* Header summary of currently inspected key */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-border dark:border-[oklch(1_0_0/6%)] pb-4">
          <div className="flex items-center gap-2.5">
            <div className="h-8 w-8 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-600 dark:text-indigo-400 shrink-0">
              <GitBranch className="h-4 w-4" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-sm font-bold text-[var(--foreground)] font-mono">{activeKey}</h2>
                {latestRevision && (
                  <span
                    className={cn(
                      'px-2 py-0.5 rounded text-[10px] font-mono font-bold border',
                      isCurrentlyDeletedOrExpired
                        ? 'bg-neutral-500/10 text-neutral-600 dark:text-neutral-400 border-neutral-500/20'
                        : 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border-emerald-500/30'
                    )}
                  >
                    {isCurrentlyDeletedOrExpired ? 'STATUS: INACTIVE / EXPIRED' : 'STATUS: ACTIVE'}
                  </span>
                )}
              </div>
              <p className="text-[11px] text-muted-foreground font-mono">
                Historical revision log from Raft state machine
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 font-mono text-xs text-muted-foreground">
            <Layers className="h-3.5 w-3.5 text-indigo-500" />
            <span>Total Recorded Revisions: </span>
            <span className="font-bold text-foreground bg-muted px-2 py-0.5 rounded border border-border">
              {revisions.length}
            </span>
          </div>
        </div>

        {/* Loading Skeletons */}
        {isLoading ? (
          <div className="space-y-4 font-mono">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="p-4 rounded-xl border border-border dark:border-[oklch(1_0_0/5%)] bg-[var(--surface-2)] space-y-2.5"
              >
                <div className="flex justify-between">
                  <div className="h-4 w-32 bg-muted/60 rounded animate-pulse" />
                  <div className="h-4 w-24 bg-muted/60 rounded animate-pulse" />
                </div>
                <div className="h-12 w-full bg-muted/40 rounded animate-pulse" />
              </div>
            ))}
          </div>
        ) : revisions.length === 0 ? (
          /* Empty State */
          <div className="py-16 text-center">
            <div className="flex flex-col items-center gap-3">
              <div className="h-14 w-14 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center shadow-xs">
                <History className="h-7 w-7 text-indigo-600 dark:text-indigo-400" />
              </div>
              <div className="space-y-1">
                <h4 className="text-sm font-bold text-[var(--foreground)]">No Revision History Found</h4>
                <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-md mx-auto">
                  No previous revision timeline exists for key <code className="font-mono text-indigo-500 font-bold">"{activeKey}"</code>.
                  Keys receive immutable history entries whenever they are created, updated, expired, or deleted.
                </p>
              </div>
            </div>
          </div>
        ) : (
          /* Reverse-Chronological Revisions Timeline */
          <div className="relative border-l-2 border-indigo-500/30 ml-4 pl-6 space-y-6">
            {revisions.map((rev, idx) => {
              const isLatest = idx === 0;
              const isFirstRev = rev.revisionNumber === 1;
              const hasLease = !!rev.leaseId;
              const hasTtl = !!rev.ttl;
              const hasValue = rev.value !== null && rev.value !== undefined;

              return (
                <motion.div
                  key={rev.revisionNumber}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.25, delay: idx * 0.04 }}
                  className="relative group"
                >
                  {/* Timeline Dot */}
                  <div
                    className={cn(
                      'absolute -left-[31px] top-1.5 h-3.5 w-3.5 rounded-full border-2 bg-[var(--surface-1)] transition-colors',
                      isLatest
                        ? 'border-indigo-500 bg-indigo-500 shadow-sm ring-4 ring-indigo-500/20'
                        : 'border-border dark:border-[oklch(1_0_0/20%)] group-hover:border-indigo-500'
                    )}
                  />

                  {/* Revision Card */}
                  <div className="p-4 rounded-xl border border-border dark:border-[oklch(1_0_0/6%)] bg-[var(--surface-2)] space-y-3 hover:border-indigo-500/30 transition-colors">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      {/* Version & Operation Badges */}
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className={cn(
                            'px-2 py-0.5 rounded-md font-mono text-[11px] font-bold border',
                            isLatest
                              ? 'bg-indigo-500/20 text-indigo-700 dark:text-indigo-300 border-indigo-500/40'
                              : 'bg-muted text-muted-foreground border-border'
                          )}
                        >
                          Revision v{rev.revisionNumber} {isLatest && '(Latest)'}
                        </span>

                        {getOperationBadge(rev, isFirstRev)}

                        {hasLease && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-purple-500/15 text-purple-700 dark:text-purple-300 border border-purple-500/30 text-[10px] font-mono font-semibold">
                            <Clock className="h-3 w-3" />
                            Lease: {rev.leaseId}
                          </span>
                        )}

                        {hasTtl && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-amber-500/15 text-amber-700 dark:text-amber-300 border border-amber-500/30 text-[10px] font-mono font-semibold">
                            <Timer className="h-3 w-3" />
                            TTL: {rev.ttl}
                          </span>
                        )}

                        {rev.nodeId && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-neutral-200/60 dark:bg-neutral-800/60 text-neutral-600 dark:text-neutral-400 border border-neutral-300/40 dark:border-neutral-700/40 text-[10px] font-mono">
                            <Server className="h-2.5 w-2.5" />
                            {rev.nodeId}
                          </span>
                        )}
                      </div>

                      {/* Timestamp & Rollback Action */}
                      <div className="flex items-center gap-2">
                        <span className="text-[11px] text-muted-foreground font-mono flex items-center gap-1">
                          <Clock className="h-3 w-3 text-muted-foreground/60" />
                          {new Date(rev.timestamp || Date.now()).toLocaleString()}
                        </span>

                        {/* Rollback button: available for historical revisions with values */}
                        {!isLatest && hasValue && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleRollback(rev.revisionNumber)}
                            disabled={rollbackMutation.isPending}
                            className="h-7 px-2.5 border-indigo-500/30 text-indigo-600 dark:text-indigo-300 hover:bg-indigo-500/10 text-[11px] gap-1 rounded-lg font-mono font-bold"
                          >
                            <RotateCcw className="h-3 w-3" />
                            Rollback to v{rev.revisionNumber}
                          </Button>
                        )}
                      </div>
                    </div>

                    {/* Value Body */}
                    {hasValue ? (
                      <div className="relative group/val">
                        <div className="p-3 rounded-lg bg-[var(--surface-1)] border border-border dark:border-[oklch(1_0_0/5%)] font-mono text-xs text-emerald-700 dark:text-emerald-400 overflow-x-auto whitespace-pre-wrap break-all pr-10">
                          {rev.value}
                        </div>
                        <button
                          type="button"
                          onClick={() => handleCopyValue(rev.value!, rev.revisionNumber)}
                          className="absolute right-2 top-2 p-1.5 rounded-md bg-muted/80 hover:bg-muted text-muted-foreground hover:text-foreground transition-all"
                          title="Copy revision value"
                        >
                          {copiedRev === rev.revisionNumber ? (
                            <Check className="h-3.5 w-3.5 text-emerald-500" />
                          ) : (
                            <Copy className="h-3.5 w-3.5" />
                          )}
                        </button>
                      </div>
                    ) : (
                      <div className="p-2.5 rounded-lg bg-neutral-500/5 border border-dashed border-border dark:border-[oklch(1_0_0/10%)] font-mono text-xs text-muted-foreground italic flex items-center gap-1.5">
                        {rev.operation === 'EXPIRE' ? (
                          <>
                            <AlertTriangle className="h-3.5 w-3.5 text-amber-500" />
                            <span>Key payload removed due to lease / TTL expiration.</span>
                          </>
                        ) : (
                          <>
                            <Trash2 className="h-3.5 w-3.5 text-rose-500" />
                            <span>Key payload explicitly deleted (tombstone recorded).</span>
                          </>
                        )}
                      </div>
                    )}
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}
      </motion.div>
    </div>
  );
}

export default function HistoryPage() {
  return (
    <Suspense
      fallback={
        <div className="space-y-6">
          <div className="h-20 bg-muted/40 rounded-xl animate-pulse" />
          <div className="h-96 bg-muted/20 rounded-xl animate-pulse" />
        </div>
      }
    >
      <HistoryContent />
    </Suspense>
  );
}
