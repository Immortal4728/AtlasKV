import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

let cachedLeaderHost: string | null = null;

export async function middleware(request: NextRequest) {
  const baseBackendUrl = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
  let activeBackendUrl = cachedLeaderHost || baseBackendUrl;

  const isWrite = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(request.method);
  const requestHeaders = new Headers(request.headers);
  requestHeaders.delete('expect');
  requestHeaders.delete('Expect');
  const serverAuthToken = process.env.BACKEND_AUTH_TOKEN || process.env.AUTH_TOKEN;

  if (serverAuthToken && !requestHeaders.has('authorization') && !requestHeaders.has('x-api-key')) {
    requestHeaders.set('authorization', `Bearer ${serverAuthToken}`);
  }

  // Helper to resolve leader backend URL from leaderId
  const resolveLeaderUrl = (leaderId: string): string => {
    const num = leaderId.replace(/\D/g, '') || '1';
    if (process.env.BACKEND_URL || process.env.NODE_ENV === 'production') {
      // Containerized docker network: atlaskv-node1:8081, atlaskv-node2:8082, atlaskv-node3:8083
      return `http://atlaskv-${leaderId}:808${num}`;
    } else {
      // Local dev setup: 8081, 8082, 8083
      return `http://localhost:808${num}`;
    }
  };

  const bodyBuffer = isWrite ? await request.arrayBuffer() : undefined;

  const executeProxy = async (targetBackend: string) => {
    const targetUrl = new URL(request.nextUrl.pathname + request.nextUrl.search, targetBackend);
    return fetch(targetUrl.toString(), {
      method: request.method,
      headers: requestHeaders,
      body: bodyBuffer ? bodyBuffer.slice(0) : undefined,
      redirect: 'manual',
    });
  };

  try {
    let response = await executeProxy(activeBackendUrl);

    // If 503 Not Leader on write or query, attempt transparent leader redirection
    if (response.status === 503) {
      const clonedResp = response.clone();
      try {
        const problemData = await clonedResp.json();
        const leaderId = problemData?.leaderId;
        if (leaderId) {
          const leaderHost = resolveLeaderUrl(leaderId);
          cachedLeaderHost = leaderHost;
          response = await executeProxy(leaderHost);
        }
      } catch {
        // Fallback to original 503 response if parsing problem detail fails
      }
    }

    const resHeaders = new Headers(response.headers);
    // Remove content-encoding to prevent double decompression issues
    resHeaders.delete('content-encoding');

    return new NextResponse(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: resHeaders,
    });
  } catch (err) {
    // If cached leader failed (e.g., after re-election), invalidate cache and try base backend
    if (cachedLeaderHost) {
      cachedLeaderHost = null;
      const fallbackResponse = await executeProxy(baseBackendUrl);
      const resHeaders = new Headers(fallbackResponse.headers);
      resHeaders.delete('content-encoding');
      return new NextResponse(fallbackResponse.body, {
        status: fallbackResponse.status,
        statusText: fallbackResponse.statusText,
        headers: resHeaders,
      });
    }
    throw err;
  }
}

export const config = {
  matcher: ['/api/v1/:path*', '/actuator/:path*'],
};
