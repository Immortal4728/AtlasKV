'use client';

import { FileCode2 } from 'lucide-react';
import { DocHeader } from '@/components/ui/doc-header';
import { CodeBlock } from '@/components/ui/code-block';
import { docsConfig } from '@/lib/docs-config';

export default function TsSdkPage() {
  return (
    <div className="space-y-8 max-w-5xl">
      {/* Header */}
      <DocHeader
        title="TypeScript SDK Documentation"
        description="Official TypeScript / JavaScript client library for Node.js, Deno, Bun, React, and Next.js."
        icon={FileCode2}
        repoUrl={docsConfig.tsSdkRepo}
      />

      {/* Package Installation */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          1. Package Installation
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">npm</h3>
            <CodeBlock language="bash" code="npm install @atlaskv/client" />
          </div>
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">pnpm</h3>
            <CodeBlock language="bash" code="pnpm add @atlaskv/client" />
          </div>
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">bun</h3>
            <CodeBlock language="bash" code="bun add @atlaskv/client" />
          </div>
        </div>
      </section>

      {/* Node.js Client Initialization */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          2. Client Initialization (Node.js / Bun)
        </h2>
        <CodeBlock
          title="client.ts"
          language="typescript"
          code={`import { AtlasKV } from '@atlaskv/client';

export const kv = new AtlasKV({
  endpoint: 'http://localhost:8081',
  timeoutMs: 5000,
});`}
        />
      </section>

      {/* Next.js & React Example */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          3. Next.js & React Integration
        </h2>
        <CodeBlock
          title="app/components/StatusBanner.tsx"
          language="tsx"
          code={`'use client';

import { useAtlasKVKey } from '@atlaskv/react';

export function StatusBanner() {
  const { data: status, loading } = useAtlasKVKey('system:maintenance_mode');

  if (loading) return <div>Loading...</div>;
  if (status === 'true') {
    return <div className="bg-amber-500 text-black font-bold p-2">System Under Maintenance</div>;
  }

  return <div className="bg-emerald-500 text-white font-bold p-2">System Operational</div>;
}`}
        />
      </section>

      {/* CRUD Operations */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          4. CRUD Operations
        </h2>
        <CodeBlock
          title="crud.ts"
          language="typescript"
          code={`// Put key
await kv.put('user:101', JSON.stringify({ id: 101, name: 'Bob' }));

// Get key
const user = await kv.get<string>('user:101');

// Delete key
const success = await kv.delete('user:101');`}
        />
      </section>

      {/* Watch API */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          5. Watch API (SSE Stream)
        </h2>
        <CodeBlock
          title="watch.ts"
          language="typescript"
          code={`const watcher = kv.watchPrefix('config/');

watcher.on('change', (event) => {
  console.log(\`Key \${event.key} updated to \${event.value}\`);
});

watcher.on('error', (err) => {
  console.error('Watch stream error:', err);
});`}
        />
      </section>

      {/* Distributed Lease API */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          6. Distributed Lease Management
        </h2>
        <CodeBlock
          title="lease.ts"
          language="typescript"
          code={`// Acquire lease with 10s TTL
const lease = await kv.grantLease('job-lock-01', 10);

// Auto keep-alive heartbeat loop
lease.keepAlive();

// Release lock
await lease.revoke();`}
        />
      </section>

      {/* Prefix Scans */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          7. Prefix Queries & Scanning
        </h2>
        <CodeBlock
          title="prefixScan.ts"
          language="typescript"
          code={`const entries = await kv.scanPrefix('settings:');

entries.forEach(({ key, value }) => {
  console.log(\`\${key} => \${value}\`);
});`}
        />
      </section>
    </div>
  );
}
