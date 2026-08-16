'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { Clock, Plus, RefreshCw, Trash2, KeyRound } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
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
import { cn } from '@/lib/utils';

export default function LeasesPage() {
  const { data: leasesData, isLoading, refetch } = useLeases();
  const createMutation = useCreateLease();
  const renewMutation = useRenewLease();
  const revokeMutation = useRevokeLease();

  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [ttlInput, setTtlInput] = useState('30s');
  const [customIdInput, setCustomIdInput] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const leases = leasesData ?? [];

  const handleCreate = async () => {
    setErrorMsg(null);
    try {
      await createMutation.mutateAsync({ ttl: ttlInput, leaseId: customIdInput || undefined });
      setCreateDialogOpen(false);
      refetch();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to create lease');
    }
  };

  const handleRenew = async (leaseId: string) => {
    try {
      await renewMutation.mutateAsync(leaseId);
      refetch();
    } catch (err: any) {
      console.error('Renew error', err);
    }
  };

  const handleRevoke = async (leaseId: string) => {
    try {
      await revokeMutation.mutateAsync(leaseId);
      refetch();
    } catch (err: any) {
      console.error('Revoke error', err);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Leases"
        description="Manage distributed leases within the active namespace."
        icon={Clock}
        iconColor="text-purple-400"
        badge={<NamespaceBadge showSwitcher={false} />}
        actions={
          <>
            <Button
              onClick={() => refetch()}
              variant="outline"
              className="border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/50%)] hover:bg-[oklch(1_0_0/4%)] hover:text-white text-xs gap-1.5 rounded-lg"
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

      {/* Leases Table */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, delay: 0.1 }}
        className="glass-card rounded-xl overflow-hidden p-0 border border-border dark:border-[oklch(1_0_0/6%)]"
      >
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[var(--surface-2)] border-b border-border dark:border-[oklch(1_0_0/6%)] text-neutral-700 dark:text-[oklch(1_0_0/50%)] uppercase tracking-[0.1em] text-[10px] font-mono font-bold">
              <tr>
                <th className="py-3 px-4">Lease ID</th>
                <th className="py-3 px-4">TTL (Duration)</th>
                <th className="py-3 px-4">Expiration</th>
                <th className="py-3 px-4">Attached Keys</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border dark:divide-[oklch(1_0_0/4%)] text-[var(--foreground)] font-mono">
              {isLoading ? (
                [1, 2, 3].map((i) => (
                  <tr key={i}>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-28 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-16 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-24 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-32 rounded" /></td>
                    <td className="py-3.5 px-4 text-right"><div className="skeleton h-4 w-20 rounded ml-auto" /></td>
                  </tr>
                ))
              ) : leases.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-16 text-center">
                    <div className="flex flex-col items-center gap-3">
                      <div className="h-14 w-14 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center shadow-xs">
                        <Clock className="h-7 w-7 text-purple-600 dark:text-purple-400" />
                      </div>
                      <div className="space-y-1">
                        <h4 className="text-sm font-bold text-[var(--foreground)]">No Active Leases</h4>
                        <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-sm">
                          Create your first distributed lease to attach automatic expiration TTLs to key-value pairs.
                        </p>
                      </div>
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
                    </div>
                  </td>
                </tr>
              ) : (
                leases.map((lease, idx) => {
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
                      className="hover:bg-neutral-100/80 dark:hover:bg-[oklch(1_0_0/2%)] transition-colors"
                    >
                      <td className="py-3 px-4 font-bold text-purple-600 dark:text-purple-400 flex items-center gap-2">
                        <Clock className="h-3.5 w-3.5 text-purple-500/60 shrink-0" />
                        {lease.leaseId}
                      </td>
                      <td className="py-3 px-4 text-neutral-700 dark:text-[oklch(1_0_0/50%)] font-semibold">
                        {lease.durationMs / 1000}s
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={cn(
                            'px-2 py-0.5 rounded-md text-[10px] border font-mono font-bold',
                            remainingSec > 10
                              ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                              : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/30'
                          )}
                        >
                          Expires in {remainingSec}s
                        </span>
                      </td>
                      <td className="py-3 px-4 text-[oklch(1_0_0/40%)]">
                        {lease.keys && lease.keys.length > 0 ? (
                          <div className="flex flex-wrap gap-1">
                            {lease.keys.map((k) => (
                              <span key={k} className="px-1.5 py-0.5 rounded bg-[oklch(1_0_0/4%)] text-[10px] text-[oklch(1_0_0/60%)] border border-[oklch(1_0_0/6%)]">
                                {k}
                              </span>
                            ))}
                          </div>
                        ) : (
                          <span className="text-[oklch(1_0_0/15%)]">None</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleRenew(lease.leaseId)}
                            className="h-7 px-2.5 border-purple-500/20 text-purple-300 hover:bg-purple-500/10 text-[11px] gap-1 rounded-lg"
                          >
                            <RefreshCw className="h-3 w-3" />
                            Renew
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleRevoke(lease.leaseId)}
                            className="h-7 px-2.5 border-rose-500/20 text-rose-400 hover:bg-rose-500/10 text-[11px] gap-1 rounded-lg"
                          >
                            <Trash2 className="h-3 w-3" />
                            Revoke
                          </Button>
                        </div>
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
            <DialogTitle className="flex items-center gap-2 text-purple-500 dark:text-purple-400">
              <Clock className="h-5 w-5" /> Create Lease
            </DialogTitle>
            <DialogDescription className="text-muted-foreground text-xs">
              Allocate a new TTL lease on the cluster.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-mono break-all">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">Lease TTL (e.g. 30s, 5m)</label>
              <Input
                value={ttlInput}
                onChange={(e) => setTtlInput(e.target.value)}
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
    </div>
  );
}
