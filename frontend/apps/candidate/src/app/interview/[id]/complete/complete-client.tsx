"use client";

import { useRouteParam } from "@interview/shared/hooks/use-route-param";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@interview/shared/components/ui/card";
import { Badge } from "@interview/shared/components/ui/badge";

export function CompleteClient() {
  const id = useRouteParam("/interview/:id", "id");

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <Card className="max-w-md w-full text-center border-t-2 border-t-emerald-400">
        <CardHeader>
          <div className="flex justify-center mb-4">
            <Badge className="bg-emerald-500/15 text-emerald-400 border-0 text-lg px-4 py-2">
              面試完成
            </Badge>
          </div>
          <CardTitle className="text-2xl">測試已完成，感謝您的時間</CardTitle>
          <CardDescription>
            我們已收到您的作答。後續結果將由面試官審閱後通知您。
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            參考編號：<span className="font-mono">{id}</span>
          </p>
          <p className="text-sm text-muted-foreground">
            您現在可以安全地關閉此頁面。
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
