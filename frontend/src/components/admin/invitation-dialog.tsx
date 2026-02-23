"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

interface InvitationDialogProps {
  inviteUrl: string;
  open: boolean;
  onClose: () => void;
}

export function InvitationDialog({
  inviteUrl,
  open,
  onClose,
}: InvitationDialogProps) {
  const [copied, setCopied] = useState(false);

  async function handleCopy() {
    await navigator.clipboard.writeText(inviteUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>面試邀請連結</DialogTitle>
          <DialogDescription>
            將以下連結傳送給候選人，連結將在 48 小時後過期。
          </DialogDescription>
        </DialogHeader>
        <div className="flex items-center gap-2">
          <Input value={inviteUrl} readOnly className="font-mono text-sm" />
          <Button onClick={handleCopy} variant="outline" className="shrink-0">
            {copied ? "已複製" : "複製"}
          </Button>
        </div>
        <Button onClick={onClose} className="w-full mt-2">
          關閉
        </Button>
      </DialogContent>
    </Dialog>
  );
}
