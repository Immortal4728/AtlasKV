import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const backendUrl = process.env.BACKEND_URL || process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8081';
  const targetUrl = new URL(request.nextUrl.pathname + request.nextUrl.search, backendUrl);
  return NextResponse.rewrite(targetUrl);
}

export const config = {
  matcher: ['/api/v1/:path*', '/actuator/:path*'],
};
