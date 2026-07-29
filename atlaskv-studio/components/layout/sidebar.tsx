'use client';

import Link from 'next/link';
import Image from 'next/image';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { useSidebar } from './sidebar-context';
import { motion, AnimatePresence } from 'framer-motion';
import { useState } from 'react';
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
  ChevronDown,
  ChevronRight,
  Rocket,
  Code2,
  FileCode2,
  Terminal,
  BookOpen,
} from 'lucide-react';

const navSections = [
  {
    id: 'overview',
    label: 'Overview',
    items: [
      { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
      { name: 'Cluster', href: '/cluster', icon: Network },
    ],
  },
  {
    id: 'data',
    label: 'Data Engine',
    items: [
      { name: 'Keys', href: '/keys', icon: KeyRound },
      { name: 'Prefix', href: '/prefix', icon: Filter },
      { name: 'Leases', href: '/leases', icon: Clock },
    ],
  },
  {
    id: 'developer',
    label: 'Developer',
    items: [
      { name: 'Quick Start', href: '/developer/quickstart', icon: Rocket },
      { name: 'Java SDK', href: '/developer/java-sdk', icon: Code2 },
      { name: 'TypeScript SDK', href: '/developer/ts-sdk', icon: FileCode2 },
      { name: 'CLI', href: '/developer/cli', icon: Terminal },
      { name: 'API Reference', href: '/developer/api-reference', icon: BookOpen },
    ],
  },
  {
    id: 'observability',
    label: 'Observability',
    items: [
      { name: 'Watch', href: '/watch', icon: Eye },
      { name: 'Metrics', href: '/metrics', icon: BarChart3 },
      { name: 'History', href: '/history', icon: History },
    ],
  },
  {
    id: 'system',
    label: 'System',
    items: [
      { name: 'Settings', href: '/settings', icon: Settings },
      { name: 'About Dev', href: '/about-dev', icon: Code2 },
    ],
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { isOpen, setIsOpen } = useSidebar();
  const [collapsedSections, setCollapsedSections] = useState<Record<string, boolean>>({});

  const toggleSection = (id: string) => {
    setCollapsedSections((prev) => ({ ...prev, [id]: !prev[id] }));
  };

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
          'bg-[var(--surface-0)] backdrop-blur-2xl',
          'border-r border-[oklch(1_0_0/8%)] dark:border-[oklch(1_0_0/8%)]',
          'transition-transform duration-300 ease-[cubic-bezier(0.25,0.46,0.45,0.94)]',
          'lg:translate-x-0 lg:z-30',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Brand Logo - Click to Dashboard */}
        <Link
          href="/dashboard"
          className="flex h-16 items-center gap-3 px-5 border-b border-[oklch(1_0_0/8%)] hover:bg-[oklch(1_0_0/3%)] transition-colors cursor-pointer group"
          title="AtlasKV Dashboard"
        >
          <div className="relative">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/95 p-1 shadow-md shadow-orange-500/10 ring-1 ring-white/30 group-hover:scale-105 transition-transform overflow-hidden">
              <Image
                src="/atlaskv-logo.png"
                alt="AtlasKV Logo"
                width={32}
                height={32}
                className="h-full w-full object-contain"
              />
            </div>
            <div className="absolute inset-0 rounded-xl bg-orange-500/20 blur-md opacity-0 group-hover:opacity-100 transition-opacity" />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-bold tracking-tight text-[var(--foreground)] flex items-center gap-1.5 font-sans group-hover:text-amber-500 dark:group-hover:text-amber-400 transition-colors">
              AtlasKV
              <span className="text-[9px] uppercase tracking-widest px-1.5 py-0.5 rounded-md bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 font-mono font-medium">
                v3.3
              </span>
            </span>
            <span className="text-[10px] font-medium text-neutral-400 dark:text-neutral-500">
              Distributed Raft Studio
            </span>
          </div>
        </Link>

        {/* Navigation Items */}
        <nav className="flex-1 px-3 py-3 overflow-y-auto space-y-4">
          {navSections.map((section) => {
            const isCollapsed = collapsedSections[section.id];

            return (
              <div key={section.id}>
                <button
                  onClick={() => toggleSection(section.id)}
                  className="w-full flex items-center justify-between px-3 mb-1.5 text-left group"
                >
                  <span className="text-xs font-bold uppercase tracking-[0.12em] text-neutral-700 dark:text-neutral-400 group-hover:text-[var(--foreground)] transition-colors">
                    {section.label}
                  </span>
                  {isCollapsed ? (
                    <ChevronRight className="h-3.5 w-3.5 text-neutral-500 group-hover:text-[var(--foreground)]" />
                  ) : (
                    <ChevronDown className="h-3.5 w-3.5 text-neutral-500 group-hover:text-[var(--foreground)]" />
                  )}
                </button>

                {!isCollapsed && (
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
                            'group relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-all duration-200',
                            isActive
                              ? 'text-emerald-600 dark:text-emerald-400 font-bold'
                              : 'text-neutral-700 dark:text-neutral-300 hover:text-neutral-900 dark:hover:text-white font-medium'
                          )}
                        >
                          {/* Active indicator pill */}
                          {isActive && (
                            <motion.div
                              layoutId="sidebar-active"
                              className="absolute inset-0 rounded-lg bg-emerald-500/12 dark:bg-gradient-to-r dark:from-emerald-500/15 dark:via-emerald-500/10 dark:to-transparent border border-emerald-500/30"
                              transition={{
                                type: 'spring',
                                stiffness: 350,
                                damping: 30,
                              }}
                            />
                          )}

                          {/* Hover highlight */}
                          {!isActive && (
                            <div className="absolute inset-0 rounded-lg bg-neutral-100/50 hover:bg-neutral-100 dark:bg-transparent dark:hover:bg-[oklch(1_0_0/4%)] transition-colors duration-200" />
                          )}

                          <item.icon
                            className={cn(
                              'relative h-4 w-4 shrink-0 transition-all duration-200',
                              isActive
                                ? 'text-emerald-600 dark:text-emerald-400'
                                : 'text-neutral-500 group-hover:text-neutral-900 dark:text-neutral-400 dark:group-hover:text-white'
                            )}
                            strokeWidth={1.8}
                          />
                          <span className="relative">{item.name}</span>
                        </Link>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </nav>

        {/* Cluster Status Footer */}
        <div className="border-t border-[oklch(1_0_0/8%)] p-3.5">
          <Link
            href="/dashboard"
            className="block rounded-xl p-3 space-y-2.5 bg-[var(--surface-1)] border border-[oklch(1_0_0/6%)] hover:border-emerald-500/30 transition-all group"
            title="AtlasKV Raft Cluster v2.0 - Dashboard"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="flex h-6 w-6 items-center justify-center rounded-lg bg-white/95 p-0.5 shadow-sm border border-amber-500/20 group-hover:scale-110 transition-transform overflow-hidden">
                  <Image
                    src="/atlaskv-logo.png"
                    alt="AtlasKV Logo"
                    width={20}
                    height={20}
                    className="h-full w-full object-contain"
                  />
                </div>
                <span className="text-[11px] font-semibold text-[var(--foreground)] group-hover:text-amber-500 dark:group-hover:text-amber-400 transition-colors">
                  Cluster Online
                </span>
              </div>
              <span className="text-[10px] font-mono font-medium text-emerald-500 dark:text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                3 Nodes
              </span>
            </div>
            <div className="flex items-center gap-2">
              <div className="flex-1 h-1.5 rounded-full bg-[oklch(0_0_0/8%)] dark:bg-[oklch(1_0_0/8%)] overflow-hidden">
                <div className="h-full w-full rounded-full bg-gradient-to-r from-emerald-500 via-teal-400 to-cyan-500 animate-gradient-shift" />
              </div>
              <span className="text-[9.5px] text-emerald-500 dark:text-emerald-400 font-mono font-semibold">100% Healthy</span>
            </div>
          </Link>
        </div>
      </aside>
    </>
  );
}
