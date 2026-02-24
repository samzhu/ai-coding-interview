"use client";

import {
  createContext,
  useContext,
  useReducer,
  useCallback,
  type ReactNode,
} from "react";
import type {
  CheckpointResultResponse,
  CheckpointFileState,
  InterviewStatus,
  ExecutionResponse,
} from "@interview/shared/types";

interface InterviewState {
  interviewId: string;
  status: InterviewStatus;
  checkpoint: CheckpointResultResponse | null;
  aiEnabled: boolean;
  // Multi-file state
  files: Map<string, CheckpointFileState>;
  activeFilePath: string | null;
  // Legacy single-file (kept for backward compat)
  code: string;
  isSubmitting: boolean;
  isRunning: boolean;
  lastExecution: ExecutionResponse | null;
}

type InterviewAction =
  | { type: "SET_CHECKPOINT"; payload: CheckpointResultResponse }
  | { type: "SET_CODE"; payload: string }
  | { type: "SET_FILE_CONTENT"; payload: { filePath: string; content: string } }
  | { type: "SET_ACTIVE_FILE"; payload: string }
  | { type: "SET_SUBMITTING"; payload: boolean }
  | { type: "SET_RUNNING"; payload: boolean }
  | { type: "SET_EXECUTION"; payload: ExecutionResponse }
  | { type: "SET_STATUS"; payload: InterviewStatus }
  | {
      type: "CHECKPOINT_RESULT";
      payload: { result: CheckpointResultResponse; execution: ExecutionResponse | null };
    };

function buildFileMap(
  checkpoint: CheckpointResultResponse
): Map<string, CheckpointFileState> {
  const map = new Map<string, CheckpointFileState>();
  if (checkpoint.projectFiles && checkpoint.projectFiles.length > 0) {
    for (const f of checkpoint.projectFiles) {
      map.set(f.filePath, {
        filePath: f.filePath,
        content: f.content,
        editable: f.editable,
        originalContent: f.content,
      });
    }
  }
  return map;
}

function getFirstEditableFile(
  files: Map<string, CheckpointFileState>
): string | null {
  for (const [path, file] of files) {
    if (file.editable) return path;
  }
  return files.size > 0 ? (files.keys().next().value as string) : null;
}

function reducer(state: InterviewState, action: InterviewAction): InterviewState {
  switch (action.type) {
    case "SET_CHECKPOINT": {
      const checkpoint = action.payload;
      const files = buildFileMap(checkpoint);
      const activeFilePath = getFirstEditableFile(files);
      const isProjectMode = checkpoint.projectFiles && checkpoint.projectFiles.length > 0;
      return {
        ...state,
        checkpoint,
        aiEnabled: checkpoint.aiEnabled ?? true,
        files,
        activeFilePath,
        code: isProjectMode
          ? (activeFilePath ? (files.get(activeFilePath)?.content ?? "") : "")
          : (checkpoint.submittedCode ?? checkpoint.starterCode ?? ""),
        lastExecution: null,
      };
    }
    case "SET_CODE":
      return {
        ...state,
        code: action.payload,
        files: state.activeFilePath
          ? new Map(state.files).set(state.activeFilePath, {
              ...state.files.get(state.activeFilePath)!,
              content: action.payload,
            })
          : state.files,
      };
    case "SET_FILE_CONTENT": {
      const { filePath, content } = action.payload;
      const newFiles = new Map(state.files);
      const existing = newFiles.get(filePath);
      if (existing) {
        newFiles.set(filePath, { ...existing, content });
      }
      return {
        ...state,
        files: newFiles,
        code: filePath === state.activeFilePath ? content : state.code,
      };
    }
    case "SET_ACTIVE_FILE": {
      const file = state.files.get(action.payload);
      return {
        ...state,
        activeFilePath: action.payload,
        code: file?.content ?? state.code,
      };
    }
    case "SET_SUBMITTING":
      return { ...state, isSubmitting: action.payload };
    case "SET_RUNNING":
      return { ...state, isRunning: action.payload };
    case "SET_EXECUTION":
      return { ...state, lastExecution: action.payload };
    case "SET_STATUS":
      return { ...state, status: action.payload };
    case "CHECKPOINT_RESULT": {
      const result = action.payload.result;
      const files = buildFileMap(result);
      const activeFilePath = state.activeFilePath ?? getFirstEditableFile(files);
      return {
        ...state,
        isSubmitting: false,
        isRunning: false,
        checkpoint: result,
        aiEnabled: result.aiEnabled ?? true,
        files,
        activeFilePath,
        lastExecution: action.payload.execution,
      };
    }
    default:
      return state;
  }
}

