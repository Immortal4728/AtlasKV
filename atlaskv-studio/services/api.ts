// ─── AtlasKV Centralized Axios API Client ─────────────────────────────────────
import axios, { AxiosInstance, AxiosError } from 'axios';
import type {
  ClusterStatusResponse,
  LeaderResponse,
  MetricsResponse,
  ClusterMembersResponse,
  KeyValueResponse,
  PrefixQueryResponse,
  LeaseResponse,
  LeaseRequest,
  CasConflictResponse,
  RevisionResponse,
  AddMemberRequest,
  HealthResponse,
  AuthInfoResponse,
} from '@/types/api';

const DEFAULT_BASE_URL = '';
const DEFAULT_TIMEOUT = 5000;

export function getSavedBaseUrl(): string {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('atlaskv-server-url') ?? DEFAULT_BASE_URL;
  }
  return DEFAULT_BASE_URL;
}

export function setSavedBaseUrl(url: string): void {
  if (typeof window !== 'undefined') {
    const trimmed = url.trim().replace(/\/+$/, '');
    if (trimmed) {
      localStorage.setItem('atlaskv-server-url', trimmed);
    } else {
      localStorage.removeItem('atlaskv-server-url');
    }
  }
}

export function getSavedApiKey(): string {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('atlaskv-api-key') ?? '';
  }
  return '';
}

export function setSavedApiKey(key: string): void {
  if (typeof window !== 'undefined') {
    const trimmed = key.trim();
    if (trimmed) {
      localStorage.setItem('atlaskv-api-key', trimmed);
    } else {
      localStorage.removeItem('atlaskv-api-key');
    }
  }
}

export function getSavedAdminNamespace(): string {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('atlaskv-admin-namespace') ?? '';
  }
  return '';
}

export function setSavedAdminNamespace(namespace: string): void {
  if (typeof window !== 'undefined') {
    const trimmed = namespace.trim();
    if (trimmed) {
      localStorage.setItem('atlaskv-admin-namespace', trimmed);
    } else {
      localStorage.removeItem('atlaskv-admin-namespace');
    }
  }
}

export function clearSavedCredentials(): void {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('atlaskv-api-key');
    localStorage.removeItem('atlaskv-admin-namespace');
  }
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public statusText: string,
    message: string,
    public details?: unknown
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export class ConflictError extends ApiError {
  constructor(
    public expectedVersion: number,
    public currentVersion: number,
    message: string
  ) {
    super(409, 'Conflict', message);
    this.name = 'ConflictError';
  }
}

// Axios instance with request/response interceptors
const httpClient: AxiosInstance = axios.create({
  timeout: DEFAULT_TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

httpClient.interceptors.request.use((config) => {
  const baseUrl = getSavedBaseUrl();
  if (baseUrl && !config.url?.startsWith('http')) {
    config.baseURL = baseUrl;
  }

  const apiKey = getSavedApiKey();
  if (apiKey && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${apiKey}`;
  }

  const adminNamespace = getSavedAdminNamespace();
  if (adminNamespace && !config.headers['X-Namespace']) {
    config.headers['X-Namespace'] = adminNamespace;
  }

  return config;
});

function cleanKeyPath(key: string): string {
  if (!key) return '';
  const trimmed = key.trim();
  return trimmed.startsWith('/') ? trimmed.slice(1) : trimmed;
}

function sanitizeErrorMessage(msg: string): string {
  if (!msg) return 'Unknown error';
  return msg
    .replace(/Bearer\s+[A-Za-z0-9_\-\.]+/gi, 'Bearer [REDACTED]')
    .replace(/(?:apiKey|token)=[A-Za-z0-9_\-\.]+/gi, 'apiKey=[REDACTED]');
}

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response) {
      const status = error.response.status;
      const data = error.response.data as any;

      if (status === 503 && data && (data.leaderAddress || data.leaderId) && !(error.config as any)?._retry) {
        (error.config as any)._retry = true;
        let targetHost: string | null = null;
        if (data.leaderId === 'node1') targetHost = 'http://localhost:8081';
        else if (data.leaderId === 'node2') targetHost = 'http://localhost:8082';
        else if (data.leaderId === 'node3') targetHost = 'http://localhost:8083';
        else if (data.leaderAddress) {
          const parts = data.leaderAddress.split(':');
          const port = parts[parts.length - 1];
          targetHost = `http://localhost:${port}`;
        }

        if (targetHost && error.config) {
          error.config.baseURL = targetHost;
          return httpClient.request(error.config);
        }
      }

      if (status === 409 && data?.expectedVersion !== undefined) {
        throw new ConflictError(
          data.expectedVersion,
          data.currentVersion,
          data.message || 'Version mismatch'
        );
      }

      // Check if data is an HTML document or raw non-JSON string
      let message: string;
      if (typeof data === 'string') {
        if (data.trim().startsWith('<') || data.includes('<html')) {
          message = `HTTP ${status} ${error.response.statusText || 'Bad Request'}`;
        } else {
          message = data.length > 200 ? `HTTP ${status} ${error.response.statusText || 'Error'}` : data;
        }
      } else if (data && typeof data === 'object') {
        message = data.detail || data.message || data.error || `HTTP ${status} ${error.response.statusText || 'Error'}`;
      } else {
        message = error.message || `HTTP ${status} ${error.response.statusText || 'Error'}`;
      }

      // Sanitize any accidentally leaked tokens/keys from message
      message = sanitizeErrorMessage(message);

      throw new ApiError(
        status,
        error.response.statusText,
        message,
        typeof data === 'object' ? data : undefined
      );
    } else if (error.request) {
      throw new ApiError(0, 'Network Error', 'Network error or cluster unreachable');
    }
    throw error;
  }
);

