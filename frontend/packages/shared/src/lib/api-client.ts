// Client-side API utilities
// Used by React components and hooks (browser context)
//
// NEXT_PUBLIC_API_BASE is injected at build-time via `next build`:
//   - dev (npm run dev):  unset → "" → relative path /api/v1/... (Next.js proxy)
//   - prod (Docker):      NEXT_PUBLIC_API_BASE=http://localhost:8080 passed as ARG/ENV at build time

function getApiBase(): string {
  return (process.env.NEXT_PUBLIC_API_BASE ?? "") + "/api/v1";
}

export function getApiUrl(path: string): string {
  return getApiBase() + path;
}

class ApiError extends Error {
  constructor(
    public status: number,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const text = await response.text().catch(() => response.statusText);
    throw new ApiError(response.status, text);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(getApiUrl(path), {
    method: "GET",
    headers: { "Content-Type": "application/json" },
  });
  return handleResponse<T>(response);
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(getApiUrl(path), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response);
}

export async function apiPut<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(getApiUrl(path), {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response);
}

export async function apiDelete<T>(path: string): Promise<T> {
  const response = await fetch(getApiUrl(path), {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
  });
  return handleResponse<T>(response);
}

/**
 * 取得 WebSocket URL。
 *
 * 設計說明：
 * Next.js 的 rewrite 只支援 HTTP，無法代理 WebSocket 連線。
 * 因此終端機 WebSocket 必須直接連後端，繞過 Next.js。
 * - dev：NEXT_PUBLIC_API_BASE 未設定時，直接連 localhost:8080
 * - prod：將 http/https 轉換為 ws/wss，指向同一後端 host
 */
export function getWsUrl(path: string): string {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE ?? "";
  if (apiBase) {
    const url = new URL(apiBase);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    return url.origin + "/api/v1" + path;
  }
  // dev 模式：直接連後端，不走 Next.js proxy
  return "ws://localhost:8080/api/v1" + path;
}

export { ApiError };