interface InterviewContextValue {
  state: InterviewState;
  setCode: (code: string) => void;
  setFileContent: (filePath: string, content: string) => void;
  setActiveFile: (filePath: string) => void;
  setCheckpoint: (checkpoint: CheckpointResultResponse) => void;
  setSubmitting: (v: boolean) => void;
  setRunning: (v: boolean) => void;
  setExecution: (result: ExecutionResponse) => void;
  setStatus: (status: InterviewStatus) => void;
  applyCheckpointResult: (
    result: CheckpointResultResponse,
    execution: ExecutionResponse | null
  ) => void;
  getEditableFiles: () => Record<string, string>;
}

const InterviewContext = createContext<InterviewContextValue | null>(null);

interface InterviewProviderProps {
  interviewId: string;
  initialStatus: InterviewStatus;
  initialCheckpoint: CheckpointResultResponse | null;
  children: ReactNode;
}

export function InterviewProvider({
  interviewId,
  initialStatus,
  initialCheckpoint,
  children,
}: InterviewProviderProps) {
  const initialFiles = initialCheckpoint ? buildFileMap(initialCheckpoint) : new Map();
  const initialActiveFile = initialCheckpoint
    ? getFirstEditableFile(initialFiles)
    : null;
  const isProjectMode =
    initialCheckpoint?.projectFiles && initialCheckpoint.projectFiles.length > 0;

  const [state, dispatch] = useReducer(reducer, {
    interviewId,
    status: initialStatus,
    checkpoint: initialCheckpoint,
    aiEnabled: initialCheckpoint?.aiEnabled ?? true,
    files: initialFiles,
    activeFilePath: initialActiveFile,
    code: isProjectMode
      ? (initialActiveFile ? (initialFiles.get(initialActiveFile)?.content ?? "") : "")
      : (initialCheckpoint?.submittedCode ?? initialCheckpoint?.starterCode ?? ""),
    isSubmitting: false,
    isRunning: false,
    lastExecution: null,
  });

  const setCode = useCallback((code: string) => {
    dispatch({ type: "SET_CODE", payload: code });
  }, []);

  const setFileContent = useCallback((filePath: string, content: string) => {
    dispatch({ type: "SET_FILE_CONTENT", payload: { filePath, content } });
  }, []);

  const setActiveFile = useCallback((filePath: string) => {
    dispatch({ type: "SET_ACTIVE_FILE", payload: filePath });
  }, []);

  const setCheckpoint = useCallback((checkpoint: CheckpointResultResponse) => {
    dispatch({ type: "SET_CHECKPOINT", payload: checkpoint });
  }, []);

  const setSubmitting = useCallback((v: boolean) => {
    dispatch({ type: "SET_SUBMITTING", payload: v });
  }, []);

  const setRunning = useCallback((v: boolean) => {
    dispatch({ type: "SET_RUNNING", payload: v });
  }, []);

  const setExecution = useCallback((result: ExecutionResponse) => {
    dispatch({ type: "SET_EXECUTION", payload: result });
  }, []);

  const setStatus = useCallback((status: InterviewStatus) => {
    dispatch({ type: "SET_STATUS", payload: status });
  }, []);

  const applyCheckpointResult = useCallback(
    (result: CheckpointResultResponse, execution: ExecutionResponse | null) => {
      dispatch({ type: "CHECKPOINT_RESULT", payload: { result, execution } });
    },
    []
  );

  const getEditableFiles = useCallback((): Record<string, string> => {
    const result: Record<string, string> = {};
    for (const [path, file] of state.files) {
      if (file.editable) {
        result[path] = file.content;
      }
    }
    return result;
  }, [state.files]);

  return (
    <InterviewContext.Provider
      value={{
        state,
        setCode,
        setFileContent,
        setActiveFile,
        setCheckpoint,
        setSubmitting,
        setRunning,
        setExecution,
        setStatus,
        applyCheckpointResult,
        getEditableFiles,
      }}
    >
      {children}
    </InterviewContext.Provider>
  );
}

export function useInterview() {
  const ctx = useContext(InterviewContext);
  if (!ctx) throw new Error("useInterview must be used within InterviewProvider");
  return ctx;
}
