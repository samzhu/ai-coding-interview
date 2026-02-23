// Question module types — mirrors backend QuestionResponse

export interface ProjectFileResponse {
  id: string;
  filePath: string;
  content: string;
  editable: boolean;
  sortOrder: number;
}

export interface TestCaseResponse {
  id: string;
  input: string;
  expectedOutput: string;
}

export interface CheckpointResponse {
  id: string;
  sequenceNumber: number;
  title: string;
  description: string;
  starterCode: string | null;
  testCommand: string | null;
  testCases: TestCaseResponse[];
}

export interface QuestionResponse {
  id: string;
  title: string;
  description: string;
  difficulty: string;
  language: string;
  type: string | null;
  level: string | null;
  projectFiles: ProjectFileResponse[];
  checkpoints: CheckpointResponse[];
}
