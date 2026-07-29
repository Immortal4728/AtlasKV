'use client';

import { useState } from 'react';
import { BookOpen, ShieldAlert } from 'lucide-react';
import { DocHeader } from '@/components/ui/doc-header';
import { CodeBlock } from '@/components/ui/code-block';
import { docsConfig } from '@/lib/docs-config';
import { cn } from '@/lib/utils';

interface ApiEndpoint {
  method: 'GET' | 'PUT' | 'POST' | 'DELETE';
  path: string;
  category: 'Health' | 'Keys' | 'Prefix' | 'Leases' | 'Watch' | 'Metrics' | 'History' | 'Settings';
  description: string;
  request: string;
  response: string;
}

const apiEndpoints: ApiEndpoint[] = [
  {
    category: 'Health',
    method: 'GET',
    path: '/health',
    description: 'Check Raft cluster health and leader readiness.',
    request: `curl -s ${docsConfig.defaultApiUrl}/health`,
    response: `{
  "status": "UP",
  "leader": "node1",
  "term": 3,
  "nodes": 3
}`,
  },
  {
    category: 'Keys',
    method: 'GET',
    path: '/api/v1/keys/{key}',
    description: 'Fetch the value and metadata for a specific key.',
    request: `curl -s ${docsConfig.defaultApiUrl}/api/v1/keys/app/config/theme`,
    response: `{
  "key": "app/config/theme",
  "value": "dark-modern",
  "version": 4,
  "lease_id": null
}`,
  },
  {
    category: 'Keys',
    method: 'PUT',
    path: '/api/v1/keys/{key}',
    description: 'Create or update a key-value entry in the Raft state machine.',
    request: `curl -X PUT ${docsConfig.defaultApiUrl}/api/v1/keys/app/config/theme \\
  -H "Content-Type: application/json" \\
  -d '{"value": "dark-modern"}'`,
    response: `{
  "success": true,
  "key": "app/config/theme",
  "version": 5,
  "index": 128
}`,
  },
  {
    category: 'Keys',
    method: 'DELETE',
    path: '/api/v1/keys/{key}',
    description: 'Delete a key from the key-value store.',
    request: `curl -X DELETE ${docsConfig.defaultApiUrl}/api/v1/keys/app/config/theme`,
    response: `{
  "success": true,
  "deleted_key": "app/config/theme"
}`,
  },
  {
    category: 'Prefix',
    method: 'GET',
    path: '/api/v1/prefix/{prefix}',
    description: 'Scan and return all key-value entries matching a prefix.',
    request: `curl -s "${docsConfig.defaultApiUrl}/api/v1/prefix/app/"`,
    response: `[
  { "key": "app/config/theme", "value": "dark-modern", "version": 5 },
  { "key": "app/config/port", "value": "8080", "version": 2 }
]`,
  },
  {
    category: 'Leases',
    method: 'POST',
    path: '/api/v1/leases',
    description: 'Grant a new distributed lease with a TTL in seconds.',
    request: `curl -X POST ${docsConfig.defaultApiUrl}/api/v1/leases \\
  -H "Content-Type: application/json" \\
  -d '{"id": "lease-01", "ttl_seconds": 15}'`,
    response: `{
  "lease_id": "lease-01",
  "ttl_seconds": 15,
  "granted_at": "2026-07-29T10:00:00Z"
}`,
  },
  {
    category: 'Leases',
    method: 'POST',
    path: '/api/v1/leases/{id}/keepalive',
    description: 'Send a keep-alive heartbeat to refresh lease TTL.',
    request: `curl -X POST ${docsConfig.defaultApiUrl}/api/v1/leases/lease-01/keepalive`,
    response: `{
  "lease_id": "lease-01",
  "status": "RENEWED",
  "remaining_ttl_seconds": 15
}`,
  },
  {
    category: 'Watch',
    method: 'GET',
    path: '/api/v1/watch/prefix/{prefix}',
    description: 'Subscribe to real-time Server-Sent Events (SSE) key updates.',
    request: `curl -N ${docsConfig.defaultApiUrl}/api/v1/watch/prefix/app/`,
    response: `event: put
data: {"type":"PUT","key":"app/config/theme","value":"dark-modern","version":6}

event: delete
data: {"type":"DELETE","key":"app/config/temp","version":7}`,
  },
  {
    category: 'Metrics',
    method: 'GET',
    path: '/api/v1/metrics',
    description: 'Get Prometheus and Raft cluster performance metrics.',
    request: `curl -s ${docsConfig.defaultApiUrl}/api/v1/metrics`,
    response: `{
  "commit_index": 128,
  "applied_index": 128,
  "read_latency_p50_ms": 0.42,
  "read_latency_p99_ms": 1.25,
  "active_leases": 4
}`,
  },
  {
    category: 'History',
    method: 'GET',
    path: '/api/v1/history/{key}',
    description: 'Fetch append-only revision history timeline for a key.',
    request: `curl -s ${docsConfig.defaultApiUrl}/api/v1/history/app/config/theme`,
    response: `[
  { "version": 6, "value": "dark-modern", "timestamp": "2026-07-29T10:05:00Z" },
  { "version": 5, "value": "light", "timestamp": "2026-07-29T09:30:00Z" }
]`,
  },
  {
    category: 'Settings',
    method: 'GET',
    path: '/api/v1/status',
    description: 'Get Raft node role, term, and cluster member details.',
    request: `curl -s ${docsConfig.defaultApiUrl}/api/v1/status`,
    response: `{
  "node_id": "node1",
  "role": "LEADER",
  "term": 3,
  "leader": "node1"
}`,
  },
];

