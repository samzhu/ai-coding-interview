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
      <Card className="max-w-md w-full text-center">
        <CardHeader>
          <div className="flex justify-center mb-4">
            <Badge variant="secondary" className="text-lg px-4 py-2">
              面試完成
            </Badge>
          </div>
          <CardTitle className="text-2xl">恭喜完成面試！</CardTitle>
          <CardDescription>
            您已完成所有程式挑戰。面試官將在審閱後與您聯絡。
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            面試 ID：<span className="font-mono">{id}</span>
          </p>
          <p className="text-sm text-muted-foreground">
            您可以關閉此頁面。
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
