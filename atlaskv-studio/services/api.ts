// ─── AtlasKV API Client ──────────────────────────────────────────────────────
// Centralized HTTP client for the AtlasKV backend REST API.

import type {
  ClusterStatusResponse,
  LeaderResponse,
  MetricsResponse,
  ClusterMembersResponse,
  KeyValueResponse,
  AddMemberRequest,
  HealthResponse,
} from '@/types';

const DEFAULT_BASE_URL = '';
const DEFAULT_TIMEOUT = 5000;

function getBaseUrl(): string {
  if (typeof window !== 'undefined') {
    const saved = localStorage.getItem('atlaskv-server-url');
    // Empty string means use relative URLs (through Next.js rewrites proxy)
    return saved ?? DEFAULT_BASE_URL;
  }
  return DEFAULT_BASE_URL;
}

class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    message: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const baseUrl = getBaseUrl();
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT);

  try {
    const response = await fetch(`${baseUrl}${path}`, {
      ...options,
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    });

    if (!response.ok) {
      const text = await response.text().catch(() => response.statusText);
      throw new ApiError(response.status, response.statusText, text);
    }

    return await response.json() as T;
  } finally {
    clearTimeout(timeout);
  }
}

// ─── Cluster APIs ────────────────────────────────────────────────────────────

export async function getClusterStatus(): Promise<ClusterStatusResponse> {
  return request<ClusterStatusResponse>('/api/v1/cluster/status');
}

export async function getLeader(): Promise<LeaderResponse> {
  return request<LeaderResponse>('/api/v1/cluster/leader');
}

export async function getMetrics(): Promise<MetricsResponse> {
  return request<MetricsResponse>('/api/v1/cluster/metrics');
}

export async function getMembers(): Promise<ClusterMembersResponse> {
  return request<ClusterMembersResponse>('/api/v1/cluster/members');
}

export async function addMember(req: AddMemberRequest): Promise<ClusterMembersResponse> {
  return request<ClusterMembersResponse>('/api/v1/cluster/members', {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export async function removeMember(nodeId: string): Promise<ClusterMembersResponse> {
  return request<ClusterMembersResponse>(`/api/v1/cluster/members/${nodeId}`, {
    method: 'DELETE',
  });
}

// ─── Key-Value APIs ──────────────────────────────────────────────────────────

export async function getValue(
  key: string,
  linearizable = true
): Promise<KeyValueResponse> {
  return request<KeyValueResponse>(
    `/api/v1/kv/${encodeURIComponent(key)}?linearizable=${linearizable}`
  );
}

export async function putValue(
  key: string,
  value: string
): Promise<KeyValueResponse> {
  return request<KeyValueResponse>(`/api/v1/kv/${encodeURIComponent(key)}`, {
    method: 'POST',
    body: JSON.stringify({ value }),
  });
}

export async function deleteValue(key: string): Promise<KeyValueResponse> {
  return request<KeyValueResponse>(`/api/v1/kv/${encodeURIComponent(key)}`, {
    method: 'DELETE',
  });
}

// ─── Health / Actuator APIs ──────────────────────────────────────────────────

export async function getHealth(): Promise<HealthResponse> {
  return request<HealthResponse>('/actuator/health');
}

// ─── Admin APIs ──────────────────────────────────────────────────────────────

export async function triggerSnapshot(): Promise<{ message: string }> {
  return request<{ message: string }>('/api/v1/admin/snapshot', {
    method: 'POST',
  });
}

export { ApiError };
