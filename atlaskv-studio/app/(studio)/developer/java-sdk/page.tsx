'use client';

import { Code2 } from 'lucide-react';
import { DocHeader } from '@/components/ui/doc-header';
import { CodeBlock } from '@/components/ui/code-block';
import { docsConfig } from '@/lib/docs-config';

export default function JavaSdkPage() {
  return (
    <div className="space-y-8 max-w-5xl">
      {/* Header */}
      <DocHeader
        title="Java SDK Documentation"
        description="Official Java client library for AtlasKV distributed consensus store."
        icon={Code2}
        repoUrl={docsConfig.javaSdkRepo}
      />

      {/* Installation */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          1. Installation & Dependency
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">Maven</h3>
            <CodeBlock
              title="pom.xml"
              language="xml"
              code={`<dependency>
    <groupId>io.atlaskv</groupId>
    <artifactId>atlaskv-client</artifactId>
    <version>${docsConfig.version.replace('v', '')}</version>
</dependency>`}
            />
          </div>
          <div>
            <h3 className="text-xs font-bold text-[var(--foreground)] font-mono mb-1">Gradle</h3>
            <CodeBlock
              title="build.gradle"
              language="groovy"
              code={`implementation 'io.atlaskv:atlaskv-client:${docsConfig.version.replace('v', '')}'`}
            />
          </div>
        </div>
      </section>

      {/* Client Connection */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          2. Client Initialization
        </h2>
        <CodeBlock
          title="AtlasKVClientInitialization.java"
          language="java"
          code={`import io.atlaskv.client.AtlasKVClient;
import java.time.Duration;

AtlasKVClient client = AtlasKVClient.builder()
    .endpoint("http://localhost:8081")
    .timeout(Duration.ofSeconds(5))
    .maxRetries(3)
    .build();`}
        />
      </section>

      {/* Spring Boot Integration */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          3. Spring Boot Integration
        </h2>
        <CodeBlock
          title="AtlasKVConfig.java"
          language="java"
          code={`import io.atlaskv.client.AtlasKVClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtlasKVConfig {

    @Bean
    public AtlasKVClient atlasKVClient() {
        return AtlasKVClient.builder()
            .endpoint("http://localhost:8081")
            .build();
    }
}`}
        />
      </section>

      {/* CRUD Examples */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          4. Basic CRUD Operations
        </h2>
        <CodeBlock
          title="CrudOperations.java"
          language="java"
          code={`// Put key
client.put("user:1001", "{\"name\": \"Alice\", \"role\": \"admin\"}");

// Get key
String userJson = client.get("user:1001");

// Delete key
boolean deleted = client.delete("user:1001");`}
        />
      </section>

      {/* Transactions & CAS */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          5. Atomic Compare-And-Swap (CAS)
        </h2>
        <CodeBlock
          title="CasTransaction.java"
          language="java"
          code={`// Atomic CAS: Replace expected old value with new value
boolean updated = client.compareAndSwap(
    "config:status",
    "PENDING",    // Expected current value
    "ACTIVE"      // Target new value
);

if (updated) {
    System.out.println("Status updated to ACTIVE");
}`}
        />
      </section>

      {/* Distributed Leases */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          6. Distributed Lease API
        </h2>
        <CodeBlock
          title="LeaseManagement.java"
          language="java"
          code={`// Grant 15 second TTL lease
String leaseId = client.grantLease("leader-lock", Duration.ofSeconds(15));

// Keep-alive heartbeat loop
client.keepAlive(leaseId);

// Revoke lease
client.revokeLease(leaseId);`}
        />
      </section>

      {/* Prefix Queries */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          7. Prefix Queries
        </h2>
        <CodeBlock
          title="PrefixScan.java"
          language="java"
          code={`import java.util.Map;

// Scan all keys starting with prefix 'user:'
Map<String, String> userKeys = client.scanPrefix("user:");

userKeys.forEach((key, val) -> {
    System.out.println(key + " -> " + val);
});`}
        />
      </section>

      {/* Watch API */}
      <section className="space-y-3">
        <h2 className="text-sm font-bold text-[var(--foreground)] uppercase font-mono tracking-wider border-b border-border dark:border-[oklch(1_0_0/8%)] pb-2">
          8. Real-Time Watch Stream API
        </h2>
        <CodeBlock
          title="WatchListener.java"
          language="java"
          code={`client.watchPrefix("config/", event -> {
    System.out.println("Event Type: " + event.getType());
    System.out.println("Key Modified: " + event.getKey());
    System.out.println("New Value: " + event.getValue());
});`}
        />
      </section>
    </div>
  );
}
