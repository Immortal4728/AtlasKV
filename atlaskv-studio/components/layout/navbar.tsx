'use client';

import { useState, useEffect } from 'react';
import { Badge } from '@/components/ui/badge';
import { useClusterStatus } from '@/hooks/use-cluster';
import { useSidebar } from './sidebar-context';
import { Search, Wifi, WifiOff, Menu, Command as CommandIcon } from 'lucide-react';
import { CommandPalette } from './command-palette';

export function Navbar() {
  const { data: status, isError } = useClusterStatus();
  const { toggle } = useSidebar();
  const [commandOpen, setCommandOpen] = useState(false);

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
  const role = status?.role ?? 'UNKNOWN';
  const leader = status?.currentLeader ?? '—';
  const term = status?.currentTerm ?? 0;

  return (
    <>
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
          <span className="text-xs font-semibold uppercase tracking-wider text-zinc-400 font-mono">AtlasKV Management Console</span>
        </div>

        {/* Right: Status indicators & Command Palette Search */}
        <div className="flex items-center gap-3 sm:gap-4">
          {/* Quick Search Button */}
          <button
            onClick={() => setCommandOpen(true)}
            className="hidden sm:flex items-center gap-2 rounded-lg border border-white/[0.08] bg-white/[0.03] px-3 py-1.5 text-zinc-400 hover:text-white hover:bg-white/[0.06] transition-colors cursor-pointer text-xs"
          >
            <Search className="h-3.5 w-3.5 text-zinc-500" />
            <span className="text-xs text-zinc-400">Quick Actions...</span>
            <kbd className="ml-4 rounded border border-white/10 bg-zinc-900 px-1.5 py-0.5 text-[10px] font-mono text-emerald-400">
              ⌘K
            </kbd>
          </button>

          {/* Term */}
          <div className="hidden md:flex items-center gap-1.5">
            <span className="text-[11px] text-white/30 uppercase tracking-wider font-mono">Term</span>
            <span className="text-xs font-mono text-white/70">{term}</span>
          </div>

          {/* Leader */}
          <div className="hidden md:flex items-center gap-1.5">
            <span className="text-[11px] text-white/30 uppercase tracking-wider font-mono">Leader</span>
            <span className="text-xs font-mono text-emerald-400">{leader}</span>
          </div>

          {/* Connection Status */}
          <Badge
            variant={isConnected ? 'default' : 'destructive'}
            className={
              isConnected
                ? 'gap-1.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 hover:bg-emerald-500/15 text-[11px] font-medium font-mono'
                : 'gap-1.5 text-[11px] font-medium font-mono'
            }
          >
            {isConnected ? (
              <Wifi className="h-3 w-3 animate-pulse" />
            ) : (
              <WifiOff className="h-3 w-3" />
            )}
            {isConnected ? role : 'Disconnected'}
          </Badge>
        </div>
      </header>

      {/* Command Palette Modal */}
      <CommandPalette open={commandOpen} onOpenChange={setCommandOpen} />
    </>
  );
}
