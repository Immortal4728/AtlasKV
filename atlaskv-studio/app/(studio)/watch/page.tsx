'use client';

import { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { Eye, Play, Square, Trash2, Radio, Terminal as TerminalIcon, Download } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { PageHeader } from '@/components/ui/page-header';
import { getSavedBaseUrl } from '@/services/api';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

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
    return logFilter === 'ALL' || l.type === logFilter;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Real-Time Watch Terminal (SSE)"
        description="Subscribe to real-time Server-Sent Events (SSE) key updates across the Raft cluster"
        icon={Eye}
        iconColor="text-cyan-400"
        badge={
          <span
            className={cn(
              'inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-[10px] font-mono font-medium border',
              isStreaming
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                : 'bg-[oklch(1_0_0/4%)] text-[oklch(1_0_0/40%)] border-[oklch(1_0_0/8%)]'
            )}
          >
            <Radio className={cn('h-3 w-3', isStreaming && 'animate-pulse text-emerald-400')} />
            {isStreaming ? 'STREAM ACTIVE' : 'PAUSED'}
          </span>
        }
      />

      {/* Control Bar */}
      <motion.div
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 glass-card rounded-xl p-3"
      >
        <div className="flex items-center gap-3 flex-1">
          <div className="relative flex-1">
            <TerminalIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[oklch(1_0_0/25%)]" />
            <Input
              placeholder="Key or prefix to watch..."
              value={targetKey}
              onChange={(e) => setTargetKey(e.target.value)}
              className="pl-9 bg-[var(--surface-0)] border-[oklch(1_0_0/8%)] text-xs font-mono text-[oklch(1_0_0/80%)] placeholder:text-[oklch(1_0_0/25%)] focus-visible:ring-cyan-500/30 rounded-lg"
            />
          </div>

          <label className="flex items-center gap-2 text-xs font-mono text-[oklch(1_0_0/60%)] cursor-pointer select-none">
            <input
              type="checkbox"
              checked={isPrefix}
              onChange={(e) => setIsPrefix(e.target.checked)}
              className="rounded bg-[var(--surface-0)] border-[oklch(1_0_0/15%)] text-cyan-500"
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
            className={cn(
              'text-xs gap-1.5 rounded-lg border',
              isStreaming
                ? 'border-rose-500/30 text-rose-400 hover:bg-rose-500/10'
                : 'border-emerald-500/30 text-emerald-400 hover:bg-emerald-500/10'
            )}
          >
            {isStreaming ? <Square className="h-3.5 w-3.5" /> : <Play className="h-3.5 w-3.5" />}
            {isStreaming ? 'Pause' : 'Start'}
          </Button>

          <Button
            onClick={handleExportLogs}
            variant="outline"
            className="border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/50%)] hover:bg-[oklch(1_0_0/4%)] text-xs gap-1.5 rounded-lg"
          >
            <Download className="h-3.5 w-3.5" />
            Export Logs
          </Button>

          <Button
            onClick={handleClear}
            variant="outline"
            className="border-[oklch(1_0_0/8%)] text-[oklch(1_0_0/40%)] hover:bg-[oklch(1_0_0/4%)] text-xs gap-1.5 rounded-lg"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Clear
          </Button>
        </div>
      </motion.div>

      {/* Terminal View */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35, delay: 0.15 }}
        className="glass-card rounded-xl overflow-hidden p-0 font-mono text-xs border border-[oklch(1_0_0/8%)]"
      >
        {/* Terminal Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between px-4 py-2.5 bg-[var(--surface-0)]/80 border-b border-[oklch(1_0_0/6%)] text-[oklch(1_0_0/40%)] gap-2">
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-[oklch(1_0_0/30%)] font-semibold">Filter:</span>
            {(['ALL', 'PUT', 'DELETE', 'EXPIRE', 'CAS'] as const).map((type) => (
              <button
                key={type}
                onClick={() => setLogFilter(type)}
                className={cn(
                  'px-2 py-0.5 rounded text-[10px] transition-colors',
                  logFilter === type
                    ? 'bg-cyan-500/15 text-cyan-400 border border-cyan-500/20 font-bold'
                    : 'text-[oklch(1_0_0/30%)] hover:text-[oklch(1_0_0/60%)]'
                )}
              >
                {type}
              </button>
            ))}
          </div>

          <label className="flex items-center gap-2 text-[11px] text-[oklch(1_0_0/40%)] cursor-pointer select-none">
            <input
              type="checkbox"
              checked={autoScroll}
              onChange={(e) => setAutoScroll(e.target.checked)}
              className="rounded bg-[var(--surface-0)] border-[oklch(1_0_0/15%)] text-cyan-500"
            />
            Auto-Scroll
          </label>
        </div>

        {/* Terminal Console */}
        <div
          ref={scrollRef}
          className="h-[480px] overflow-y-auto p-4 space-y-2 font-mono scrollbar-thin scrollbar-thumb-[oklch(1_0_0/8%)] bg-[#050507]/90"
        >
          {filteredLogs.length === 0 ? (
            <div className="text-[oklch(1_0_0/20%)] text-center py-20">
              No watch events matching filter
            </div>
          ) : (
            filteredLogs.map((log) => (
              <div key={log.id} className="flex items-start gap-3 hover:bg-[oklch(1_0_0/2%)] p-1 rounded transition-colors">
                <span className="text-[oklch(1_0_0/20%)] shrink-0 select-none">[{log.timestamp}]</span>
                <span
                  className={cn(
                    'font-bold shrink-0',
                    log.type === 'PUT' && 'text-emerald-400',
                    log.type === 'DELETE' && 'text-rose-400',
                    log.type === 'EXPIRE' && 'text-amber-400',
                    log.type === 'CAS' && 'text-purple-400',
                    log.type === 'STATUS' && 'text-cyan-400'
                  )}
                >
                  [{log.type}]
                </span>
                <span className="text-[oklch(1_0_0/80%)] font-medium">{log.key}</span>
                {log.value && <span className="text-[oklch(1_0_0/40%)] truncate max-w-md">= {log.value}</span>}
                {log.version && <span className="text-[oklch(1_0_0/25%)] text-[10px] ml-auto">v{log.version}</span>}
              </div>
            ))
          )}
        </div>
      </motion.div>
    </div>
  );
}
