'use client';

import { useState } from 'react';
import { Filter, Search, ChevronLeft, ChevronRight, KeyRound, Clock, RefreshCw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { usePrefix } from '@/hooks/use-kv';

export default function PrefixPage() {
  const [prefixInput, setPrefixInput] = useState('app/');
  const [activePrefix, setActivePrefix] = useState('app/');
  const [offset, setOffset] = useState(0);
  const limit = 50;

  const { data, isLoading, isError, refetch } = usePrefix(activePrefix, offset, limit);

  const results = data?.entries ?? [];
  const totalCount = data?.totalCount ?? results.length;

  const handleScan = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setActivePrefix(prefixInput);
    setOffset(0);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
          <Filter className="h-5 w-5 text-emerald-400" />
          Prefix Query Explorer
        </h1>
        <p className="text-xs text-zinc-400 mt-1">
          Perform live range scans and prefix queries across the AtlasKV key space
        </p>
      </div>

      {/* Query Bar */}
      <form onSubmit={handleScan} className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 bg-zinc-900/60 p-3 rounded-xl border border-white/[0.08] backdrop-blur-md">
        <div className="relative flex-1">
          <Filter className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
          <Input
            placeholder="Enter key prefix (e.g. app/, session/)"
            value={prefixInput}
            onChange={(e) => setPrefixInput(e.target.value)}
            className="pl-9 bg-zinc-950/50 border-white/10 text-xs font-mono text-zinc-200 placeholder:text-zinc-500 focus-visible:ring-emerald-500/50"
          />
        </div>

        <div className="flex items-center gap-2">
          <Button
            type="submit"
            className="bg-emerald-500 hover:bg-emerald-600 text-zinc-950 font-semibold text-xs px-4 py-2 shadow-lg shadow-emerald-500/20 gap-1.5"
          >
            <Search className="h-4 w-4" />
            Scan Prefix
          </Button>
        </div>
      </form>

      {/* Quick Prefix Buttons */}
      <div className="flex items-center gap-2 text-xs">
        <span className="text-zinc-500 text-[11px] font-mono">Quick Prefixes:</span>
        {['app/', 'session/', 'cache/', 'leader/'].map((p) => (
          <button
            key={p}
            onClick={() => {
              setPrefixInput(p);
              setActivePrefix(p);
              setOffset(0);
            }}
            className={`px-2.5 py-1 rounded-md text-[11px] font-mono transition-colors border ${
              activePrefix === p
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-zinc-900 text-zinc-400 border-white/10 hover:text-zinc-200'
            }`}
          >
            {p}
          </button>
        ))}
      </div>

      {/* Results Table */}
      <div className="rounded-xl border border-white/[0.08] bg-zinc-900/40 backdrop-blur-md overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-zinc-950/60 border-b border-white/[0.08] text-zinc-400 uppercase tracking-wider text-[10px] font-mono">
              <tr>
                <th className="py-3 px-4">Matching Key</th>
                <th className="py-3 px-4">Value</th>
                <th className="py-3 px-4">Version</th>
                <th className="py-3 px-4">Lease</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.06] text-zinc-300 font-mono">
              {isLoading ? (
                [1, 2, 3].map((i) => (
                  <tr key={i} className="animate-pulse">
                    <td className="py-3 px-4"><div className="h-4 w-36 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-48 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-12 bg-zinc-800 rounded" /></td>
                    <td className="py-3 px-4"><div className="h-4 w-20 bg-zinc-800 rounded" /></td>
                  </tr>
                ))
              ) : results.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-12 text-center text-zinc-500">
                    No keys match prefix "{activePrefix}"
                  </td>
                </tr>
              ) : (
                results.map((item) => (
                  <tr key={item.key} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3 px-4 text-emerald-400 flex items-center gap-2 font-medium">
                      <KeyRound className="h-3.5 w-3.5 text-emerald-500/60 shrink-0" />
                      {item.key}
                    </td>
                    <td className="py-3 px-4 max-w-[360px] truncate text-zinc-300">
                      {item.value ?? '<null>'}
                    </td>
                    <td className="py-3 px-4 text-zinc-400">
                      <span className="px-2 py-0.5 rounded bg-zinc-800 border border-zinc-700 text-[11px]">
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
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-4 py-3 bg-zinc-950/40 border-t border-white/[0.08] text-xs text-zinc-400 font-mono">
          <span>Matches: {results.length} | Total: {totalCount}</span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={offset === 0}
              onClick={() => setOffset(Math.max(0, offset - limit))}
              className="h-7 px-2 border-white/10 text-zinc-300 disabled:text-zinc-600"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
            </Button>
            <span>Offset {offset}</span>
            <Button
              variant="outline"
              size="sm"
              disabled={offset + limit >= totalCount}
              onClick={() => setOffset(offset + limit)}
              className="h-7 px-2 border-white/10 text-zinc-300 disabled:text-zinc-600"
            >
              <ChevronRight className="h-3.5 w-3.5" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
