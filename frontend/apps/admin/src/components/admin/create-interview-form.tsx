"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@interview/shared/components/ui/button";
import { Input } from "@interview/shared/components/ui/input";
import { Label } from "@interview/shared/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@interview/shared/components/ui/select";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@interview/shared/components/ui/card";
import { InvitationDialog } from "./invitation-dialog";
import { apiPost, apiGet } from "@interview/shared/lib/api-client";
import type { QuestionResponse, InvitationResponse } from "@interview/shared/types";

interface AiModelInfo {
  id: string;
  name: string;
  provider: string;
}

interface CreateInterviewFormProps {
  questions: QuestionResponse[];
}

export function CreateInterviewForm({ questions }: CreateInterviewFormProps) {
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [inviteUrl, setInviteUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [models, setModels] = useState<AiModelInfo[]>([]);
  const [modelsLoading, setModelsLoading] = useState(true);

  const [form, setForm] = useState({
    title: "",
    candidateId: crypto.randomUUID(),
    interviewerId: crypto.randomUUID(),
    questionId: "",
    scheduledAt: new Date(Date.now() + 3600_000).toISOString().slice(0, 16),
    aiModel: "",
    durationMinutes: "60",
  });

  useEffect(() => {
    apiGet<AiModelInfo[]>("/ai/models")
      .then((data) => {
        setModels(data);
        if (data.length > 0 && !form.aiModel) {
          setForm((prev) => ({ ...prev, aiModel: data[0].id }));
        }
      })
      .catch(() => {
        setModels([]);
      })
      .finally(() => setModelsLoading(false));
  }, []);

  function update(field: string, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    try {
      const interview = await apiPost<{ id: string }>("/interviews", {
        ...form,
        scheduledAt: new Date(form.scheduledAt).toISOString(),
        durationMinutes: parseInt(form.durationMinutes, 10) || 60,
      });

      const invitation = await apiPost<InvitationResponse>(
        `/interviews/${interview.id}/invitation`
      );

      const candidateUrl = process.env.NEXT_PUBLIC_CANDIDATE_URL ?? window.location.origin;
      setInviteUrl(`${candidateUrl}/invite/${invitation.token}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "建立失敗");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      <Card className="max-w-lg">
        <CardHeader>
          <CardTitle>建立新面試</CardTitle>
          <CardDescription>填寫面試資訊並選擇題目</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="title">面試標題</Label>
              <Input
                id="title"
                placeholder="例：前端工程師面試 - 王小明"
                value={form.title}
                onChange={(e) => update("title", e.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="question">選擇題目</Label>
              <Select
                value={form.questionId}
                onValueChange={(v) => update("questionId", v)}
                required
              >
                <SelectTrigger id="question">
                  <SelectValue placeholder="選擇題目..." />
                </SelectTrigger>
                <SelectContent>
                  {questions.map((q) => (
                    <SelectItem key={q.id} value={q.id}>
                      {q.title}{" "}
                      <span className="text-muted-foreground">
                        ({q.difficulty})
                      </span>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="scheduledAt">排程時間</Label>
              <Input
                id="scheduledAt"
                type="datetime-local"
                value={form.scheduledAt}
                onChange={(e) => update("scheduledAt", e.target.value)}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="durationMinutes">面試時長（分鐘）</Label>
              <Select
                value={form.durationMinutes}
                onValueChange={(v) => update("durationMinutes", v)}
              >
                <SelectTrigger id="durationMinutes">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="30">30 分鐘</SelectItem>
                  <SelectItem value="45">45 分鐘</SelectItem>
                  <SelectItem value="60">60 分鐘（預設）</SelectItem>
                  <SelectItem value="90">90 分鐘</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="aiModel">AI 模型</Label>
              <Select
                value={form.aiModel}
                onValueChange={(v) => update("aiModel", v)}
                disabled={modelsLoading}
              >
                <SelectTrigger id="aiModel">
                  <SelectValue
                    placeholder={modelsLoading ? "載入模型中..." : "選擇模型"}
                  />
                </SelectTrigger>
                <SelectContent>
                  {models.map((m) => (
                    <SelectItem key={m.id} value={m.id}>
                      {m.name}
                    </SelectItem>
                  ))}
                  {models.length === 0 && !modelsLoading && (
                    <SelectItem value="gemini-2.5-flash" disabled>
                      無可用模型（請設定 API Key）
                    </SelectItem>
                  )}
                </SelectContent>
              </Select>
            </div>

            {error && (
              <p className="text-sm text-destructive">{error}</p>
            )}

            <Button type="submit" disabled={isSubmitting} className="w-full">
              {isSubmitting ? "建立中..." : "建立面試並產生邀請連結"}
            </Button>
          </form>
        </CardContent>
      </Card>

      {inviteUrl && (
        <InvitationDialog
          inviteUrl={inviteUrl}
          open={true}
          onClose={() => router.push("/")}
        />
      )}
    </>
  );
}
