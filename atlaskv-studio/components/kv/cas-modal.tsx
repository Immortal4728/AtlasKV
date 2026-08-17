'use client';

import { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  GitCompare,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Clock,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  ShieldAlert,
  Server,
  Layers,
  Check,
  RotateCcw,
} from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useKey, usePrefix, useCasPutValue } from '@/hooks/use-kv';
import { ConflictError } from '@/services/api';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

interface CasModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialKey?: string;
  onSuccess?: (key: string) => void;
}

interface CasOutcome {
  type: 'success' | 'conflict' | 'error';
  message: string;
  expectedVersion?: number;
  currentVersion?: number;
  newVersion?: number;
  newValue?: string;
}

export function CasModal({
  open,
  onOpenChange,
  initialKey = '',
  onSuccess,
}: CasModalProps) {
  const [selectedKey, setSelectedKey] = useState(initialKey);
  const [expectedVersion, setExpectedVersion] = useState<string>('');
  const [newValue, setNewValue] = useState<string>('');
  const [outcome, setOutcome] = useState<CasOutcome | null>(null);

  // Fetch live cluster keys for quick autocomplete chips
  const { data: prefixData } = usePrefix('', 0, 100);
  const availableKeys = useMemo(() => {
    const live = prefixData?.entries?.map((e) => e.key) ?? [];
    return Array.from(new Set(live));
  }, [prefixData]);

  // Live query for the selected key state
  const {
    data: keyData,
    isLoading: isLoadingKey,
    isFetching: isFetchingKey,
    refetch: refetchKey,
  } = useKey(selectedKey, open && !!selectedKey.trim());

  const casMutation = useCasPutValue();

  // Sync initial key when modal opens or prop changes
  useEffect(() => {
    if (open) {
      if (initialKey) {
        setSelectedKey(initialKey);
      }
      setOutcome(null);
    }
  }, [open, initialKey]);

  // Auto-fill expected version when key data resolves if user hasn't set custom
  useEffect(() => {
    if (keyData && keyData.found && keyData.version !== undefined && keyData.version !== null) {
      setExpectedVersion(String(keyData.version));
      if (!newValue && keyData.value) {
        setNewValue(keyData.value + '_cas');
      }
    } else if (keyData && !keyData.found) {
      setExpectedVersion('0');
    }
  }, [keyData]);

  const handleSelectKey = (k: string) => {
    setSelectedKey(k);
    setOutcome(null);
    setExpectedVersion('');
  };

  const handleExecuteCas = async () => {
    if (!selectedKey.trim()) {
      toast.error('Please specify a valid key name');
      return;
    }

    const parsedExpected = parseInt(expectedVersion, 10);
    if (isNaN(parsedExpected) || parsedExpected < 0) {
      toast.error('Expected version must be a non-negative integer (0, 1, 2...)');
      return;
    }

    setOutcome(null);

    try {
      const res = await casMutation.mutateAsync({
        key: selectedKey.trim(),
        value: newValue,
        expectedVersion: parsedExpected,
      });

      const updatedVer = res.version ?? (parsedExpected + 1);
      setOutcome({
        type: 'success',
        message: `CAS update committed via consensus! Version advanced from v${parsedExpected} → v${updatedVer}.`,
        expectedVersion: parsedExpected,
        newVersion: updatedVer,
        newValue: res.value ?? newValue,
      });

      // Update expected version to new version for immediate follow-up CAS
      setExpectedVersion(String(updatedVer));
      toast.success(`CAS succeeded for key '${selectedKey.trim()}' (now v${updatedVer})`);

      // Refresh live state
      await refetchKey();
      onSuccess?.(selectedKey.trim());
    } catch (err: any) {
      await refetchKey();

      if (err instanceof ConflictError) {
        setOutcome({
          type: 'conflict',
          message: `CAS rejected: Version mismatch! Expected v${err.expectedVersion}, but key '${selectedKey.trim()}' is currently at v${err.currentVersion}.`,
          expectedVersion: err.expectedVersion,
          currentVersion: err.currentVersion,
        });
        toast.error(`CAS Conflict on '${selectedKey.trim()}': expected v${err.expectedVersion}, server is v${err.currentVersion}`);
      } else {
        const msg = err?.message || 'Compare-And-Set operation failed';
        setOutcome({
          type: 'error',
          message: msg,
        });
        toast.error(`CAS failed: ${msg}`);
      }
    }
  };

  const handleSyncExpectedVersion = () => {
    if (outcome?.currentVersion !== undefined) {
      setExpectedVersion(String(outcome.currentVersion));
      setOutcome(null);
    } else if (keyData?.version !== undefined && keyData.version !== null) {
      setExpectedVersion(String(keyData.version));
      setOutcome(null);
    }
  };

  const currentVer = keyData?.version;
  const isKeyFound = keyData?.found;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="bg-card dark:bg-[var(--surface-2)] backdrop-blur-2xl border-border dark:border-[oklch(1_0_0/10%)] text-foreground max-w-xl shadow-2xl shadow-black/50 rounded-2xl p-6">
        <DialogHeader className="space-y-1">
          <DialogTitle className="flex items-center gap-2 text-indigo-600 dark:text-indigo-400 font-bold text-base">
            <GitCompare className="h-5 w-5 text-indigo-500" />
            Compare-And-Set (CAS) Workbench
          </DialogTitle>
          <DialogDescription className="text-muted-foreground text-xs font-mono">
            Execute atomic conditional updates guarded by Raft state-machine version checks.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-2 text-xs">
          {/* Step 1: Target Key Input & Quick Chips */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="font-semibold text-foreground/80 font-mono text-[11px] flex items-center gap-1.5">
                <Layers className="h-3.5 w-3.5 text-indigo-400" />
                Target Key Name *
              </label>
              {selectedKey && (
                <button
                  type="button"
                  onClick={() => refetchKey()}
                  disabled={isFetchingKey}
                  className="text-[10px] font-mono text-muted-foreground hover:text-indigo-500 flex items-center gap-1 transition-colors"
                >
                  <RefreshCw className={cn('h-3 w-3', isFetchingKey && 'animate-spin')} />
                  Refresh Key
                </button>
              )}
            </div>

            <Input
              placeholder="e.g. app/config/theme or test/lease-key"
              value={selectedKey}
              onChange={(e) => {
                setSelectedKey(e.target.value);
                setOutcome(null);
              }}
              className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-indigo-500/30 rounded-lg"
            />

            {/* Quick Suggestions */}
            {availableKeys.length > 0 && (
              <div className="flex flex-wrap items-center gap-1 pt-1">
                <span className="text-[10px] text-muted-foreground font-mono mr-1">Cluster Keys:</span>
                {availableKeys.slice(0, 5).map((k) => (
                  <button
                    key={k}
                    type="button"
                    onClick={() => handleSelectKey(k)}
                    className={cn(
                      'px-2 py-0.5 rounded text-[10px] font-mono border transition-all',
                      selectedKey === k
                        ? 'bg-indigo-500/20 text-indigo-700 dark:text-indigo-300 border-indigo-500/40 font-bold'
                        : 'bg-muted/40 text-muted-foreground border-border hover:border-indigo-500/30 hover:text-foreground'
                    )}
                  >
                    {k}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Step 2: Live Server State Inspection Card */}
          {selectedKey.trim() && (
            <motion.div
              initial={{ opacity: 0, y: 4 }}
              animate={{ opacity: 1, y: 0 }}
              className="p-3.5 rounded-xl bg-[var(--surface-1)] border border-border dark:border-[oklch(1_0_0/8%)] space-y-2 font-mono"
            >
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-foreground/90 flex items-center gap-1.5">
                  <Server className="h-3.5 w-3.5 text-emerald-500" />
                  Live Server State
                </span>
                {isLoadingKey ? (
                  <span className="text-[10px] text-muted-foreground animate-pulse">Querying consensus...</span>
                ) : isKeyFound ? (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border border-emerald-500/30 text-[10px] font-bold">
                    <ShieldCheck className="h-3 w-3" />
                    FOUND · v{currentVer ?? 1}
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded bg-amber-500/15 text-amber-700 dark:text-amber-300 border border-amber-500/30 text-[10px] font-bold">
                    <AlertTriangle className="h-3 w-3" />
                    NOT FOUND (Unset)
                  </span>
                )}
              </div>

              <div className="grid grid-cols-2 gap-2 text-[11px] pt-1 border-t border-border dark:border-[oklch(1_0_0/6%)]">
                <div>
                  <span className="text-muted-foreground text-[10px]">Current Version:</span>
                  <div className="font-bold text-foreground">
                    {currentVer !== undefined && currentVer !== null ? `v${currentVer}` : 'None (v0)'}
                  </div>
                </div>
                <div>
                  <span className="text-muted-foreground text-[10px]">Current Value:</span>
                  <div className="text-emerald-700 dark:text-emerald-400 truncate max-w-full font-bold">
                    {keyData?.value !== null && keyData?.value !== undefined ? `"${keyData.value}"` : 'null'}
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {/* Step 3: CAS Parameters (Expected Version & New Value) */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
            {/* Expected Version Input */}
            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px] flex items-center justify-between">
                <span>Expected Version *</span>
                {currentVer !== undefined && currentVer !== null && (
                  <span className="text-[10px] text-muted-foreground">Server has v{currentVer}</span>
                )}
              </label>
              <Input
                type="number"
                min="0"
                placeholder="e.g. 1"
                value={expectedVersion}
                onChange={(e) => setExpectedVersion(e.target.value)}
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground focus-visible:ring-indigo-500/30 rounded-lg"
              />

              {/* Version Quick Presets */}
              <div className="flex items-center gap-1.5 pt-0.5">
                {typeof currentVer === 'number' && (
                  <button
                    type="button"
                    onClick={() => setExpectedVersion(String(currentVer))}
                    className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-700 dark:text-emerald-300 border border-emerald-500/20 hover:bg-emerald-500/20 transition-colors"
                    title="Set expected version to match current server version"
                  >
                    Match Current (v{currentVer})
                  </button>
                )}
                {typeof currentVer === 'number' && currentVer > 1 && (
                  <button
                    type="button"
                    onClick={() => setExpectedVersion(String(currentVer - 1))}
                    className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-rose-500/10 text-rose-700 dark:text-rose-300 border border-rose-500/20 hover:bg-rose-500/20 transition-colors"
                    title="Set to a stale version to test conflict rejection"
                  >
                    Test Stale (v{currentVer - 1})
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setExpectedVersion('999')}
                  className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-muted text-muted-foreground border border-border hover:text-foreground transition-colors"
                  title="Test invalid version"
                >
                  v999
                </button>
              </div>
            </div>

            {/* New Value Input */}
            <div className="space-y-1.5">
              <label className="font-semibold text-foreground/80 font-mono text-[11px]">
                New Value to Set *
              </label>
              <Input
                placeholder="e.g. updated_value_payload"
                value={newValue}
                onChange={(e) => setNewValue(e.target.value)}
                className="bg-background dark:bg-[var(--surface-0)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-foreground placeholder:text-muted-foreground focus-visible:ring-indigo-500/30 rounded-lg"
              />
            </div>
          </div>

          {/* Outcome / Feedback Banner */}
          <AnimatePresence>
            {outcome && (
              <motion.div
                initial={{ opacity: 0, y: -6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -6 }}
                className={cn(
                  'p-3.5 rounded-xl border text-xs font-mono space-y-2',
                  outcome.type === 'success' && 'bg-emerald-500/10 border-emerald-500/30 text-emerald-700 dark:text-emerald-300',
                  outcome.type === 'conflict' && 'bg-rose-500/10 border-rose-500/30 text-rose-700 dark:text-rose-300',
                  outcome.type === 'error' && 'bg-amber-500/10 border-amber-500/30 text-amber-700 dark:text-amber-300'
                )}
              >
                <div className="flex items-start gap-2">
                  {outcome.type === 'success' && <CheckCircle2 className="h-4 w-4 text-emerald-500 shrink-0 mt-0.5" />}
                  {outcome.type === 'conflict' && <ShieldAlert className="h-4 w-4 text-rose-500 shrink-0 mt-0.5" />}
                  {outcome.type === 'error' && <AlertTriangle className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />}

                  <div className="space-y-1 flex-1">
                    <div className="font-bold flex items-center justify-between">
                      <span>
                        {outcome.type === 'success' && 'STATUS 200 OK — CAS Committed'}
                        {outcome.type === 'conflict' && 'STATUS 409 CONFLICT — Version Mismatch'}
                        {outcome.type === 'error' && 'CAS Execution Error'}
                      </span>
                    </div>
                    <p className="text-[11px] leading-relaxed">{outcome.message}</p>

                    {outcome.type === 'conflict' && (
                      <div className="pt-2 flex items-center gap-2">
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          onClick={handleSyncExpectedVersion}
                          className="h-6 px-2 text-[10px] font-mono font-bold bg-rose-500/15 border-rose-500/30 hover:bg-rose-500/25 text-rose-700 dark:text-rose-300 rounded"
                        >
                          <RotateCcw className="h-3 w-3 mr-1" />
                          Sync Expected Version to v{outcome.currentVersion}
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <DialogFooter className="gap-2 sm:gap-0 pt-2 border-t border-border dark:border-[oklch(1_0_0/8%)]">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            className="border-border dark:border-[oklch(1_0_0/8%)] text-muted-foreground hover:bg-muted text-xs rounded-lg font-mono"
          >
            Close
          </Button>

          <Button
            onClick={handleExecuteCas}
            disabled={casMutation.isPending || !selectedKey.trim() || expectedVersion === ''}
            className="bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-lg border-0 gap-1.5 shadow-sm font-mono"
          >
            {casMutation.isPending ? (
              <>
                <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                Executing CAS...
              </>
            ) : (
              <>
                <GitCompare className="h-3.5 w-3.5" />
                Execute Compare-And-Set
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
