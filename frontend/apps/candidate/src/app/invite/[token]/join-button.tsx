"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@interview/shared/components/ui/button";
import { apiGet, apiPost, setBearerToken } from "@interview/shared/lib/api-client";
import type { InvitationResponse } from "@interview/shared/types";

interface JoinButtonProps {
  token: string;
}

export function JoinButton({ token }: JoinButtonProps) {
  const router = useRouter();
  const [isJoining, setIsJoining] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleJoin() {
    setIsJoining(true);
    setError(null);
    try {
      const invitation = await apiPost<InvitationResponse>(
        `/invitations/${token}/join`
      );

      // 設計說明：安全模式下，join 成功後將 invitation token 設為 Bearer token，
      // 後續 API 呼叫（提交程式碼、AI 聊天、終端機等）均以此 token 認證。
      // 同時存入 sessionStorage，頁面重整後可恢復（由 interview 頁面讀取）。
      const config = await apiGet<{ securityEnabled: boolean }>("/config").catch(() => ({
        securityEnabled: false,
      }));
      if (config.securityEnabled) {
        sessionStorage.setItem("invitation_token", invitation.token);
        setBearerToken(invitation.token);
      }

      router.push(`/interview/${invitation.interviewId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加入失敗，請稍後再試");
      setIsJoining(false);
    }
  }

  return (
    <div className="space-y-3">
      {error && <p className="text-sm text-destructive">{error}</p>}
      <Button size="lg" onClick={handleJoin} disabled={isJoining}>
        {isJoining ? "正在加入..." : "加入面試"}
      </Button>
    </div>
  );
}
