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
  RefreshCw,
  Search,
} from 'lucide-react';
import { toast } from 'sonner';

export function CommandPalette({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const router = useRouter();

  const handleNavigate = (path: string, title: string) => {
    onOpenChange(false);
    router.push(path);
    toast.info(`Navigated to ${title}`);
  };

  return (
    <CommandDialog open={open} onOpenChange={onOpenChange}>
      <CommandInput placeholder="Type a command or search studio pages (Ctrl+K)..." />
      <CommandList className="bg-zinc-950 border-t border-white/10 text-xs">
        <CommandEmpty className="p-4 text-center text-zinc-500">
          No matching commands or pages found.
        </CommandEmpty>

        <CommandGroup heading="Navigation">
          <CommandItem onSelect={() => handleNavigate('/dashboard', 'Dashboard')}>
            <LayoutDashboard className="h-4 w-4 mr-2 text-emerald-400" />
            Dashboard Overview
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/cluster', 'Cluster Topology')}>
            <Network className="h-4 w-4 mr-2 text-emerald-400" />
            Cluster & Node Topology
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/keys', 'Key Explorer')}>
            <KeyRound className="h-4 w-4 mr-2 text-emerald-400" />
            Key-Value Explorer
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/prefix', 'Prefix Queries')}>
            <Filter className="h-4 w-4 mr-2 text-emerald-400" />
            Prefix Query Explorer
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/leases', 'Leases')}>
            <Clock className="h-4 w-4 mr-2 text-purple-400" />
            Distributed Lease Management
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/watch', 'Watch Stream')}>
            <Eye className="h-4 w-4 mr-2 text-cyan-400" />
            Real-Time SSE Watch Terminal
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/metrics', 'Metrics')}>
            <BarChart3 className="h-4 w-4 mr-2 text-emerald-400" />
            Cluster Metrics & Analytics
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/history', 'History')}>
            <History className="h-4 w-4 mr-2 text-indigo-400" />
            Key Revision History & Rollback
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/settings', 'Settings')}>
            <Settings className="h-4 w-4 mr-2 text-zinc-400" />
            Studio Connection & Settings
          </CommandItem>
          <CommandItem onSelect={() => handleNavigate('/about-dev', 'About Dev')}>
            <Code2 className="h-4 w-4 mr-2 text-emerald-400" />
            About Dev - Project & Architecture Spec
          </CommandItem>
        </CommandGroup>

        <CommandSeparator className="bg-white/10" />

        <CommandGroup heading="Quick Actions">
          <CommandItem
            onSelect={() => {
              onOpenChange(false);
              router.push('/keys');
              toast.info('Open Create Key dialog in Explorer');
            }}
          >
            <Plus className="h-4 w-4 mr-2 text-emerald-400" />
            Create New Key-Value Pair
          </CommandItem>
          <CommandItem
            onSelect={() => {
              onOpenChange(false);
              router.push('/leases');
              toast.info('Open Create Lease dialog');
            }}
          >
            <Plus className="h-4 w-4 mr-2 text-purple-400" />
            Allocate New Distributed Lease
          </CommandItem>
        </CommandGroup>
      </CommandList>
    </CommandDialog>
  );
}
