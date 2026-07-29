'use client';

import { Terminal as TerminalIcon } from 'lucide-react';
import { DocHeader } from '@/components/ui/doc-header';
import { CodeBlock } from '@/components/ui/code-block';
import { docsConfig } from '@/lib/docs-config';

export default function CliPage() {
  return (
    <div className="space-y-8 max-w-5xl">
      {/* Header */}
      <DocHeader
        title="AtlasKV Command-Line Tool (CLI)"
        description="Native command-line interface for inspecting and interacting with Raft clusters."
        icon={TerminalIcon}
        repoUrl={docsConfig.cliRepo}
      />

      {/* Installation */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          1. Installation
        </h2>
        <CodeBlock
          title="macOS / Linux / Windows"
          language="bash"
          code={`# Homebrew (macOS / Linux)
brew install atlaskv

# Go install
go install github.com/Immortal4728/AtlasKV/cmd/atlaskv@latest`}
        />
      </section>

      {/* Basic Commands */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          2. Endpoint Configuration
        </h2>
        <CodeBlock
          title="Set Endpoint"
          language="bash"
          code={`atlaskv config set-endpoint http://localhost:8080`}
        />
      </section>

      {/* Key-Value Commands */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          3. Key-Value Commands
        </h2>
        <div className="space-y-4">
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono">atlaskv put</h3>
            <CodeBlock
              language="bash"
              code={`atlaskv put app/server/port 8080`}
            />
          </div>
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono">atlaskv get</h3>
            <CodeBlock
              language="bash"
              code={`atlaskv get app/server/port`}
            />
          </div>
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono">atlaskv delete</h3>
            <CodeBlock
              language="bash"
              code={`atlaskv delete app/server/port`}
            />
          </div>
        </div>
      </section>

      {/* Prefix Scans */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          4. Prefix Queries
        </h2>
        <CodeBlock
          title="atlaskv prefix"
          language="bash"
          code={`atlaskv prefix app/`}
        />
      </section>

      {/* Lease Commands */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          5. Distributed Leases
        </h2>
        <CodeBlock
          title="atlaskv lease"
          language="bash"
          code={`# Grant lease for 10 seconds
atlaskv lease grant worker-lock --ttl=10s

# Keep-alive heartbeat
atlaskv lease keepalive worker-lock`}
        />
      </section>

      {/* Watch Stream */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          6. Real-Time Watch Stream
        </h2>
        <CodeBlock
          title="atlaskv watch"
          language="bash"
          code={`atlaskv watch app/`}
        />
      </section>

      {/* Revision History */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          7. Key Revision History
        </h2>
        <CodeBlock
          title="atlaskv history"
          language="bash"
          code={`atlaskv history app/server/port`}
        />
      </section>
    </div>
  );
}
