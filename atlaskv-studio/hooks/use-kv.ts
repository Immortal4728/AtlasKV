'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { KeyValueApi, PrefixApi } from '@/services/api';

export function useKey(key: string, enabled = true) {
  return useQuery({
    queryKey: ['kv', 'key', key],
    queryFn: () => KeyValueApi.get(key),
    enabled: enabled && !!key,
    retry: 1,
  });
}

export function usePrefix(prefix: string, offset = 0, limit = 100) {
  return useQuery({
    queryKey: ['kv', 'prefix', prefix, offset, limit],
    queryFn: () => PrefixApi.query(prefix, offset, limit),
    refetchInterval: 3000,
    retry: 1,
  });
}

export function usePutValue() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ key, value, ttl, leaseId }: { key: string; value: string; ttl?: string; leaseId?: string }) =>
      KeyValueApi.put(key, value, ttl, leaseId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['kv', 'key', variables.key] });
      queryClient.invalidateQueries({ queryKey: ['kv', 'prefix'] });
      queryClient.invalidateQueries({ queryKey: ['leases'] });
      queryClient.invalidateQueries({ queryKey: ['cluster', 'metrics'] });
    },
  });
}

export function useCasPutValue() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ key, value, expectedVersion }: { key: string; value: string; expectedVersion: number }) =>
      KeyValueApi.casPut(key, value, expectedVersion),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['kv', 'key', variables.key] });
      queryClient.invalidateQueries({ queryKey: ['kv', 'prefix'] });
      queryClient.invalidateQueries({ queryKey: ['leases'] });
      queryClient.invalidateQueries({ queryKey: ['cluster', 'metrics'] });
    },
  });
}

export function useDeleteValue() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (key: string) => KeyValueApi.delete(key),
    onSuccess: (_, key) => {
      queryClient.invalidateQueries({ queryKey: ['kv', 'key', key] });
      queryClient.invalidateQueries({ queryKey: ['kv', 'prefix'] });
      queryClient.invalidateQueries({ queryKey: ['leases'] });
      queryClient.invalidateQueries({ queryKey: ['cluster', 'metrics'] });
    },
  });
}
