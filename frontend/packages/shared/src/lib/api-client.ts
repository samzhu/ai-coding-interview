// Client-side API utilities
// Used by React components and hooks (browser context)
//
// NEXT_PUBLIC_API_BASE controls the API host:
//   - dev (npm run dev):  leave unset → "" → relative path /api/v1/... (Next.js proxy)
//   - prod (Docker):      set to http://localhost:8080 → absolute path http://localhost:8080/api/v1/...
//   - Cloud Run:          set to https://my-service.run.app → https://my-service.run.app/api/v1/...

const API_HOST = process.env.NEXT_PUBLIC_API_BASE ?? "";
const API_BASE = API_HOST + "/api/v1";

export function getApiUrl(path: string): string {
  return API_BASE + path;
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

export { ApiError };
