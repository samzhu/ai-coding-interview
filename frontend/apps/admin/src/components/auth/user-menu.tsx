"use client";

import { useAuth } from "react-oidc-context";
import { Button } from "@interview/shared/components/ui/button";

interface UserMenuProps {
  name: string;
  email?: string;
}

/**
 * Admin 使用者選單，顯示使用者名稱與登出按鈕。
 * 僅在 securityEnabled=true 時由 layout 渲染。
 *
 * 設計說明：
 * 使用 react-oidc-context 的 signoutRedirect() 觸發 OIDC Provider 端的登出，
 * 並清除本地的 access token。後端不參與登出流程。
 */
export function UserMenu({ name, email }: UserMenuProps) {
  const auth = useAuth();

  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-muted-foreground">
        {email ?? name}
      </span>
      <Button variant="outline" size="sm" onClick={() => auth.signoutRedirect()}>
        登出
      </Button>
    </div>
  );
}
