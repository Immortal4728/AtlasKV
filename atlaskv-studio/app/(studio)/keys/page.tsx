'use client';

import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Search,
  Filter,
  Plus,
  Edit2,
  Trash2,
  KeyRound,
  RefreshCw,
  Clock,
  ChevronLeft,
  ChevronRight,
  Copy,
  Check,
  Star,
  FileJson,
  X,
  Database,
} from 'lucide-react';
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
import { PageHeader } from '@/components/ui/page-header';
import { usePrefix, usePutValue, useCasPutValue, useDeleteValue } from '@/hooks/use-kv';
import { ConflictError } from '@/services/api';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

const rowVariant = {
  initial: { opacity: 0, x: -8 },
  animate: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: 8 },
};

export default function KeysPage() {
  const [prefixFilter, setPrefixFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [offset, setOffset] = useState(0);
  const limit = 50;

  // Favorite keys in localStorage
  const [favorites, setFavorites] = useState<string[]>([]);
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('atlaskv-fav-keys');
      if (saved) {
        try {
          setFavorites(JSON.parse(saved));
        } catch {}
      }
    }
  }, []);

  const toggleFavorite = (key: string) => {
    let next: string[];
    if (favorites.includes(key)) {
      next = favorites.filter((k) => k !== key);
      toast.info(`Removed '${key}' from favorites`);
    } else {
      next = [...favorites, key];
      toast.success(`Added '${key}' to favorites`);
    }
    setFavorites(next);
    if (typeof window !== 'undefined') {
      localStorage.setItem('atlaskv-fav-keys', JSON.stringify(next));
    }
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(text);
    toast.success(`Copied ${label} to clipboard`);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  // Real backend prefix query hook
  const { data: prefixData, isLoading, refetch } = usePrefix(prefixFilter, offset, limit);

  // Mutations
  const putMutation = usePutValue();
  const casMutation = useCasPutValue();
  const deleteMutation = useDeleteValue();

  // Dialog States
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedKey, setSelectedKey] = useState<any>(null);

  // Form input states
  const [inputKey, setInputKey] = useState('');
  const [inputValue, setInputValue] = useState('');
  const [inputTtl, setInputTtl] = useState('');
  const [inputLeaseId, setInputLeaseId] = useState('');
  const [isCasMode, setIsCasMode] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const entries = prefixData?.entries ?? [];
  const filteredEntries = entries.filter((e) =>
    e.key.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (e.value && e.value.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const handleCreateKey = async () => {
    setErrorMsg(null);
    try {
      await putMutation.mutateAsync({
        key: inputKey,
        value: inputValue,
        ttl: inputTtl || undefined,
        leaseId: inputLeaseId || undefined,
      });
      toast.success(`Successfully created key '${inputKey}'`);
      setCreateDialogOpen(false);
      refetch();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to create key');
      toast.error(`Error creating key: ${err.message}`);
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
        await putMutation.mutateAsync({
          key: selectedKey.key,
          value: inputValue,
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
      toast.success(`Deleted key '${selectedKey.key}'`);
      setDeleteDialogOpen(false);
      refetch();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to delete key');
      toast.error(`Delete failed: ${err.message}`);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Key-Value Explorer"
        description="Browse, create, update, and manage key-value pairs stored in AtlasKV"
        icon={KeyRound}
        iconColor="text-emerald-400"
        actions={
          <>
            <Button
              onClick={() => {
                refetch();
                toast.info('Refreshed key store entries');
              }}
              variant="outline"
              className="border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/50%)] hover:bg-[oklch(1_0_0/4%)] hover:text-white text-xs gap-1.5 rounded-lg"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>

            <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
              <Button
                onClick={() => {
                  setInputKey('');
                  setInputValue('');
                  setInputTtl('');
                  setInputLeaseId('');
                  setErrorMsg(null);
                  setCreateDialogOpen(true);
                }}
                className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-semibold text-xs px-4 py-2 shadow-lg shadow-emerald-500/20 gap-1.5 rounded-lg border-0"
              >
                <Plus className="h-4 w-4" />
                Create Key
              </Button>
            </motion.div>
          </>
        }
      />

      {/* Filter and Search Bar */}
      <motion.div
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.1 }}
        className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 glass-card rounded-xl p-3"
      >
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[oklch(1_0_0/25%)]" />
          <Input
            placeholder="Search keys or values..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 pr-8 bg-[var(--surface-0)] border-[oklch(1_0_0/8%)] text-xs text-[oklch(1_0_0/80%)] placeholder:text-[oklch(1_0_0/25%)] focus-visible:ring-emerald-500/30 rounded-lg"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[oklch(1_0_0/30%)] hover:text-[oklch(1_0_0/60%)] transition-colors"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 bg-[var(--surface-0)] p-1 rounded-lg border border-[oklch(1_0_0/6%)] text-xs">
            <Filter className="h-3.5 w-3.5 text-[oklch(1_0_0/25%)] ml-1.5" />
            {['', 'app/', 'session/', 'cache/'].map((p) => (
              <button
                key={p || 'all'}
                onClick={() => setPrefixFilter(p)}
                className={cn(
                  'px-2.5 py-1 rounded-md font-medium text-[11px] transition-all duration-200',
                  prefixFilter === p
                    ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20 shadow-sm'
                    : 'text-[oklch(1_0_0/35%)] hover:text-[oklch(1_0_0/60%)] hover:bg-[oklch(1_0_0/4%)]'
                )}
              >
                {p || 'ALL'}
              </button>
            ))}
          </div>
        </div>
      </motion.div>

      {/* Table */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, delay: 0.15 }}
        className="glass-card rounded-xl overflow-hidden p-0"
      >
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[var(--surface-0)]/60 border-b border-[oklch(1_0_0/6%)] text-[oklch(1_0_0/28%)] uppercase tracking-[0.1em] text-[10px] font-mono">
              <tr>
                <th className="py-3 px-4 w-10">Fav</th>
                <th className="py-3 px-4">Key</th>
                <th className="py-3 px-4">Value</th>
                <th className="py-3 px-4">Version</th>
                <th className="py-3 px-4">Lease / TTL</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[oklch(1_0_0/4%)] text-[oklch(1_0_0/60%)] font-mono">
              {isLoading ? (
                [1, 2, 3, 4].map((i) => (
                  <tr key={i}>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-4 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-32 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-48 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-12 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-20 rounded" /></td>
                    <td className="py-3.5 px-4 text-right"><div className="skeleton h-4 w-16 rounded ml-auto" /></td>
                  </tr>
                ))
              ) : filteredEntries.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-16 text-center">
                    <div className="flex flex-col items-center gap-3">
                      <div className="h-12 w-12 rounded-xl bg-[oklch(1_0_0/4%)] flex items-center justify-center">
                        <Database className="h-6 w-6 text-[oklch(1_0_0/15%)]" />
                      </div>
                      <p className="text-[oklch(1_0_0/30%)] text-xs">No key-value pairs found in cluster</p>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredEntries.map((item, idx) => {
                  const isFav = favorites.includes(item.key);

                  return (
                    <motion.tr
                      key={item.key}
                      initial={{ opacity: 0, x: -6 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ duration: 0.25, delay: idx * 0.03 }}
                      className="hover:bg-[oklch(1_0_0/2%)] transition-colors group"
                    >
                      <td className="py-3 px-4">
                        <button
                          onClick={() => toggleFavorite(item.key)}
                          className="text-[oklch(1_0_0/20%)] hover:text-amber-400 transition-colors"
                        >
                          <Star className={cn('h-3.5 w-3.5', isFav && 'text-amber-400 fill-amber-400')} />
                        </button>
                      </td>
                      <td className="py-3 px-4 text-emerald-400/90 font-medium flex items-center gap-2">
                        <KeyRound className="h-3.5 w-3.5 text-emerald-500/40 shrink-0" />
                        <span className="truncate max-w-[200px]">{item.key}</span>
                        <button
                          onClick={() => copyToClipboard(item.key, 'Key')}
                          className="opacity-0 group-hover:opacity-100 text-[oklch(1_0_0/25%)] hover:text-[oklch(1_0_0/60%)] transition-all ml-1"
                        >
                          {copiedKey === item.key ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
                        </button>
                      </td>
                      <td className="py-3 px-4 max-w-[280px] truncate text-[oklch(1_0_0/50%)]">
                        {item.value ?? '<null>'}
                      </td>
                      <td className="py-3 px-4">
                        <span className="px-2 py-0.5 rounded-md bg-[oklch(1_0_0/4%)] border border-[oklch(1_0_0/6%)] text-[11px] text-[oklch(1_0_0/45%)]">
                          v{item.version}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        {item.leaseId ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-purple-500/8 text-purple-400/80 border border-purple-500/15 text-[10px]">
                            <Clock className="h-3 w-3" />
                            {item.leaseId}
                          </span>
                        ) : (
                          <span className="text-[oklch(1_0_0/15%)]">—</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => copyToClipboard(item.value || '', 'Value')}
                            className="h-7 w-7 text-[oklch(1_0_0/25%)] hover:text-white hover:bg-[oklch(1_0_0/6%)] rounded-lg"
                            title="Copy Value"
                          >
                            <Copy className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setSelectedKey(item);
                              setInputValue(item.value || '');
                              setIsCasMode(false);
                              setErrorMsg(null);
                              setEditDialogOpen(true);
                            }}
                            className="h-7 w-7 text-[oklch(1_0_0/25%)] hover:text-white hover:bg-[oklch(1_0_0/6%)] rounded-lg"
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
                            className="h-7 w-7 text-rose-400/60 hover:text-rose-300 hover:bg-rose-500/8 rounded-lg"
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
      </motion.div>

      {/* Create Key Dialog */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="bg-[var(--surface-2)] backdrop-blur-2xl border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/85%)] max-w-md shadow-2xl shadow-black/50 rounded-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-400">
              <Plus className="h-5 w-5" /> Create Key-Value
            </DialogTitle>
            <DialogDescription className="text-[oklch(1_0_0/35%)] text-xs">
              Store a key-value entry into the AtlasKV Raft state machine.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/8 border border-rose-500/15 text-rose-400 text-xs font-mono">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="space-y-1.5">
              <label className="font-semibold text-[oklch(1_0_0/55%)] font-mono text-[11px]">Key</label>
              <Input
                placeholder="e.g. app/config/theme"
                value={inputKey}
                onChange={(e) => setInputKey(e.target.value)}
                className="bg-[var(--surface-0)] border-[oklch(1_0_0/8%)] text-xs font-mono text-[oklch(1_0_0/80%)] focus-visible:ring-emerald-500/30"
              />
            </div>
            <div className="space-y-1.5">
              <label className="font-semibold text-[oklch(1_0_0/55%)] font-mono text-[11px]">Value</label>
              <Input
                placeholder="e.g. dark"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                className="bg-[var(--surface-0)] border-[oklch(1_0_0/8%)] text-xs font-mono text-[oklch(1_0_0/80%)] focus-visible:ring-emerald-500/30"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setCreateDialogOpen(false)}
              className="border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/50%)] hover:bg-[oklch(1_0_0/4%)] text-xs rounded-lg"
            >
              Cancel
            </Button>
            <Button
              onClick={handleCreateKey}
              disabled={putMutation.isPending}
              className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-semibold text-xs rounded-lg border-0"
            >
              {putMutation.isPending ? 'Saving...' : 'Save Key'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit / CAS Key Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="bg-[var(--surface-2)] backdrop-blur-2xl border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/85%)] max-w-md shadow-2xl shadow-black/50 rounded-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-400">
              <Edit2 className="h-5 w-5" /> Edit Key: {selectedKey?.key}
            </DialogTitle>
            <DialogDescription className="text-[oklch(1_0_0/35%)] text-xs">
              Perform a direct update or Compare-And-Swap (CAS) transaction.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/8 border border-rose-500/15 text-rose-400 text-xs font-mono">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="flex items-center justify-between p-2.5 rounded-lg bg-[var(--surface-0)] border border-[oklch(1_0_0/8%)]">
              <span className="font-mono text-[oklch(1_0_0/50%)]">Expected Version: v{selectedKey?.version}</span>
              <label className="flex items-center gap-2 font-mono text-xs text-emerald-400 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isCasMode}
                  onChange={(e) => setIsCasMode(e.target.checked)}
                  className="rounded bg-[var(--surface-0)] border-[oklch(1_0_0/15%)] text-emerald-500"
                />
                Atomic CAS
              </label>
            </div>

            <div className="space-y-1.5">
              <label className="font-semibold text-[oklch(1_0_0/55%)] font-mono text-[11px]">New Value</label>
              <Input
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                className="bg-[var(--surface-0)] border-[oklch(1_0_0/8%)] text-xs font-mono text-[oklch(1_0_0/80%)] focus-visible:ring-emerald-500/30"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setEditDialogOpen(false)}
              className="border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/50%)] hover:bg-[oklch(1_0_0/4%)] text-xs rounded-lg"
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpdateKey}
              disabled={putMutation.isPending || casMutation.isPending}
              className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-semibold text-xs rounded-lg border-0"
            >
              {isCasMode ? 'CAS Update' : 'Update Value'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Key Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="bg-[var(--surface-2)] backdrop-blur-2xl border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/85%)] max-w-md shadow-2xl shadow-black/50 rounded-xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-rose-400">
              <Trash2 className="h-5 w-5" /> Delete Key
            </DialogTitle>
            <DialogDescription className="text-[oklch(1_0_0/35%)] text-xs">
              Are you sure you want to delete <span className="font-mono text-[oklch(1_0_0/65%)]">{selectedKey?.key}</span>?
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/8 border border-rose-500/15 text-rose-400 text-xs font-mono">
              {errorMsg}
            </div>
          )}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
              className="border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/50%)] hover:bg-[oklch(1_0_0/4%)] text-xs rounded-lg"
            >
              Cancel
            </Button>
            <Button
              onClick={handleDeleteKey}
              disabled={deleteMutation.isPending}
              className="bg-rose-500 hover:bg-rose-600 text-white font-semibold text-xs rounded-lg"
            >
              Confirm Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
