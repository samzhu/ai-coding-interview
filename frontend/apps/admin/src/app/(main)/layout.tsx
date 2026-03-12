"use client";

import { TopNav } from "@/components/admin/top-nav";
import { useAuth } from "@/hooks/use-auth";
import { LoginButton } from "@/components/auth/login-button";
import { UserMenu } from "@/components/auth/user-menu";

/**
 * Admin 主 Layout，包含認證守衛（Auth Guard）。
 *
 * 設計說明：
 * - securityEnabled=false（開發模式）：直接渲染頁面，TopNav 不顯示使用者選單
 * - securityEnabled=true + user=null（未登入）：顯示登入頁面
 * - securityEnabled=true + user≠null（已登入）：渲染頁面，TopNav 顯示 UserMenu
 *
 * 使用 "use client" 因為 useAuth hook 需要 browser context（useEffect + fetch）。
 * Next.js App Router 允許 client layout 包含 server component children。
 */
export default function MainLayout({ children }: { children: React.ReactNode }) {
  const { loading, user, securityEnabled } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-muted-foreground text-sm">載入中...</p>
      </div>
    );
  }

  if (securityEnabled && !user) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center space-y-6">
          <div className="space-y-2">
            <h1 className="text-2xl font-bold">AI 面試平台</h1>
            <p className="text-muted-foreground text-sm">請登入以繼續</p>
          </div>
          <LoginButton />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col">
      <TopNav
        rightSlot={
          securityEnabled && user ? (
            <UserMenu name={user.name} email={user.email} />
          ) : undefined
        }
      />
      <main className="flex-1 p-8">{children}</main>
    </div>
  );
}
