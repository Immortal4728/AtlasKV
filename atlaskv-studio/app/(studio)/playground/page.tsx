'use client';

import { useState, useEffect, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  Send,
  History,
  Terminal,
  FileJson,
  Trash2,
  Clock,
  ArrowRight,
  Database,
  CheckCircle,
  AlertTriangle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

interface HistoryItem {
  id: string;
  method: 'GET' | 'POST' | 'DELETE';
  key: string;
  body?: string;
  timestamp: string;
  status: number;
  statusText: string;
  latencyMs: number;
}

export default function PlaygroundPage() {
  const queryClient = useQueryClient();

  const [method, setMethod] = useState<'GET' | 'POST' | 'DELETE'>('GET');
  const [key, setKey] = useState('');
  const [requestBody, setRequestBody] = useState('');
  const [loading, setLoading] = useState(false);

  // Response state
  const [responseStatus, setResponseStatus] = useState<number | null>(null);
  const [responseStatusText, setResponseStatusText] = useState('');
  const [responseLatency, setResponseLatency] = useState<number | null>(null);
  const [responseHeaders, setResponseHeaders] = useState<Record<string, string>>({});
  const [responseBody, setResponseBody] = useState<any>(null);

  // History state
  const [history, setHistory] = useState<HistoryItem[]>([]);

  // Load history from localStorage
  useEffect(() => {
    const saved = localStorage.getItem('atlaskv-playground-history');
    if (saved) {
      try {
        setHistory(JSON.parse(saved));
      } catch (e) {}
    }
  }, []);

  const saveHistory = (items: HistoryItem[]) => {
    setHistory(items);
    localStorage.setItem('atlaskv-playground-history', JSON.stringify(items));
  };

  const clearHistory = () => {
    saveHistory([]);
  };

  // Perform the API call
  const handleSend = async () => {
    if (!key.trim()) return;

    setLoading(true);
    const start = performance.now();
    const url = `/api/v1/kv/${encodeURIComponent(key.trim())}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    let fetchOptions: RequestInit = {
      method,
      headers,
    };

    if (method === 'POST') {
      let parsedValue = requestBody;
      // If valid JSON is entered in requestBody, we can just send it.
      // But the backend expects {"value": "..."} format for PUT/POST.
      // If requestBody is a simple string, wrap it. If it is already a JSON containing 'value', pass it.
      try {
        const obj = JSON.parse(requestBody);
        if (obj && typeof obj === 'object' && 'value' in obj) {
          fetchOptions.body = requestBody;
        } else {
          fetchOptions.body = JSON.stringify({ value: requestBody });
        }
      } catch (e) {
        fetchOptions.body = JSON.stringify({ value: requestBody });
      }
    }

    try {
      const response = await fetch(url, fetchOptions);
      const latencyMs = Math.round(performance.now() - start);

      // Parse headers
      const resHeaders: Record<string, string> = {};
      response.headers.forEach((val, name) => {
        resHeaders[name] = val;
      });

      let resBody: any = null;
      const text = await response.text();
      try {
        resBody = JSON.parse(text);
      } catch (e) {
        resBody = text;
      }

      setResponseStatus(response.status);
      setResponseStatusText(response.statusText);
      setResponseLatency(latencyMs);
      setResponseHeaders(resHeaders);
      setResponseBody(resBody);

      // Add to history
      const newHistoryItem: HistoryItem = {
        id: Math.random().toString(36).substring(2, 9),
        method,
        key: key.trim(),
        body: method === 'POST' ? requestBody : undefined,
        timestamp: new Date().toLocaleTimeString(),
        status: response.status,
        statusText: response.statusText,
        latencyMs,
      };

      saveHistory([newHistoryItem, ...history.slice(0, 19)]); // Keep last 20 items

      // Invalidate cluster/metrics queries to refresh dashboard/explorer stats
      queryClient.invalidateQueries({ queryKey: ['cluster'] });
    } catch (error: any) {
      const latencyMs = Math.round(performance.now() - start);
      setResponseStatus(500);
      setResponseStatusText('Fetch Error');
      setResponseLatency(latencyMs);
      setResponseHeaders({});
      setResponseBody({ error: error.message || 'Failed to execute request' });
    } finally {
      setLoading(false);
    }
  };

  // Load history item into composer
  const loadHistoryItem = (item: HistoryItem) => {
    setMethod(item.method);
    setKey(item.key);
    if (item.body) {
      setRequestBody(item.body);
    } else {
      setRequestBody('');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl font-semibold tracking-tight text-white/90">
          API Playground
        </h1>
        <p className="text-sm text-white/30 mt-0.5">
          Send direct REST commands to the AtlasKV consensus store
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 h-[calc(100vh-200px)]">
        {/* Left Side: Request Composer (7 cols) */}
        <div className="lg:col-span-7 flex flex-col gap-6 overflow-hidden">
          <Card className="border-white/[0.06] bg-[#111113] flex-1 flex flex-col overflow-hidden">
            <CardContent className="p-5 flex flex-col h-full space-y-4">
              {/* Method Selector Tabs */}
              <div className="flex gap-1 bg-white/[0.02] border border-white/[0.06] p-1 rounded-lg self-start">
                {(['GET', 'POST', 'DELETE'] as const).map((m) => (
                  <button
                    key={m}
                    onClick={() => setMethod(m)}
                    className={`px-3 py-1.5 rounded-md text-xs font-semibold tracking-wider transition-colors ${
                      method === m
                        ? m === 'GET'
                          ? 'bg-blue-600 text-white shadow-sm'
                          : m === 'POST'
                          ? 'bg-emerald-600 text-white shadow-sm'
                          : 'bg-rose-600 text-white shadow-sm'
                        : 'text-white/40 hover:text-white/60 hover:bg-white/[0.02]'
                    }`}
                  >
                    {m}
                  </button>
                ))}
              </div>

              {/* URL Composer */}
              <div className="flex items-center gap-2">
                <div className="bg-white/[0.04] border border-white/[0.06] rounded-lg px-3 py-2 text-xs text-white/40 font-mono select-none">
                  /api/v1/kv/
                </div>
                <Input
                  placeholder="enter-key-name..."
                  value={key}
                  onChange={(e) => setKey(e.target.value)}
                  className="bg-white/[0.02] border-white/[0.08] text-xs h-10 font-mono text-white/80 placeholder:text-white/20 focus-visible:ring-emerald-500/30 flex-1"
                />
                <Button
                  onClick={handleSend}
                  disabled={loading || !key.trim()}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-xs h-10 px-4 gap-1.5"
                >
                  <Send className="h-3.5 w-3.5" />
                  Send
                </Button>
              </div>

              {/* POST Request Body area */}
              {method === 'POST' && (
                <div className="flex-1 flex flex-col space-y-1.5 min-h-[140px]">
                  <label className="text-[11px] font-semibold uppercase tracking-wider text-white/35">
                    Request Value / Body
                  </label>
                  <textarea
                    placeholder="Enter value string or JSON body..."
                    value={requestBody}
                    onChange={(e) => setRequestBody(e.target.value)}
                    className="w-full flex-1 rounded-md border border-white/[0.08] bg-white/[0.02] p-3 text-xs font-mono text-white/80 placeholder:text-white/20 focus:outline-none focus:ring-1 focus:ring-emerald-500/30"
                  />
                </div>
              )}

              {/* Request History Log */}
              <div className="flex-1 flex flex-col space-y-2 overflow-hidden border-t border-white/[0.04] pt-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <History className="h-3.5 w-3.5 text-white/30" />
                    <span className="text-[11px] font-semibold uppercase tracking-wider text-white/35">
                      Request History
                    </span>
                  </div>
                  {history.length > 0 && (
                    <button
                      onClick={clearHistory}
                      className="text-[10px] text-rose-400/60 hover:text-rose-400 hover:underline flex items-center gap-1"
                    >
                      <Trash2 className="h-3 w-3" />
                      Clear
                    </button>
                  )}
                </div>

                <div className="flex-1 overflow-y-auto divide-y divide-white/[0.03]">
                  {history.length === 0 ? (
                    <div className="flex flex-col items-center justify-center p-4 text-center h-full">
                      <Terminal className="h-6 w-6 text-white/10 mb-1" />
                      <span className="text-[11px] text-white/25">No recent requests</span>
                    </div>
                  ) : (
                    history.map((item) => (
                      <button
                        key={item.id}
                        onClick={() => loadHistoryItem(item)}
                        className="w-full flex items-center justify-between py-2.5 text-left text-xs text-white/50 hover:bg-white/[0.02] transition-all px-1 rounded"
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          <span
                            className={`text-[9px] font-semibold px-1.5 py-0.5 rounded font-mono ${
                              item.method === 'GET'
                                ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                                : item.method === 'POST'
                                ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                            }`}
                          >
                            {item.method}
                          </span>
                          <span className="font-mono text-white/70 truncate max-w-[140px]">
                            {item.key}
                          </span>
                        </div>
                        <div className="flex items-center gap-3">
                          <span
                            className={`font-semibold text-[10px] ${
                              item.status >= 200 && item.status < 300
                                ? 'text-emerald-400'
                                : 'text-rose-400'
                            }`}
                          >
                            {item.status}
                          </span>
                          <span className="text-[10px] text-white/20 font-mono">
                            {item.latencyMs}ms
                          </span>
                        </div>
                      </button>
                    ))
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Right Side: Response View (5 cols) */}
        <div className="lg:col-span-5 flex flex-col gap-6 overflow-hidden">
          <Card className="border-white/[0.06] bg-[#111113] flex-1 flex flex-col overflow-hidden">
            <CardContent className="p-5 flex flex-col h-full space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-white/35">
                  Response Console
                </span>
                {responseStatus !== null && (
                  <div className="flex items-center gap-3">
                    <Badge
                      className={`text-[10px] font-semibold tracking-wider ${
                        responseStatus >= 200 && responseStatus < 300
                          ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                          : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                      }`}
                    >
                      {responseStatus} {responseStatusText}
                    </Badge>
                    <div className="flex items-center gap-1 text-[10px] text-white/35 font-mono">
                      <Clock className="h-3 w-3" />
                      <span>{responseLatency}ms</span>
                    </div>
                  </div>
                )}
              </div>

              {responseStatus !== null ? (
                <div className="flex-1 flex flex-col space-y-4 overflow-hidden">
                  {/* Headers sub-panel */}
                  <div className="space-y-1.5">
                    <span className="text-[9px] font-semibold uppercase tracking-wider text-white/20 block">
                      Headers
                    </span>
                    <div className="border border-white/[0.06] bg-black/20 rounded-lg p-3 text-[10px] font-mono text-white/50 max-h-[100px] overflow-y-auto space-y-1">
                      {Object.keys(responseHeaders).length === 0 ? (
                        <span className="italic text-white/20">No response headers</span>
                      ) : (
                        Object.entries(responseHeaders).map(([k, v]) => (
                          <div key={k} className="flex justify-between">
                            <span className="text-white/30">{k}</span>
                            <span className="text-white/60 truncate max-w-[200px]">{v}</span>
                          </div>
                        ))
                      )}
                    </div>
                  </div>

                  {/* Body output panel */}
                  <div className="flex-1 flex flex-col space-y-1.5 overflow-hidden">
                    <div className="flex items-center justify-between">
                      <span className="text-[9px] font-semibold uppercase tracking-wider text-white/20 block">
                        Response Body
                      </span>
                      {typeof responseBody === 'object' && (
                        <Badge className="gap-1 bg-purple-500/10 text-purple-400 border border-purple-500/20 text-[9px] font-semibold tracking-wider uppercase py-0.5 px-1">
                          <FileJson className="h-3.5 w-3.5" />
                          JSON
                        </Badge>
                      )}
                    </div>

                    <div className="flex-1 border border-white/[0.06] bg-black/40 rounded-lg p-4 font-mono text-xs overflow-auto text-white/80">
                      {typeof responseBody === 'object' ? (
                        <pre className="text-emerald-400/90">
                          {JSON.stringify(responseBody, null, 2)}
                        </pre>
                      ) : (
                        <pre className="whitespace-pre-wrap">{responseBody}</pre>
                      )}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex-1 flex flex-col items-center justify-center text-center p-8">
                  <Database className="h-8 w-8 text-white/10 mb-2" />
                  <span className="text-xs text-white/30 max-w-[200px]">
                    Configure and send a request to view the response telemetry.
                  </span>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