// ─── Cluster API ─────────────────────────────────────────────────────────────
export const ClusterApi = {
  async getStatus(): Promise<ClusterStatusResponse> {
    const res = await httpClient.get<ClusterStatusResponse>('/api/v1/cluster/status');
    return res.data;
  },

  async getLeader(): Promise<LeaderResponse> {
    const res = await httpClient.get<LeaderResponse>('/api/v1/cluster/leader');
    return res.data;
  },

  async getMembers(): Promise<ClusterMembersResponse> {
    const res = await httpClient.get<ClusterMembersResponse>('/api/v1/cluster/members');
    return res.data;
  },

  async addMember(req: AddMemberRequest): Promise<ClusterMembersResponse> {
    const res = await httpClient.post<ClusterMembersResponse>('/api/v1/cluster/members', req);
    return res.data;
  },

  async removeMember(nodeId: string): Promise<ClusterMembersResponse> {
    const res = await httpClient.delete<ClusterMembersResponse>(`/api/v1/cluster/members/${nodeId}`);
    return res.data;
  },
};

// ─── Key-Value API ───────────────────────────────────────────────────────────
export const KeyValueApi = {
  async get(key: string, linearizable = true): Promise<KeyValueResponse> {
    const cleanKey = cleanKeyPath(key);
    const res = await httpClient.get<KeyValueResponse>(
      `/api/v1/kv/${cleanKey}?linearizable=${linearizable}`
    );
    return res.data;
  },

  async put(key: string, value: string, ttl?: string, leaseId?: string): Promise<KeyValueResponse> {
    const cleanKey = cleanKeyPath(key);
    const res = await httpClient.post<KeyValueResponse>(`/api/v1/kv/${cleanKey}`, {
      value,
      ttl: ttl || null,
      leaseId: leaseId || null,
    });
    return res.data;
  },

  async casPut(key: string, value: string, expectedVersion: number): Promise<KeyValueResponse> {
    const cleanKey = cleanKeyPath(key);
    const res = await httpClient.put<KeyValueResponse>(
      `/api/v1/kv/${cleanKey}?expectedVersion=${expectedVersion}`,
      { value }
    );
    return res.data;
  },

  async delete(key: string): Promise<KeyValueResponse> {
    const cleanKey = cleanKeyPath(key);
    const res = await httpClient.delete<KeyValueResponse>(`/api/v1/kv/${cleanKey}`);
    return res.data;
  },
};

// ─── Prefix API ──────────────────────────────────────────────────────────────
export const PrefixApi = {
  async query(prefix: string, offset = 0, limit = 100): Promise<PrefixQueryResponse> {
    const cleanPrefix = cleanKeyPath(prefix);
    const res = await httpClient.get<PrefixQueryResponse>(
      `/api/v1/kv/prefix/${cleanPrefix}?offset=${offset}&limit=${limit}&linearizable=false`
    );
    return res.data;
  },
};

// ─── Lease API ───────────────────────────────────────────────────────────────
export const LeaseApi = {
  async create(ttl: string, leaseId?: string): Promise<LeaseResponse> {
    const res = await httpClient.post<LeaseResponse>('/api/v1/lease', { ttl, leaseId });
    return res.data;
  },

  async renew(leaseId: string): Promise<LeaseResponse> {
    const res = await httpClient.post<LeaseResponse>(`/api/v1/lease/${leaseId}/renew`);
    return res.data;
  },

  async revoke(leaseId: string): Promise<void> {
    await httpClient.delete(`/api/v1/lease/${leaseId}`);
  },

  async list(): Promise<LeaseResponse[]> {
    const res = await httpClient.get<LeaseResponse[]>('/api/v1/lease');
    return res.data;
  },
};

// ─── Metrics API ─────────────────────────────────────────────────────────────
export const MetricsApi = {
  async getMetrics(): Promise<MetricsResponse> {
    const res = await httpClient.get<MetricsResponse>('/api/v1/cluster/metrics');
    return res.data;
  },
};

// ─── History API ─────────────────────────────────────────────────────────────
export const HistoryApi = {
  async getHistory(key: string): Promise<RevisionResponse> {
    const cleanKey = cleanKeyPath(key);
    const res = await httpClient.get<RevisionResponse>(`/api/v1/kv/${cleanKey}/history`);
    return res.data;
  },

  async rollback(key: string, revision: number): Promise<KeyValueResponse> {
    const cleanKey = cleanKeyPath(key);
    const res = await httpClient.post<KeyValueResponse>(
      `/api/v1/kv/${cleanKey}/rollback/${revision}`
    );
    return res.data;
  },
};

// ─── Health API ──────────────────────────────────────────────────────────────
export const HealthApi = {
  async getHealth(): Promise<HealthResponse> {
    const res = await httpClient.get<HealthResponse>('/actuator/health');
    return res.data;
  },
};

// ─── Auth API ────────────────────────────────────────────────────────────────
export const AuthApi = {
  async getAuthInfo(): Promise<AuthInfoResponse> {
    const res = await httpClient.get<AuthInfoResponse>('/api/v1/auth/me');
    return res.data;
  },
};

// Top-level export aliases for backward compatibility
export const getClusterStatus = ClusterApi.getStatus;
export const getLeader = ClusterApi.getLeader;
export const getMetrics = MetricsApi.getMetrics;
export const getMembers = ClusterApi.getMembers;
export const addMember = ClusterApi.addMember;
export const removeMember = ClusterApi.removeMember;
export const getValue = KeyValueApi.get;
export const putValue = KeyValueApi.put;
export const deleteValue = KeyValueApi.delete;
export const getHealth = HealthApi.getHealth;
export const getAuthInfo = AuthApi.getAuthInfo;

