import { NextRequest } from 'next/server';

export const dynamic = 'force-dynamic';

export async function GET(request: NextRequest) {
  const backendUrl =
    process.env.BACKEND_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
  const pathname = request.nextUrl.pathname;
  const search = request.nextUrl.search;
  const targetUrl = `${backendUrl}${pathname}${search}`;

  const requestHeaders = new Headers(request.headers);
  requestHeaders.delete('host');

  const serverAuthToken = process.env.BACKEND_AUTH_TOKEN || process.env.AUTH_TOKEN;
  if (serverAuthToken && !requestHeaders.has('authorization') && !requestHeaders.has('x-api-key')) {
    requestHeaders.set('authorization', `Bearer ${serverAuthToken}`);
  }

  let upstreamRes = await fetch(targetUrl, {
    headers: requestHeaders,
    cache: 'no-store',
  });

  // If initial node is not leader (503 Service Unavailable), discover leader and connect to leader
  if (upstreamRes.status === 503) {
    try {
      const leaderCheck = await fetch(`${backendUrl}/api/v1/cluster/leader`, {
        headers: requestHeaders,
        cache: 'no-store',
      });
      if (leaderCheck.ok) {
        const leaderData = await leaderCheck.json();
        if (leaderData.address) {
          const leaderBase = leaderData.address.startsWith('http')
            ? leaderData.address
            : `http://${leaderData.address}`;
          const retryRes = await fetch(`${leaderBase}${pathname}${search}`, {
            headers: requestHeaders,
            cache: 'no-store',
          });
          if (retryRes.ok) {
            upstreamRes = retryRes;
          }
        }
      }
    } catch {
      // Keep original upstream response if leader discovery fails
    }
  }

  if (!upstreamRes.ok) {
    return new Response(upstreamRes.body, {
      status: upstreamRes.status,
      statusText: upstreamRes.statusText,
    });
  }

  return new Response(upstreamRes.body, {
    status: 200,
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
      'X-Accel-Buffering': 'no',
    },
  });
}
