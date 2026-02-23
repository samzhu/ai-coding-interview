// Interview module types — mirrors backend InterviewResponse and CheckpointResultResponse

export type InterviewStatus =
  | "SCHEDULED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "CANCELLED";

export interface InterviewResponse {
  id: string;
  candidateId: string;
  interviewerId: string;
  title: string;
  status: InterviewStatus;
  questionId: string;
  aiModel: string | null;
  durationMinutes: number;
  scheduledAt: string;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface CheckpointFileState {
  filePath: string;
  content: string;
  editable: boolean;
  originalContent: string;
}

export interface CheckpointResultResponse {
  checkpointId: string;
  sequenceNumber: number;
  title: string;
  description: string;
  starterCode: string | null;
  testCommand: string | null;
  projectFiles: Array<{
    filePath: string;
    content: string;
    editable: boolean;
  }>;
  status: "PENDING" | "IN_PROGRESS" | "PASSED" | "FAILED";
  submittedCode: string | null;
  executionOutput: string | null;
  passedAt: string | null;
  aiEnabled: boolean;
}

export interface CreateInterviewRequest {
  candidateId: string;
  interviewerId: string;
  title: string;
  scheduledAt: string;
  questionId: string;
  aiModel?: string;
}

export interface SubmitCodeRequest {
  code?: string;
  files?: Record<string, string>;
}
