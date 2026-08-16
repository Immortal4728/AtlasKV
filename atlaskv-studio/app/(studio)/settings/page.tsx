'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
  Settings,
  Server,
  Key,
  Save,
  Check,
  Shield,
  ShieldAlert,
  ShieldCheck,
  User,
  Layers,
  Lock,
  Globe,
  RefreshCw,
  LogOut,
  Eye,
  EyeOff,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { PageHeader } from '@/components/ui/page-header';
import { useAuth } from '@/hooks/use-auth';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

export default function SettingsPage() {
  const {
    authInfo,
    isLoading: authLoading,
    isError: authError,
    apiKey: currentApiKey,
    adminNamespace: currentAdminNs,
    serverUrl: currentServerUrl,
    isAdmin,
    isUser,
    activeNamespace,
    userId,
    username,
    role,
    isAuthenticated,
    updateApiKey,
    updateAdminNamespace,
    updateServerUrl,
    disconnect,
    refetch: refetchAuth,
  } = useAuth();

  const [endpoint, setEndpoint] = useState('');
  const [apiKeyInput, setApiKeyInput] = useState('');
  const [showApiKey, setShowApiKey] = useState(false);
  const [adminNsInput, setAdminNsInput] = useState('');
  const [timeoutMs, setTimeoutMs] = useState('5000');
  const [refreshIntervalSec, setRefreshIntervalSec] = useState('2');
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    setEndpoint(currentServerUrl);
    setApiKeyInput(currentApiKey);
    setAdminNsInput(currentAdminNs);
  }, [currentServerUrl, currentApiKey, currentAdminNs]);

  const handleSaveConnection = async () => {
    updateServerUrl(endpoint);
    updateApiKey(apiKeyInput);
    if (isAdmin) {
      updateAdminNamespace(adminNsInput);
    }
    setSaved(true);
    toast.success('Connection settings saved and authenticated');
    setTimeout(() => setSaved(false), 2500);
  };

  const handleDisconnect = () => {
    disconnect();
    setApiKeyInput('');
    setAdminNsInput('');
    toast.info('API key and credentials cleared');
  };

  const handleApplyNamespace = () => {
    updateAdminNamespace(adminNsInput);
    toast.success(
      adminNsInput.trim()
        ? `Target namespace switched to '${adminNsInput.trim()}'`
        : 'Switched to Global Root Keyspace'
    );
  };

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Header */}
      <PageHeader
        title="Settings & Authentication"
        description="Configure remote AtlasKV connection, API keys, and multi-tenant namespace targeting."
        icon={Settings}
        iconColor="text-emerald-400"
      />

      {/* Authenticated Identity Card */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className={cn(
          'rounded-xl p-6 border transition-all glass-card space-y-4',
          authError
            ? 'border-rose-500/30 bg-rose-500/5'
            : isAdmin
            ? 'border-amber-500/30 bg-amber-500/5'
            : 'border-indigo-500/30 bg-indigo-500/5'
        )}
      >
        <div className="flex items-center justify-between border-b border-border/60 dark:border-[oklch(1_0_0/8%)] pb-3">
          <div className="flex items-center gap-2.5">
            {authError ? (
              <ShieldAlert className="h-5 w-5 text-rose-500" />
            ) : isAdmin ? (
              <ShieldCheck className="h-5 w-5 text-amber-500" />
            ) : (
              <User className="h-5 w-5 text-indigo-500" />
            )}
            <div>
              <h2 className="text-sm font-bold text-[var(--foreground)]">Active Session Identity</h2>
              <p className="text-[11px] text-neutral-500 dark:text-neutral-400">
                Principal authenticated by AtlasKV server
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button
              onClick={() => {
                refetchAuth();
                toast.info('Refreshed authentication state');
              }}
              variant="outline"
              size="sm"
              className="text-xs h-7 gap-1 border-border font-mono"
            >
              <RefreshCw className={cn('h-3 w-3', authLoading && 'animate-spin')} />
              Refresh
            </Button>

            {currentApiKey && (
              <Button
                onClick={handleDisconnect}
                variant="outline"
                size="sm"
                className="text-xs h-7 gap-1 text-rose-500 border-rose-500/30 hover:bg-rose-500/10 font-mono"
              >
                <LogOut className="h-3 w-3" />
                Clear Key
              </Button>
            )}
          </div>
        </div>

        {authError ? (
          <div className="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-600 dark:text-rose-400 text-xs font-mono">
            ⚠️ Authentication failed: Invalid or missing API key. Enter a valid key below to authenticate.
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 font-mono text-xs">
            <div className="p-3 rounded-lg bg-neutral-100/80 dark:bg-black/20 border border-border/40">
              <span className="text-[10px] text-neutral-500 uppercase tracking-wider block font-bold">
                User ID
              </span>
              <span className="text-sm font-bold text-[var(--foreground)] truncate block mt-0.5">
                {userId}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-neutral-100/80 dark:bg-black/20 border border-border/40">
              <span className="text-[10px] text-neutral-500 uppercase tracking-wider block font-bold">
                Display Name
              </span>
              <span className="text-sm font-bold text-[var(--foreground)] truncate block mt-0.5">
                {username}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-neutral-100/80 dark:bg-black/20 border border-border/40">
              <span className="text-[10px] text-neutral-500 uppercase tracking-wider block font-bold">
                Role
              </span>
              <span
                className={cn(
                  'inline-block px-2 py-0.5 rounded text-[11px] font-bold mt-1',
                  isAdmin
                    ? 'bg-amber-500/20 text-amber-600 dark:text-amber-400 border border-amber-500/30'
                    : 'bg-indigo-500/20 text-indigo-600 dark:text-indigo-400 border border-indigo-500/30'
                )}
              >
                {role}
              </span>
            </div>

            <div className="p-3 rounded-lg bg-neutral-100/80 dark:bg-black/20 border border-border/40">
              <span className="text-[10px] text-neutral-500 uppercase tracking-wider block font-bold">
                Active Namespace
              </span>
              <span className="text-sm font-bold text-emerald-600 dark:text-emerald-400 truncate block mt-0.5">
                {activeNamespace || '(root/global)'}
              </span>
            </div>
          </div>
        )}
      </motion.div>

      {/* Connection & Key Configuration */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.1 }}
        className="glass-card rounded-xl p-6 space-y-4 border border-border dark:border-[oklch(1_0_0/6%)]"
      >
        <div className="flex items-center gap-2 border-b border-border dark:border-[oklch(1_0_0/6%)] pb-3">
          <Server className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
          <h2 className="text-sm font-bold text-[var(--foreground)]">Cluster Endpoint & API Key</h2>
        </div>

        <div className="space-y-4 text-xs">
          {/* Server Base URL */}
          <div className="space-y-1.5">
            <label className="font-bold text-neutral-700 dark:text-[oklch(1_0_0/55%)] font-mono text-[11px]">
              AtlasKV Server Base URL
            </label>
            <Input
              value={endpoint}
              onChange={(e) => setEndpoint(e.target.value)}
              placeholder="http://localhost:8081 (or empty for Studio Proxy)"
              className="bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] focus-visible:ring-emerald-500/30"
            />
            <p className="text-[11px] text-neutral-600 dark:text-[oklch(1_0_0/30%)]">
              Leave blank to use the Next.js Studio proxy route, or specify an explicit remote HTTPS endpoint.
            </p>
          </div>

          {/* API Key */}
          <div className="space-y-1.5">
            <label className="font-bold text-neutral-700 dark:text-[oklch(1_0_0/55%)] font-mono text-[11px] flex items-center gap-1.5">
              <Key className="h-3 w-3 text-amber-500" />
              API Key / Bearer Secret
            </label>
            <div className="relative">
              <Input
                type={showApiKey ? 'text' : 'password'}
                value={apiKeyInput}
                onChange={(e) => setApiKeyInput(e.target.value)}
                placeholder="Enter admin-token, user API key, or leave blank if server auth is disabled"
                className="bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] pr-10 focus-visible:ring-emerald-500/30"
              />
              <button
                type="button"
                onClick={() => setShowApiKey(!showApiKey)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-200 cursor-pointer"
                title={showApiKey ? 'Hide API key' : 'Show API key'}
              >
                {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            <p className="text-[11px] text-neutral-600 dark:text-[oklch(1_0_0/30%)]">
              Injected automatically via <code>Authorization: Bearer</code> headers.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 dark:text-[oklch(1_0_0/55%)] font-mono text-[11px]">
                HTTP Request Timeout (ms)
              </label>
              <Input
                value={timeoutMs}
                onChange={(e) => setTimeoutMs(e.target.value)}
                className="bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] focus-visible:ring-emerald-500/30"
              />
            </div>

            <div className="space-y-1.5">
              <label className="font-bold text-neutral-700 dark:text-[oklch(1_0_0/55%)] font-mono text-[11px]">
                Metrics Auto-Refresh (seconds)
              </label>
              <Input
                value={refreshIntervalSec}
                onChange={(e) => setRefreshIntervalSec(e.target.value)}
                className="bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] focus-visible:ring-emerald-500/30"
              />
            </div>
          </div>
        </div>
      </motion.div>

      {/* Namespace Targeting Section */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.15 }}
        className="glass-card rounded-xl p-6 space-y-4 border border-border dark:border-[oklch(1_0_0/6%)]"
      >
        <div className="flex items-center gap-2 border-b border-border dark:border-[oklch(1_0_0/6%)] pb-3">
          <Layers className="h-4 w-4 text-cyan-600 dark:text-cyan-400" />
          <h2 className="text-sm font-bold text-[var(--foreground)]">Multi-Tenant Namespace Scope</h2>
        </div>

        {isUser ? (
          <div className="p-4 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-xs space-y-2">
            <div className="flex items-center gap-2 font-bold text-indigo-600 dark:text-indigo-400 font-mono">
              <Lock className="h-4 w-4" />
              Tenant Namespace Enforced: {userId}
            </div>
            <p className="text-neutral-600 dark:text-neutral-300 leading-relaxed">
              Your account has <strong>USER</strong> privileges. All storage operations (Key Explorer, Prefix
              Queries, Leases, Watches, History) are strictly isolated to your personal namespace by
              AtlasKV server-side policy. Namespace switching is restricted to administrators.
            </p>
          </div>
        ) : (
          <div className="space-y-3 text-xs">
            <p className="text-neutral-600 dark:text-neutral-400">
              As an <strong>ADMIN</strong>, you can inspect or target specific tenant namespaces via the{' '}
              <code>X-Namespace</code> header, or leave it blank to operate on the global root keyspace.
            </p>

            <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
              <div className="flex-1">
                <Input
                  value={adminNsInput}
                  onChange={(e) => setAdminNsInput(e.target.value)}
                  placeholder="Target tenant namespace (e.g. user-alice), or leave empty for Global"
                  className="bg-[var(--input)] border-border dark:border-[oklch(1_0_0/8%)] text-xs font-mono text-[var(--foreground)] focus-visible:ring-cyan-500/30"
                />
              </div>

              <div className="flex items-center gap-2">
                <Button
                  onClick={handleApplyNamespace}
                  variant="outline"
                  className="text-xs font-mono gap-1.5 border-cyan-500/30 text-cyan-600 dark:text-cyan-400 hover:bg-cyan-500/10"
                >
                  <Layers className="h-3.5 w-3.5" />
                  Apply Scope
                </Button>
                {adminNsInput && (
                  <Button
                    onClick={() => {
                      setAdminNsInput('');
                      updateAdminNamespace('');
                      toast.info('Reset to Global Root Keyspace');
                    }}
                    variant="ghost"
                    className="text-xs font-mono text-neutral-500 hover:text-neutral-200"
                  >
                    Reset
                  </Button>
                )}
              </div>
            </div>
          </div>
        )}
      </motion.div>

      {/* Save Button */}
      <div className="flex items-center justify-end gap-3 pt-2">
        <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
          <Button
            onClick={handleSaveConnection}
            className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-semibold text-xs px-5 py-2.5 shadow-lg shadow-emerald-500/20 gap-2 rounded-lg border-0 cursor-pointer"
          >
            {saved ? <Check className="h-4 w-4" /> : <Save className="h-4 w-4" />}
            {saved ? 'Saved & Connected!' : 'Save Connection'}
          </Button>
        </motion.div>
      </div>
    </div>
  );
}
