// ─── TanStack Query Hooks for AtlasKV Cluster ─────────────────────────────────
'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ClusterApi, MetricsApi, HealthApi } from '@/services/api';
import type { AddMemberRequest } from '@/types/api';

const DEFAULT_REFETCH_INTERVAL = 3000;

export function useClusterStatus() {
  return useQuery({
    queryKey: ['cluster', 'status'],
    queryFn: () => ClusterApi.getStatus(),
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useLeader() {
  return useQuery({
    queryKey: ['cluster', 'leader'],
    queryFn: () => ClusterApi.getLeader(),
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useMetrics() {
  return useQuery({
    queryKey: ['cluster', 'metrics'],
    queryFn: () => MetricsApi.getMetrics(),
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useMembers() {
  return useQuery({
    queryKey: ['cluster', 'members'],
    queryFn: () => ClusterApi.getMembers(),
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useHealth() {
  return useQuery({
    queryKey: ['health'],
    queryFn: () => HealthApi.getHealth(),
    refetchInterval: DEFAULT_REFETCH_INTERVAL * 2,
    retry: 1,
  });
}

export function useAddMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (req: AddMemberRequest) => ClusterApi.addMember(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cluster', 'members'] });
      queryClient.invalidateQueries({ queryKey: ['cluster', 'status'] });
    },
  });
}

export function useRemoveMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (nodeId: string) => ClusterApi.removeMember(nodeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cluster', 'members'] });
      queryClient.invalidateQueries({ queryKey: ['cluster', 'status'] });
    },
  });
}
