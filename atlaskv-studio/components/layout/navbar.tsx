'use client';

import Link from 'next/link';
import { useState, useEffect } from 'react';
import { usePathname } from 'next/navigation';
import Image from 'next/image';
import { Badge } from '@/components/ui/badge';
import { useClusterStatus } from '@/hooks/use-cluster';
import { useAuth } from '@/hooks/use-auth';
import { useSidebar } from './sidebar-context';
import { Search, Wifi, WifiOff, Menu, Bell, User, ShieldCheck } from 'lucide-react';
import { CommandPalette } from './command-palette';
import { ThemeToggle } from '@/components/ui/theme-toggle';
import { NamespaceBadge } from '@/components/ui/namespace-badge';
import { motion } from 'framer-motion';

const pageTitles: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/cluster': 'Cluster Topology',
  '/keys': 'Key Explorer',
  '/prefix': 'Prefix Queries',
  '/leases': 'Lease Management',
  '/watch': 'Watch Terminal',
  '/metrics': 'Metrics & Analytics',
  '/history': 'Version History',
  '/settings': 'Settings',
  '/members': 'Members',
  '/explorer': 'Data Explorer',
  '/playground': 'Playground',
  '/logs': 'Raft Logs',
  '/about': 'About',
  '/about-dev': 'About Dev',
};

