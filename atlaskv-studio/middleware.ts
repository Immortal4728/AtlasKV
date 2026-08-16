import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const backendUrl = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
  const targetUrl = new URL(request.nextUrl.pathname + request.nextUrl.search, backendUrl);

  const requestHeaders = new Headers(request.headers);
  const serverAuthToken = process.env.BACKEND_AUTH_TOKEN || process.env.AUTH_TOKEN;

  // If client didn't supply an Authorization or X-API-Key header, use server-side token if configured
  if (serverAuthToken && !requestHeaders.has('authorization') && !requestHeaders.has('x-api-key')) {
    requestHeaders.set('authorization', `Bearer ${serverAuthToken}`);
  }

  return NextResponse.rewrite(targetUrl, {
    request: {
      headers: requestHeaders,
    },
  });
}

export const config = {
  matcher: ['/api/v1/:path*', '/actuator/:path*'],
};

