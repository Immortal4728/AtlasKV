'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { HistoryApi } from '@/services/api';

export function useHistory(key: string, enabled = true) {
  return useQuery({
    queryKey: ['history', key],
    queryFn: () => HistoryApi.getHistory(key),
    enabled: enabled && !!key,
    retry: 1,
  });
}

export function useRollbackKey() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ key, revision }: { key: string; revision: number }) =>
      HistoryApi.rollback(key, revision),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['history', variables.key] });
      queryClient.invalidateQueries({ queryKey: ['kv', 'key', variables.key] });
      queryClient.invalidateQueries({ queryKey: ['kv', 'prefix'] });
    },
  });
}
