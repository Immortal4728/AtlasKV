'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import {
  KeyRound,
  Plus,
  RefreshCw,
  Trash2,
  Edit2,
  Copy,
  Check,
  Filter,
  Search,
  ChevronLeft,
  ChevronRight,
  Clock,
  Timer,
  Shield,
  Layers,
  Sparkles,
  History,
  GitCompare,
} from 'lucide-react';
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
import { CasModal } from '@/components/kv/cas-modal';
import { usePrefix, usePutValue, useCasPutValue, useDeleteValue } from '@/hooks/use-kv';
import { useLeases } from '@/hooks/use-leases';
import { ConflictError } from '@/services/api';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

export default function KeysPage() {
  const [prefixFilter, setPrefixFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [offset, setOffset] = useState(0);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);
  const limit = 50;

  // Real backend prefix query hook (with 3s auto-refetch for TTL accuracy)
  const { data: prefixData, isLoading, refetch } = usePrefix(prefixFilter, offset, limit);
  const { data: activeLeases = [] } = useLeases();

  const liveActiveLeases = useMemo(() => {
    return activeLeases.filter(
      (l) => (l.status === 'ACTIVE' || !l.status) && (l.expiryTimeMs > Date.now())
    );
  }, [activeLeases]);

  // Mutations
  const putMutation = usePutValue();
  const casMutation = useCasPutValue();
  const deleteMutation = useDeleteValue();

  // Dialog States
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [casDialogOpen, setCasDialogOpen] = useState(false);
  const [casSelectedKey, setCasSelectedKey] = useState<string>('');
  const [selectedKey, setSelectedKey] = useState<any>(null);

  // Form input states
  const [inputKey, setInputKey] = useState('');
  const [inputValue, setInputValue] = useState('');
  const [leaseOption, setLeaseOption] = useState<'none' | 'existing' | 'custom_ttl' | 'custom_lease'>('none');
  const [inputTtl, setInputTtl] = useState('');
  const [inputLeaseId, setInputLeaseId] = useState('');
  const [isCasMode, setIsCasMode] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const entries = prefixData?.entries ?? [];
  const filteredEntries = entries.filter((e) =>
    e.key.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (e.value && e.value.toLowerCase().includes(searchTerm.toLowerCase()))
  );
  const totalCount = prefixData?.totalCount ?? entries.length;

  const resetForm = () => {
    setInputKey('');
    setInputValue('');
    setLeaseOption('none');
    setInputTtl('');
    setInputLeaseId('');
    setIsCasMode(false);
    setErrorMsg(null);
  };

  const handleCreateKey = async () => {
    if (!inputKey.trim()) {
      setErrorMsg('Key name is required');
      return;
    }
    setErrorMsg(null);

    let effectiveTtl: string | undefined = undefined;
    let effectiveLeaseId: string | undefined = undefined;

    if (leaseOption === 'existing') {
      if (!inputLeaseId.trim()) {
        setErrorMsg('Please select an active lease to attach, or switch to "No Lease".');
        return;
      }
      effectiveLeaseId = inputLeaseId.trim();
    } else if (leaseOption === 'custom_ttl' && inputTtl.trim()) {
      effectiveTtl = inputTtl.trim();
    } else if (leaseOption === 'custom_lease' && inputLeaseId.trim()) {
      effectiveLeaseId = inputLeaseId.trim();
    }

    try {
      await putMutation.mutateAsync({
        key: inputKey.trim(),
        value: inputValue,
        ttl: effectiveTtl,
        leaseId: effectiveLeaseId,
      });
      toast.success(`Successfully created key '${inputKey}'`);
      setCreateDialogOpen(false);
      resetForm();
      refetch();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to create key');
      toast.error(`Create failed: ${err.message}`);
    }
  };

  const handleUpdateKey = async () => {
    setErrorMsg(null);
    if (!selectedKey) return;

    try {
      if (isCasMode) {
        await casMutation.mutateAsync({
          key: selectedKey.key,
          value: inputValue,
          expectedVersion: selectedKey.version,
        });
        toast.success(`Atomic CAS update succeeded for '${selectedKey.key}'`);
      } else {
        let effectiveTtl: string | undefined = undefined;
        let effectiveLeaseId: string | undefined = undefined;

        if (leaseOption === 'existing') {
          if (!inputLeaseId.trim()) {
            setErrorMsg('Please select an active lease to attach, or switch to "No Lease".');
            return;
          }
          effectiveLeaseId = inputLeaseId.trim();
        } else if (leaseOption === 'custom_ttl' && inputTtl.trim()) {
          effectiveTtl = inputTtl.trim();
        } else if (leaseOption === 'custom_lease' && inputLeaseId.trim()) {
          effectiveLeaseId = inputLeaseId.trim();
        }

        await putMutation.mutateAsync({
          key: selectedKey.key,
          value: inputValue,
          ttl: effectiveTtl,
          leaseId: effectiveLeaseId,
        });
        toast.success(`Successfully updated '${selectedKey.key}'`);
      }
      setEditDialogOpen(false);
      refetch();
    } catch (err: any) {
      if (err instanceof ConflictError) {
        const msg = `CAS Conflict! Expected v${err.expectedVersion}, but found v${err.currentVersion}`;
        setErrorMsg(msg);
        toast.error(msg);
      } else {
        setErrorMsg(err.message || 'Failed to update key');
        toast.error(`Update failed: ${err.message}`);
      }
    }
  };

  const handleDeleteKey = async () => {
    setErrorMsg(null);
    if (!selectedKey) return;

    try {
      await deleteMutation.mutateAsync(selectedKey.key);
      toast.success(`Successfully deleted key '${selectedKey.key}'`);
      setDeleteDialogOpen(false);
      refetch();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to delete key');
      toast.error(`Delete failed: ${err.message}`);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(text);
    toast.success('Copied key to clipboard');
    setTimeout(() => setCopiedKey(null), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Key Explorer"
        description="Browse, create, update, and manage distributed key-value pairs with authoritative Lease and TTL expiration."
        icon={KeyRound}
        iconColor="text-emerald-400"
        badge={<NamespaceBadge showSwitcher={true} />}
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

            <Button
              onClick={() => {
                setCasSelectedKey('');
                setCasDialogOpen(true);
              }}
              variant="outline"
              className="border-indigo-500/30 bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-500/20 text-xs gap-1.5 rounded-lg font-mono font-medium"
            >
              <GitCompare className="h-3.5 w-3.5 text-indigo-500" />
              Compare-And-Set (CAS)
            </Button>

            <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
              <Button
                onClick={() => {
                  resetForm();
                  setCreateDialogOpen(true);
                }}
                className="bg-emerald-600 hover:bg-emerald-700 dark:bg-emerald-500 dark:hover:bg-emerald-600 text-white font-bold text-xs px-4 py-2 shadow-sm gap-1.5 rounded-lg border-0"
              >
                <Plus className="h-4 w-4" />
                Create Key
              </Button>
            </motion.div>
          </>
        }
      />

      {/* Filter / Search Bar */}
      <motion.div
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 glass-card rounded-xl p-3 border border-border dark:border-[oklch(1_0_0/8%)]"
      >
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-neutral-500 dark:text-neutral-400" />
          <Input
            placeholder="Search matching keys or values..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 bg-card dark:bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-neutral-900 dark:text-[var(--foreground)] placeholder:text-neutral-500 dark:placeholder:text-neutral-400 focus-visible:ring-emerald-500/30 rounded-lg font-medium"
          />
        </div>

        <div className="flex items-center gap-2">
          <div className="relative min-w-[180px]">
            <Filter className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-neutral-500 dark:text-neutral-400" />
            <Input
              placeholder="Prefix filter (e.g. app/)"
              value={prefixFilter}
              onChange={(e) => {
                setPrefixFilter(e.target.value);
                setOffset(0);
              }}
              className="pl-8 bg-card dark:bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-neutral-900 dark:text-[var(--foreground)] placeholder:text-neutral-500 dark:placeholder:text-neutral-400 focus-visible:ring-emerald-500/30 rounded-lg font-medium"
            />
          </div>
        </div>
      </motion.div>

      {/* Main Keys Table */}
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
                <th className="py-3 px-4">Key</th>
                <th className="py-3 px-4">Value</th>
                <th className="py-3 px-4">Version</th>
                <th className="py-3 px-4">Lease & TTL Lifecycle</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border dark:divide-[oklch(1_0_0/4%)] text-neutral-900 dark:text-[var(--foreground)] font-mono">
              {isLoading ? (
                [1, 2, 3, 4, 5].map((i) => (
                  <tr key={i}>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-36 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-48 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-12 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-28 rounded" /></td>
                    <td className="py-3.5 px-4 text-right"><div className="skeleton h-4 w-16 rounded ml-auto" /></td>
                  </tr>
                ))
              ) : filteredEntries.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-16 text-center">
                    <div className="flex flex-col items-center gap-3">
                      <div className="h-14 w-14 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center shadow-xs">
                        <KeyRound className="h-7 w-7 text-emerald-600 dark:text-emerald-400" />
                      </div>
                      <div className="space-y-1">
                        <h4 className="text-sm font-bold text-neutral-900 dark:text-[var(--foreground)]">No Key-Value Entries Found</h4>
                        <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-sm">
                          {searchTerm || prefixFilter
                            ? 'No entries matched your search or prefix criteria.'
                            : 'Get started by creating your first distributed key-value pair.'}
                        </p>
                      </div>
                      <Button
                        onClick={() => {
                          resetForm();
                          setCreateDialogOpen(true);
                        }}
                        className="mt-2 bg-emerald-600 hover:bg-emerald-700 dark:bg-emerald-500 dark:hover:bg-emerald-600 text-white font-bold text-xs px-4 py-2 rounded-lg shadow-sm gap-1.5"
                      >
                        <Plus className="h-4 w-4" />
                        Create Key
                      </Button>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredEntries.map((item, idx) => {
                  const hasLease = !!item.leaseId;
                  const hasTtl = item.ttlRemaining != null;
                  const remainingSeconds = hasTtl ? Math.ceil(item.ttlRemaining! / 1000) : null;
                  const isExpiringSoon = remainingSeconds !== null && remainingSeconds <= 5;

                  return (
                    <motion.tr
                      key={item.key}
                      initial={{ opacity: 0, x: -6 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ duration: 0.25, delay: idx * 0.03 }}
                      className="hover:bg-neutral-100/80 dark:hover:bg-[oklch(1_0_0/2%)] transition-colors group"
                    >
                      <td className="py-3 px-4 font-semibold text-emerald-700 dark:text-emerald-400 flex items-center gap-2">
                        <KeyRound className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-500/60 shrink-0" />
                        <span className="truncate max-w-[240px]">{item.key}</span>
                        <button
                          onClick={() => copyToClipboard(item.key)}
                          className="opacity-0 group-hover:opacity-100 text-neutral-500 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white transition-all ml-1"
                        >
                          {copiedKey === item.key ? <Check className="h-3 w-3 text-emerald-600 dark:text-emerald-500" /> : <Copy className="h-3 w-3" />}
                        </button>
                      </td>
                      <td className="py-3 px-4 max-w-[320px] truncate text-neutral-900 dark:text-neutral-200 font-medium">
                        {item.value ?? '<null>'}
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded-md bg-neutral-200/70 dark:bg-[oklch(1_0_0/6%)] border border-neutral-300 dark:border-[oklch(1_0_0/8%)] text-neutral-900 dark:text-neutral-300 text-[11px] font-semibold font-mono">
                          v{item.version ?? 1}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex flex-wrap items-center gap-1.5">
                          {hasLease && (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-purple-500/15 text-purple-700 dark:text-purple-300 border border-purple-500/30 text-[10px] font-mono font-semibold">
                              <Clock className="h-3 w-3" />
                              {item.leaseId}
                            </span>
                          )}

                          {hasTtl && (
                            <span
                              className={cn(
                                'inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-mono font-semibold border',
                                isExpiringSoon
                                  ? 'bg-rose-500/20 text-rose-700 dark:text-rose-300 border-rose-500/40 animate-pulse'
                                  : 'bg-amber-500/15 text-amber-700 dark:text-amber-300 border-amber-500/30'
                              )}
                            >
                              <Timer className="h-3 w-3" />
                              {remainingSeconds}s remaining
                            </span>
                          )}

                          {!hasLease && !hasTtl && (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-neutral-200/50 dark:bg-neutral-800/50 text-neutral-600 dark:text-neutral-400 border border-neutral-300/50 dark:border-neutral-700/50 text-[10px] font-mono font-medium">
                              <Shield className="h-3 w-3" />
                              Persistent
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Link
                            href={`/history?key=${encodeURIComponent(item.key)}`}
                            className="inline-flex items-center justify-center h-7 w-7 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-500/10 dark:hover:bg-indigo-500/15 rounded-lg transition-colors"
                            title="View Revision History"
                          >
                            <History className="h-3.5 w-3.5" />
                          </Link>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setCasSelectedKey(item.key);
                              setCasDialogOpen(true);
                            }}
                            className="h-7 w-7 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-500/10 dark:hover:bg-indigo-500/15 rounded-lg"
                            title="Compare-And-Set (CAS)"
                          >
                            <GitCompare className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setSelectedKey(item);
                              setInputValue(item.value || '');
                              if (item.leaseId) {
                                setLeaseOption('existing');
                                setInputLeaseId(item.leaseId);
                                setInputTtl('');
                              } else if (item.ttlRemaining != null) {
                                setLeaseOption('custom_ttl');
                                setInputTtl(`${Math.ceil(item.ttlRemaining / 1000)}s`);
                                setInputLeaseId('');
                              } else {
                                setLeaseOption('none');
                                setInputTtl('');
                                setInputLeaseId('');
                              }
                              setIsCasMode(false);
                              setErrorMsg(null);
                              setEditDialogOpen(true);
                            }}
                            className="h-7 w-7 text-emerald-700 dark:text-emerald-400 hover:bg-emerald-500/10 dark:hover:bg-emerald-500/15 rounded-lg"
                            title="Edit Key"
                          >
                            <Edit2 className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setSelectedKey(item);
                              setErrorMsg(null);
                              setDeleteDialogOpen(true);
                            }}
                            className="h-7 w-7 text-rose-500 dark:text-rose-400/80 hover:text-rose-600 dark:hover:text-rose-300 hover:bg-rose-500/10 dark:hover:bg-rose-500/15 rounded-lg"
                            title="Delete Key"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
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

        {/* Footer */}
        <div className="flex items-center justify-between px-4 py-3 bg-neutral-100/80 dark:bg-[var(--surface-0)]/40 border-t border-border dark:border-[oklch(1_0_0/6%)] text-xs text-neutral-800 dark:text-[oklch(1_0_0/35%)] font-mono font-medium">
          <span>Entries: {filteredEntries.length} | Total: {totalCount}</span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={offset === 0}
              onClick={() => setOffset(Math.max(0, offset - limit))}
              className="h-7 px-2 border-border dark:border-[oklch(1_0_0/8%)] bg-card dark:bg-transparent text-neutral-700 dark:text-[oklch(1_0_0/50%)] hover:text-neutral-900 dark:hover:text-white disabled:text-neutral-400 dark:disabled:text-[oklch(1_0_0/20%)]"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
            </Button>
            <span className="text-neutral-800 dark:text-neutral-300 font-semibold">Offset {offset}</span>
            <Button
              variant="outline"
              size="sm"
              disabled={offset + limit >= totalCount}
              onClick={() => setOffset(offset + limit)}
              className="h-7 px-2 border-border dark:border-[oklch(1_0_0/8%)] bg-card dark:bg-transparent text-neutral-700 dark:text-[oklch(1_0_0/50%)] hover:text-neutral-900 dark:hover:text-white disabled:text-neutral-400 dark:disabled:text-[oklch(1_0_0/20%)]"
            >
              <ChevronRight className="h-3.5 w-3.5" />
            </Button>
          </div>
        </div>
      </motion.div>

      {/* Create Key Dialog */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="bg-card dark:bg-[var(--surface-2)] backdrop-blur-2xl border-border dark:border-[oklch(1_0_0/8%)] text-foreground max-w-lg shadow-2xl shadow-black/50 rounded-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
              <Plus className="h-5 w-5" /> Create Key-Value
            </DialogTitle>
            <DialogDescription className="text-muted-foreground text-xs">
              Store a distributed key-value entry into the Raft state machine with optional Lease/TTL binding.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-mono break-all">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">Key Name *</label>
              <Input
                placeholder="e.g. app/config/theme"
                value={inputKey}
                onChange={(e) => setInputKey(e.target.value)}
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-emerald-500/30"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">Value</label>
              <Input
                placeholder="e.g. dark_mode_v2"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-emerald-500/30"
              />
            </div>

            {/* Lease / TTL Selector */}
            <div className="space-y-3 pt-2 border-t border-border dark:border-[oklch(1_0_0/8%)]">
              <label className="font-semibold text-foreground/80 font-mono text-[11px] flex items-center justify-between">
                <span className="flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-purple-400" />
                  Expiration & Lease Policy
                </span>
                <span className="text-[10px] text-muted-foreground font-normal">
                  {leaseOption === 'none' && 'No expiration (Persistent)'}
                  {leaseOption === 'existing' && 'Bound to cluster lease'}
                  {leaseOption === 'custom_ttl' && 'Custom TTL duration'}
                  {leaseOption === 'custom_lease' && 'Manual lease ID'}
                </span>
              </label>

              {/* Mode Selector Tabs */}
              <div className="grid grid-cols-4 gap-1 p-1 bg-muted/40 dark:bg-[var(--surface-0)] rounded-lg border border-border dark:border-[oklch(1_0_0/6%)]">
                <button
                  type="button"
                  onClick={() => setLeaseOption('none')}
                  className={cn(
                    'py-1.5 px-2 rounded text-[10px] font-mono font-medium transition-all',
                    leaseOption === 'none'
                      ? 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-300 font-bold border border-emerald-500/30'
                      : 'text-muted-foreground hover:text-foreground'
                  )}
                >
                  No Lease
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setLeaseOption('existing');
                    if (liveActiveLeases.length > 0 && (!inputLeaseId || !liveActiveLeases.some(l => l.leaseId === inputLeaseId))) {
                      setInputLeaseId(liveActiveLeases[0].leaseId);
                    }
                  }}
                  className={cn(
                    'py-1.5 px-2 rounded text-[10px] font-mono font-medium transition-all',
                    leaseOption === 'existing'
                      ? 'bg-purple-500/20 text-purple-700 dark:text-purple-300 font-bold border border-purple-500/30'
                      : 'text-muted-foreground hover:text-foreground'
                  )}
                >
                  Active Lease
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setLeaseOption('custom_ttl');
                    if (!inputTtl) setInputTtl('30s');
                  }}
                  className={cn(
                    'py-1.5 px-2 rounded text-[10px] font-mono font-medium transition-all',
                    leaseOption === 'custom_ttl'
                      ? 'bg-amber-500/20 text-amber-700 dark:text-amber-300 font-bold border border-amber-500/30'
                      : 'text-muted-foreground hover:text-foreground'
                  )}
                >
                  Inline TTL
                </button>
                <button
                  type="button"
                  onClick={() => setLeaseOption('custom_lease')}
                  className={cn(
                    'py-1.5 px-2 rounded text-[10px] font-mono font-medium transition-all',
                    leaseOption === 'custom_lease'
                      ? 'bg-indigo-500/20 text-indigo-700 dark:text-indigo-300 font-bold border border-indigo-500/30'
                      : 'text-muted-foreground hover:text-foreground'
                  )}
                >
                  Custom ID
                </button>
              </div>

              {/* Conditional Inputs */}
              {leaseOption === 'existing' && (
                <div className="space-y-1.5 p-2.5 rounded-lg bg-purple-500/5 border border-purple-500/20">
                  <label className="font-semibold text-purple-700 dark:text-purple-300 font-mono text-[10px]">
                    Select Active Lease
                  </label>
                  {liveActiveLeases.length > 0 ? (
                    <select
                      value={inputLeaseId}
                      onChange={(e) => setInputLeaseId(e.target.value)}
                      className="w-full h-9 rounded-md px-3 bg-background dark:bg-[var(--surface-0)] border border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground focus-visible:ring-purple-500/30"
                    >
                      <option value="">-- Choose Active Lease --</option>
                      {liveActiveLeases.map((l) => {
                        const remSec = Math.max(0, Math.round((l.expiryTimeMs - Date.now()) / 1000));
                        return (
                          <option key={l.leaseId} value={l.leaseId}>
                            {l.leaseId} ({Math.round(l.durationMs / 1000)}s TTL, {remSec}s remaining, {l.keys?.length ?? 0} keys)
                          </option>
                        );
                      })}
                    </select>
                  ) : (
                    <div className="text-xs text-muted-foreground py-1">
                      No active leases found. You can create one on the Leases page or use Inline TTL.
                    </div>
                  )}
                </div>
              )}

              {leaseOption === 'custom_ttl' && (
                <div className="space-y-1.5 p-2.5 rounded-lg bg-amber-500/5 border border-amber-500/20">
                  <label className="font-semibold text-amber-700 dark:text-amber-300 font-mono text-[10px]">
                    TTL Duration String
                  </label>
                  <Input
                    placeholder="e.g. 10s, 30s, 5m, 1h"
                    value={inputTtl}
                    onChange={(e) => setInputTtl(e.target.value)}
                    className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-amber-500/30"
                  />
                  <p className="text-[10px] text-muted-foreground font-mono">
                    Supported units: ms, s, m, h (e.g. 45s or 2m)
                  </p>
                </div>
              )}

              {leaseOption === 'custom_lease' && (
                <div className="space-y-1.5 p-2.5 rounded-lg bg-indigo-500/5 border border-indigo-500/20">
                  <label className="font-semibold text-indigo-700 dark:text-indigo-300 font-mono text-[10px]">
                    Target Lease Identifier
                  </label>
                  <Input
                    placeholder="e.g. cluster-worker-lease-42"
                    value={inputLeaseId}
                    onChange={(e) => setInputLeaseId(e.target.value)}
                    className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-indigo-500/30"
                  />
                </div>
              )}
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
              onClick={handleCreateKey}
              disabled={putMutation.isPending}
              className="bg-emerald-600 hover:bg-emerald-700 dark:bg-emerald-500 dark:hover:bg-emerald-600 text-white font-semibold text-xs rounded-lg border-0"
            >
              {putMutation.isPending ? 'Saving...' : 'Save Key'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit / CAS Key Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="bg-card dark:bg-[var(--surface-2)] backdrop-blur-2xl border-border dark:border-[oklch(1_0_0/8%)] text-foreground max-w-lg shadow-2xl shadow-black/50 rounded-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
              <Edit2 className="h-5 w-5" /> Edit Key: {selectedKey?.key}
            </DialogTitle>
            <DialogDescription className="text-muted-foreground text-xs">
              Perform a direct update, Compare-And-Swap (CAS), or update Lease/TTL binding.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-mono break-all">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="flex items-center justify-between p-2.5 rounded-lg bg-muted/50 dark:bg-[var(--surface-0)] border border-border dark:border-[oklch(1_0_0/8%)]">
              <span className="font-mono text-muted-foreground">Current Version: v{selectedKey?.version ?? 1}</span>
              <label className="flex items-center gap-2 font-mono text-xs text-emerald-600 dark:text-emerald-400 cursor-pointer font-semibold">
                <input
                  type="checkbox"
                  checked={isCasMode}
                  onChange={(e) => setIsCasMode(e.target.checked)}
                  className="rounded bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/15%)] text-emerald-500"
                />
                Atomic CAS
              </label>
            </div>

            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">Updated Value</label>
              <Input
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-emerald-500/30"
              />
            </div>

            {!isCasMode && (
              <div className="space-y-3 pt-2 border-t border-border dark:border-[oklch(1_0_0/8%)]">
                <label className="font-semibold text-foreground/80 font-mono text-[11px] flex items-center justify-between">
                  <span className="flex items-center gap-1.5">
                    <Clock className="h-3.5 w-3.5 text-purple-400" />
                    Expiration & Lease Policy
                  </span>
                </label>

                {/* Mode Selector Tabs */}
                <div className="grid grid-cols-4 gap-1 p-1 bg-muted/40 dark:bg-[var(--surface-0)] rounded-lg border border-border dark:border-[oklch(1_0_0/6%)]">
                  <button
                    type="button"
                    onClick={() => {
                      setLeaseOption('none');
                      setInputLeaseId('');
                      setInputTtl('');
                    }}
                    className={cn(
                      'py-1.5 px-2 rounded text-[10px] font-mono font-medium transition-all',
                      leaseOption === 'none'
                        ? 'bg-emerald-500/20 text-emerald-700 dark:text-emerald-300 font-bold border border-emerald-500/30'
                        : 'text-muted-foreground hover:text-foreground'
                    )}
                  >
                    No Lease
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setLeaseOption('existing');
                      if (liveActiveLeases.length > 0 && (!inputLeaseId || !liveActiveLeases.some(l => l.leaseId === inputLeaseId))) {
                        setInputLeaseId(liveActiveLeases[0].leaseId);
                      }
                    }}
                    className={cn(
                      'py-1.5 px-2 rounded text-[10px] font-mono font-medium transition-all',
                      leaseOption === 'existing'
                        ? 'bg-purple-500/20 text-purple-700 dark:text-purple-300 font-bold border border-purple-500/30'
                        : 'text-muted-foreground hover:text-foreground'
                    )}
                  >
                    Active Lease
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setLeaseOption('custom_ttl');
                      if (!inputTtl) setInputTtl('30s');
                    }}
                    className={cn(
                      'py-1.5 px-2 rounded text-[10px] font-mono font-medium transition-all',
                      leaseOption === 'custom_ttl'
                        ? 'bg-amber-500/20 text-amber-700 dark:text-amber-300 font-bold border border-amber-500/30'
                        : 'text-muted-foreground hover:text-foreground'
                    )}
                  >
                    Inline TTL
                  </button>
                  <button
                    type="button"
                    onClick={() => setLeaseOption('custom_lease')}
                    className={cn(
                      'py-1.5 px-2 rounded text-[10px] font-mono font-medium transition-all',
                      leaseOption === 'custom_lease'
                        ? 'bg-indigo-500/20 text-indigo-700 dark:text-indigo-300 font-bold border border-indigo-500/30'
                        : 'text-muted-foreground hover:text-foreground'
                    )}
                  >
                    Custom ID
                  </button>
                </div>

                {leaseOption === 'existing' && (
                  <div className="space-y-1.5 p-2.5 rounded-lg bg-purple-500/5 border border-purple-500/20">
                    <label className="font-semibold text-purple-700 dark:text-purple-300 font-mono text-[10px]">
                      Select Active Lease
                    </label>
                    {liveActiveLeases.length > 0 ? (
                      <select
                        value={inputLeaseId}
                        onChange={(e) => setInputLeaseId(e.target.value)}
                        className="w-full h-9 rounded-md px-3 bg-background dark:bg-[var(--surface-0)] border border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground focus-visible:ring-purple-500/30"
                      >
                        <option value="">-- Choose Active Lease --</option>
                        {liveActiveLeases.map((l) => {
                          const remSec = Math.max(0, Math.round((l.expiryTimeMs - Date.now()) / 1000));
                          return (
                            <option key={l.leaseId} value={l.leaseId}>
                              {l.leaseId} ({Math.round(l.durationMs / 1000)}s TTL, {remSec}s remaining, {l.keys?.length ?? 0} keys)
                            </option>
                          );
                        })}
                      </select>
                    ) : (
                      <div className="text-xs text-muted-foreground py-1">
                        No active leases found.
                      </div>
                    )}
                  </div>
                )}

                {leaseOption === 'custom_ttl' && (
                  <div className="space-y-1.5 p-2.5 rounded-lg bg-amber-500/5 border border-amber-500/20">
                    <label className="font-semibold text-amber-700 dark:text-amber-300 font-mono text-[10px]">
                      TTL Duration String
                    </label>
                    <Input
                      placeholder="e.g. 10s, 30s, 5m, 1h"
                      value={inputTtl}
                      onChange={(e) => setInputTtl(e.target.value)}
                      className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-amber-500/30"
                    />
                  </div>
                )}

                {leaseOption === 'custom_lease' && (
                  <div className="space-y-1.5 p-2.5 rounded-lg bg-indigo-500/5 border border-indigo-500/20">
                    <label className="font-semibold text-indigo-700 dark:text-indigo-300 font-mono text-[10px]">
                      Target Lease Identifier
                    </label>
                    <Input
                      placeholder="e.g. lease-1"
                      value={inputLeaseId}
                      onChange={(e) => setInputLeaseId(e.target.value)}
                      className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-indigo-500/30"
                    />
                  </div>
                )}
              </div>
            )}
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setEditDialogOpen(false)}
              className="border-border dark:border-[oklch(1_0_0/8%)] text-muted-foreground hover:bg-muted text-xs rounded-lg"
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpdateKey}
              disabled={putMutation.isPending || casMutation.isPending}
              className="bg-emerald-600 hover:bg-emerald-700 dark:bg-emerald-500 dark:hover:bg-emerald-600 text-white font-semibold text-xs rounded-lg border-0"
            >
              {isCasMode ? 'CAS Update' : 'Save Changes'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Key Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="bg-card dark:bg-[var(--surface-2)] backdrop-blur-2xl border-border dark:border-[oklch(1_0_0/8%)] text-foreground max-w-md shadow-2xl shadow-black/50 rounded-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-rose-500 dark:text-rose-400">
              <Trash2 className="h-5 w-5" /> Delete Key
            </DialogTitle>
            <DialogDescription className="text-muted-foreground text-xs">
              Are you sure you want to delete <span className="font-mono font-medium text-foreground">{selectedKey?.key}</span>?
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-mono break-all">
              {errorMsg}
            </div>
          )}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
              className="border-border dark:border-[oklch(1_0_0/8%)] text-muted-foreground hover:bg-muted text-xs rounded-lg"
            >
              Cancel
            </Button>
            <Button
              onClick={handleDeleteKey}
              disabled={deleteMutation.isPending}
              className="bg-rose-600 hover:bg-rose-700 text-white font-semibold text-xs rounded-lg border-0"
            >
              {deleteMutation.isPending ? 'Deleting...' : 'Delete Key'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Compare-And-Set (CAS) Workbench Modal */}
      <CasModal
        open={casDialogOpen}
        onOpenChange={setCasDialogOpen}
        initialKey={casSelectedKey}
        onSuccess={() => refetch()}
      />
    </div>
  );
}
