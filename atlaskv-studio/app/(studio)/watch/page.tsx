'use client';

import { useState, useEffect, useRef } from 'react';
import { Eye, Play, Square, Trash2, Radio, Terminal as TerminalIcon, Download, Search, Filter } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { getSavedBaseUrl } from '@/services/api';
import { toast } from 'sonner';

interface LogEntry {
  id: string;
  timestamp: string;
  type: 'PUT' | 'DELETE' | 'EXPIRE' | 'STATUS' | 'CAS';
  key: string;
  value?: string | null;
  version?: number;
}

export default function WatchPage() {
  const [targetKey, setTargetKey] = useState('app/');
  const [isPrefix, setIsPrefix] = useState(true);
  const [isStreaming, setIsStreaming] = useState(true);
  const [autoScroll, setAutoScroll] = useState(true);
  const [logFilter, setLogFilter] = useState<'ALL' | 'PUT' | 'DELETE' | 'EXPIRE' | 'CAS'>('ALL');
  const [logSearch, setLogSearch] = useState('');

  const [logs, setLogs] = useState<LogEntry[]>([
    {
      id: 'init',
      timestamp: new Date().toLocaleTimeString(),
      type: 'STATUS',
      key: 'System',
      value: 'Watch terminal initialized. Enter key/prefix and press Start Stream.',
    },
  ]);
  const scrollRef = useRef<HTMLDivElement>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  // Auto-scroll terminal console
  useEffect(() => {
    if (autoScroll && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  // Connect to real AtlasKV backend SSE stream
  useEffect(() => {
    if (!isStreaming || !targetKey) return;

    const baseUrl = getSavedBaseUrl() || window.location.origin;
    const path = isPrefix
      ? `/api/v1/watch/prefix/${encodeURIComponent(targetKey)}`
      : `/api/v1/watch/${encodeURIComponent(targetKey)}`;
    const streamUrl = `${baseUrl}${path}`;

    const nowStr = () => new Date().toLocaleTimeString();

    const addLog = (type: LogEntry['type'], key: string, value?: string | null, version?: number) => {
      setLogs((prev) => [
        ...prev.slice(-300),
        {
          id: Math.random().toString(),
          timestamp: nowStr(),
          type,
          key,
          value,
          version,
        },
      ]);
    };

    addLog('STATUS', 'Stream', `Connecting SSE stream to ${streamUrl}...`);

    try {
      const es = new EventSource(streamUrl);
      eventSourceRef.current = es;

      es.onopen = () => {
        addLog('STATUS', 'Stream', `Connected to AtlasKV SSE event stream`);
        toast.success(`SSE Watch connected to '${targetKey}'`);
      };

      es.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          addLog(
            data.type || 'PUT',
            data.key || targetKey,
            data.value,
            data.version
          );
        } catch {
          addLog('PUT', targetKey, event.data);
        }
      };

      es.onerror = () => {
        addLog('STATUS', 'Stream', 'Connection error or leader redirecting. Retrying in 3s...');
      };
    } catch (err: any) {
      addLog('STATUS', 'Error', err.message || 'Failed to open SSE stream');
      toast.error('SSE Watch stream failed to connect');
    }

    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
    };
  }, [isStreaming, targetKey, isPrefix]);

  const handleClear = () => {
    setLogs([]);
    toast.info('Console log buffer cleared');
  };

  const handleExportLogs = () => {
    const text = JSON.stringify(logs, null, 2);
    const blob = new Blob([text], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `atlaskv-watch-events-${Date.now()}.json`;
    a.click();
    toast.success('Downloaded watch event logs as JSON');
  };

  const filteredLogs = logs.filter((l) => {
    const matchesFilter = logFilter === 'ALL' || l.type === logFilter;
    const matchesSearch =
      l.key.toLowerCase().includes(logSearch.toLowerCase()) ||
      (l.value && l.value.toLowerCase().includes(logSearch.toLowerCase()));
    return matchesFilter && matchesSearch;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
            <Eye className="h-5 w-5 text-cyan-400" />
            Real-Time Watch Terminal (SSE)
          </h1>
          <p className="text-xs text-zinc-400 mt-1">
            Subscribe to real-time Server-Sent Events (SSE) key updates across the Raft cluster
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Badge
            variant="outline"
            className={
              isStreaming
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20 text-xs py-1 px-3 gap-1.5 font-mono'
                : 'bg-zinc-800 text-zinc-400 border-zinc-700 text-xs py-1 px-3 gap-1.5 font-mono'
            }
          >
            <Radio className={`h-3 w-3 ${isStreaming ? 'animate-pulse text-emerald-400' : ''}`} />
            {isStreaming ? 'STREAM ACTIVE' : 'STREAM PAUSED'}
          </Badge>
        </div>
      </div>

      {/* Control Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-zinc-900/60 p-3 rounded-xl border border-white/[0.08] backdrop-blur-md">
        <div className="flex items-center gap-3 flex-1">
          <div className="relative flex-1">
            <TerminalIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
            <Input
              placeholder="Key or prefix to watch..."
              value={targetKey}
              onChange={(e) => setTargetKey(e.target.value)}
              className="pl-9 bg-zinc-950/50 border-white/10 text-xs font-mono text-zinc-200 placeholder:text-zinc-500 focus-visible:ring-cyan-500/50"
            />
          </div>

          <label className="flex items-center gap-2 text-xs font-mono text-zinc-300 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={isPrefix}
              onChange={(e) => setIsPrefix(e.target.checked)}
              className="rounded bg-zinc-950 border-white/20 text-cyan-500"
            />
            Prefix Watch
          </label>
        </div>

        <div className="flex items-center gap-2">
          <Button
            onClick={() => {
              setIsStreaming(!isStreaming);
              toast.info(isStreaming ? 'Watch stream paused' : 'Watch stream resumed');
            }}
            variant="outline"
            className={
              isStreaming
                ? 'border-rose-500/30 text-rose-400 hover:bg-rose-500/10 text-xs gap-1.5'
                : 'border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/10 text-xs gap-1.5'
            }
          >
            {isStreaming ? <Square className="h-3.5 w-3.5" /> : <Play className="h-3.5 w-3.5" />}
            {isStreaming ? 'Pause' : 'Start'}
          </Button>

          <Button
            onClick={handleExportLogs}
            variant="outline"
            className="border-white/10 text-zinc-300 hover:bg-white/5 text-xs gap-1.5"
          >
            <Download className="h-3.5 w-3.5" />
            Export Logs
          </Button>

          <Button
            onClick={handleClear}
            variant="outline"
            className="border-white/10 text-zinc-400 hover:bg-white/5 text-xs gap-1.5"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Clear
          </Button>
        </div>
      </div>

      {/* Terminal View */}
      <div className="rounded-xl border border-white/[0.12] bg-[#050507] overflow-hidden shadow-2xl font-mono text-xs">
        {/* Terminal Sub-header / Filters */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between px-4 py-2 bg-zinc-950 border-b border-white/[0.08] text-zinc-400 gap-2">
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-zinc-500 font-semibold">Filter:</span>
            {(['ALL', 'PUT', 'DELETE', 'EXPIRE', 'CAS'] as const).map((type) => (
              <button
                key={type}
                onClick={() => setLogFilter(type)}
                className={`px-2 py-0.5 rounded text-[10px] transition-colors ${
                  logFilter === type
                    ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30'
                    : 'text-zinc-500 hover:text-zinc-300'
                }`}
              >
                {type}
              </button>
            ))}
          </div>

          <label className="flex items-center gap-2 text-[11px] text-zinc-400 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={autoScroll}
              onChange={(e) => setAutoScroll(e.target.checked)}
              className="rounded bg-zinc-900 border-white/20 text-cyan-500"
            />
            Auto-Scroll
          </label>
        </div>

        {/* Terminal Body */}
        <div
          ref={scrollRef}
          className="h-[480px] overflow-y-auto p-4 space-y-2 font-mono scrollbar-thin scrollbar-thumb-zinc-800"
        >
          {filteredLogs.length === 0 ? (
            <div className="text-zinc-600 text-center py-20">
              No watch events matching filter
            </div>
          ) : (
            filteredLogs.map((log) => (
              <div key={log.id} className="flex items-start gap-3 hover:bg-white/[0.02] p-1 rounded transition-colors">
                <span className="text-zinc-600 shrink-0 select-none">[{log.timestamp}]</span>
                <span
                  className={
                    log.type === 'PUT'
                      ? 'text-emerald-400 font-bold shrink-0'
                      : log.type === 'DELETE'
                      ? 'text-rose-400 font-bold shrink-0'
                      : log.type === 'EXPIRE'
                      ? 'text-amber-400 font-bold shrink-0'
                      : log.type === 'CAS'
                      ? 'text-purple-400 font-bold shrink-0'
                      : 'text-cyan-400 font-bold shrink-0'
                  }
                >
                  [{log.type}]
                </span>
                <span className="text-zinc-200 font-medium">{log.key}</span>
                {log.value && <span className="text-zinc-400 truncate max-w-md">= {log.value}</span>}
                {log.version && <span className="text-zinc-500 text-[10px] ml-auto">v{log.version}</span>}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
