// Client-side API utilities
// Used by React components and hooks (browser context)
//
// NEXT_PUBLIC_API_BASE is injected at build-time via `next build`:
//   - dev (npm run dev):  unset → "" → relative path /api/v1/... (Next.js proxy)
//   - prod (Docker):      NEXT_PUBLIC_API_BASE=http://localhost:8080 passed as ARG/ENV at build time
//
// 設計說明：Bearer token 認證模式
// - Admin：前端透過 oidc-client-ts 取得 access token → setBearerToken → Authorization: Bearer
// - Candidate：invitation token → setBearerToken → Authorization: Bearer
// - Dev（security disabled）：不設 token，後端 permitAll

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

// Bearer token（Admin access token 或 Candidate invitation token），由 setBearerToken 設定
let _bearerToken: string | null = null;

/**
 * 設定 Bearer token。
 * Admin：呼叫 oidc-client-ts 取得 access token 後呼叫此方法。
 * Candidate：呼叫 invitation token 驗證後呼叫此方法。
 * 呼叫後，所有 API 請求都會帶上 Authorization: Bearer <token>。
 * 傳入 null 清除 token。
 */
export function setBearerToken(token: string | null): void {
  _bearerToken = token;
}

function buildFetchOptions(method: string, body?: unknown): RequestInit {
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (_bearerToken) {
    headers["Authorization"] = `Bearer ${_bearerToken}`;
  }
  return {
    method,
    headers,
    credentials: "same-origin",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  };
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
  const response = await fetch(getApiUrl(path), buildFetchOptions("GET"));
  return handleResponse<T>(response);
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(getApiUrl(path), buildFetchOptions("POST", body));
  return handleResponse<T>(response);
}

export async function apiPut<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(getApiUrl(path), buildFetchOptions("PUT", body));
  return handleResponse<T>(response);
}

export async function apiDelete<T>(path: string): Promise<T> {
  const response = await fetch(getApiUrl(path), buildFetchOptions("DELETE"));
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
 *
 * 安全模式下，WebSocket 連線需帶 ?token=<invitation-token> 參數（由 caller 負責附加）。
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
