'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import { useSidebar } from './sidebar-context';
import {
  LayoutDashboard,
  Network,
  Database,
  BarChart3,
  ScrollText,
  Terminal,
  Users,
  Settings,
  Info,
  Hexagon,
} from 'lucide-react';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'Cluster', href: '/cluster', icon: Network },
  { name: 'Explorer', href: '/explorer', icon: Database },
  { name: 'Metrics', href: '/metrics', icon: BarChart3 },
  { name: 'Logs', href: '/logs', icon: ScrollText },
  { name: 'Playground', href: '/playground', icon: Terminal },
  { name: 'Members', href: '/members', icon: Users },
  { name: 'Settings', href: '/settings', icon: Settings },
  { name: 'About', href: '/about', icon: Info },
];

export function Sidebar() {
  const pathname = usePathname();
  const { isOpen, setIsOpen } = useSidebar();

  return (
    <>
      {/* Mobile backdrop overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden transition-opacity duration-300"
          onClick={() => setIsOpen(false)}
        />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-[220px] flex-col border-r border-white/[0.06] bg-[#0a0a0b] transition-transform duration-300 ease-in-out lg:translate-x-0 lg:z-30',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Logo */}
        <div className="flex h-14 items-center gap-2.5 px-5 border-b border-white/[0.06]">
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-gradient-to-br from-emerald-500 to-teal-600 shadow-lg shadow-emerald-500/20">
            <Hexagon className="h-3.5 w-3.5 text-white" strokeWidth={2.5} />
          </div>
          <div className="flex flex-col">
            <span className="text-[13px] font-semibold tracking-tight text-white/90">
              AtlasKV
            </span>
            <span className="text-[10px] font-medium text-white/30 -mt-0.5">
              Studio
            </span>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-3 space-y-0.5 overflow-y-auto">
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
                  'group flex items-center gap-2.5 rounded-md px-2.5 py-[7px] text-[13px] font-medium transition-all duration-150',
                  isActive
                    ? 'bg-white/[0.08] text-white shadow-sm'
                    : 'text-white/40 hover:text-white/70 hover:bg-white/[0.04]'
                )}
              >
                <item.icon
                  className={cn(
                    'h-4 w-4 shrink-0 transition-colors duration-150',
                    isActive
                      ? 'text-emerald-400'
                      : 'text-white/30 group-hover:text-white/50'
                  )}
                  strokeWidth={1.8}
                />
                {item.name}
              </Link>
            );
          })}
        </nav>

        {/* Footer */}
        <div className="border-t border-white/[0.06] px-4 py-3">
          <div className="flex items-center gap-2">
            <div className="h-2 w-2 rounded-full bg-emerald-500 shadow-sm shadow-emerald-500/50 animate-pulse" />
            <span className="text-[11px] text-white/30">v1.0.0</span>
          </div>
        </div>
      </aside>
    </>
  );
}
