"use client";

import { useEffect, useState } from "react";
import { QuestionListTable } from "@/components/admin/question-list-table";
import { apiGet } from "@interview/shared/lib/api-client";
import type { QuestionResponse } from "@interview/shared/types";

export default function QuestionsPage() {
  const [questions, setQuestions] = useState<QuestionResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGet<QuestionResponse[]>("/questions")
      .then(setQuestions)
      .catch(() => setQuestions([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold">題目管理</h1>
          <p className="text-muted-foreground mt-1">
            {loading ? "載入中..." : `共 ${questions.length} 道題目`}
          </p>
        </div>
      </div>

      {loading ? (
        <div className="text-center py-12 text-muted-foreground">載入中...</div>
      ) : (
        <QuestionListTable questions={questions} />
      )}
    </div>
  );
}
