"use client";

import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useAuth as useOidcAuth } from "react-oidc-context";
import { setBearerToken } from "@interview/shared/lib/api-client";
import { apiGet } from "@interview/shared/lib/api-client";

interface AdminUser {
  name: string;
  email?: string;
  picture?: string;
}

interface AuthContextValue {
  loading: boolean;
  user: AdminUser | null;
  securityEnabled: boolean;
}

const AuthContext = createContext<AuthContextValue>({
  loading: true,
  user: null,
  securityEnabled: false,
});

/**
 * OIDC 橋接器：將 react-oidc-context 的 auth 狀態橋接至 AuthContext。
 *
 * 設計說明：
 * useAuth() from react-oidc-context 只能在 AuthProvider 內部呼叫。
 * 此元件掛載在 AuthProvider 內，負責：
 * 1. 監聽 OIDC 登入狀態
 * 2. 取得 access_token → setBearerToken → 更新 api-client
 * 3. 呼叫 /auth/me 確認後端接受此 token
 * 4. 將結果向下提供給 AuthContext consumers
 */
function OidcAuthBridge({ children }: { children: React.ReactNode }) {
  const oidcAuth = useOidcAuth();
  const [user, setUser] = useState<AdminUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (oidcAuth.isLoading) return;

    if (oidcAuth.isAuthenticated && oidcAuth.user) {
      const token = oidcAuth.user.access_token;
      setBearerToken(token);

      // 驗證後端接受此 JWT（auth health check）
      apiGet<AdminUser>("/auth/me")
        .then((me) => {
          setUser({
            name: me.name || oidcAuth.user?.profile?.name || "Admin",
            email: me.email || oidcAuth.user?.profile?.email,
            picture: oidcAuth.user?.profile?.picture as string | undefined,
          });
        })
        .catch(() => {
          // /me 失敗時仍使用 OIDC profile（後端可能尚未就緒）
          setUser({
            name: oidcAuth.user?.profile?.name || "Admin",
            email: oidcAuth.user?.profile?.email,
          });
        })
        .finally(() => setLoading(false));
    } else {
      setBearerToken(null);
      setUser(null);
      setLoading(false);
    }
  }, [oidcAuth.isLoading, oidcAuth.isAuthenticated, oidcAuth.user]);

  const value = useMemo(
    () => ({ loading, user, securityEnabled: true }),
    [loading, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * 開發模式 Provider：security disabled，直接視為 Developer 已登入。
 */
function DevAuthProvider({ children }: { children: React.ReactNode }) {
  const value = useMemo(
    () => ({
      loading: false,
      user: { name: "Developer" } as AdminUser,
      securityEnabled: false,
    }),
    []
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

const oidcAuthority = process.env.NEXT_PUBLIC_OIDC_AUTHORITY;
const oidcClientId = process.env.NEXT_PUBLIC_OIDC_CLIENT_ID;

/**
 * AuthContextProvider：根據 OIDC env vars 決定使用 OIDC bridge 或 dev provider。
 *
 * 設計說明：
 * - OIDC env vars 存在 → OidcAuthBridge（包在 react-oidc-context 的 AuthProvider 內）
 * - OIDC env vars 不存在 → DevAuthProvider（直接回傳固定的 Developer 狀態）
 *
 * 此元件需掛載在 OidcProviderWrapper 內部（確保 OidcAuthBridge 在 AuthProvider scope 中）。
 */
export function AuthContextProvider({ children }: { children: React.ReactNode }) {
  if (oidcAuthority && oidcClientId) {
    return <OidcAuthBridge>{children}</OidcAuthBridge>;
  }
  return <DevAuthProvider>{children}</DevAuthProvider>;
}

/**
 * 讀取 Admin 認證狀態的 hook。
 * 透過 AuthContext 取得，不直接依賴 react-oidc-context。
 */
export function useAppAuth(): AuthContextValue {
  return useContext(AuthContext);
}