export function Navbar() {
  const { data: status, isError } = useClusterStatus();
  const { authInfo, username, role: userRole, isAdmin, isAuthenticated } = useAuth();
  const { toggle } = useSidebar();
  const [commandOpen, setCommandOpen] = useState(false);
  const pathname = usePathname();

  // Keyboard shortcut listener for Ctrl+K / Cmd+K
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setCommandOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const isConnected = !!status && !isError;
  const clusterRole = status?.role ?? 'UNKNOWN';
  const leader = status?.currentLeader ?? '—';
  const term = status?.currentTerm ?? 0;

  // Determine page title from pathname
  const currentPage = Object.entries(pageTitles).find(([path]) =>
    path === '/dashboard'
      ? pathname === '/dashboard'
      : pathname.startsWith(path)
  );
  const pageTitle = currentPage?.[1] ?? 'Studio';

  return (
    <>
      <header className="sticky top-0 z-40 flex h-14 items-center justify-between border-b border-[oklch(1_0_0/5%)] bg-[var(--surface-0)]/70 backdrop-blur-2xl px-4 sm:px-6">
        {/* Left: Hamburger + Breadcrumb */}
        <div className="flex items-center gap-3">
          <button
            onClick={toggle}
            aria-label="Toggle sidebar menu"
            className="lg:hidden p-1.5 -ml-1 text-[oklch(1_0_0/40%)] hover:text-white rounded-lg hover:bg-[oklch(1_0_0/4%)] transition-colors"
          >
            <Menu className="h-4 w-4" />
          </button>

          <div className="flex items-center gap-2 text-sm">
            <Link href="/dashboard" className="flex items-center gap-1.5 text-neutral-700 dark:text-neutral-300 hover:text-amber-600 dark:hover:text-amber-400 font-bold tracking-tight transition-colors hidden sm:inline-flex group">
              <span className="flex h-5 w-5 items-center justify-center rounded bg-white p-0.5 shadow-sm border border-amber-500/20 group-hover:scale-105 transition-transform overflow-hidden">
                <Image
                  src="/atlaskv-logo.png"
                  alt="AtlasKV Logo"
                  width={16}
                  height={16}
                  className="h-full w-full object-contain"
                />
              </span>
              <span>AtlasKV</span>
            </Link>
            <span className="text-neutral-400 dark:text-neutral-600 hidden sm:inline">/</span>
            <motion.span
              key={pageTitle}
              initial={{ opacity: 0, y: -4 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.2 }}
              className="font-bold text-neutral-900 dark:text-white"
            >
              {pageTitle}
            </motion.span>
          </div>
        </div>

        {/* Right: Status indicators & Search */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Quick Search Button */}
          <button
            onClick={() => setCommandOpen(true)}
            className="hidden sm:flex items-center gap-2 rounded-lg border border-border dark:border-[oklch(1_0_0/8%)] bg-neutral-100/80 dark:bg-[oklch(1_0_0/3%)] px-3 py-1.5 text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white hover:bg-neutral-200/60 dark:hover:bg-[oklch(1_0_0/5%)] transition-all cursor-pointer text-xs font-medium"
          >
            <Search className="h-3.5 w-3.5 text-neutral-500 dark:text-neutral-400" />
            <span className="text-xs text-neutral-600 dark:text-neutral-400">Search...</span>
            <kbd className="ml-4 rounded-md border border-border dark:border-[oklch(1_0_0/8%)] bg-white dark:bg-[var(--surface-2)] px-1.5 py-0.5 text-[10px] font-mono text-emerald-600 dark:text-emerald-400 font-semibold shadow-xs">
              ⌘K
            </kbd>
          </button>

          {/* Namespace Indicator with Admin Switcher */}
          <div className="hidden lg:flex items-center">
            <NamespaceBadge showSwitcher={true} />
          </div>

          {/* User / Identity Pill */}
          <Link
            href="/settings"
            className="hidden md:flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-neutral-100 dark:bg-[oklch(1_0_0/3%)] border border-border dark:border-[oklch(1_0_0/5%)] text-xs font-mono hover:border-emerald-500/40 transition-colors"
            title="Configure connection and API keys in Settings"
          >
            {isAdmin ? (
              <ShieldCheck className="h-3.5 w-3.5 text-amber-500" />
            ) : (
              <User className="h-3.5 w-3.5 text-indigo-500" />
            )}
            <span className="font-bold text-neutral-800 dark:text-neutral-200 truncate max-w-[120px]">
              {username}
            </span>
            <span className="text-[9px] uppercase px-1 py-0.2 rounded bg-neutral-200 dark:bg-neutral-700 text-neutral-600 dark:text-neutral-300 font-bold">
              {userRole}
            </span>
          </Link>

          {/* Term */}
          <div className="hidden xl:flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-neutral-100 dark:bg-[oklch(1_0_0/3%)] border border-border dark:border-[oklch(1_0_0/5%)]">
            <span className="text-xs text-neutral-500 dark:text-neutral-400 uppercase tracking-wider font-mono font-bold">Term</span>
            <span className="text-xs font-mono text-neutral-800 dark:text-neutral-200 font-bold">{term}</span>
          </div>

          {/* Leader */}
          <div className="hidden xl:flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-neutral-100 dark:bg-[oklch(1_0_0/3%)] border border-border dark:border-[oklch(1_0_0/5%)]">
            <span className="text-xs text-neutral-500 dark:text-neutral-400 uppercase tracking-wider font-mono font-bold">Leader</span>
            <span className="text-xs font-mono text-emerald-600 dark:text-emerald-400 font-bold">{leader}</span>
          </div>

          {/* Theme Switcher Toggle */}
          <ThemeToggle />

          {/* Connection Status */}
          <div
            className={cn(
              'flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold font-mono border transition-all',
              isConnected
                ? 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                : 'bg-rose-500/10 text-rose-600 dark:text-rose-400 border-rose-500/30'
            )}
          >
            <span className="relative">
              {isConnected ? (
                <>
                  <Wifi className="h-3 w-3" />
                  <span className="absolute inset-0 animate-ping">
                    <Wifi className="h-3 w-3 text-emerald-400/30" />
                  </span>
                </>
              ) : (
                <WifiOff className="h-3 w-3" />
              )}
            </span>
            <span>{isConnected ? clusterRole : 'Offline'}</span>
          </div>
        </div>
      </header>

      {/* Command Palette Modal */}
      <CommandPalette open={commandOpen} onOpenChange={setCommandOpen} />
    </>
  );
}

function cn(...classes: (string | undefined | false)[]) {
  return classes.filter(Boolean).join(' ');
}
