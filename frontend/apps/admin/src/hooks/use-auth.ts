"use client";

/**
 * Admin 認證狀態 Hook。
 *
 * 設計說明：
 * 委派至 AuthContext，由 AuthContextProvider（auth-context.tsx）根據
 * OIDC env vars 決定使用 OIDC bridge 或 dev provider。
 * 此 hook 是對外介面，元件只需 import useAuth，不須感知底層實作。
 */
export { useAppAuth as useAuth } from "@/contexts/auth-context";
