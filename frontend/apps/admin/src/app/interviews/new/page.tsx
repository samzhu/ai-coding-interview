"use client";

import { useEffect, useState } from "react";
import { CreateInterviewForm } from "@/components/admin/create-interview-form";
import { apiGet } from "@interview/shared/lib/api-client";
import type { QuestionResponse } from "@interview/shared/types";

export default function NewInterviewPage() {
  const [questions, setQuestions] = useState<QuestionResponse[]>([]);

  useEffect(() => {
    apiGet<QuestionResponse[]>("/questions")
      .then(setQuestions)
      .catch(() => setQuestions([]));
  }, []);

  return (
    <div className="min-h-screen bg-background p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">建立新面試</h1>
        <CreateInterviewForm questions={questions} />
      </div>
    </div>
  );
}
