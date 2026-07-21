'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { LeaseApi } from '@/services/api';

const DEFAULT_REFETCH_INTERVAL = 3000;

export function useLeases() {
  return useQuery({
    queryKey: ['leases'],
    queryFn: () => LeaseApi.list(),
    refetchInterval: DEFAULT_REFETCH_INTERVAL,
    retry: 1,
  });
}

export function useCreateLease() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ ttl, leaseId }: { ttl: string; leaseId?: string }) =>
      LeaseApi.create(ttl, leaseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leases'] });
    },
  });
}

export function useRenewLease() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (leaseId: string) => LeaseApi.renew(leaseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leases'] });
    },
  });
}

export function useRevokeLease() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (leaseId: string) => LeaseApi.revoke(leaseId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['leases'] });
    },
  });
}
