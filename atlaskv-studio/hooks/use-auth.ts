// ─── TanStack Query Hooks for AtlasKV Authentication & Identity ──────────────
'use client';

import { useState, useEffect, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AuthApi,
  getSavedApiKey,
  setSavedApiKey,
  getSavedAdminNamespace,
  setSavedAdminNamespace,
  getSavedBaseUrl,
  setSavedBaseUrl,
  getSavedTimeout,
  setSavedTimeout,
  getSavedMetricsInterval,
  setSavedMetricsInterval,
  clearSavedCredentials,
} from '@/services/api';
import type { AuthInfoResponse } from '@/types/api';

const DEFAULT_AUTH_REFETCH_INTERVAL = 10000;

export function useAuthInfo() {
  return useQuery<AuthInfoResponse>({
    queryKey: ['auth', 'me'],
    queryFn: () => AuthApi.getAuthInfo(),
    refetchInterval: DEFAULT_AUTH_REFETCH_INTERVAL,
    retry: false,
  });
}

export function useAuth() {
  const queryClient = useQueryClient();

  const [apiKey, setLocalApiKey] = useState<string>('');
  const [adminNamespace, setLocalAdminNamespace] = useState<string>('');
  const [serverUrl, setLocalServerUrl] = useState<string>('');
  const [timeoutMs, setLocalTimeoutMs] = useState<number>(5000);
  const [refreshIntervalSec, setLocalRefreshIntervalSec] = useState<number>(2);

  useEffect(() => {
    setLocalApiKey(getSavedApiKey());
    setLocalAdminNamespace(getSavedAdminNamespace());
    setLocalServerUrl(getSavedBaseUrl());
    setLocalTimeoutMs(getSavedTimeout());
    setLocalRefreshIntervalSec(getSavedMetricsInterval());
  }, []);

  const { data: authInfo, isLoading, isError, error, refetch } = useAuthInfo();

  const updateApiKey = useCallback(
    (key: string) => {
      setSavedApiKey(key);
      setLocalApiKey(key);
      queryClient.invalidateQueries({ queryKey: ['auth'] });
      queryClient.invalidateQueries({ queryKey: ['kv'] });
      queryClient.invalidateQueries({ queryKey: ['leases'] });
      queryClient.invalidateQueries({ queryKey: ['cluster'] });
    },
    [queryClient]
  );

  const updateAdminNamespace = useCallback(
    (ns: string) => {
      setSavedAdminNamespace(ns);
      setLocalAdminNamespace(ns);
      queryClient.invalidateQueries({ queryKey: ['auth'] });
      queryClient.invalidateQueries({ queryKey: ['kv'] });
      queryClient.invalidateQueries({ queryKey: ['leases'] });
    },
    [queryClient]
  );

  const updateServerUrl = useCallback(
    (url: string) => {
      setSavedBaseUrl(url);
      setLocalServerUrl(url);
      queryClient.invalidateQueries();
    },
    [queryClient]
  );

  const updateTimeoutMs = useCallback((ms: number | string) => {
    setSavedTimeout(ms);
    setLocalTimeoutMs(getSavedTimeout());
  }, []);

  const updateRefreshIntervalSec = useCallback(
    (sec: number | string) => {
      setSavedMetricsInterval(sec);
      setLocalRefreshIntervalSec(getSavedMetricsInterval());
      queryClient.invalidateQueries({ queryKey: ['cluster', 'metrics'] });
    },
    [queryClient]
  );

  const disconnect = useCallback(() => {
    clearSavedCredentials();
    setLocalApiKey('');
    setLocalAdminNamespace('');
    setLocalTimeoutMs(5000);
    setLocalRefreshIntervalSec(2);
    queryClient.invalidateQueries();
  }, [queryClient]);

  const isAdmin = authInfo?.role === 'ADMIN';
  const isUser = authInfo?.role === 'USER';
  const activeNamespace = isUser
    ? authInfo?.namespace || authInfo?.userId || ''
    : adminNamespace || authInfo?.namespace || '';

  return {
    authInfo,
    isLoading,
    isError,
    error,
    refetch,
    apiKey,
    adminNamespace,
    serverUrl,
    timeoutMs,
    refreshIntervalSec,
    isAdmin,
    isUser,
    activeNamespace,
    userId: authInfo?.userId || (isError ? 'unauthorized' : 'anonymous'),
    username: authInfo?.username || (isError ? 'Unauthorized' : 'Local Developer'),
    role: authInfo?.role || 'USER',
    isAuthenticated: authInfo?.authenticated ?? false,
    updateApiKey,
    updateAdminNamespace,
    updateServerUrl,
    updateTimeoutMs,
    updateRefreshIntervalSec,
    disconnect,
  };
}
