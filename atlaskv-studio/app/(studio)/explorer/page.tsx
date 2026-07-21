'use client';

import { useState, useEffect, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  Database,
  Search,
  Plus,
  Trash2,
  Edit,
  Eye,
  Key,
  Info,
  Check,
  AlertCircle,
  FileJson,
} from 'lucide-react';
import * as api from '@/services/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';

interface KeyMetadata {
  sizeBytes: number;
  isJson: boolean;
  type: string;
}

const DEFAULT_KEYS = ['cluster:name', 'node:id', 'system:status'];

export default function ExplorerPage() {
  const queryClient = useQueryClient();

  // Local storage registry for tracked keys
  const [trackedKeys, setTrackedKeys] = useState<string[]>([]);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [keyValue, setKeyValue] = useState<string | null>(null);
  const [keyFound, setKeyFound] = useState<boolean>(false);
  const [keyLoading, setKeyLoading] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);
  const [modalKey, setModalKey] = useState('');
  const [modalValue, setModalValue] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Load tracked keys from localStorage on mount
  useEffect(() => {
    const saved = localStorage.getItem('atlaskv-tracked-keys');
    if (saved) {
      try {
        setTrackedKeys(JSON.parse(saved));
      } catch (e) {
        setTrackedKeys(DEFAULT_KEYS);
      }
    } else {
      setTrackedKeys(DEFAULT_KEYS);
      localStorage.setItem('atlaskv-tracked-keys', JSON.stringify(DEFAULT_KEYS));
    }
  }, []);

  // Save tracked keys to localStorage
  const saveTrackedKeys = (keys: string[]) => {
    setTrackedKeys(keys);
    localStorage.setItem('atlaskv-tracked-keys', JSON.stringify(keys));
  };

  // Fetch detail of selected key
  const fetchKeyDetails = async (key: string) => {
    setKeyLoading(true);
    try {
      const res = await api.getValue(key);
      setKeyValue(res.value);
      setKeyFound(res.found);
    } catch (e) {
      setKeyValue(null);
      setKeyFound(false);
    } finally {
      setKeyLoading(false);
    }
  };

  useEffect(() => {
    if (selectedKey) {
      fetchKeyDetails(selectedKey);
    } else {
      setKeyValue(null);
      setKeyFound(false);
    }
  }, [selectedKey]);

  // Compute key list filtered by search query
  const filteredKeys = useMemo(() => {
    return trackedKeys.filter((k) =>
      k.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [trackedKeys, searchQuery]);

  // Compute metadata of the selected key value
  const keyMetadata = useMemo<KeyMetadata | null>(() => {
    if (!selectedKey || !keyFound || keyValue === null) return null;

    let isJson = false;
    try {
      JSON.parse(keyValue);
      isJson = true;
    } catch (e) {}

    return {
      sizeBytes: new Blob([keyValue]).size,
      isJson,
      type: isJson ? 'JSON' : 'Plain Text',
    };
  }, [selectedKey, keyFound, keyValue]);

  // Handle Write (Create or Update)
  const handleWriteKey = async () => {
    if (!modalKey.trim()) {
      setErrorMsg('Key name cannot be empty');
      return;
    }
    setErrorMsg('');
    try {
      await api.putValue(modalKey.trim(), modalValue);

      // Add to tracked keys list if not already present
      if (!trackedKeys.includes(modalKey.trim())) {
        saveTrackedKeys([...trackedKeys, modalKey.trim()]);
      }

      setIsCreateOpen(false);
      setSelectedKey(modalKey.trim());
      fetchKeyDetails(modalKey.trim());

      // Trigger metric queries refresh
      queryClient.invalidateQueries({ queryKey: ['cluster', 'metrics'] });
    } catch (e: any) {
      setErrorMsg(e.message || 'Failed to write key to AtlasKV');
    }
  };

  // Handle Delete
  const handleDeleteKey = async () => {
    if (!selectedKey) return;
    try {
      await api.deleteValue(selectedKey);

      // Remove from tracked keys list
      const updated = trackedKeys.filter((k) => k !== selectedKey);
      saveTrackedKeys(updated);

      setSelectedKey(null);
      setIsDeleteOpen(false);

      // Trigger metric queries refresh
      queryClient.invalidateQueries({ queryKey: ['cluster', 'metrics'] });
    } catch (e: any) {
      alert(e.message || 'Failed to delete key from AtlasKV');
    }
  };

  // Handle Query arbitrary custom key directly
  const handleQueryCustomKey = async (key: string) => {
    if (!key.trim()) return;
    setKeyLoading(true);
    try {
      const res = await api.getValue(key.trim());
      if (res.found) {
        if (!trackedKeys.includes(key.trim())) {
          saveTrackedKeys([...trackedKeys, key.trim()]);
        }
        setSelectedKey(key.trim());
      } else {
        alert(`Key "${key.trim()}" not found in AtlasKV backend.`);
      }
    } catch (e: any) {
      alert(e.message || 'Error querying key');
    } finally {
      setKeyLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-white/90">
            Key Explorer
          </h1>
          <p className="text-sm text-white/30 mt-0.5">
            Manage and query key-value pairs stored in AtlasKV
          </p>
        </div>
        <Button
          onClick={() => {
            setModalKey('');
            setModalValue('');
            setErrorMsg('');
            setIsCreateOpen(true);
          }}
          className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-xs h-9 px-4 gap-1.5"
        >
          <Plus className="h-4 w-4" />
          Create Key
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 h-[calc(100vh-200px)]">
        {/* Left column: Key registry/search */}
        <Card className="lg:col-span-1 border-white/[0.06] bg-[#111113] flex flex-col overflow-hidden">
          <div className="p-4 border-b border-white/[0.06] space-y-3">
            {/* Search Input */}
            <div className="relative">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-white/20" />
              <Input
                placeholder="Filter tracked keys..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 bg-white/[0.02] border-white/[0.08] text-xs h-9 text-white/80 placeholder:text-white/20 focus-visible:ring-emerald-500/30"
              />
            </div>

            {/* Direct Query Input */}
            <div className="flex gap-2">
              <Input
                placeholder="Query arbitrary key..."
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    handleQueryCustomKey(e.currentTarget.value);
                    e.currentTarget.value = '';
                  }
                }}
                className="bg-white/[0.02] border-white/[0.08] text-xs h-8 text-white/80 placeholder:text-white/20"
              />
            </div>
          </div>

          {/* Keys list */}
          <div className="flex-1 overflow-y-auto divide-y divide-white/[0.04]">
            {filteredKeys.length === 0 ? (
              <div className="flex flex-col items-center justify-center p-8 text-center h-full">
                <Database className="h-8 w-8 text-white/10 mb-2" />
                <span className="text-xs text-white/30">No keys tracked yet</span>
              </div>
            ) : (
              filteredKeys.map((key) => {
                const isActive = selectedKey === key;
                return (
                  <button
                    key={key}
                    onClick={() => setSelectedKey(key)}
                    className={`w-full flex items-center justify-between p-3.5 text-left transition-colors ${
                      isActive
                        ? 'bg-white/[0.06] text-white'
                        : 'text-white/50 hover:bg-white/[0.02] hover:text-white/80'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 min-w-0">
                      <Key className={`h-3.5 w-3.5 shrink-0 ${isActive ? 'text-emerald-400' : 'text-white/20'}`} />
                      <span className="font-mono text-xs truncate">{key}</span>
                    </div>
                    {isActive && (
                      <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500/50" />
                    )}
                  </button>
                );
              })
            )}
          </div>
        </Card>

        {/* Right column: Value/Metadata view */}
        <Card className="lg:col-span-2 border-white/[0.06] bg-[#111113] flex flex-col overflow-hidden">
          {selectedKey ? (
            <div className="flex flex-col h-full">
              {/* Toolbar */}
              <div className="flex items-center justify-between px-5 h-14 border-b border-white/[0.06] shrink-0">
                <div className="flex items-center gap-2 min-w-0">
                  <span className="text-xs font-semibold text-white/30 uppercase tracking-wider">
                    Key Details:
                  </span>
                  <span className="font-mono text-xs text-white/90 truncate max-w-[240px]">
                    {selectedKey}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setModalKey(selectedKey);
                      setModalValue(keyValue || '');
                      setErrorMsg('');
                      setIsCreateOpen(true);
                    }}
                    className="h-8 text-white/50 hover:text-white hover:bg-white/[0.04] text-xs gap-1.5"
                  >
                    <Edit className="h-3.5 w-3.5" />
                    Edit
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setIsDeleteOpen(true)}
                    className="h-8 text-rose-400/80 hover:text-rose-400 hover:bg-rose-500/10 text-xs gap-1.5"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                    Delete
                  </Button>
                </div>
              </div>

              {/* Inspector Content */}
              {keyLoading ? (
                <div className="flex-1 flex items-center justify-center">
                  <span className="text-xs text-white/30">Loading value...</span>
                </div>
              ) : (
                <div className="flex-1 overflow-y-auto p-5 space-y-6">
                  {/* Metadata cards */}
                  <div className="grid grid-cols-3 gap-4">
                    <div className="border border-white/[0.04] bg-white/[0.01] rounded-lg p-3 space-y-1">
                      <span className="text-[10px] font-semibold uppercase tracking-wider text-white/20 block">
                        Status
                      </span>
                      {keyFound ? (
                        <Badge className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px] font-medium py-0.5 px-2">
                          Active
                        </Badge>
                      ) : (
                        <Badge className="bg-rose-500/10 text-rose-400 border border-rose-500/20 text-[10px] font-medium py-0.5 px-2">
                          Not Found
                        </Badge>
                      )}
                    </div>

                    <div className="border border-white/[0.04] bg-white/[0.01] rounded-lg p-3 space-y-1">
                      <span className="text-[10px] font-semibold uppercase tracking-wider text-white/20 block">
                        Data Type
                      </span>
                      <span className="text-xs font-semibold text-white/70">
                        {keyMetadata?.type || '—'}
                      </span>
                    </div>

                    <div className="border border-white/[0.04] bg-white/[0.01] rounded-lg p-3 space-y-1">
                      <span className="text-[10px] font-semibold uppercase tracking-wider text-white/20 block">
                        Value Size
                      </span>
                      <span className="text-xs font-semibold text-white/70">
                        {keyMetadata?.sizeBytes !== undefined ? `${keyMetadata.sizeBytes} B` : '—'}
                      </span>
                    </div>
                  </div>

                  {/* Value Inspector block */}
                  <div className="space-y-2">
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-semibold uppercase tracking-wider text-white/35">
                        Value Output
                      </span>
                      {keyMetadata?.isJson && (
                        <Badge className="gap-1 bg-purple-500/10 text-purple-400 border border-purple-500/20 text-[9px] font-semibold tracking-wider uppercase py-0.5 px-1.5">
                          <FileJson className="h-3.5 w-3.5" />
                          Formatted JSON
                        </Badge>
                      )}
                    </div>

                    <div className="border border-white/[0.06] bg-black/40 rounded-lg p-4 font-mono text-xs overflow-x-auto max-h-[300px] text-white/80">
                      {keyFound ? (
                        keyMetadata?.isJson ? (
                          <pre className="text-emerald-400/90">
                            {JSON.stringify(JSON.parse(keyValue || ''), null, 2)}
                          </pre>
                        ) : (
                          <pre className="whitespace-pre-wrap">{keyValue}</pre>
                        )
                      ) : (
                        <span className="text-white/25 italic">
                          Key not present on node or has been deleted.
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-center p-8">
              <Info className="h-8 w-8 text-white/10 mb-2" />
              <span className="text-sm font-medium text-white/60">No key selected</span>
              <span className="text-xs text-white/30 mt-1 max-w-[280px]">
                Select a key from the registry list or enter a custom key search to inspect its contents.
              </span>
            </div>
          )}
        </Card>
      </div>

      {/* CREATE / EDIT DIALOG */}
      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent className="bg-[#111113] border border-white/[0.08] text-white max-w-md rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-sm font-semibold text-white/90">
              {selectedKey && modalKey === selectedKey ? 'Edit Key-Value Pair' : 'Create Key-Value Pair'}
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-3 text-xs">
            <div className="space-y-1.5">
              <label className="text-white/40 font-medium">Key Name</label>
              <Input
                placeholder="e.g. user:profile"
                value={modalKey}
                onChange={(e) => setModalKey(e.target.value)}
                disabled={!!(selectedKey && modalKey === selectedKey)}
                className="bg-white/[0.02] border-white/[0.08] text-xs h-9 text-white/80 focus-visible:ring-emerald-500/30"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-white/40 font-medium">Value</label>
              <textarea
                placeholder="Enter string value or JSON payload..."
                value={modalValue}
                onChange={(e) => setModalValue(e.target.value)}
                rows={5}
                className="w-full rounded-md border border-white/[0.08] bg-white/[0.02] px-3 py-2 text-xs text-white/80 placeholder:text-white/20 focus:outline-none focus:ring-1 focus:ring-emerald-500/30"
              />
            </div>

            {errorMsg && (
              <div className="flex items-center gap-2 text-rose-400 bg-rose-500/10 border border-rose-500/20 p-2.5 rounded-lg">
                <AlertCircle className="h-4 w-4 shrink-0" />
                <span className="leading-normal">{errorMsg}</span>
              </div>
            )}
          </div>

          <DialogFooter className="gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsCreateOpen(false)}
              className="text-white/40 hover:text-white hover:bg-white/[0.04]"
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleWriteKey}
              className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium"
            >
              Save Key
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* DELETE CONFIRMATION DIALOG */}
      <Dialog open={isDeleteOpen} onOpenChange={setIsDeleteOpen}>
        <DialogContent className="bg-[#111113] border border-white/[0.08] text-white max-w-sm rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-sm font-semibold text-white/90">
              Confirm Delete
            </DialogTitle>
          </DialogHeader>

          <div className="py-3 text-xs text-white/60 space-y-2">
            <p>
              Are you sure you want to delete the key <span className="font-mono text-emerald-400 font-semibold">{selectedKey}</span>?
            </p>
            <p className="text-[11px] text-rose-400/80 bg-rose-500/10 border border-rose-500/20 p-2 rounded-lg leading-normal">
              This action routes a DELETE command to the Raft cluster consensus engine and cannot be undone.
            </p>
          </div>

          <DialogFooter className="gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setIsDeleteOpen(false)}
              className="text-white/40 hover:text-white hover:bg-white/[0.04]"
            >
              Cancel
            </Button>
            <Button
              size="sm"
              onClick={handleDeleteKey}
              className="bg-rose-600 hover:bg-rose-700 text-white font-medium"
            >
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
