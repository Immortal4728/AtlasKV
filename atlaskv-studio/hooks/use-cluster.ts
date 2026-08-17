// ─── TanStack Query Hooks for AtlasKV Cluster ─────────────────────────────────
'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ClusterApi, MetricsApi, HealthApi, AdminApi, getSavedMetricsInterval } from '@/services/api';
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
  const intervalSec = typeof window !== 'undefined' ? getSavedMetricsInterval() : 2;
  return useQuery({
    queryKey: ['cluster', 'metrics'],
    queryFn: () => MetricsApi.getMetrics(),
    refetchInterval: intervalSec * 1000,
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

export function useNodes() {
  return useQuery({
    queryKey: ['cluster', 'nodes'],
    queryFn: () => ClusterApi.getNodes(),
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
      queryClient.invalidateQueries({ queryKey: ['cluster', 'nodes'] });
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
      queryClient.invalidateQueries({ queryKey: ['cluster', 'nodes'] });
    },
  });
}

export function useTakeSnapshot() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => AdminApi.takeSnapshot(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cluster', 'status'] });
      queryClient.invalidateQueries({ queryKey: ['cluster', 'metrics'] });
      queryClient.invalidateQueries({ queryKey: ['cluster', 'nodes'] });
    },
  });
}
