'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { Filter, Search, ChevronLeft, ChevronRight, KeyRound, Clock, Copy, Check, Timer } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';
import { NamespaceBadge } from '@/components/ui/namespace-badge';
import { usePrefix } from '@/hooks/use-kv';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

export default function PrefixPage() {
  const [prefixInput, setPrefixInput] = useState('test/users/');
  const [activePrefix, setActivePrefix] = useState('test/users/');
  const [offset, setOffset] = useState(0);
  const [copiedText, setCopiedText] = useState<string | null>(null);
  const limit = 50;

  const { data, isLoading } = usePrefix(activePrefix, offset, limit);

  const results = data?.entries ?? [];
  const totalCount = data?.totalCount ?? results.length;

  const handleScan = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setActivePrefix(prefixInput);
    setOffset(0);
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopiedText(text);
    toast.success(`Copied ${label} to clipboard`);
    setTimeout(() => setCopiedText(null), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Prefix Explorer"
        description="Query and iterate over keys matching a common prefix within the active namespace."
        icon={Filter}
        iconColor="text-emerald-400"
        badge={<NamespaceBadge showSwitcher={false} />}
      />

      {/* Query Bar */}
      <motion.form
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        onSubmit={handleScan}
        className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 glass-card rounded-xl p-3 border border-border dark:border-[oklch(1_0_0/8%)]"
      >
        <div className="relative flex-1">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-neutral-500 dark:text-neutral-400" />
          <Input
            placeholder="Enter key prefix (e.g. test/users/, app/)"
            value={prefixInput}
            onChange={(e) => setPrefixInput(e.target.value)}
            className="pl-9 bg-card dark:bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-neutral-900 dark:text-[var(--foreground)] placeholder:text-neutral-500 dark:placeholder:text-neutral-400 focus-visible:ring-emerald-500/30 rounded-lg font-medium"
          />
        </div>

        <div className="flex items-center gap-2">
          <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
            <Button
              type="submit"
              className="bg-emerald-600 hover:bg-emerald-700 dark:bg-emerald-500 dark:hover:bg-emerald-600 text-white font-bold text-xs px-4 py-2 shadow-sm gap-1.5 rounded-lg border-0"
            >
              <Search className="h-4 w-4" />
              Scan Prefix
            </Button>
          </motion.div>
        </div>
      </motion.form>

      {/* Quick Prefix Buttons */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.1 }}
        className="flex flex-wrap items-center gap-2 text-xs"
      >
        <span className="text-neutral-700 dark:text-neutral-400 text-xs font-mono font-semibold">Quick Prefixes:</span>
        {['test/users/', 'app/', 'session/', 'cache/', 'leader/'].map((p) => (
          <button
            key={p}
            onClick={() => {
              setPrefixInput(p);
              setActivePrefix(p);
              setOffset(0);
            }}
            className={cn(
              'px-2.5 py-1 rounded-md text-xs font-mono transition-all border font-semibold',
              activePrefix === p
                ? 'bg-emerald-500/15 text-emerald-700 dark:text-emerald-400 border-emerald-500/40 shadow-xs'
                : 'bg-card dark:bg-[var(--surface-3)] text-neutral-800 dark:text-[oklch(1_0_0/60%)] border-border dark:border-[oklch(1_0_0/8%)] hover:text-neutral-900 dark:hover:text-[var(--foreground)] hover:bg-neutral-100 dark:hover:bg-[oklch(1_0_0/4%)]'
            )}
          >
            {p}
          </button>
        ))}
      </motion.div>

      {/* Results Table */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, delay: 0.15 }}
        className="glass-card rounded-xl overflow-hidden p-0 border border-border dark:border-[oklch(1_0_0/6%)]"
      >
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-neutral-100/90 dark:bg-[var(--surface-2)] border-b border-border dark:border-[oklch(1_0_0/6%)] text-neutral-800 dark:text-[oklch(1_0_0/50%)] uppercase tracking-[0.1em] text-[10px] font-mono font-bold">
              <tr>
                <th className="py-3 px-4">Matching Key</th>
                <th className="py-3 px-4">Value</th>
                <th className="py-3 px-4">Version</th>
                <th className="py-3 px-4">Lease / TTL</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border dark:divide-[oklch(1_0_0/4%)] text-neutral-900 dark:text-[var(--foreground)] font-mono">
              {isLoading ? (
                [1, 2, 3].map((i) => (
                  <tr key={i}>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-36 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-48 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-12 rounded" /></td>
                    <td className="py-3.5 px-4"><div className="skeleton h-4 w-20 rounded" /></td>
                    <td className="py-3.5 px-4 text-right"><div className="skeleton h-4 w-12 rounded ml-auto" /></td>
                  </tr>
                ))
              ) : results.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-16 text-center">
                    <div className="flex flex-col items-center gap-3">
                      <div className="h-14 w-14 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center shadow-xs">
                        <Filter className="h-7 w-7 text-emerald-600 dark:text-emerald-400" />
                      </div>
                      <div className="space-y-1">
                        <h4 className="text-sm font-bold text-neutral-900 dark:text-[var(--foreground)]">No Matching Prefix Keys</h4>
                        <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-sm">
                          No key-value entries matched prefix "{activePrefix}". Try scanning with a different prefix like "test/users/" or "app/".
                        </p>
                      </div>
                    </div>
                  </td>
                </tr>
              ) : (
                results.map((item, idx) => (
                  <motion.tr
                    key={item.key}
                    initial={{ opacity: 0, x: -6 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.25, delay: idx * 0.03 }}
                    className="hover:bg-neutral-100/80 dark:hover:bg-[oklch(1_0_0/2%)] transition-colors group"
                  >
                    <td className="py-3 px-4 text-emerald-700 dark:text-emerald-400 font-semibold flex items-center gap-2">
                      <KeyRound className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-500/60 shrink-0" />
                      <span className="truncate max-w-[240px]">{item.key}</span>
                      <button
                        onClick={() => copyToClipboard(item.key, 'Key')}
                        className="opacity-0 group-hover:opacity-100 text-neutral-500 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white transition-all ml-1"
                      >
                        {copiedText === item.key ? <Check className="h-3 w-3 text-emerald-600 dark:text-emerald-500" /> : <Copy className="h-3 w-3" />}
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
                        {item.leaseId && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-purple-500/15 text-purple-700 dark:text-purple-400 border border-purple-500/25 text-[10px] font-mono font-semibold">
                            <Clock className="h-3 w-3" />
                            {item.leaseId}
                          </span>
                        )}
                        {item.ttlRemaining != null && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-amber-500/15 text-amber-700 dark:text-amber-400 border border-amber-500/25 text-[10px] font-mono font-semibold">
                            <Timer className="h-3 w-3" />
                            {Math.ceil(item.ttlRemaining / 1000)}s remaining
                          </span>
                        )}
                        {!item.leaseId && item.ttlRemaining == null && (
                          <span className="text-neutral-500 dark:text-neutral-500 font-semibold">—</span>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => copyToClipboard(item.value || '', 'Value')}
                          className="h-7 w-7 text-neutral-500 dark:text-[oklch(1_0_0/40%)] hover:text-neutral-900 dark:hover:text-white hover:bg-neutral-200/60 dark:hover:bg-[oklch(1_0_0/6%)] rounded-lg"
                          title="Copy Value"
                        >
                          <Copy className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    </td>
                  </motion.tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-4 py-3 bg-neutral-100/80 dark:bg-[var(--surface-0)]/40 border-t border-border dark:border-[oklch(1_0_0/6%)] text-xs text-neutral-800 dark:text-[oklch(1_0_0/35%)] font-mono font-medium">
          <span>Matches: {results.length} | Total: {totalCount}</span>
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
    </div>
  );
}
