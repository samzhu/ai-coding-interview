"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@interview/shared/components/ui/button";
import { apiPost } from "@interview/shared/lib/api-client";
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
