"use client";

import { useEffect, useState } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@interview/shared/components/ui/card";
import { Badge } from "@interview/shared/components/ui/badge";
import { JoinButton } from "./join-button";
import { useRouteParam } from "@interview/shared/hooks/use-route-param";
import { apiGet } from "@interview/shared/lib/api-client";
import type { InvitationResponse } from "@interview/shared/types";

export function InviteClient() {
  const token = useRouteParam("/invite/:token", "token");
  const [invitation, setInvitation] = useState<InvitationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!token) return;
    apiGet<InvitationResponse>(`/invitations/${token}`)
      .then(setInvitation)
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [token]);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        <p className="text-muted-foreground">載入中...</p>
      </div>
    );
  }

  if (notFound || !invitation) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        <Card className="max-w-md w-full">
          <CardHeader>
            <CardTitle>邀請連結無效</CardTitle>
            <CardDescription>找不到此邀請連結，請確認連結是否正確。</CardDescription>
          </CardHeader>
        </Card>
      </div>
    );
  }

  const expiresAt = new Date(invitation.expiresAt);
  const isExpired = expiresAt < new Date();

  if (isExpired) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center p-4">
        <Card className="max-w-md w-full">
          <CardHeader>
            <CardTitle>邀請連結已過期</CardTitle>
            <CardDescription>請聯絡面試官重新產生邀請連結。</CardDescription>
          </CardHeader>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <Card className="max-w-md w-full">
        <CardHeader>
          <div className="flex items-center gap-2 mb-2">
            <Badge variant="secondary">面試邀請</Badge>
          </div>
          <CardTitle>您已受邀參加程式面試</CardTitle>
          <CardDescription>
            點擊「加入面試」按鈕即可開始面試。請確保您在安靜的環境中進行。
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="text-sm text-muted-foreground space-y-1">
            <p>邀請有效期限：{expiresAt.toLocaleString("zh-TW")}</p>
          </div>
          <JoinButton token={token!} />
        </CardContent>
      </Card>
    </div>
  );
}
