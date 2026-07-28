'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { useSidebar } from './sidebar-context';
import { motion, AnimatePresence } from 'framer-motion';
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
  Sparkles,
} from 'lucide-react';

const navSections = [
  {
    label: 'Overview',
    items: [
      { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
      { name: 'Cluster', href: '/cluster', icon: Network },
    ],
  },
  {
    label: 'Data',
    items: [
      { name: 'Keys', href: '/keys', icon: KeyRound },
      { name: 'Prefix', href: '/prefix', icon: Filter },
      { name: 'Leases', href: '/leases', icon: Clock },
    ],
  },
  {
    label: 'Observability',
    items: [
      { name: 'Watch', href: '/watch', icon: Eye },
      { name: 'Metrics', href: '/metrics', icon: BarChart3 },
      { name: 'History', href: '/history', icon: History },
    ],
  },
  {
    label: 'System',
    items: [
      { name: 'Settings', href: '/settings', icon: Settings },
    ],
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { isOpen, setIsOpen } = useSidebar();

  return (
    <>
      {/* Mobile backdrop overlay */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden"
            onClick={() => setIsOpen(false)}
          />
        )}
      </AnimatePresence>

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-[240px] flex-col',
          'bg-[var(--surface-0)]/80 backdrop-blur-2xl',
          'border-r border-[oklch(1_0_0/6%)]',
          'transition-transform duration-300 ease-[cubic-bezier(0.25,0.46,0.45,0.94)]',
          'lg:translate-x-0 lg:z-30',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Brand Logo */}
        <div className="flex h-16 items-center gap-3 px-5 border-b border-[oklch(1_0_0/6%)]">
          <div className="relative">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 via-teal-500 to-cyan-600 shadow-lg shadow-emerald-500/20 ring-1 ring-white/20">
              <Hexagon className="h-4 w-4 text-white" strokeWidth={2.5} />
            </div>
            {/* Ambient glow behind logo */}
            <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-emerald-400 to-cyan-500 blur-lg opacity-20 animate-pulse-glow" />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-bold tracking-tight text-white flex items-center gap-1.5">
              AtlasKV
              <span className="text-[9px] uppercase tracking-widest px-1.5 py-0.5 rounded-md bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono font-medium">
                v2.0
              </span>
            </span>
            <span className="text-[10px] font-medium text-[oklch(1_0_0/35%)]">
              Distributed Raft Studio
            </span>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 px-3 py-3 overflow-y-auto space-y-4">
          {navSections.map((section) => (
            <div key={section.label}>
              <div className="flex items-center gap-2 px-3 mb-1.5">
                <span className="text-[10px] font-semibold uppercase tracking-[0.12em] text-[oklch(1_0_0/20%)]">
                  {section.label}
                </span>
                <div className="flex-1 h-px bg-gradient-to-r from-[oklch(1_0_0/6%)] to-transparent" />
              </div>

              <div className="space-y-0.5">
                {section.items.map((item) => {
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
                        'group relative flex items-center gap-3 rounded-lg px-3 py-2 text-[13px] font-medium transition-all duration-200',
                        isActive
                          ? 'text-white'
                          : 'text-[oklch(1_0_0/40%)] hover:text-[oklch(1_0_0/70%)]'
                      )}
                    >
                      {/* Active indicator pill */}
                      {isActive && (
                        <motion.div
                          layoutId="sidebar-active"
                          className="absolute inset-0 rounded-lg bg-gradient-to-r from-emerald-500/12 via-emerald-500/8 to-transparent border border-emerald-500/15"
                          transition={{
                            type: 'spring',
                            stiffness: 350,
                            damping: 30,
                          }}
                        />
                      )}

                      {/* Hover highlight */}
                      {!isActive && (
                        <div className="absolute inset-0 rounded-lg bg-[oklch(1_0_0/0%)] group-hover:bg-[oklch(1_0_0/3%)] transition-colors duration-200" />
                      )}

                      <item.icon
                        className={cn(
                          'relative h-4 w-4 shrink-0 transition-all duration-200',
                          isActive
                            ? 'text-emerald-400'
                            : 'text-[oklch(1_0_0/25%)] group-hover:text-[oklch(1_0_0/50%)]'
                        )}
                        strokeWidth={1.8}
                      />
                      <span className="relative">{item.name}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Cluster Status Footer */}
        <div className="border-t border-[oklch(1_0_0/6%)] p-4">
          <div className="glass-subtle rounded-lg p-3 space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="relative flex h-2 w-2">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-60" />
                  <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500 shadow-[0_0_6px_oklch(0.72_0.19_160/60%)]" />
                </span>
                <span className="text-[11px] font-medium text-[oklch(1_0_0/60%)]">Cluster Online</span>
              </div>
              <span className="text-[10px] font-mono text-emerald-400/80 bg-emerald-500/8 px-1.5 py-0.5 rounded border border-emerald-500/15">
                3 Nodes
              </span>
            </div>
            <div className="flex items-center gap-1.5">
              <div className="flex-1 h-1 rounded-full bg-[oklch(1_0_0/6%)] overflow-hidden">
                <div className="h-full w-full rounded-full bg-gradient-to-r from-emerald-500 to-cyan-500 animate-gradient-shift" />
              </div>
              <span className="text-[9px] text-[oklch(1_0_0/25%)] font-mono">Healthy</span>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
}
