'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { HistoryApi } from '@/services/api';
import { KeyRevisionResponse } from '@/types/api';

export function useHistory(key: string, enabled = true) {
  return useQuery<KeyRevisionResponse[]>({
    queryKey: ['history', key],
    queryFn: async () => {
      try {
        return await HistoryApi.getHistory(key);
      } catch (err: any) {
        if (err?.response?.status === 404) {
          return [];
        }
        throw err;
      }
    },
    enabled: enabled && !!key.trim(),
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
