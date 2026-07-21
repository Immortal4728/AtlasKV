'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { useSidebar } from './sidebar-context';
import {
  LayoutDashboard,
  Network,
  KeyRound,
  Filter,
  Clock,
  Eye,
  BarChart3,
  History,
  Settings,
  Hexagon,
} from 'lucide-react';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'Cluster', href: '/cluster', icon: Network },
  { name: 'Keys', href: '/keys', icon: KeyRound },
  { name: 'Prefix', href: '/prefix', icon: Filter },
  { name: 'Leases', href: '/leases', icon: Clock },
  { name: 'Watch', href: '/watch', icon: Eye },
  { name: 'Metrics', href: '/metrics', icon: BarChart3 },
  { name: 'History', href: '/history', icon: History },
  { name: 'Settings', href: '/settings', icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();
  const { isOpen, setIsOpen } = useSidebar();

  return (
    <>
      {/* Mobile backdrop overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm lg:hidden transition-opacity duration-300"
          onClick={() => setIsOpen(false)}
        />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-[230px] flex-col border-r border-white/[0.08] bg-[#09090b] transition-transform duration-300 ease-in-out lg:translate-x-0 lg:z-30',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Brand Logo */}
        <div className="flex h-16 items-center gap-3 px-5 border-b border-white/[0.08]">
          <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 via-teal-500 to-cyan-600 shadow-md shadow-emerald-500/20 ring-1 ring-white/20">
            <Hexagon className="h-4 w-4 text-zinc-950 font-bold" strokeWidth={2.5} />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-bold tracking-tight text-white flex items-center gap-1.5">
              AtlasKV <span className="text-[10px] uppercase tracking-widest px-1.5 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono">v2.0</span>
            </span>
            <span className="text-[11px] font-medium text-zinc-400">
              Distributed Raft Studio
            </span>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navigation.map((item) => {
            const isActive =
              item.href === '/dashboard'
                ? pathname === '/dashboard'
                : pathname.startsWith(item.href);

            return (
              <Link
                key={item.name}
                href={item.href}
                onClick={() => setIsOpen(false)}
                className={cn(
                  'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-xs font-semibold transition-all duration-150',
                  isActive
                    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-sm shadow-emerald-500/5'
                    : 'text-zinc-400 hover:text-zinc-200 hover:bg-white/[0.04]'
                )}
              >
                <item.icon
                  className={cn(
                    'h-4 w-4 shrink-0 transition-colors duration-150',
                    isActive
                      ? 'text-emerald-400'
                      : 'text-zinc-500 group-hover:text-zinc-300'
                  )}
                  strokeWidth={2}
                />
                {item.name}
              </Link>
            );
          })}
        </nav>

        {/* Cluster Status Footer */}
        <div className="border-t border-white/[0.08] p-4 bg-zinc-950/50">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
              <span className="text-xs font-medium text-zinc-300">Cluster Online</span>
            </div>
            <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded border border-emerald-500/20">3 Nodes</span>
          </div>
        </div>
      </aside>
    </>
  );
}
