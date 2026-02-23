// Execution module types — mirrors backend ExecutionResponse

export type ExecutionStatus =
  | "SUCCESS"
  | "COMPILE_ERROR"
  | "RUNTIME_ERROR"
  | "TIMEOUT"
  | "MEMORY_LIMIT_EXCEEDED";

export interface ExecuteCodeRequest {
  language: string;
  code: string;
  stdin?: string;
  timeoutSeconds: number;
}

export interface ExecutionResponse {
  exitCode: number;
  stdout: string;
  stderr: string;
  durationMs: number;
  status: ExecutionStatus;
}
