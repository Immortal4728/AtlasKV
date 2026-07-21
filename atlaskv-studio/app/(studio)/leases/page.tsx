'use client';

import { useState } from 'react';
import { Clock, Plus, RefreshCw, Trash2, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { useLeases, useCreateLease, useRenewLease, useRevokeLease } from '@/hooks/use-leases';

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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Clock className="h-5 w-5 text-purple-400" />
            Distributed Lease Management
          </h1>
          <p className="text-xs text-zinc-400 mt-1">
            Live cluster leases, expiration timers, and keep-alive renewals
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            onClick={() => refetch()}
            variant="outline"
            className="border-white/10 text-zinc-300 hover:bg-white/5 text-xs gap-1.5"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>

          <Button
            onClick={() => {
              setTtlInput('30s');
              setCustomIdInput('');
              setErrorMsg(null);
              setCreateDialogOpen(true);
            }}
            className="bg-purple-600 hover:bg-purple-700 text-white font-semibold text-xs px-3.5 py-2 shadow-lg shadow-purple-600/20 gap-1.5"
          >
            <Plus className="h-4 w-4" />
            Create Lease
          </Button>
        </div>
      </div>

      {/* Leases Table */}
      <div className="rounded-xl border border-white/[0.08] bg-zinc-900/40 backdrop-blur-md overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-zinc-950/60 border-b border-white/[0.08] text-zinc-400 uppercase tracking-wider text-[10px] font-mono">
              <tr>
                <th className="py-3 px-4">Lease ID</th>
                <th className="py-3 px-4">TTL (Duration)</th>
                <th className="py-3 px-4">Expiration</th>
                <th className="py-3 px-4">Attached Keys</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.06] text-zinc-300 font-mono">
              {isLoading ? (
                [1, 2, 3].map((i) => (
                  <tr key={i} className="animate-pulse">
                    <td className="py-3 px-4"><div className="h-4 w-28 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-16 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-24 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-32 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4 text-right"><div className="h-4 w-20 bg-zinc-800 rounded ml-auto" /></td>
                  </tr>
                ))
              ) : leases.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-12 text-center text-zinc-500">
                    No active leases in cluster
                  </td>
                </tr>
              ) : (
                leases.map((lease) => {
                  const remainingSec = Math.max(
                    0,
                    Math.round((lease.expiryTimeMs - Date.now()) / 1000)
                  );

                  return (
                    <tr key={lease.leaseId} className="hover:bg-white/[0.02] transition-colors">
                      <td className="py-3 px-4 font-medium text-purple-400 flex items-center gap-2">
                        <Clock className="h-3.5 w-3.5 text-purple-500/70 shrink-0" />
                        {lease.leaseId}
                      </td>
                      <td className="py-3 px-4 text-zinc-300">
                        {lease.durationMs / 1000}s
                      </td>
                      <td className="py-3 px-4">
                        <Badge
                          variant="outline"
                          className={
                            remainingSec > 10
                              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 text-[10px]'
                              : 'bg-rose-500/10 text-rose-400 border-rose-500/20 text-[10px]'
                          }
                        >
                          Expires in {remainingSec}s
                        </Badge>
                      </td>
                      <td className="py-3 px-4 text-zinc-400">
                        {lease.keys && lease.keys.length > 0 ? (
                          <div className="flex flex-wrap gap-1">
                            {lease.keys.map((k) => (
                              <span key={k} className="px-1.5 py-0.5 rounded bg-zinc-800 text-[10px] text-zinc-300">
                                {k}
                              </span>
                            ))}
                          </div>
                        ) : (
                          <span className="text-zinc-600">None</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleRenew(lease.leaseId)}
                            className="h-7 px-2 border-purple-500/20 text-purple-300 hover:bg-purple-500/10 text-[11px] gap-1"
                          >
                            <RefreshCw className="h-3 w-3" />
                            Renew
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleRevoke(lease.leaseId)}
                            className="h-7 px-2 border-rose-500/20 text-rose-400 hover:bg-rose-500/10 text-[11px] gap-1"
                          >
                            <Trash2 className="h-3 w-3" />
                            Revoke
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Lease Dialog */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="bg-zinc-900 border-white/10 text-zinc-100 max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-purple-400">
              <Clock className="h-5 w-5" /> Create Lease
            </DialogTitle>
            <DialogDescription className="text-zinc-400 text-xs">
              Allocate a new TTL lease on the cluster.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-mono">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="space-y-1.5">
              <label className="font-semibold text-zinc-300 font-mono">Lease TTL (e.g. 30s, 5m)</label>
              <Input
                value={ttlInput}
                onChange={(e) => setTtlInput(e.target.value)}
                className="bg-zinc-950 border-white/10 text-xs font-mono"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setCreateDialogOpen(false)}
              className="border-white/10 text-zinc-300 hover:bg-white/5 text-xs"
            >
              Cancel
            </Button>
            <Button
              onClick={handleCreate}
              disabled={createMutation.isPending}
              className="bg-purple-600 hover:bg-purple-700 text-white font-semibold text-xs"
            >
              {createMutation.isPending ? 'Creating...' : 'Create Lease'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
