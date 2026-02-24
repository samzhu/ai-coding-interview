"use client";

import { usePathname } from "next/navigation";

/**
 * Extract a dynamic route parameter from the actual browser URL pathname.
 *
 * Next.js static export pre-renders dynamic routes with placeholder "_",
 * so useParams() returns "_" instead of the real value. This hook reads
 * the actual pathname to get the correct segment.
 *
 * @param segmentName - the param name matching the folder (e.g. "token" for /invite/[token])
 * @param routePattern - the route pattern with the param placeholder
 *   e.g. "/invite/:token", "/interview/:id", "/admin/interviews/:id/monitor"
 */
export function useRouteParam(routePattern: string, segmentName: string): string {
  const pathname = usePathname();
  const patternParts = routePattern.split("/");
  const pathParts = pathname.split("/");

  const index = patternParts.findIndex((p) => p === `:${segmentName}`);
  if (index === -1 || index >= pathParts.length) {
    return "";
  }
  return decodeURIComponent(pathParts[index]);
}
