'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
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
import { usePrefix, usePutValue, useCasPutValue, useDeleteValue } from '@/hooks/use-kv';
import { ConflictError } from '@/services/api';
import { toast } from 'sonner';

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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <KeyRound className="h-5 w-5 text-emerald-400" />
            Key-Value Explorer
          </h1>
          <p className="text-xs text-zinc-400 mt-1">
            Browse, create, update, and manage key-value pairs stored in AtlasKV
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            onClick={() => {
              refetch();
              toast.info('Refreshed key store entries');
            }}
            variant="outline"
            className="border-white/10 text-zinc-300 hover:bg-white/5 text-xs gap-1.5"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </Button>

          <Button
            onClick={() => {
              setInputKey('');
              setInputValue('');
              setInputTtl('');
              setInputLeaseId('');
              setErrorMsg(null);
              setCreateDialogOpen(true);
            }}
            className="bg-emerald-500 hover:bg-emerald-600 text-zinc-950 font-semibold text-xs px-3.5 py-2 shadow-lg shadow-emerald-500/20 gap-1.5"
          >
            <Plus className="h-4 w-4" />
            Create Key
          </Button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-zinc-900/60 p-3 rounded-xl border border-white/[0.08] backdrop-blur-md">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
          <Input
            placeholder="Search keys or values..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 pr-8 bg-zinc-950/50 border-white/10 text-xs text-zinc-200 placeholder:text-zinc-500 focus-visible:ring-emerald-500/50"
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 bg-zinc-950/50 p-1 rounded-lg border border-white/10 text-xs">
            <Filter className="h-3.5 w-3.5 text-zinc-400 ml-1.5" />
            {['', 'app/', 'session/', 'cache/'].map((p) => (
              <button
                key={p || 'all'}
                onClick={() => setPrefixFilter(p)}
                className={`px-2.5 py-1 rounded-md font-medium text-[11px] transition-colors ${
                  prefixFilter === p
                    ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                    : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                {p || 'ALL'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="rounded-xl border border-white/[0.08] bg-zinc-900/40 backdrop-blur-md overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-zinc-950/60 border-b border-white/[0.08] text-zinc-400 uppercase tracking-wider text-[10px] font-mono">
              <tr>
                <th className="py-3 px-4">Fav</th>
                <th className="py-3 px-4">Key</th>
                <th className="py-3 px-4">Value</th>
                <th className="py-3 px-4">Version</th>
                <th className="py-3 px-4">Lease / TTL</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.06] text-zinc-300 font-mono">
              {isLoading ? (
                [1, 2, 3, 4].map((i) => (
                  <tr key={i} className="animate-pulse">
                    <td className="py-3 px-4"><div className="h-4 w-4 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-32 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-48 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-12 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-20 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4 text-right"><div className="h-4 w-16 bg-zinc-800 rounded ml-auto" /></td>
                  </tr>
                ))
              ) : filteredEntries.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-zinc-500">
                    No key-value pairs found in cluster
                  </td>
                </tr>
              ) : (
                filteredEntries.map((item) => {
                  const isFav = favorites.includes(item.key);

                  return (
                    <tr key={item.key} className="hover:bg-white/[0.02] transition-colors group">
                      <td className="py-3 px-4">
                        <button
                          onClick={() => toggleFavorite(item.key)}
                          className="text-zinc-600 hover:text-amber-400 transition-colors"
                        >
                          <Star className={`h-3.5 w-3.5 ${isFav ? 'text-amber-400 fill-amber-400' : ''}`} />
                        </button>
                      </td>
                      <td className="py-3 px-4 text-emerald-400 font-medium flex items-center gap-2">
                        <KeyRound className="h-3.5 w-3.5 text-emerald-500/60 shrink-0" />
                        <span className="truncate max-w-[200px]">{item.key}</span>
                        <button
                          onClick={() => copyToClipboard(item.key, 'Key')}
                          className="opacity-0 group-hover:opacity-100 text-zinc-500 hover:text-zinc-300 transition-opacity ml-1"
                        >
                          {copiedKey === item.key ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
                        </button>
                      </td>
                      <td className="py-3 px-4 max-w-[280px] truncate text-zinc-300">
                        {item.value ?? '<null>'}
                      </td>
                      <td className="py-3 px-4 text-zinc-400">
                        <span className="px-2 py-0.5 rounded bg-zinc-800 border border-zinc-700 text-[11px] text-zinc-300">
                          v{item.version}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-zinc-400">
                        {item.leaseId ? (
                          <Badge variant="outline" className="bg-purple-500/10 text-purple-400 border-purple-500/20 text-[10px]">
                            <Clock className="h-3 w-3 mr-1" />
                            {item.leaseId}
                          </Badge>
                        ) : (
                          <span className="text-zinc-600">—</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => copyToClipboard(item.value || '', 'Value')}
                            className="h-7 w-7 text-zinc-400 hover:text-white hover:bg-white/10"
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
                            className="h-7 w-7 text-zinc-400 hover:text-white hover:bg-white/10"
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
                            className="h-7 w-7 text-rose-400 hover:text-rose-300 hover:bg-rose-500/10"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
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

      {/* Create Key Dialog */}
      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="bg-zinc-900 border-white/10 text-zinc-100 max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-400">
              <Plus className="h-5 w-5" /> Create Key-Value
            </DialogTitle>
            <DialogDescription className="text-zinc-400 text-xs">
              Store a key-value entry into the AtlasKV Raft state machine.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-mono">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="space-y-1.5">
              <label className="font-semibold text-zinc-300 font-mono">Key</label>
              <Input
                placeholder="e.g. app/config/theme"
                value={inputKey}
                onChange={(e) => setInputKey(e.target.value)}
                className="bg-zinc-950 border-white/10 text-xs font-mono"
              />
            </div>
            <div className="space-y-1.5">
              <label className="font-semibold text-zinc-300 font-mono">Value</label>
              <Input
                placeholder="e.g. dark"
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
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
              onClick={handleCreateKey}
              disabled={putMutation.isPending}
              className="bg-emerald-500 hover:bg-emerald-600 text-zinc-950 font-semibold text-xs"
            >
              {putMutation.isPending ? 'Saving...' : 'Save Key'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit / CAS Key Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="bg-zinc-900 border-white/10 text-zinc-100 max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-emerald-400">
              <Edit2 className="h-5 w-5" /> Edit Key: {selectedKey?.key}
            </DialogTitle>
            <DialogDescription className="text-zinc-400 text-xs">
              Perform a direct update or Compare-And-Swap (CAS) transaction.
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-mono">
              {errorMsg}
            </div>
          )}

          <div className="space-y-4 py-2 text-xs">
            <div className="flex items-center justify-between p-2.5 rounded-lg bg-zinc-950 border border-white/10">
              <span className="font-mono text-zinc-300">Expected Version: v{selectedKey?.version}</span>
              <label className="flex items-center gap-2 font-mono text-xs text-emerald-400 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isCasMode}
                  onChange={(e) => setIsCasMode(e.target.checked)}
                  className="rounded bg-zinc-900 border-white/20 text-emerald-500"
                />
                Atomic CAS
              </label>
            </div>

            <div className="space-y-1.5">
              <label className="font-semibold text-zinc-300 font-mono">New Value</label>
              <Input
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                className="bg-zinc-950 border-white/10 text-xs font-mono"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setEditDialogOpen(false)}
              className="border-white/10 text-zinc-300 hover:bg-white/5 text-xs"
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpdateKey}
              disabled={putMutation.isPending || casMutation.isPending}
              className="bg-emerald-500 hover:bg-emerald-600 text-zinc-950 font-semibold text-xs"
            >
              {isCasMode ? 'CAS Update' : 'Update Value'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Key Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="bg-zinc-900 border-white/10 text-zinc-100 max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-rose-400">
              <Trash2 className="h-5 w-5" /> Delete Key
            </DialogTitle>
            <DialogDescription className="text-zinc-400 text-xs">
              Are you sure you want to delete <span className="font-mono text-zinc-200">{selectedKey?.key}</span>?
            </DialogDescription>
          </DialogHeader>

          {errorMsg && (
            <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-mono">
              {errorMsg}
            </div>
          )}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteDialogOpen(false)}
              className="border-white/10 text-zinc-300 hover:bg-white/5 text-xs"
            >
              Cancel
            </Button>
            <Button
              onClick={handleDeleteKey}
              disabled={deleteMutation.isPending}
              className="bg-rose-500 hover:bg-rose-600 text-white font-semibold text-xs"
            >
              Confirm Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
