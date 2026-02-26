"use client";

import { Badge } from "@interview/shared/components/ui/badge";
import type { QuestionResponse } from "@interview/shared/types";

interface QuestionListTableProps {
  questions: QuestionResponse[];
}

const DIFFICULTY_BADGE: Record<string, { label: string; className: string }> = {
  EASY:   { label: "簡單", className: "bg-emerald-500/15 text-emerald-400 border-0" },
  MEDIUM: { label: "中等", className: "bg-amber-500/15 text-amber-400 border-0" },
  HARD:   { label: "困難", className: "bg-red-500/15 text-red-400 border-0" },
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
          </tr>
        </thead>
        <tbody>
          {questions.map((question) => {
            const badge = DIFFICULTY_BADGE[question.difficulty] ?? { label: question.difficulty, className: "bg-muted text-muted-foreground border-0" };
            return (
              <tr key={question.id} className="border-t hover:bg-muted/30">
                <td className="px-4 py-3 font-medium">{question.title}</td>
                <td className="px-4 py-3">
                  <Badge className={badge.className}>{badge.label}</Badge>
                </td>
                <td className="px-4 py-3 text-muted-foreground">{question.language}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
