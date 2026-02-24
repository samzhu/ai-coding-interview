"use client";

import { Badge } from "@interview/shared/components/ui/badge";
import type { QuestionResponse } from "@interview/shared/types";

interface QuestionListTableProps {
  questions: QuestionResponse[];
}

const DIFFICULTY_BADGE: Record<
  string,
  { label: string; variant: "default" | "secondary" | "destructive" | "outline" }
> = {
  EASY: { label: "簡單", variant: "default" },
  MEDIUM: { label: "中等", variant: "secondary" },
  HARD: { label: "困難", variant: "destructive" },
};

export function QuestionListTable({ questions }: QuestionListTableProps) {
  if (questions.length === 0) {
    return (
      <p className="text-center text-muted-foreground py-12">
        目前沒有題目。
      </p>
    );
  }

  return (
    <div className="rounded-lg border overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-muted/50">
          <tr>
            <th className="text-left px-4 py-3 font-medium">標題</th>
            <th className="text-left px-4 py-3 font-medium">難度</th>
            <th className="text-left px-4 py-3 font-medium">語言</th>
            <th className="text-left px-4 py-3 font-medium">Checkpoint 數</th>
          </tr>
        </thead>
        <tbody>
          {questions.map((question) => {
            const badge = DIFFICULTY_BADGE[question.difficulty] ?? { label: question.difficulty, variant: "outline" as const };
            return (
              <tr key={question.id} className="border-t hover:bg-muted/30">
                <td className="px-4 py-3 font-medium">{question.title}</td>
                <td className="px-4 py-3">
                  <Badge variant={badge.variant}>{badge.label}</Badge>
                </td>
                <td className="px-4 py-3 text-muted-foreground">{question.language}</td>
                <td className="px-4 py-3 text-muted-foreground">
                  {question.checkpoints.length} 個
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
