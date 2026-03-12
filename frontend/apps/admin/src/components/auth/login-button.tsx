"use client";

import { useAuth } from "react-oidc-context";
import { Button } from "@interview/shared/components/ui/button";

/**
 * OIDC 登入按鈕。
 *
 * 設計說明：
 * 使用 react-oidc-context 的 signinRedirect()，由 oidc-client-ts 執行 PKCE flow，
 * 直接導向 OIDC Provider 登入頁。前端自行持有 access token，後端純 Resource Server。
 * 此元件只在 OIDC 已配置（OidcProviderWrapper 掛載了 AuthProvider）時渲染。
 */
export function LoginButton() {
  const auth = useAuth();

  return (
    <Button onClick={() => auth.signinRedirect()} size="lg">
      登入
    </Button>
  );
}
