'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import {
  CommandDialog,
  CommandInput,
  CommandList,
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandSeparator,
} from '@/components/ui/command';
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
  Code2,
  Plus,
} from 'lucide-react';
import { toast } from 'sonner';

interface NavItem {
  path: string;
  title: string;
  subtitle: string;
  icon: any;
  iconColor: string;
  keywords: string;
}

const NAV_ITEMS: NavItem[] = [
  { path: '/dashboard', title: 'Dashboard', subtitle: 'Dashboard Overview', icon: LayoutDashboard, iconColor: 'text-emerald-400', keywords: 'home overview status consensus stats' },
  { path: '/cluster', title: 'Cluster Topology', subtitle: 'Cluster & Node Topology', icon: Network, iconColor: 'text-emerald-400', keywords: 'nodes raft topology leader followers peers health' },
  { path: '/keys', title: 'Key Explorer', subtitle: 'Key-Value Explorer', icon: KeyRound, iconColor: 'text-emerald-400', keywords: 'kv store keys values search crud cas' },
  { path: '/prefix', title: 'Prefix Queries', subtitle: 'Prefix Query Explorer', icon: Filter, iconColor: 'text-emerald-400', keywords: 'prefix scan filter ranges batch' },
  { path: '/leases', title: 'Leases', subtitle: 'Distributed Lease Management', icon: Clock, iconColor: 'text-purple-400', keywords: 'ttl expiration leases revoke renew' },
  { path: '/watch', title: 'Watch Stream', subtitle: 'Real-Time SSE Watch Terminal', icon: Eye, iconColor: 'text-cyan-400', keywords: 'events streams sse live observe' },
  { path: '/metrics', title: 'Metrics', subtitle: 'Cluster Metrics & Analytics', icon: BarChart3, iconColor: 'text-emerald-400', keywords: 'performance latency throughput charts analytics' },
  { path: '/history', title: 'History', subtitle: 'Key Revision History & Rollback', icon: History, iconColor: 'text-indigo-400', keywords: 'revisions audit rollback versions linearizability' },
  { path: '/settings', title: 'Settings', subtitle: 'Studio Connection & Settings', icon: Settings, iconColor: 'text-zinc-400', keywords: 'configuration token endpoint namespace theme' },
  { path: '/about-dev', title: 'About Dev', subtitle: 'About Dev - Project & Architecture Spec', icon: Code2, iconColor: 'text-emerald-400', keywords: 'docs specifications architecture about info' },
];

export function CommandPalette({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const router = useRouter();
  const [search, setSearch] = useState('');

  // Reset search when dialog opens/closes
  useEffect(() => {
    if (!open) {
      setSearch('');
    }
  }, [open]);

  const handleNavigate = (path: string, title: string) => {
    onOpenChange(false);
    setSearch('');
    router.push(path);
    toast.info(`Navigated to ${title}`);
  };

  const query = search.trim().toLowerCase();

  const filteredNav = NAV_ITEMS.filter(
    (item) =>
      !query ||
      item.title.toLowerCase().includes(query) ||
      item.subtitle.toLowerCase().includes(query) ||
      item.keywords.toLowerCase().includes(query)
  );

  const filteredActions = [
    {
      title: 'Create New Key-Value Pair',
      action: () => {
        onOpenChange(false);
        setSearch('');
        router.push('/keys');
        toast.info('Open Create Key dialog in Explorer');
      },
      keywords: 'create add key value put',
      iconColor: 'text-emerald-400',
    },
    {
      title: 'Allocate New Distributed Lease',
      action: () => {
        onOpenChange(false);
        setSearch('');
        router.push('/leases');
        toast.info('Open Create Lease dialog');
      },
      keywords: 'create allocate lease ttl',
      iconColor: 'text-purple-400',
    },
  ].filter(
    (act) =>
      !query ||
      act.title.toLowerCase().includes(query) ||
      act.keywords.toLowerCase().includes(query)
  );

  const totalResults = filteredNav.length + filteredActions.length;

  return (
    <CommandDialog open={open} onOpenChange={onOpenChange}>
      <CommandInput
        value={search}
        onValueChange={setSearch}
        placeholder="Type a command or search studio pages (Ctrl+K)..."
      />
      <CommandList className="bg-zinc-950 border-t border-white/10 text-xs">
        {totalResults === 0 ? (
          <CommandEmpty className="p-4 text-center text-zinc-500">
            No matching commands or pages found for &quot;{search}&quot;.
          </CommandEmpty>
        ) : (
          <>
            {filteredNav.length > 0 && (
              <CommandGroup heading="Navigation">
                {filteredNav.map((item) => {
                  const Icon = item.icon;
                  return (
                    <CommandItem
                      key={item.path}
                      onSelect={() => handleNavigate(item.path, item.title)}
                    >
                      <Icon className={`h-4 w-4 mr-2 ${item.iconColor}`} />
                      {item.subtitle}
                    </CommandItem>
                  );
                })}
              </CommandGroup>
            )}

            {filteredNav.length > 0 && filteredActions.length > 0 && (
              <CommandSeparator className="bg-white/10" />
            )}

            {filteredActions.length > 0 && (
              <CommandGroup heading="Quick Actions">
                {filteredActions.map((act) => (
                  <CommandItem key={act.title} onSelect={act.action}>
                    <Plus className={`h-4 w-4 mr-2 ${act.iconColor}`} />
                    {act.title}
                  </CommandItem>
                ))}
              </CommandGroup>
            )}
          </>
        )}
      </CommandList>
    </CommandDialog>
  );
}
