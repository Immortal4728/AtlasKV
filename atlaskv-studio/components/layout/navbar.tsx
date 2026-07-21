'use client';

import { Badge } from '@/components/ui/badge';
import { useClusterStatus } from '@/hooks/use-cluster';
import { useSidebar } from './sidebar-context';
import { Search, Wifi, WifiOff, Menu } from 'lucide-react';

export function Navbar() {
  const { data: status, isError } = useClusterStatus();
  const { toggle } = useSidebar();

  const isConnected = !!status && !isError;
  const role = status?.role ?? 'UNKNOWN';
  const leader = status?.currentLeader ?? '—';
  const term = status?.currentTerm ?? 0;

  return (
    <header className="sticky top-0 z-40 flex h-14 items-center justify-between border-b border-white/[0.06] bg-[#0a0a0b]/80 backdrop-blur-xl px-4 sm:px-6">
      {/* Left: Hamburger + Page title context */}
      <div className="flex items-center gap-3">
        <button
          onClick={toggle}
          aria-label="Toggle sidebar menu"
          className="lg:hidden p-1.5 -ml-1 text-white/50 hover:text-white rounded hover:bg-white/[0.04]"
        >
          <Menu className="h-4 w-4" />
        </button>
        <span className="text-sm font-medium text-white/60">Overview</span>
      </div>

      {/* Right: Status indicators */}
      <div className="flex items-center gap-3 sm:gap-4">
        {/* Search (placeholder) */}
        <div className="hidden sm:flex items-center gap-2 rounded-md border border-white/[0.08] bg-white/[0.03] px-3 py-1.5 text-white/30">
          <Search className="h-3.5 w-3.5" />
          <span className="text-xs">Search...</span>
          <kbd className="ml-4 rounded border border-white/[0.08] bg-white/[0.04] px-1.5 py-0.5 text-[10px] font-mono text-white/20">
            ⌘K
          </kbd>
        </div>

        {/* Term */}
        <div className="hidden md:flex items-center gap-1.5">
          <span className="text-[11px] text-white/30 uppercase tracking-wider">Term</span>
          <span className="text-xs font-mono text-white/70">{term}</span>
        </div>

        {/* Leader */}
        <div className="hidden md:flex items-center gap-1.5">
          <span className="text-[11px] text-white/30 uppercase tracking-wider">Leader</span>
          <span className="text-xs font-mono text-emerald-400/80">{leader}</span>
        </div>

        {/* Connection Status */}
        <Badge
          variant={isConnected ? 'default' : 'destructive'}
          className={
            isConnected
              ? 'gap-1.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 hover:bg-emerald-500/15 text-[11px] font-medium'
              : 'gap-1.5 text-[11px] font-medium'
          }
        >
          {isConnected ? (
            <Wifi className="h-3 w-3" />
          ) : (
            <WifiOff className="h-3 w-3" />
          )}
          {isConnected ? role : 'Disconnected'}
        </Badge>
      </div>
    </header>
  );
}
