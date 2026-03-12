"use client";

import { AuthProvider } from "react-oidc-context";

/**
 * 條件式 OIDC Provider。
 *
 * 設計說明：
 * - NEXT_PUBLIC_OIDC_AUTHORITY + NEXT_PUBLIC_OIDC_CLIENT_ID 齊全 → 掛載 OIDC AuthProvider（生產模式）
 * - 任一缺少（開發模式）→ 直接 render children，不掛載 OIDC 邏輯
 *
 * 使用 oidc-client-ts / react-oidc-context 支援任何標準 OIDC Provider（Keycloak、Auth0、Azure AD 等），
 * PKCE flow 為 oidc-client-ts 預設行為，無需額外設定。
 */

const authority = process.env.NEXT_PUBLIC_OIDC_AUTHORITY;
const clientId = process.env.NEXT_PUBLIC_OIDC_CLIENT_ID;

const oidcConfig = authority && clientId
  ? {
      authority,
      client_id: clientId,
      redirect_uri: typeof window !== "undefined" ? window.location.origin : "",
      post_logout_redirect_uri: typeof window !== "undefined" ? window.location.origin : "",
      // Spring Authorization Server scopes_supported 只有 ["openid"]
      // profile/email scope 不可用，否則 token exchange 可能報錯
      scope: "openid",
      // Token exchange 完成後移除 URL 上的 ?code=&state= 參數
      // 避免 re-render 時重複處理已消費的 authorization code
      onSigninCallback: () => {
        window.history.replaceState({}, document.title, window.location.pathname);
      },
    }
  : null;

export function OidcProviderWrapper({ children }: { children: React.ReactNode }) {
  if (!oidcConfig) {
    // 開發模式：env vars 未設定，直接渲染
    return <>{children}</>;
  }

  return <AuthProvider {...oidcConfig}>{children}</AuthProvider>;
}