const categories = ['ALL', 'Health', 'Keys', 'Prefix', 'Leases', 'Watch', 'Metrics', 'History', 'Settings'] as const;

export default function ApiReferencePage() {
  const [selectedCategory, setSelectedCategory] = useState<typeof categories[number]>('ALL');

  const filtered = apiEndpoints.filter(
    (ep) => selectedCategory === 'ALL' || ep.category === selectedCategory
  );

  return (
    <div className="space-y-8 max-w-5xl">
      {/* Header */}
      <DocHeader
        title="API Reference"
        description="REST API endpoints for AtlasKV cluster management."
        icon={BookOpen}
        repoUrl={docsConfig.githubRepo}
      />

      {/* Category Tabs */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-2 border-b border-border dark:border-[oklch(1_0_0/8%)]">
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => setSelectedCategory(cat)}
            className={cn(
              'px-3 py-1.5 rounded-lg text-xs font-mono font-bold transition-all cursor-pointer whitespace-nowrap',
              selectedCategory === cat
                ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30'
                : 'text-neutral-600 dark:text-neutral-400 hover:text-[var(--foreground)] hover:bg-neutral-100 dark:hover:bg-[oklch(1_0_0/4%)]'
            )}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* API Endpoint List */}
      <div className="space-y-6">
        {filtered.map((ep, idx) => (
          <div
            key={idx}
            className="glass-card rounded-xl p-5 border border-border dark:border-[oklch(1_0_0/8%)] space-y-4"
          >
            {/* Endpoint Header */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border dark:border-[oklch(1_0_0/6%)] pb-3">
              <div className="flex items-center gap-3">
                <span
                  className={cn(
                    'px-2.5 py-1 rounded-md font-mono text-xs font-bold uppercase tracking-wider',
                    ep.method === 'GET' && 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/30',
                    ep.method === 'PUT' && 'bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/30',
                    ep.method === 'POST' && 'bg-cyan-500/15 text-cyan-600 dark:text-cyan-400 border border-cyan-500/30',
                    ep.method === 'DELETE' && 'bg-rose-500/15 text-rose-600 dark:text-rose-400 border border-rose-500/30'
                  )}
                >
                  {ep.method}
                </span>
                <code className="text-sm font-mono font-bold text-[var(--foreground)]">{ep.path}</code>
              </div>

              <div className="flex items-center gap-2">
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-mono font-semibold bg-neutral-200/80 dark:bg-[oklch(1_0_0/6%)] text-neutral-600 dark:text-neutral-400 border border-border dark:border-[oklch(1_0_0/10%)]">
                  <ShieldAlert className="h-3 w-3 text-amber-500" />
                  Auth: Coming Soon
                </span>
              </div>
            </div>

            <p className="text-xs text-neutral-600 dark:text-neutral-400 font-sans leading-relaxed">
              {ep.description}
            </p>

            {/* Request & Response Blocks */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              <div>
                <h4 className="text-xs font-bold text-neutral-700 dark:text-neutral-300 font-mono mb-1">Sample Request</h4>
                <CodeBlock title="cURL" language="bash" code={ep.request} />
              </div>
              <div>
                <h4 className="text-xs font-bold text-neutral-700 dark:text-neutral-300 font-mono mb-1">Response (JSON)</h4>
                <CodeBlock title="Response" language="json" code={ep.response} />
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
