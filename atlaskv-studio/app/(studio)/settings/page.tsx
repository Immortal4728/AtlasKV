'use client';

import { useState } from 'react';
import { motion } from 'framer-motion';
import { Settings, Server, Save, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';

export default function SettingsPage() {
  const [endpoint, setEndpoint] = useState('');
  const [timeoutMs, setTimeoutMs] = useState('5000');
  const [refreshIntervalSec, setRefreshIntervalSec] = useState('2');
  const [saved, setSaved] = useState(false);

  const handleSave = () => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('atlaskv-server-url', endpoint);
    }
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <PageHeader
        title="Settings"
        description="Configure Studio and cluster preferences."
        icon={Settings}
        iconColor="text-emerald-400"
      />

      {/* Connection Settings Card */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.1 }}
        className="glass-card rounded-xl p-6 space-y-4 border border-border dark:border-[oklch(1_0_0/6%)]"
      >
        <div className="flex items-center gap-2 border-b border-border dark:border-[oklch(1_0_0/6%)] pb-3">
          <Server className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
          <h2 className="text-sm font-bold text-[var(--foreground)]">Cluster REST Endpoint</h2>
        </div>

        <div className="space-y-4 text-xs">
          <div className="space-y-1.5">
            <label className="font-bold text-neutral-700 dark:text-[oklch(1_0_0/55%)] font-mono text-[11px]">AtlasKV Server Base URL</label>
            <Input
              value={endpoint}
              onChange={(e) => setEndpoint(e.target.value)}
              placeholder="http://localhost:8081"
              className="bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] focus-visible:ring-emerald-500/30"
            />
            <p className="text-[11px] text-neutral-600 dark:text-[oklch(1_0_0/30%)]">
              Enter any cluster node endpoint. The SDK/Studio will automatically handle 503 Leader Redirection.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 dark:text-[oklch(1_0_0/55%)] font-mono text-[11px]">HTTP Request Timeout (ms)</label>
              <Input
                value={timeoutMs}
                onChange={(e) => setTimeoutMs(e.target.value)}
                className="bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] focus-visible:ring-emerald-500/30"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 dark:text-[oklch(1_0_0/55%)] font-mono text-[11px]">Metrics Auto-Refresh (seconds)</label>
              <Input
                value={refreshIntervalSec}
                onChange={(e) => setRefreshIntervalSec(e.target.value)}
                className="bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] focus-visible:ring-emerald-500/30"
              />
            </div>
          </div>
        </div>
      </motion.div>

      {/* Save Button */}
      <div className="flex items-center justify-end gap-3 pt-2">
        <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
          <Button
            onClick={handleSave}
            className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-semibold text-xs px-5 py-2.5 shadow-lg shadow-emerald-500/20 gap-2 rounded-lg border-0"
          >
            {saved ? <Check className="h-4 w-4" /> : <Save className="h-4 w-4" />}
            {saved ? 'Settings Saved!' : 'Save Preferences'}
          </Button>
        </motion.div>
      </div>
    </div>
  );
}
