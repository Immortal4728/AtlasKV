// ─── React Query Hooks for AtlasKV ───────────────────────────────────────────

'use client';

import { useQuery } from '@tanstack/react-query';
import * as api from '@/services/api';

const DEFAULT_REFETCH_INTERVAL = 3000;

export function useClusterStatus() {
  return useQuery({
    queryKey: ['cluster', 'status'],
    queryFn: api.getClusterStatus,
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useLeader() {
  return useQuery({
    queryKey: ['cluster', 'leader'],
    queryFn: api.getLeader,
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useMetrics() {
  return useQuery({
    queryKey: ['cluster', 'metrics'],
    queryFn: api.getMetrics,
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useMembers() {
  return useQuery({
    queryKey: ['cluster', 'members'],
    queryFn: api.getMembers,
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useHealth() {
  return useQuery({
    queryKey: ['health'],
    queryFn: api.getHealth,
    refetchInterval: DEFAULT_REFETCH_INTERVAL * 2,
    retry: 1,
  });
}
