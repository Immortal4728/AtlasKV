'use client';

import { useState, useMemo } from 'react';
import { motion } from 'framer-motion';
import { Clock, Plus, RefreshCw, Trash2, KeyRound, Link, CheckCircle2, AlertCircle, Ban, History } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';
import { NamespaceBadge } from '@/components/ui/namespace-badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { useLeases, useCreateLease, useRenewLease, useRevokeLease } from '@/hooks/use-leases';
import { usePutValue } from '@/hooks/use-kv';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import type { LeaseResponse } from '@/types/api';

type LeaseFilterTab = 'ALL' | 'ACTIVE' | 'EXPIRED' | 'REVOKED';

export default function LeasesPage() {
  const { data: leasesData, isLoading, refetch } = useLeases();
  const createMutation = useCreateLease();
  const renewMutation = useRenewLease();
  const revokeMutation = useRevokeLease();
  const putMutation = usePutValue();

  const [filterTab, setFilterTab] = useState<LeaseFilterTab>('ALL');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [attachDialogOpen, setAttachDialogOpen] = useState(false);

  const [ttlInput, setTtlInput] = useState('30s');
  const [customIdInput, setCustomIdInput] = useState('');

  const [selectedLeaseId, setSelectedLeaseId] = useState<string>('');
  const [attachKeyInput, setAttachKeyInput] = useState('');
  const [attachValueInput, setAttachValueInput] = useState('');

  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const leases: LeaseResponse[] = leasesData ?? [];

  // Helper to determine lease state
  const getLeaseStatus = (lease: LeaseResponse): 'ACTIVE' | 'EXPIRED' | 'REVOKED' => {
    if (lease.status === 'REVOKED') return 'REVOKED';
    if (lease.status === 'EXPIRED') return 'EXPIRED';
    if (lease.expiryTimeMs <= Date.now()) return 'EXPIRED';
    return 'ACTIVE';
  };

  const activeCount = useMemo(() => leases.filter((l) => getLeaseStatus(l) === 'ACTIVE').length, [leases]);
  const expiredCount = useMemo(() => leases.filter((l) => getLeaseStatus(l) === 'EXPIRED').length, [leases]);
  const revokedCount = useMemo(() => leases.filter((l) => getLeaseStatus(l) === 'REVOKED').length, [leases]);

  const filteredLeases = useMemo(() => {
    if (filterTab === 'ALL') return leases;
    return leases.filter((l) => getLeaseStatus(l) === filterTab);
  }, [leases, filterTab]);

  const handleCreate = async () => {
    setErrorMsg(null);
    try {
      await createMutation.mutateAsync({ ttl: ttlInput, leaseId: customIdInput || undefined });
      toast.success('Lease created successfully');
      setCreateDialogOpen(false);
      refetch();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to create lease');
      toast.error(`Create failed: ${err.message}`);
    }
  };

  const handleRenew = async (leaseId: string) => {
    try {
      await renewMutation.mutateAsync(leaseId);
      toast.success(`Lease '${leaseId}' renewed`);
      refetch();
    } catch (err: any) {
      toast.error(`Renew failed: ${err.message}`);
    }
  };

  const handleRevoke = async (leaseId: string) => {
    try {
      await revokeMutation.mutateAsync(leaseId);
      toast.success(`Lease '${leaseId}' revoked`);
      refetch();
    } catch (err: any) {
      toast.error(`Revoke failed: ${err.message}`);
    }
  };

  const handleAttachKey = async () => {
    if (!attachKeyInput.trim()) {
      setErrorMsg('Key is required');
      return;
    }
    setErrorMsg(null);
    try {
      await putMutation.mutateAsync({
        key: attachKeyInput.trim(),
        value: attachValueInput,
        leaseId: selectedLeaseId,
      });
      toast.success(`Key '${attachKeyInput}' attached to lease '${selectedLeaseId}'`);
      setAttachDialogOpen(false);
      setAttachKeyInput('');
      setAttachValueInput('');
      refetch();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to attach key to lease');
      toast.error(`Attach key failed: ${err.message}`);
    }
  };

  const formatTimestamp = (ts?: number | null) => {
    if (!ts) return '—';
    const d = new Date(ts);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Leases & TTLs"
        description="Manage distributed TTL leases, active lifecycles, and historical audit records within the active namespace."
        icon={Clock}
        iconColor="text-purple-400"
        badge={<NamespaceBadge showSwitcher={false} />}
        actions={
          <>
            <Button
              onClick={() => refetch()}
              variant="outline"
              className="border-border dark:border-[oklch(1_0_0/8%)] text-neutral-700 dark:text-[oklch(1_0_0/50%)] hover:bg-neutral-100 dark:hover:bg-[oklch(1_0_0/4%)] hover:text-neutral-900 dark:hover:text-white text-xs gap-1.5 rounded-lg font-medium"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>

            <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
              <Button
                onClick={() => {
                  setTtlInput('30s');
                  setCustomIdInput('');
                  setErrorMsg(null);
                  setCreateDialogOpen(true);
                }}
                className="bg-gradient-to-r from-purple-500 to-indigo-600 hover:from-purple-400 hover:to-indigo-500 text-white font-semibold text-xs px-4 py-2 shadow-lg shadow-purple-500/20 gap-1.5 rounded-lg border-0"
              >
                <Plus className="h-4 w-4" />
                Create Lease
              </Button>
            </motion.div>
          </>
        }
      />

      {/* Filter Tabs */}
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 p-1 bg-neutral-100 dark:bg-[var(--surface-1)] rounded-lg border border-border dark:border-[oklch(1_0_0/6%)]">
          <button
            onClick={() => setFilterTab('ALL')}
            className={cn(
              'px-3 py-1.5 rounded-md text-xs font-mono font-medium transition-all',
              filterTab === 'ALL'
                ? 'bg-card dark:bg-[var(--surface-3)] text-foreground font-bold shadow-xs border border-border/50'
                : 'text-muted-foreground hover:text-foreground'
            )}
          >
            All ({leases.length})
          </button>
          <button
            onClick={() => setFilterTab('ACTIVE')}
            className={cn(
              'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-mono font-medium transition-all',
              filterTab === 'ACTIVE'
                ? 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-300 font-bold border border-emerald-500/30'
                : 'text-muted-foreground hover:text-foreground'
            )}
          >
            <span className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse" />
            Active ({activeCount})
          </button>
          <button
            onClick={() => setFilterTab('EXPIRED')}
            className={cn(
              'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-mono font-medium transition-all',
              filterTab === 'EXPIRED'
                ? 'bg-amber-500/20 text-amber-700 dark:text-amber-300 font-bold border border-amber-500/30'
                : 'text-muted-foreground hover:text-foreground'
            )}
          >
            <AlertCircle className="h-3 w-3 text-amber-500" />
            Expired ({expiredCount})
          </button>
          <button
            onClick={() => setFilterTab('REVOKED')}
            className={cn(
              'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-mono font-medium transition-all',
              filterTab === 'REVOKED'
                ? 'bg-rose-500/20 text-rose-700 dark:text-rose-300 font-bold border border-rose-500/30'
                : 'text-muted-foreground hover:text-foreground'
            )}
          >
            <Ban className="h-3 w-3 text-rose-500" />
            Revoked ({revokedCount})
          </button>
        </div>

        <div className="text-xs text-muted-foreground font-mono flex items-center gap-1.5">
          <History className="h-3.5 w-3.5 text-purple-400" />
          <span>Audit History: {leases.length} tracked leases</span>
        </div>
      </div>

      {/* Leases Table */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, delay: 0.1 }}
        className="glass-card rounded-xl overflow-hidden p-0 border border-border dark:border-[oklch(1_0_0/6%)]"
      >
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-neutral-100/90 dark:bg-[var(--surface-2)] border-b border-border dark:border-[oklch(1_0_0/6%)] text-neutral-800 dark:text-[oklch(1_0_0/50%)] uppercase tracking-[0.1em] text-[10px] font-mono font-bold">
              <tr>
                <th className="py-3 px-4">Lease ID</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Duration</th>
                <th className="py-3 px-4">Timeline</th>
                <th className="py-3 px-4">Bound Keys</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border dark:divide-[oklch(1_0_0/4%)] text-neutral-900 dark:text-[var(--foreground)] font-mono">
              {isLoading ? (
                [1, 2, 3].map((i) => (
                  <tr key={i}>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-28 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-16 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-24 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-32 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-24 rounded" /></td>
                    <td className="py-3.5 px-4 text-right"><div className="skeleton h-4 w-20 rounded ml-auto" /></td>
                  </tr>
                ))
              ) : filteredLeases.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-16 text-center">
                    <div className="flex flex-col items-center gap-3">
                      <div className="h-14 w-14 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center shadow-xs">
                        <Clock className="h-7 w-7 text-purple-600 dark:text-purple-400" />
                      </div>
                      <div className="space-y-1">
                        <h4 className="text-sm font-bold text-neutral-900 dark:text-[var(--foreground)]">
                          {filterTab === 'ALL' ? 'No Leases Found' : `No ${filterTab} Leases`}
                        </h4>
                        <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-sm">
                          {filterTab === 'ALL'
                            ? 'Create your first distributed lease to attach automatic expiration TTLs to key-value pairs.'
                            : `There are currently no leases in ${filterTab.toLowerCase()} status.`}
                        </p>
                      </div>
                      {filterTab === 'ALL' && (
                        <Button
                          onClick={() => {
                            setTtlInput('30s');
                            setCustomIdInput('');
                            setErrorMsg(null);
                            setCreateDialogOpen(true);
                          }}
                          className="mt-2 bg-purple-500 hover:bg-purple-600 text-white font-bold text-xs px-4 py-2 rounded-lg shadow-sm gap-1.5"
                        >
                          <Plus className="h-4 w-4" />
                          Create Lease
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                filteredLeases.map((lease, idx) => {
                  const status = getLeaseStatus(lease);
                  const remainingSec = Math.max(
                    0,
                    Math.round((lease.expiryTimeMs - Date.now()) / 1000)
                  );

                  return (
                    <motion.tr
                      key={lease.leaseId}
                      initial={{ opacity: 0, x: -6 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ duration: 0.25, delay: idx * 0.03 }}
                      className={cn(
                        'transition-colors',
                        status === 'ACTIVE'
                          ? 'hover:bg-neutral-100/80 dark:hover:bg-[oklch(1_0_0/2%)]'
                          : 'opacity-70 hover:opacity-100 bg-neutral-50/50 dark:bg-[oklch(1_0_0/1%)]'
                      )}
                    >
                      {/* Lease ID */}
                      <td className="py-3 px-4 font-bold text-purple-700 dark:text-purple-400 flex items-center gap-2">
                        <Clock className="h-3.5 w-3.5 text-purple-600 dark:text-purple-500/60 shrink-0" />
                        <span className="truncate max-w-[200px]">{lease.leaseId}</span>
                      </td>

                      {/* Status Badge */}
                      <td className="py-3 px-4">
                        {status === 'ACTIVE' && (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border border-emerald-500/30">
                            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
                            ACTIVE ({remainingSec}s)
                          </span>
                        )}
                        {status === 'EXPIRED' && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-amber-500/15 text-amber-700 dark:text-amber-400 border border-amber-500/30">
                            <AlertCircle className="h-3 w-3 text-amber-500" />
                            EXPIRED
                          </span>
                        )}
                        {status === 'REVOKED' && (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-rose-500/15 text-rose-700 dark:text-rose-400 border border-rose-500/30">
                            <Ban className="h-3 w-3 text-rose-500" />
                            REVOKED
                          </span>
                        )}
                      </td>

                      {/* Duration */}
                      <td className="py-3 px-4 text-neutral-800 dark:text-neutral-300 font-semibold">
                        {lease.durationMs / 1000}s
                      </td>

                      {/* Timeline */}
                      <td className="py-3 px-4 text-[11px] text-muted-foreground">
                        <div>Created: {formatTimestamp(lease.createdAtMs)}</div>
                        <div className="text-[10px]">
                          {status === 'ACTIVE'
                            ? `Deadline: ${formatTimestamp(lease.expiryTimeMs)}`
                            : `Ended: ${formatTimestamp(lease.lastActionTimeMs || lease.expiryTimeMs)}`}
                        </div>
                      </td>

                      {/* Attached Keys */}
                      <td className="py-3 px-4 text-neutral-800 dark:text-[oklch(1_0_0/60%)] font-semibold">
                        {lease.keys && lease.keys.length > 0 ? (
                          <div className="flex flex-wrap gap-1 max-w-[220px]">
                            {lease.keys.map((k) => (
                              <span
                                key={k}
                                className={cn(
                                  'px-1.5 py-0.5 rounded text-[10px] font-mono font-semibold border flex items-center gap-1',
                                  status === 'ACTIVE'
                                    ? 'bg-neutral-200/70 dark:bg-[oklch(1_0_0/4%)] text-neutral-800 dark:text-[oklch(1_0_0/60%)] border-neutral-300 dark:border-[oklch(1_0_0/6%)]'
                                    : 'bg-neutral-200/40 dark:bg-neutral-800/40 text-neutral-500 dark:text-neutral-500 border-neutral-300/40 line-through'
                                )}
                              >
                                <KeyRound className="h-2.5 w-2.5" />
                                {k}
                              </span>
                            ))}
                          </div>
                        ) : (
                          <span className="text-neutral-400 dark:text-[oklch(1_0_0/20%)]">—</span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-4 text-right">
                        {status === 'ACTIVE' ? (
                          <div className="flex items-center justify-end gap-1.5">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setSelectedLeaseId(lease.leaseId);
                                setAttachKeyInput('');
                                setAttachValueInput('');
                                setErrorMsg(null);
                                setAttachDialogOpen(true);
                              }}
                              className="h-7 px-2 border-emerald-500/30 text-emerald-700 dark:text-emerald-400 hover:bg-emerald-500/10 text-[10px] gap-1 rounded-lg font-semibold"
                              title="Attach Key to Lease"
                            >
                              <Link className="h-3 w-3" />
                              Attach Key
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleRenew(lease.leaseId)}
                              className="h-7 px-2 border-purple-500/30 text-purple-700 dark:text-purple-300 hover:bg-purple-500/10 text-[10px] gap-1 rounded-lg font-semibold"
                              title="Renew Lease"
                            >
                              <RefreshCw className="h-3 w-3" />
                              Renew
                            </Button>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleRevoke(lease.leaseId)}
                              className="h-7 px-2 border-rose-500/30 text-rose-700 dark:text-rose-400 hover:bg-rose-500/10 text-[10px] gap-1 rounded-lg font-semibold"
                              title="Revoke Lease"
                            >
                              <Trash2 className="h-3 w-3" />
                              Revoke
                            </Button>
                          </div>
                        ) : (
                          <span className="text-[10px] text-muted-foreground font-mono">
                            Archived
                          </span>
                        )}
                      </td>
                    </motion.tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </motion.div>

      {/* Create Lease Dialog */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="bg-card dark:bg-[var(--surface-2)] backdrop-blur-2xl border-border dark:border-[oklch(1_0_0/8%)] text-foreground max-w-md shadow-2xl shadow-black/50 rounded-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-purple-600 dark:text-purple-400">
              <Clock className="h-5 w-5" /> Create Lease
            </DialogTitle>
            <DialogDescription className="text-muted-foreground text-xs">
              Allocate a new distributed TTL lease on the cluster with full audit tracking.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-mono break-all">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">Lease TTL (e.g. 30s, 5m, 1h) *</label>
              <Input
                value={ttlInput}
                onChange={(e) => setTtlInput(e.target.value)}
                placeholder="e.g. 30s or 5m"
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-purple-500/30"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">Custom Lease ID (Optional)</label>
              <Input
                value={customIdInput}
                onChange={(e) => setCustomIdInput(e.target.value)}
                placeholder="e.g. worker-heartbeat-lease"
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-purple-500/30"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setCreateDialogOpen(false)}
              className="border-border dark:border-[oklch(1_0_0/8%)] text-muted-foreground hover:bg-muted text-xs rounded-lg"
            >
              Cancel
            </Button>
            <Button
              onClick={handleCreate}
              disabled={createMutation.isPending}
              className="bg-gradient-to-r from-purple-500 to-indigo-600 hover:from-purple-400 hover:to-indigo-500 text-white font-semibold text-xs rounded-lg border-0"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Lease'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Attach Key to Lease Dialog */}
      <Dialog open={attachDialogOpen} onOpenChange={setAttachDialogOpen}>
        <DialogContent className="bg-card dark:bg-[var(--surface-2)] backdrop-blur-2xl border-border dark:border-[oklch(1_0_0/8%)] text-foreground max-w-md shadow-2xl shadow-black/50 rounded-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
              <Link className="h-5 w-5" /> Attach Key to Lease: {selectedLeaseId}
            </DialogTitle>
            <DialogDescription className="text-muted-foreground text-xs">
              Create a new key bound to active lease <span className="font-mono font-bold text-purple-600 dark:text-purple-400">{selectedLeaseId}</span>.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-mono break-all">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">Key *</label>
              <Input
                placeholder="e.g. session/user-123"
                value={attachKeyInput}
                onChange={(e) => setAttachKeyInput(e.target.value)}
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-emerald-500/30"
              />
            </div>
            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">Value</label>
              <Input
                placeholder="e.g. active-session-token"
                value={attachValueInput}
                onChange={(e) => setAttachValueInput(e.target.value)}
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-emerald-500/30"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setAttachDialogOpen(false)}
              className="border-border dark:border-[oklch(1_0_0/8%)] text-muted-foreground hover:bg-muted text-xs rounded-lg"
            >
              Cancel
            </Button>
            <Button
              onClick={handleAttachKey}
              disabled={putMutation.isPending}
              className="bg-emerald-600 hover:bg-emerald-700 dark:bg-emerald-500 dark:hover:bg-emerald-600 text-white font-semibold text-xs rounded-lg border-0"
            >
              {putMutation.isPending ? 'Attaching...' : 'Attach Key'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
