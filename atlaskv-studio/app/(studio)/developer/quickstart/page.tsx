'use client';

import { Rocket, Code2, FileCode2, Terminal as TerminalIcon, Globe, ArrowRight } from 'lucide-react';
import { DocHeader } from '@/components/ui/doc-header';
import { CodeBlock } from '@/components/ui/code-block';
import { docsConfig } from '@/lib/docs-config';
import Link from 'next/link';

export default function QuickStartPage() {
  return (
    <div className="space-y-8 max-w-5xl">
      {/* Header */}
      <DocHeader
        title="Quick Start"
        description="Connect AtlasKV to your application."
        icon={Rocket}
      />

      {/* Getting Started Overview */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          1. Getting Started
        </h2>
        <p className="text-xs text-neutral-600 dark:text-neutral-400 font-sans leading-relaxed">
          AtlasKV exposes a gRPC and REST API on port <code className="font-mono bg-[var(--surface-2)] px-1.5 py-0.5 rounded text-emerald-600 dark:text-emerald-400">8080</code> for key-value operations, prefix queries, distributed leases, and Server-Sent Events (SSE) watch streams.
        </p>
        <CodeBlock
          title="Verify Local or Remote Cluster Health"
          language="bash"
          code={`curl -s ${docsConfig.defaultApiUrl}/health | jq`}
        />
      </section>

      {/* Java SDK Section */}
      <section className="space-y-3 pt-2">
        <div className="flex items-center justify-between border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider flex items-center gap-2">
            <Code2 className="h-4 w-4 text-purple-500" />
            2. Java SDK
          </h2>
          <Link
            href="/developer/java-sdk"
            className="text-xs font-mono text-purple-600 dark:text-purple-400 hover:underline flex items-center gap-1 font-semibold"
          >
            <span>Full Java SDK Guide</span>
            <ArrowRight className="h-3 w-3" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">Maven</h3>
            <CodeBlock
              title="pom.xml"
              language="xml"
              code={`<dependency>
    <groupId>io.atlaskv</groupId>
    <artifactId>atlaskv-client</artifactId>
    <version>3.3.0</version>
</dependency>`}
            />
          </div>
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">Gradle</h3>
            <CodeBlock
              title="build.gradle"
              language="groovy"
              code={`implementation 'io.atlaskv:atlaskv-client:3.3.0'`}
            />
          </div>
        </div>

        <CodeBlock
          title="Java Quick Example"
          language="java"
          code={`import io.atlaskv.client.AtlasKVClient;

AtlasKVClient client = AtlasKVClient.builder()
    .endpoint("http://localhost:8081")
    .build();

// Put & Get
client.put("app/config/theme", "dark-modern");
String theme = client.get("app/config/theme");
System.out.println("Config theme: " + theme);`}
        />
      </section>

      {/* TypeScript SDK Section */}
      <section className="space-y-3 pt-2">
        <div className="flex items-center justify-between border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider flex items-center gap-2">
            <FileCode2 className="h-4 w-4 text-cyan-500" />
            3. TypeScript SDK
          </h2>
          <Link
            href="/developer/ts-sdk"
            className="text-xs font-mono text-cyan-600 dark:text-cyan-400 hover:underline flex items-center gap-1 font-semibold"
          >
            <span>Full TypeScript Guide</span>
            <ArrowRight className="h-3 w-3" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">npm</h3>
            <CodeBlock
              title="npm"
              language="bash"
              code={`npm install @atlaskv/client`}
            />
          </div>
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">pnpm</h3>
            <CodeBlock
              title="pnpm"
              language="bash"
              code={`pnpm add @atlaskv/client`}
            />
          </div>
        </div>

        <CodeBlock
          title="TypeScript Example"
          language="typescript"
          code={`import { AtlasKV } from '@atlaskv/client';

const client = new AtlasKV({ endpoint: 'http://localhost:8080' });

async function main() {
  await client.put('session:user_102', JSON.stringify({ role: 'admin', active: true }));
  const val = await client.get('session:user_102');
  console.log('Session:', val);
}

main();`}
        />
      </section>

      {/* CLI Section */}
      <section className="space-y-3 pt-2">
        <div className="flex items-center justify-between border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider flex items-center gap-2">
            <TerminalIcon className="h-4 w-4 text-emerald-500" />
            4. CLI Tool
          </h2>
          <Link
            href="/developer/cli"
            className="text-xs font-mono text-emerald-600 dark:text-emerald-400 hover:underline flex items-center gap-1 font-semibold"
          >
            <span>Full CLI Guide</span>
            <ArrowRight className="h-3 w-3" />
          </Link>
        </div>

        <CodeBlock
          title="CLI Installation & Commands"
          language="bash"
          code={`# Install CLI
brew install atlaskv

# Store & query key
atlaskv put app/db/url "postgres://localhost:5432/production"
atlaskv get app/db/url

# Watch prefix updates live
atlaskv watch app/`}
        />
      </section>

      {/* REST API Section */}
      <section className="space-y-3 pt-2">
        <div className="flex items-center justify-between border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider flex items-center gap-2">
            <Globe className="h-4 w-4 text-amber-500" />
            5. REST API
          </h2>
          <Link
            href="/developer/api-reference"
            className="text-xs font-mono text-amber-600 dark:text-amber-400 hover:underline flex items-center gap-1 font-semibold"
          >
            <span>Full REST API Reference</span>
            <ArrowRight className="h-3 w-3" />
          </Link>
        </div>

        <CodeBlock
          title="Sample cURL Requests"
          language="bash"
          code={`# Create or Update Key
curl -X PUT http://localhost:8080/api/v1/keys/config/max_connections \\
  -H "Content-Type: application/json" \\
  -d '{"value": "500"}'

# Fetch Key
curl -s http://localhost:8080/api/v1/keys/config/max_connections

# Prefix Query
curl -s "http://localhost:8080/api/v1/prefix/config/"`}
        />
      </section>
    </div>
  );
}
