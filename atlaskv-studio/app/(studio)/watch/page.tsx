'use client';

import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Eye,
  Play,
  Square,
  Trash2,
  Radio,
  Terminal as TerminalIcon,
  Download,
  ChevronDown,
  Copy,
  Settings,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';
import { NamespaceBadge } from '@/components/ui/namespace-badge';
import { useAuth } from '@/hooks/use-auth';
import { getSavedBaseUrl, getSavedApiKey, getSavedAdminNamespace, normalizeAndValidateServerUrl } from '@/services/api';
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
  const { serverUrl, apiKey: currentApiKey, adminNamespace: currentAdminNs } = useAuth();
  const [targetKey, setTargetKey] = useState('app');
  const [isPrefix, setIsPrefix] = useState(true);
  const [isStreaming, setIsStreaming] = useState(true);
  const [autoScroll, setAutoScroll] = useState(true);
  const [logFilter, setLogFilter] = useState<'ALL' | 'PUT' | 'DELETE' | 'EXPIRE' | 'CAS'>('ALL');
  const [actionsMenuOpen, setActionsMenuOpen] = useState(false);

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
    if (!isStreaming) return;

    const savedUrl = getSavedBaseUrl();
    const baseUrl = savedUrl
      ? normalizeAndValidateServerUrl(savedUrl).normalized || savedUrl
      : window.location.origin;
    const cleanTarget = targetKey.trim().replace(/^\/+|\/+$/g, '');
    const path = isPrefix
      ? cleanTarget ? `/api/v1/watch/prefix/${encodeURIComponent(cleanTarget)}` : `/api/v1/watch/prefix`
      : `/api/v1/watch/${encodeURIComponent(cleanTarget)}`;
    
    // Construct query parameters for browser EventSource
    const params = new URLSearchParams();
    const apiKey = getSavedApiKey();
    if (apiKey) {
      params.set('apiKey', apiKey);
    }
    const adminNs = getSavedAdminNamespace();
    if (adminNs) {
      params.set('namespace', adminNs);
    }
    const queryString = params.toString() ? `?${params.toString()}` : '';
    const streamUrl = `${baseUrl}${path}${queryString}`;

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

    addLog('STATUS', 'Stream', `Connecting SSE stream to ${baseUrl}${path}...`);

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
  }, [isStreaming, targetKey, isPrefix, serverUrl, currentApiKey, currentAdminNs]);

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

  const handleCopyLogs = () => {
    const text = logs
      .map((l) => `[${l.timestamp}] [${l.type}] ${l.key} ${l.value ? '= ' + l.value : ''}`)
      .join('\n');
    navigator.clipboard.writeText(text);
    toast.success('Copied watch logs to clipboard');
  };

  const filteredLogs = logs.filter((l) => {
    return logFilter === 'ALL' || l.type === logFilter;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Watch Terminal"
        description="Monitor real-time cluster storage mutation events via SSE."
        icon={Eye}
        iconColor="text-cyan-400"
        badge={
          <div className="flex items-center gap-2">
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
            <NamespaceBadge showSwitcher={false} />
          </div>
        }
      />

      {/* Control Bar */}
      <motion.div
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 glass-card rounded-xl p-3 border border-border dark:border-[oklch(1_0_0/8%)]"
      >
        <div className="flex items-center gap-3 flex-1">
          <div className="relative flex-1">
            <TerminalIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-neutral-400" />
            <Input
              placeholder="Key or prefix to watch..."
              value={targetKey}
              onChange={(e) => setTargetKey(e.target.value)}
              className="pl-9 bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] placeholder:text-neutral-400 focus-visible:ring-cyan-500/30 rounded-lg"
            />
          </div>

          <label className="flex items-center gap-2 text-xs font-mono text-neutral-700 dark:text-[oklch(1_0_0/60%)] font-semibold cursor-pointer select-none">
            <input
              type="checkbox"
              checked={isPrefix}
              onChange={(e) => setIsPrefix(e.target.checked)}
              className="rounded bg-[var(--surface-2)] border-border dark:border-[oklch(1_0_0/15%)] text-cyan-600 dark:text-cyan-500"
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
              'text-xs gap-1.5 rounded-lg border font-bold',
              isStreaming
                ? 'border-rose-500/30 text-rose-600 dark:text-rose-400 hover:bg-rose-500/10'
                : 'border-emerald-500/30 text-emerald-600 dark:text-emerald-400 hover:bg-emerald-500/10'
            )}
          >
            {isStreaming ? <Square className="h-3.5 w-3.5" /> : <Play className="h-3.5 w-3.5" />}
            {isStreaming ? 'Pause' : 'Start'}
          </Button>

          {/* Actions Dropdown Menu */}
          <div className="relative">
            <button
              onClick={() => setActionsMenuOpen((prev) => !prev)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-mono font-bold bg-neutral-200/80 dark:bg-[oklch(1_0_0/5%)] hover:bg-neutral-300/80 dark:hover:bg-[oklch(1_0_0/10%)] text-[var(--foreground)] border border-border dark:border-[oklch(1_0_0/10%)] transition-all cursor-pointer shadow-xs"
            >
              Actions
              <ChevronDown className="h-3.5 w-3.5 text-neutral-500 transition-transform duration-200" />
            </button>

            {/* Floating Premium Dropdown Menu */}
            <AnimatePresence>
              {actionsMenuOpen && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.95, y: 6 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  className="absolute right-0 mt-2 w-48 rounded-xl bg-[#1B1B1B] border border-[#2A2A2A] p-1.5 shadow-2xl z-50 font-mono text-xs text-neutral-300 space-y-0.5"
                >
                  <button
                    onClick={() => {
                      handleExportLogs();
                      setActionsMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-lg hover:bg-neutral-800 hover:text-white transition-colors text-left cursor-pointer"
                  >
                    <Download className="h-3.5 w-3.5 text-emerald-400" />
                    📥 Export Logs
                  </button>
                  <button
                    onClick={() => {
                      handleClear();
                      setActionsMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-lg hover:bg-neutral-800 hover:text-rose-400 transition-colors text-left cursor-pointer"
                  >
                    <Trash2 className="h-3.5 w-3.5 text-rose-400" />
                    🗑 Clear Terminal
                  </button>
                  <button
                    onClick={() => {
                      handleCopyLogs();
                      setActionsMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-lg hover:bg-neutral-800 hover:text-cyan-400 transition-colors text-left cursor-pointer"
                  >
                    <Copy className="h-3.5 w-3.5 text-cyan-400" />
                    📋 Copy Logs
                  </button>
                  <button
                    onClick={() => {
                      toast.info('Terminal Settings: Buffer max 300 SSE events');
                      setActionsMenuOpen(false);
                    }}
                    className="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-lg hover:bg-neutral-800 hover:text-amber-400 transition-colors text-left cursor-pointer"
                  >
                    <Settings className="h-3.5 w-3.5 text-amber-400" />
                    ⚙ Terminal Settings
                  </button>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </motion.div>

      {/* Terminal Outer Container with Moving Animated Background Glow & Deep Drop Shadow */}
      <div className="relative group">
        {/* Animated Moving Ambient Background Glow Orbs */}
        <motion.div
          animate={{
            scale: [1, 1.1, 1],
            x: [-20, 20, -20],
            y: [-10, 10, -10],
            opacity: [0.35, 0.65, 0.35],
          }}
          transition={{
            duration: 8,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
          className="absolute -top-12 -left-12 w-72 h-72 rounded-full bg-gradient-to-r from-emerald-500/25 via-cyan-500/25 to-indigo-500/25 blur-[90px] pointer-events-none"
        />

        <motion.div
          animate={{
            scale: [1.1, 1, 1.1],
            x: [20, -20, 20],
            y: [10, -10, 10],
            opacity: [0.4, 0.7, 0.4],
          }}
          transition={{
            duration: 10,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
          className="absolute -bottom-12 -right-12 w-80 h-80 rounded-full bg-gradient-to-r from-purple-500/25 via-cyan-500/25 to-teal-500/25 blur-[100px] pointer-events-none"
        />

        {/* Adaptive macOS Terminal Window Container */}
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35, delay: 0.15 }}
          className="relative z-10 rounded-xl overflow-hidden p-0 font-mono text-xs bg-[#1E1E1E] dark:bg-white border border-[#3A3A3A] dark:border-neutral-300 shadow-[0_25px_60px_-15px_rgba(0,0,0,0.6)] dark:shadow-[0_25px_60px_-15px_rgba(0,0,0,0.18)] ring-1 ring-cyan-500/20 dark:ring-emerald-500/20"
        >
          {/* macOS Title Bar Header (#2A2A2A in light mode, #E5E7EB in dark mode) */}
          <div className="flex items-center justify-between px-4 py-3 bg-[#2A2A2A] dark:bg-[#E5E7EB] border-b border-[#3A3A3A] dark:border-neutral-300 text-xs font-mono select-none">
            {/* Top Left Decorative macOS Dots */}
            <div className="flex items-center gap-2">
              <span className="h-3 w-3 rounded-full bg-[#FF5F57] inline-block shadow-xs" />
              <span className="h-3 w-3 rounded-full bg-[#FEBC2E] inline-block shadow-xs" />
              <span className="h-3 w-3 rounded-full bg-[#28C840] inline-block shadow-xs" />
            </div>

            {/* Center Window Title */}
            <div className="flex items-center gap-2 text-neutral-300 dark:text-neutral-800 text-xs font-mono font-bold">
              <span>Watch Terminal</span>
            </div>

            {/* Top Right Shell Info */}
            <div className="flex items-center gap-2 text-[10px] text-neutral-400 dark:text-neutral-500 font-mono font-semibold">
              <span>bash — 80x24</span>
            </div>
          </div>

          {/* Filter & Options Bar */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between px-4 py-2 bg-[#242424] dark:bg-[#F3F4F6] border-b border-[#3A3A3A] dark:border-neutral-300 text-neutral-300 dark:text-neutral-700 gap-2 font-mono text-xs">
            <div className="flex items-center gap-2">
              <span className="text-[11px] text-neutral-400 dark:text-neutral-600 font-bold uppercase tracking-wider">Filter:</span>
              {(['ALL', 'PUT', 'DELETE', 'EXPIRE', 'CAS'] as const).map((type) => (
                <button
                  key={type}
                  onClick={() => setLogFilter(type)}
                  className={cn(
                    'px-2 py-0.5 rounded text-[10px] transition-colors cursor-pointer font-bold',
                    logFilter === type
                      ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/40 dark:bg-cyan-600 dark:text-white dark:border-cyan-600 shadow-xs'
                      : 'text-neutral-400 hover:text-white dark:text-neutral-600 dark:hover:text-neutral-900'
                  )}
                >
                  {type}
                </button>
              ))}
            </div>

            <label className="flex items-center gap-2 text-[11px] text-neutral-300 dark:text-neutral-700 cursor-pointer select-none font-bold">
              <input
                type="checkbox"
                checked={autoScroll}
                onChange={(e) => setAutoScroll(e.target.checked)}
                className="rounded bg-[#1E1E1E] border-[#444] text-cyan-500 dark:bg-white dark:border-neutral-300 dark:text-cyan-600"
              />
              Auto-Scroll
            </label>
          </div>

          {/* Terminal Console Output Area (#1E1E1E in light mode, #FFFFFF in dark mode) */}
          <div
            ref={scrollRef}
            className="h-[480px] overflow-y-auto p-4 space-y-2 font-mono scrollbar-thin scrollbar-thumb-neutral-500 bg-[#1E1E1E] dark:bg-white text-emerald-400/90 dark:text-slate-900"
          >
            {filteredLogs.length === 0 ? (
              <div className="text-neutral-500 text-center py-20 font-mono font-medium">
                No watch events matching filter
              </div>
            ) : (
              filteredLogs.map((log) => (
                <div key={log.id} className="flex items-start gap-3 hover:bg-white/[0.03] dark:hover:bg-slate-100/80 p-1 rounded transition-colors">
                  <span className="text-neutral-500 dark:text-slate-500 shrink-0 select-none font-medium">[{log.timestamp}]</span>
                  <span
                    className={cn(
                      'font-bold shrink-0',
                      log.type === 'PUT' && 'text-emerald-400 dark:text-emerald-700',
                      log.type === 'DELETE' && 'text-rose-400 dark:text-rose-700',
                      log.type === 'EXPIRE' && 'text-amber-400 dark:text-amber-700',
                      log.type === 'CAS' && 'text-purple-400 dark:text-purple-700',
                      log.type === 'STATUS' && 'text-cyan-400 dark:text-cyan-700'
                    )}
                  >
                    [{log.type}]
                  </span>
                  <span className="text-neutral-200 dark:text-slate-900 font-bold">{log.key}</span>
                  {log.value && <span className="text-neutral-400 dark:text-slate-600 font-medium truncate max-w-md">= {log.value}</span>}
                  {log.version && <span className="text-neutral-500 dark:text-slate-500 text-[10px] ml-auto font-semibold">v{log.version}</span>}
                </div>
              ))
            )}
          </div>
        </motion.div>
      </div>
    </div>
  );
}
